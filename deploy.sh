#!/usr/bin/env bash
#
# YETI-125 배포 스크립트 — 맥에서 실행한다.
#
#   ./deploy.sh              빌드 → 전송 → 교체 → 검증 (확인 후 진행)
#   ./deploy.sh -y           확인 없이 바로 진행
#   ./deploy.sh --build-only 빌드까지만 (전송·배포 안 함)
#
# 배포 후 검증에 실패하면 백업으로 자동 롤백한다.
# 운영 DB 설정은 mvn -Pprod 프로파일이 넣어주므로 손으로 바꿀 것이 없다.
#
set -euo pipefail

SERVER="root@1.201.123.155"
WEBAPPS="/var/lib/tomcat9/webapps"
WAR="target/yeti-125.war"

ASSUME_YES=0
BUILD_ONLY=0
for arg in "$@"; do
  case "$arg" in
    -y|--yes)     ASSUME_YES=1 ;;
    --build-only) BUILD_ONLY=1 ;;
    *) echo "알 수 없는 옵션: $arg"; exit 2 ;;
  esac
done

cd "$(dirname "$0")"

say() { printf '\n\033[1m▶ %s\033[0m\n' "$1"; }
die() { printf '\n\033[31m✗ %s\033[0m\n' "$1" >&2; exit 1; }

# ─────────────────────────────────────────────────────────────
say "1/4  운영용 빌드"
# -Pprod 가 운영 설정을 넣고, 설정이 비어 있으면 빌드 자체가 실패한다
mvn -q clean package -DskipTests -Pprod

[ -f "$WAR" ] || die "war 가 생성되지 않았습니다: $WAR"

# 벨트 앤 서스펜더 — 빌드 가드를 통과했어도 한 번 더 확인
if ! unzip -p "$WAR" WEB-INF/classes/properties/database.properties | grep -q "localhost:3306"; then
  die "war 안의 db.url 이 운영 주소가 아닙니다. 배포를 중단합니다."
fi
printf '   war: %s (%s)\n' "$WAR" "$(du -h "$WAR" | cut -f1)"
printf '   db.url: 운영 주소 확인됨\n'

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

say "2/4  서버로 전송"
scp "$WAR" "$SERVER:/tmp/yeti-125.war"

# ─────────────────────────────────────────────────────────────
say "3/4  교체 및 검증 (서버)"
# 원격 작업은 한 세션에서 끝낸다 — 실패하면 그 자리에서 롤백한다
REMOTE_OK=1
ssh "$SERVER" 'bash -s' <<'REMOTE' || REMOTE_OK=0
set -uo pipefail

WEBAPPS=/var/lib/tomcat9/webapps
CHECK="http://localhost:8080/schedule/list?start=2020-01-01&end=2030-12-31"

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

wait_ok() {
  local code=""
  for _ in $(seq 1 30); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$CHECK" || true)
    [ "$code" = "200" ] && { echo "$code"; return 0; }
    sleep 2
  done
  echo "$code"
  return 1
}

echo "   새 war 배포 중..."
deploy_war /tmp/yeti-125.war

echo "   DB 연동까지 검증 중 (최대 60초)..."
if CODE=$(wait_ok); then
  echo "   검증 통과 (HTTP $CODE)"
  exit 0
fi

echo "   검증 실패 (HTTP ${CODE:-무응답})"
if [ -n "$BAK" ]; then
  echo "   백업으로 롤백합니다: $BAK"
  deploy_war "$BAK"
  if CODE2=$(wait_ok); then
    echo "   롤백 완료 — 서비스 정상 (HTTP $CODE2)"
  else
    echo "   !! 롤백 후에도 비정상 (HTTP ${CODE2:-무응답}). 수동 확인 필요:"
    echo "      journalctl -u tomcat9 -n 40 --no-pager"
  fi
fi
exit 1
REMOTE

if [ "$REMOTE_OK" != "1" ]; then
  die "배포 실패 — 위 로그를 확인하세요. 롤백이 수행됐다면 서비스는 이전 버전으로 복구된 상태입니다."
fi

# ─────────────────────────────────────────────────────────────
say "4/4  외부에서 최종 확인"
CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://yeti-125.com/schedule/list?start=2020-01-01&end=2030-12-31" || true)
if [ "$CODE" = "200" ]; then
  printf '\n\033[32m✓ 배포 완료 — https://yeti-125.com (HTTP %s)\033[0m\n\n' "$CODE"
else
  die "외부 접근 확인 실패 (HTTP ${CODE:-무응답}) — 서버 상태를 확인하세요."
fi
