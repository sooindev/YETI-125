#!/usr/bin/env bash
#
# YETI-125 배포 스크립트 — 맥에서 실행한다.
#
#   ./scripts/deploy.sh              테스트 → 빌드 → 전송 → 교체 → 검증 (확인 후 진행)
#   ./scripts/deploy.sh -y           확인 없이 바로 진행
#   ./scripts/deploy.sh --build-only 빌드까지만 (전송·배포 안 함)
#   ./scripts/deploy.sh --skip-tests 테스트를 건너뛴다 (급할 때만)
#
# 배포 후 검증에 실패하면 백업으로 자동 롤백한다.
# 운영 DB 설정은 mvn -Pprod 프로파일이 넣어주므로 손으로 바꿀 것이 없다.
#
# 배포 대상 서버는 저장소에 적지 않는다 — 공개 저장소이기 때문이다.
# deploy.env (.gitignore 대상) 또는 환경변수로 넘긴다. deploy.env.example 참고.
#
set -euo pipefail

cd "$(dirname "$0")/.."

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
# DB 연동 · 페이지 렌더 · 관리자 입구 · 오류 화면을 함께 본다.
#
# 방명록(/guestbook · /guestbook/list)은 3주년이 끝나 내려둔 뒤로 302 라 여기 없다.
# 되살리면 두 주소를 다시 넣는다 — 페이지만 보면 테이블이나 권한이 빠져도 200 이라
# 그냥 통과한다. 목록 API 까지 봐야 DB 까지 닿은 것이 증명된다.
# 어느 버전에나 있는 화면. 롤백한 뒤에는 이것만 본다
BASE_CHECKS="
http://localhost:8080/schedule/list?start=2020-01-01&end=2030-12-31
http://localhost:8080/
http://localhost:8080/schedule
http://localhost:8080/info
http://localhost:8080/admin/admin-login
"

# 이번 판에서 새로 생긴 화면.
#
# 롤백 뒤 검증에서는 빼야 한다 — 되돌린 버전에는 없는 주소라 404 가 나고,
# 멀쩡히 복구된 서비스가 "롤백 후에도 비정상" 으로 보고된다(2026-09-15에 그랬다).
# 다음 판을 올릴 때 여기 있는 것을 BASE_CHECKS 로 옮긴다.
NEW_CHECKS="
http://localhost:8080/clips
"

CHECKS="$BASE_CHECKS
$NEW_CHECKS"

# 오류 화면 검증용 주소. 아무 핸들러에도 걸리지 않아야 404 가 난다.
ERROR_URL="http://localhost:8080/__deploy-check-404__"

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

# 200 이어야 하는 주소들. 첫 실패만 알려주고 멈춘다
check_pages() {
  local url code
  for url in $CHECKS; do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$url" || true)
    if [ "$code" != "200" ]; then
      echo "$url → ${code:-무응답} (기대 200)"
      return 0
    fi
  done
}

# 오류 화면도 함께 본다.
#
# 200 페이지만 확인하면 놓치는 회귀가 있다 — 필터의 <dispatcher> 에 ERROR 가 빠지면
# 404·500 페이지만 보안 헤더 없이 나가는데, 정상 페이지는 멀쩡해서 티가 안 난다
# (트러블슈팅의 "404·500 페이지에만 보안 헤더가 붙지 않던 문제").
#
# 상태 코드만 보면 안 된다. 톰캣이 뜨긴 했는데 우리 앱이 아직 안 붙은 순간에도
# 404 는 나온다 — 그때는 톰캣의 기본 404 라 헤더가 없다. 헤더까지 확인해야
# "우리 오류 화면이 우리 필터를 타고 나왔다"가 증명된다.
check_error_page() {
  local head code
  head=$(curl -s -D - -o /dev/null --max-time 3 "$ERROR_URL" || true)
  code=$(printf '%s\n' "$head" | head -1 | awk '{print $2}')

  if [ "$code" != "404" ]; then
    echo "$ERROR_URL → ${code:-무응답} (기대 404)"
  elif ! printf '%s\n' "$head" | grep -qi '^content-security-policy:'; then
    echo "$ERROR_URL → 404 는 맞지만 보안 헤더가 없다 — web.xml 의 <dispatcher>ERROR</dispatcher> 확인"
  fi
}

# 사이트맵을 확인한다 — 다만 <b>배포를 막지 않는다.</b>
#
# 두 번 되돌린 뒤에 내린 결론이다(2026-09-14, 09-15). 사이트맵이 틀린 것은
# 검색 유입의 문제지 서비스 장애가 아니다. 화면도 API 도 멀쩡한 릴리스를
# 사이트맵 하나 때문에 통째로 되돌리는 것은 균형이 맞지 않는다.
#
# 대신 무엇이 나갔는지를 남긴다. 다음 배포에서 원인을 볼 수 있어야 한다 —
# 지금까지는 "무엇이 기대와 달랐다" 만 알고 "무엇이 나왔는지" 를 몰라 추측만 했다.
#
# 첫 호출은 캐시를 데우기만 한다. 사이트맵은 XML 을 통째로 만든 뒤에야 첫 바이트를
# 내보내는데, 톰캣을 막 띄운 직후에는 그 안에서 치지직을 서른 번 넘게 부른다.
# 판정은 찬 캐시가 답하는 두 번째 호출로만 한다.
check_sitemap() {
  local head body

  curl -s -o /dev/null --max-time 60 "http://localhost:8080/sitemap.xml" >/dev/null 2>&1 || true

  head=$(curl -s -D - -o /dev/null --max-time 10 "http://localhost:8080/sitemap.xml" 2>/dev/null || true)
  body=$(curl -s --max-time 10 "http://localhost:8080/sitemap.xml" 2>/dev/null || true)

  local urlset clips_page clips_entry
  printf '%s' "$body" | grep -q "</urlset>" && urlset=yes || urlset=no
  printf '%s' "$body" | grep -q "<loc>https://yeti-125.com/clips</loc>" && clips_page=yes || clips_page=no
  clips_entry=$(printf '%s' "$body" | grep -c "<loc>https://yeti-125.com/clips/" || true)

  if [ "$urlset" = yes ] && [ "$clips_page" = yes ] && [ "$clips_entry" -gt 0 ]; then
    echo "   사이트맵 정상 — 클립 주소 ${clips_entry}개" >&2
    return 0
  fi

  # 여기부터는 전부 경고다. 판정($failed)에 섞이지 않도록 표준오류로만 낸다
  echo "   ! 사이트맵이 기대와 다릅니다 (배포는 계속합니다)" >&2
  echo "     온전히 끝남=$urlset  /clips 고정주소=$clips_page  클립 주소=${clips_entry}개" >&2
  echo "     ── 응답 헤더 ──" >&2
  printf '%s\n' "$head" | sed "s/^/     /" >&2
  echo "     ── 본문 앞부분 ──" >&2
  printf '%s\n' "$body" | head -14 | sed "s/^/     /" >&2
  echo "     Last-Modified 가 있으면 정적 파일이고, 없으면 SitemapController 가 그린 것입니다." >&2
  echo "     앱 로그: grep 치지직 /var/log/tomcat9/yeti-125.log | tail -20" >&2
}

# 홈이 실제로 부르는 css · js 가 전부 200 인가.
#
# 주소를 여기 박아두면 JSP 가 다른 경로를 부르도록 바뀐 순간 검증이 헛돈다 —
# 페이지에서 뽑아내야 "이 배포본이 실제로 부르는 파일" 을 본다.
#
# 페이지만 보면 놓친다 (2026-09-10 디렉터리 세분화): css · js 경로가 통째로
# 바뀌었는데, 자산이 하나도 안 내려와 스타일이 다 빠진 화면도 200 이라 그냥 통과한다.
#
# 홈만 보면 놓치는 것이 또 있다 (2026-09-14 클립 아카이브): /clips 는 홈이 부르지 않는
# clips.css · clips.js 를 부른다. 화면마다 제 자산을 들고 있으므로 화면마다 확인한다.
# 자산을 확인할 화면. 롤백 뒤에는 홈만 본다 (CHECKS 와 같은 이유)
ASSET_PAGES="/ /clips"

check_assets() {
  local path failed
  for path in $ASSET_PAGES; do
    failed="$(check_page_assets "$path")"
    if [ -n "$failed" ]; then
      echo "$failed"
      return 0
    fi
  done
}

# 한 화면이 부르는 자산이 전부 200 인가.
# 이 파일의 검증 함수는 모두 "아무 말도 없으면 통과" 다 — 반환값이 아니라 출력이 신호다
check_page_assets() {
  local path page assets url code
  path="$1"
  page=$(curl -s --max-time 3 "http://localhost:8080$path" || true)
  assets=$(printf '%s' "$page" | grep -oE '/resources/[A-Za-z0-9._/-]+\.(css|js)' | sort -u)

  # 화면이 200 인데 자산 주소가 하나도 없으면 JSP 가 제대로 그려지지 않은 것이다
  if [ -z "$assets" ]; then
    echo "$path 에서 css · js 주소를 하나도 찾지 못했습니다 — 페이지가 제대로 렌더되지 않았을 수 있습니다"
    return 0
  fi

  for url in $assets; do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:8080$url" || true)
    if [ "$code" != "200" ]; then
      echo "http://localhost:8080$url → ${code:-무응답} (기대 200)"
      return 0
    fi
  done
}

# 톰캣이 뜰 때까지 되풀이해 본다. 전부 통과해야 배포를 확정한다.
#
# 사이트맵은 여기 넣지 않는다 — 한 번에 최대 20초라, 30번 도는 이 루프에 넣으면
# "최대 60초" 이던 검증이 몇 분으로 늘어난다. 기동을 기다리는 검사와
# 한 번만 보면 되는 검사는 성격이 다르다.
wait_ok() {
  local failed=""
  for _ in $(seq 1 30); do
    failed="$(check_pages)"
    [ -n "$failed" ] || failed="$(check_error_page)"
    [ -n "$failed" ] || failed="$(check_assets)"
    [ -z "$failed" ] && { echo "정상 페이지 + 오류 화면 + 자산 모두 통과"; return 0; }
    sleep 2
  done
  echo "$failed"
  return 1
}

echo "   새 war 배포 중..."
deploy_war /tmp/yeti-125.war

echo "   DB 연동 · 페이지 · 오류 화면 · css/js · 사이트맵까지 검증 중 (최대 60초)..."
if CODE=$(wait_ok); then
  # 앱이 확실히 선 뒤에 한 번만 본다. 결과는 알리기만 하고 배포를 막지 않는다
  check_sitemap
  echo "   검증 통과 ($CODE)"
  exit 0
fi

echo "   검증 실패: ${CODE:-무응답}"
if [ -n "$BAK" ]; then
  echo "   백업으로 롤백합니다: $BAK"
  deploy_war "$BAK"

  # 되돌린 버전에 없는 화면을 찾지 않는다 — 찾으면 성공한 롤백이 실패로 보고된다
  CHECKS="$BASE_CHECKS"
  ASSET_PAGES="/"

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
