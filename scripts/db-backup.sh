#!/usr/bin/env bash
#
# YETI-125 DB 백업 — 운영 서버(가비아)에서 cron 으로 돌린다.
#
#   설치 위치 : /usr/local/sbin/yeti-db-backup.sh
#   보관 위치 : /var/backups/yeti-125
#   로그      : /var/log/yeti-db-backup.log
#
# 우분투의 MariaDB root 는 unix_socket 인증이라, root 로 실행하면
# 비밀번호 없이 접속된다. 그래서 스크립트에 비밀번호를 적지 않는다.
#
set -euo pipefail

DB="for_125"
DIR="/var/backups/yeti-125"
KEEP_DAYS=14          # 이 개수만큼만 남기고 오래된 것부터 지운다

mkdir -p "$DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$DIR/${DB}-${STAMP}.sql.gz"
TMP="$OUT.part"

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"; }

# --single-transaction : InnoDB 를 잠그지 않고 일관된 시점으로 덤프
# --quick              : 한 행씩 흘려보내 메모리를 아낀다 (2GB 서버)
# --routines/--events  : 프로시저·이벤트까지 포함
mysqldump \
    --single-transaction \
    --quick \
    --default-character-set=utf8mb4 \
    --routines \
    --events \
    "$DB" | gzip -c > "$TMP"

# 압축이 온전한지 확인한 뒤에야 정식 파일명으로 바꾼다.
# 덤프가 중간에 끊겼는데 정상 백업인 척 남는 상황을 막는다.
if ! gzip -t "$TMP"; then
    rm -f "$TMP"
    log "실패: 덤프가 손상되었습니다"
    exit 1
fi

mv "$TMP" "$OUT"
chmod 600 "$OUT"

SIZE="$(du -h "$OUT" | cut -f1)"
log "성공: $(basename "$OUT") ($SIZE)"

# 오래된 백업 정리
DELETED="$(ls -1t "$DIR/${DB}"-*.sql.gz 2>/dev/null | tail -n +$((KEEP_DAYS + 1)) || true)"
if [ -n "$DELETED" ]; then
    echo "$DELETED" | xargs -r rm -f
    log "정리: $(echo "$DELETED" | wc -l | tr -d ' ')개 삭제"
fi
