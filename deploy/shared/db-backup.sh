#!/bin/bash

DATE=$(date +%Y-%m-%d)
BACKUP_DIR="/home/didit-prod/backups"
BACKUP_FILE="$BACKUP_DIR/didit-$DATE.sql.gz"
BUCKET="didit-db-backup"
RETENTION_DAYS=30

if [ -f "/home/didit-prod/didit/deploy/prod/.env" ]; then
    export DB_USER=$(grep '^DB_USER=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
    export DB_PASSWORD=$(grep '^DB_PASSWORD=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
    export DISCORD_MONITOR_WEBHOOK_URL=$(grep '^DISCORD_MONITOR_WEBHOOK_URL=' /home/didit-prod/didit/deploy/prod/.env | cut -d= -f2-)
fi

mkdir -p $BACKUP_DIR

# mysqldump 실행 + 압축
docker exec didit-prod-db mysqldump \
  -u$DB_USER \
  -p$DB_PASSWORD \
  didit | gzip > $BACKUP_FILE

if [ $? -ne 0 ]; then
  echo "[ERROR] DB 백업 실패 - $DATE"
  curl -s -H "Content-Type: application/json" \
    -X POST \
    -d "{\"username\": \"Didit Bot\", \"content\": \"DB 백업 실패 - $DATE\"}" \
    "$DISCORD_MONITOR_WEBHOOK_URL" > /dev/null
  exit 1
fi

echo "[INFO] DB 백업 완료 - $BACKUP_FILE"

s3cmd put $BACKUP_FILE s3://$BUCKET/didit-$DATE.sql.gz

if [ $? -ne 0 ]; then
  echo "[ERROR] Object Storage 업로드 실패 - $DATE"
  curl -s -H "Content-Type: application/json" \
    -X POST \
    -d "{\"username\": \"Didit Bot\", \"content\": \"DB 백업 업로드 실패 - $DATE\"}" \
    "$DISCORD_MONITOR_WEBHOOK_URL" > /dev/null
  exit 1
fi

echo "[INFO] Object Storage 업로드 완료"

find $BACKUP_DIR -name "didit-*.sql.gz" -mtime +$RETENTION_DAYS -delete

s3cmd ls s3://$BUCKET/ | awk '{print $4}' | while read file; do
    file_date=$(echo $file | grep -oP '\d{4}-\d{2}-\d{2}')
    if [ ! -z "$file_date" ]; then
        days_old=$(( ( $(date +%s) - $(date -d "$file_date" +%s) ) / 86400 ))
        if [ $days_old -gt $RETENTION_DAYS ]; then
            s3cmd del $file
            echo "[INFO] 오래된 백업 삭제 - $file"
        fi
    fi
done

echo "[INFO] DB 백업 프로세스 완료 - $DATE"