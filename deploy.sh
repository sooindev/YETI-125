#!/usr/bin/env bash
#
# YETI-125 배포 스크립트 — 맥에서 실행한다.
#
#   ./deploy.sh              테스트 → 빌드 → 전송 → 교체 → 검증 (확인 후 진행)
#   ./deploy.sh -y           확인 없이 바로 진행
#   ./deploy.sh --build-only 빌드까지만 (전송·배포 안 함)
#   ./deploy.sh --skip-tests 테스트를 건너뛴다 (급할 때만)
#
# 배포 후 검증에 실패하면 백업으로 자동 롤백한다.
# 운영 DB 설정은 mvn -Pprod 프로파일이 넣어주므로 손으로 바꿀 것이 없다.
#
# 배포 대상 서버는 저장소에 적지 않는다 — 공개 저장소이기 때문이다.
# deploy.env (.gitignore 대상) 또는 환경변수로 넘긴다. deploy.env.example 참고.
#
set -euo pipefail

cd "$(dirname "$0")"

# 저장소에 없는 파일이다. 없으면 환경변수로 넘겼는지 아래에서 확인한다.
# shellcheck source=/dev/null
[ -f deploy.env ] && . ./deploy.env

SERVER="${YETI_DEPLOY_SERVER:-}"
WAR="target/yeti-125.war"

ASSUME_YES=0
BUILD_ONLY=0
SKIP_TESTS=0
for arg in "$@"; do
  case "$arg" in
    -y|--yes)     ASSUME_YES=1 ;;
    --build-only) BUILD_ONLY=1 ;;
    --skip-tests) SKIP_TESTS=1 ;;
    *) echo "알 수 없는 옵션: $arg"; exit 2 ;;
  esac
done

say() { printf '\n\033[1m▶ %s\033[0m\n' "$1"; }
die() { printf '\n\033[31m✗ %s\033[0m\n' "$1" >&2; exit 1; }

[ -n "$SERVER" ] || die "배포 대상 서버가 지정되지 않았습니다.
    deploy.env 를 만들어 YETI_DEPLOY_SERVER=사용자@주소 를 적으세요.
    deploy.env.example 을 복사해 쓰면 됩니다:  cp deploy.env.example deploy.env"

# ─────────────────────────────────────────────────────────────
# 테스트를 건너뛰면 깨진 코드가 그대로 운영에 올라간다.
# 예전에는 늘 -DskipTests 였다 — 손으로 mvn test 를 칠 때만 확인이 됐다.
if [ "$SKIP_TESTS" = "1" ]; then
  say "1/5  운영용 빌드 (테스트 건너뜀)"
  mvn -q clean package -DskipTests -Pprod
else
  say "1/5  운영용 빌드 (테스트 포함)"
  # -Pprod 가 운영 설정을 넣고, 설정이 비어 있으면 빌드 자체가 실패한다
  mvn -q clean package -Pprod || die "테스트 또는 빌드가 실패했습니다. 배포를 중단합니다."
fi

[ -f "$WAR" ] || die "war 가 생성되지 않았습니다: $WAR"

# war 안의 DB 설정이 운영용인지 대조한다.
#
# 예전에는 db.url 에 localhost 가 있는지만 봤는데, 운영도 톰캣과 DB 가 같은
# 서버라 로컬 war 와 값이 똑같아 구분이 되지 않았다. 값은 비교만 하고
# 출력하지 않는다.
PROD_CONF="src/main/resources-prod/properties/database.properties"
[ -f "$PROD_CONF" ] || die "운영 DB 설정이 없습니다: $PROD_CONF"

WANT=$(shasum "$PROD_CONF" | cut -d' ' -f1)
HAVE=$(unzip -p "$WAR" WEB-INF/classes/properties/database.properties 2>/dev/null | shasum | cut -d' ' -f1)
[ -n "$HAVE" ] || die "war 안에 DB 설정이 없습니다: $WAR"
if [ "$WANT" != "$HAVE" ]; then
  die "war 의 DB 설정이 운영 설정과 다릅니다. -Pprod 로 빌드됐는지 확인하세요."
fi
printf '   war: %s (%s)\n' "$WAR" "$(du -h "$WAR" | cut -f1)"
printf '   DB 설정: 운영용 확인됨\n'

if [ "$BUILD_ONLY" = "1" ]; then
  say "빌드까지만 수행했습니다 (--build-only)"
  exit 0
fi

# ─────────────────────────────────────────────────────────────
if [ "$ASSUME_YES" != "1" ]; then
  printf '\n운영 서버(%s)에 배포합니다. 계속할까요? [y/N] ' "$SERVER"
  read -r reply
  case "$reply" in [yY]*) ;; *) die "취소했습니다." ;; esac
fi

say "2/5  서버 자바 확인"
# 톰캣이 JSP 를 실행 중에 컴파일할 때 쓰는 ECJ 가 자바 11 이상을 요구한다.
# 자바 8 서버에 올리면 모든 페이지가 500 이 된다 (2026-08-24 장애).
REMOTE_JAVA=$(ssh "$SERVER" '
  . /etc/default/tomcat9 2>/dev/null || true
  "${JAVA_HOME:-/usr}/bin/java" -version 2>&1 | head -1
') || die "서버에 접속할 수 없습니다"
printf '   %s\n' "$REMOTE_JAVA"

# "1.8.0_402" 는 8, "17.0.20" 은 17 로 읽는다
REMOTE_MAJOR=$(printf '%s' "$REMOTE_JAVA" | sed -n 's/.*version "\([0-9][0-9.]*\).*/\1/p')
case "$REMOTE_MAJOR" in
  1.*) REMOTE_MAJOR=${REMOTE_MAJOR#1.}; REMOTE_MAJOR=${REMOTE_MAJOR%%.*} ;;
  *)   REMOTE_MAJOR=${REMOTE_MAJOR%%.*} ;;
esac
if [ -z "$REMOTE_MAJOR" ]; then
  die "서버 자바 버전을 읽지 못했습니다: $REMOTE_JAVA"
fi
if [ "$REMOTE_MAJOR" -lt 11 ]; then
  die "서버 자바가 $REMOTE_MAJOR 입니다. JSP 컴파일에 11 이상이 필요합니다 (README 참고)"
fi
printf '   자바 %s — JSP 컴파일 가능\n' "$REMOTE_MAJOR"

say "3/5  서버로 전송"
scp "$WAR" "$SERVER:/tmp/yeti-125.war"

# ─────────────────────────────────────────────────────────────
say "4/5  교체 및 검증 (서버)"
# 원격 작업은 한 세션에서 끝낸다 — 실패하면 그 자리에서 롤백한다
REMOTE_OK=1
ssh "$SERVER" 'bash -s' <<'REMOTE' || REMOTE_OK=0
set -uo pipefail

WEBAPPS=/var/lib/tomcat9/webapps

# API 하나만 보면 JSP 가 전부 깨져도 통과한다 (2026-08-24 장애).
# DB 연동 · 페이지 렌더 · 관리자 입구를 함께 본다.
CHECKS="
http://localhost:8080/schedule/list?start=2020-01-01&end=2030-12-31
http://localhost:8080/
http://localhost:8080/schedule
http://localhost:8080/info
http://localhost:8080/admin/admin-login
"

[ -f /tmp/yeti-125.war ] || { echo "전송된 war 가 없습니다"; exit 1; }

BAK=""
if [ -f "$WEBAPPS/ROOT.war" ]; then
  BAK="/root/ROOT.war.$(date +%Y%m%d-%H%M%S).bak"
  cp "$WEBAPPS/ROOT.war" "$BAK"
  echo "   백업: $BAK"
else
  echo "   기존 ROOT.war 없음 — 백업 생략"
fi

deploy_war() {
  systemctl stop tomcat9
  rm -rf "$WEBAPPS/ROOT"
  cp "$1" "$WEBAPPS/ROOT.war"
  chown tomcat:tomcat "$WEBAPPS/ROOT.war"
  systemctl start tomcat9
}

# 모든 주소가 200 이어야 통과. 하나라도 아니면 그 주소를 알려준다
wait_ok() {
  local url code last=""
  for _ in $(seq 1 30); do
    last=""
    for url in $CHECKS; do
      code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$url" || true)
      [ "$code" = "200" ] || { last="$url → ${code:-무응답}"; break; }
    done
    [ -z "$last" ] && { echo "전체 200"; return 0; }
    sleep 2
  done
  echo "$last"
  return 1
}

echo "   새 war 배포 중..."
deploy_war /tmp/yeti-125.war

echo "   DB 연동까지 검증 중 (최대 60초)..."
if CODE=$(wait_ok); then
  echo "   검증 통과 ($CODE)"
  exit 0
fi

echo "   검증 실패: ${CODE:-무응답}"
if [ -n "$BAK" ]; then
  echo "   백업으로 롤백합니다: $BAK"
  deploy_war "$BAK"
  if CODE2=$(wait_ok); then
    echo "   롤백 완료 — 서비스 정상 ($CODE2)"
  else
    echo "   !! 롤백 후에도 비정상: ${CODE2:-무응답}. 수동 확인 필요:"
    echo "      journalctl -u tomcat9 -n 40 --no-pager"
  fi
fi
exit 1
REMOTE

if [ "$REMOTE_OK" != "1" ]; then
  die "배포 실패 — 위 로그를 확인하세요. 롤백이 수행됐다면 서비스는 이전 버전으로 복구된 상태입니다."
fi

# ─────────────────────────────────────────────────────────────
say "5/5  외부에서 최종 확인"
CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://yeti-125.com/schedule/list?start=2020-01-01&end=2030-12-31" || true)
if [ "$CODE" = "200" ]; then
  printf '\n\033[32m✓ 배포 완료 — https://yeti-125.com (HTTP %s)\033[0m\n\n' "$CODE"
else
  die "외부 접근 확인 실패 (HTTP ${CODE:-무응답}) — 서버 상태를 확인하세요."
fi
