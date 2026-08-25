#!/usr/bin/env bash
#
# YETI-125 로컬 실행 스크립트 — 맥에서 실행한다.
#
#   ./run-local.sh              빌드 → 로컬 톰캣에 ROOT 로 배포 → 기동 → 확인
#   ./run-local.sh --skip-build 빌드 없이 기존 war 로 재배포
#   ./run-local.sh --stop       로컬 톰캣 정지
#   ./run-local.sh --logs       catalina.out 실시간 보기
#
# 운영 배포는 이 스크립트가 아니라 ./deploy.sh 를 쓴다.
#
set -euo pipefail

BREW_PREFIX="$(brew --prefix)"
JAVA_HOME="${JAVA_HOME:-$BREW_PREFIX/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
CATALINA_HOME="$BREW_PREFIX/opt/tomcat@9/libexec"
WAR="target/yeti-125.war"
URL="http://localhost:8080"

# 로컬 DB 접속 정보. 비밀번호는 여기 두지 않는다.
#   ~/.my.cnf 의 [client] password="..." (큰따옴표 필수 — # 는 주석이라 거기서 잘린다.
#   user= 는 넣지 말 것, 모든 mariadb 클라이언트에 적용된다)
#   또는 YETI_DB_PASSWORD=... ./run-local.sh
DB_NAME="${YETI_DB_NAME:-for_125}"
DB_USER="${YETI_DB_USER:-yeti}"
CHECK="$URL/schedule/list?start=2020-01-01&end=2030-12-31"
export JAVA_HOME PATH="$BREW_PREFIX/opt/mariadb/bin:$PATH"

SKIP_BUILD=0
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=1 ;;
    --stop)       "$CATALINA_HOME/bin/catalina.sh" stop 2>/dev/null || true; echo "톰캣을 정지했습니다."; exit 0 ;;
    --logs)       exec tail -f "$CATALINA_HOME/logs/catalina.out" ;;
    *) echo "알 수 없는 옵션: $arg"; exit 2 ;;
  esac
done

cd "$(dirname "$0")"

say() { printf '\n\033[1m▶ %s\033[0m\n' "$1"; }
die() { printf '\n\033[31m✗ %s\033[0m\n' "$1" >&2; exit 1; }

# 비밀번호를 명령줄에 싣지 않는다 — ps 에 그대로 보인다
db_query() {
  if [ -n "${YETI_DB_PASSWORD:-}" ]; then
    MYSQL_PWD="$YETI_DB_PASSWORD" mariadb -u "$DB_USER" -h 127.0.0.1 "$DB_NAME" -e "$1"
  else
    mariadb -u "$DB_USER" -h 127.0.0.1 "$DB_NAME" -e "$1"
  fi
}

[ -d "$JAVA_HOME" ]      || die "JDK 17 이 없습니다: brew install openjdk@17"
[ -d "$CATALINA_HOME" ]  || die "Tomcat 9 가 없습니다: brew install tomcat@9"
[ -f src/main/resources/properties/database.properties ] \
  || die "src/main/resources/properties/database.properties 가 없습니다 (.gitignore 대상이라 클론 후 직접 만들어야 합니다)"

# ─────────────────────────────────────────────────────────────
say "1/4  MariaDB 확인"
if ! brew services list | grep -qE '^mariadb\s+started'; then
  echo "   MariaDB 가 꺼져 있어 시작합니다..."
  brew services start mariadb >/dev/null
fi
for _ in $(seq 1 20); do
  db_query "SELECT 1" >/dev/null 2>&1 && break
  sleep 1
done
db_query "SELECT 1" >/dev/null 2>&1 \
  || die "$DB_NAME DB 에 접속하지 못했습니다.
    비밀번호를 ~/.my.cnf 의 [client] 섹션에 두거나
    YETI_DB_PASSWORD 환경변수로 넘기세요. (계정: $DB_USER)"
echo "   $DB_NAME 접속 확인"

# ─────────────────────────────────────────────────────────────
# war 안의 DB 설정이 로컬 것과 같은지 본다.
#
# deploy.sh 는 -Pprod 로 빌드하므로 배포 뒤 target/ 에는 운영용 war 가 남는다.
# --skip-build 로 그걸 로컬에 올리면 운영 계정으로 로컬 DB 에 붙어 500 이 난다
# (2026-08-25). 값은 비교만 하고 출력하지 않는다.
assert_local_war() {
  local want have
  want=$(shasum "src/main/resources/properties/database.properties" | cut -d' ' -f1)
  have=$(unzip -p "$WAR" WEB-INF/classes/properties/database.properties 2>/dev/null | shasum | cut -d' ' -f1)

  [ -n "$have" ] || die "war 안에 DB 설정이 없습니다: $WAR"
  [ "$want" = "$have" ] || die "war 의 DB 설정이 로컬 설정과 다릅니다 (운영용이거나 낡은 war). --skip-build 없이 다시 빌드하세요."
}

if [ "$SKIP_BUILD" = "1" ]; then
  say "2/4  빌드 건너뜀 (--skip-build)"
  [ -f "$WAR" ] || die "war 가 없습니다: $WAR"
  assert_local_war
else
  say "2/4  빌드 (로컬 프로파일)"
  mvn -q clean package -DskipTests
  [ -f "$WAR" ] || die "war 가 생성되지 않았습니다: $WAR"
  assert_local_war
fi
printf '   war: %s (%s)\n' "$WAR" "$(du -h "$WAR" | cut -f1)"

# /resources/... 절대경로를 쓰므로 반드시 ROOT 컨텍스트로 배포한다
say "3/4  로컬 톰캣에 ROOT 로 배포"
"$CATALINA_HOME/bin/catalina.sh" stop 2>/dev/null || true
sleep 2
rm -rf "$CATALINA_HOME/webapps/ROOT" "$CATALINA_HOME/webapps/ROOT.war"
cp "$WAR" "$CATALINA_HOME/webapps/ROOT.war"
"$CATALINA_HOME/bin/catalina.sh" start >/dev/null

# ─────────────────────────────────────────────────────────────
say "4/4  DB 연동까지 확인 (최대 60초)"
CODE=""
for _ in $(seq 1 30); do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$CHECK" || true)
  [ "$CODE" = "200" ] && break
  sleep 2
done

if [ "$CODE" = "200" ]; then
  printf '\n\033[32m✓ 실행 중\033[0m\n'
  printf '   사이트   : %s\n' "$URL"
  printf '   관리자   : %s/admin/admin-login\n' "$URL"
  printf '   로그     : ./run-local.sh --logs\n'
  printf '   정지     : ./run-local.sh --stop\n\n'
else
  die "기동 확인 실패 (HTTP ${CODE:-무응답}) — 로그를 확인하세요: ./run-local.sh --logs"
fi
