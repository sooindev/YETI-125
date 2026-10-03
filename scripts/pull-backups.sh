#!/usr/bin/env bash
#
# YETI-125 DB 백업 외부 보관 — 맥에서 실행한다.
#
#   ./scripts/pull-backups.sh            운영 서버의 백업을 맥으로 가져온다
#
# 서버의 db-backup.sh 는 같은 서버 디스크에만 쌓는다. 서버가 망가지면 백업도
# 함께 사라지므로, 서버 밖에 한 벌을 더 둔다.
#
#   받는 곳 : ~/Backups/yeti-125   (YETI_BACKUP_DIR 로 바꿀 수 있다)
#   보관    : 최근 60개 — 서버(14개)보다 길게 둔다
#
# 대상 서버는 deploy.sh 와 같은 deploy.env 의 YETI_DEPLOY_SERVER 를 쓴다.
#
set -euo pipefail

cd "$(dirname "$0")/.."

# shellcheck source=/dev/null
[ -f deploy.env ] && . ./deploy.env

SERVER="${YETI_DEPLOY_SERVER:-}"
REMOTE_DIR="/var/backups/yeti-125"
LOCAL_DIR="${YETI_BACKUP_DIR:-$HOME/Backups/yeti-125}"
KEEP=60

die() { printf '\n\033[31m✗ %s\033[0m\n' "$1" >&2; exit 1; }

[ -n "$SERVER" ] || die "배포 대상 서버가 지정되지 않았습니다. deploy.env 를 확인하세요."

mkdir -p "$LOCAL_DIR"
chmod 700 "$LOCAL_DIR"

# 완성된 덤프만 — .part 는 아직 쓰는 중이거나 실패한 것
# --ignore-existing : 덤프는 만든 뒤 바뀌지 않는다. 받은 것은 다시 받지 않는다
rsync -a --ignore-existing --include='*.sql.gz' --exclude='*' \
    "$SERVER:$REMOTE_DIR/" "$LOCAL_DIR/" \
    || die "백업을 가져오지 못했습니다 — 서버 접속과 $REMOTE_DIR 를 확인하세요."

# 받는 도중 끊겼으면 깨진 파일이 남는다. 서버처럼 압축 검사로 거른다
broken=0
for f in "$LOCAL_DIR"/*.sql.gz; do
  [ -e "$f" ] || continue
  if ! gzip -t "$f" 2>/dev/null; then
    rm -f "$f"
    broken=$((broken + 1))
  fi
done
[ "$broken" -eq 0 ] || printf '   ! 손상된 파일 %d개를 지웠습니다 — 다음 실행에서 다시 받습니다\n' "$broken"

# 아래 ls 들은 파일이 없으면 실패해 pipefail 에 조용히 끝난다 — 먼저 확인
compgen -G "$LOCAL_DIR/*.sql.gz" >/dev/null \
    || die "가져온 백업이 하나도 없습니다 — 서버에서 백업 cron 이 도는지 확인하세요."

chmod 600 "$LOCAL_DIR"/*.sql.gz

# 오래된 것부터 정리
ls -1t "$LOCAL_DIR"/*.sql.gz | tail -n +$((KEEP + 1)) | while read -r old; do
  rm -f "$old"
done

COUNT=$(ls -1 "$LOCAL_DIR"/*.sql.gz | wc -l | tr -d ' ')
LATEST=$(ls -1t "$LOCAL_DIR"/*.sql.gz | head -1)

printf '\033[32m✓ 외부 보관 완료 — %s 에 %s개 (최신: %s)\033[0m\n' \
    "$LOCAL_DIR" "$COUNT" "$(basename "$LATEST")"
