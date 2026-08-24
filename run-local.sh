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

# 로컬 DB 접속 정보.
#
# 비밀번호는 이 파일에 두지 않는다. 저장소에 올라가는 스크립트이고,
# database.properties 를 .gitignore 해 둔 설계와도 어긋난다.
#
#   권장) ~/.my.cnf 에 적어두면 아무것도 설정할 필요가 없다
#           [client]
#           password="비밀번호"
#         chmod 600 ~/.my.cnf
#
#         값은 반드시 큰따옴표로 감쌀 것. my.cnf 에서 # 는 주석 시작
#         문자라, 비밀번호에 # 가 들어 있으면 거기서 잘린 채로 읽혀
#         Access denied 가 난다. 원인을 짐작하기 어려운 실패다.
#
#         user= 는 넣지 않는다. [client] 는 모든 mariadb 클라이언트에
#         적용되므로, 소켓 인증으로 쓰는 mariadb -u $(whoami) 까지
#         yeti 로 끌려간다.
#
#   또는) 환경변수로 넘긴다
#           YETI_DB_PASSWORD=... ./run-local.sh
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

# 비밀번호를 명령줄에 싣지 않는다 (ps 에 그대로 보인다).
# YETI_DB_PASSWORD 가 있으면 MYSQL_PWD 로 넘기고, 없으면
# mariadb 클라이언트가 ~/.my.cnf 를 읽게 둔다.
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
if [ "$SKIP_BUILD" = "1" ]; then
  say "2/4  빌드 건너뜀 (--skip-build)"
  [ -f "$WAR" ] || die "war 가 없습니다: $WAR"
else
  say "2/4  빌드 (로컬 프로파일)"
  mvn -q clean package -DskipTests
  [ -f "$WAR" ] || die "war 가 생성되지 않았습니다: $WAR"
fi
printf '   war: %s (%s)\n' "$WAR" "$(du -h "$WAR" | cut -f1)"

# ─────────────────────────────────────────────────────────────
# HTML 이 /resources/... 절대경로를 쓰므로 반드시 ROOT 컨텍스트로 배포한다
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
