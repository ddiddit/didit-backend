#!/bin/bash
set -o pipefail

DATE=$(date +%Y-%m-%d)
BACKUP_DIR="/home/didit-prod/backups"
BACKUP_FILE="$BACKUP_DIR/didit-$DATE.sql.gz"
BUCKET="didit-prod-db-backup-493168377333-ap-northeast-2-an"
RETENTION_DAYS=30

if [ -f "/home/didit-prod/didit/deploy/prod/.env" ]; then
    export DB_USER=$(grep '^DB_USER=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
    export DB_PASSWORD=$(grep '^DB_PASSWORD=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
    export DISCORD_DB_WEBHOOK_URL=$(grep '^DISCORD_DB_WEBHOOK_URL=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
fi

mkdir -p $BACKUP_DIR

docker exec didit-prod-db mysqldump --single-transaction --no-tablespaces -u$DB_USER -p$DB_PASSWORD didit | gzip > $BACKUP_FILE

if [ $? -ne 0 ]; then
  echo "[ERROR] DB 백업 실패 - $DATE"
  curl -s -H "Content-Type: application/json" -X POST \
    -d "{\"username\": \"Didit Bot\", \"content\": \"@here DB 백업 실패 - $DATE\"}" \
    "$DISCORD_DB_WEBHOOK_URL" > /dev/null
  exit 1
fi

echo "[INFO] DB 백업 완료 - $BACKUP_FILE"

aws s3 cp "$BACKUP_FILE" "s3://$BUCKET/didit-$DATE.sql.gz"

if [ $? -ne 0 ]; then
  echo "[ERROR] S3 업로드 실패 - $DATE"
  curl -s -H "Content-Type: application/json" -X POST \
    -d "{\"username\": \"Didit Bot\", \"content\": \"@here DB 백업 업로드 실패 - $DATE\"}" \
    "$DISCORD_DB_WEBHOOK_URL" > /dev/null
  exit 1
fi

echo "[INFO] S3 업로드 완료"

# 로컬 보존 (S3는 라이프사이클 규칙으로 자동 삭제)
find $BACKUP_DIR -name "didit-*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "[INFO] DB 백업 프로세스 완료 - $DATE"

curl -s -H "Content-Type: application/json" -X POST \
  -d "{\"username\": \"Didit Bot\", \"embeds\": [{\"title\": \"DB 백업 완료\", \"color\": 3066993, \"fields\": [{\"name\": \"날짜\", \"value\": \"$DATE\", \"inline\": true}, {\"name\": \"파일\", \"value\": \"didit-$DATE.sql.gz\", \"inline\": true}]}]}" \
  "$DISCORD_DB_WEBHOOK_URL" > /dev/null
