#!/bin/bash
set -e

ENV=${1:-prod}
WORK_DIR="/home/didit-prod/didit"
DEPLOY_DIR="$WORK_DIR/deploy/$ENV"
STATUS_FILE="$DEPLOY_DIR/.system-status"
MAX_HEALTH_FAILURES=3

if [ -f "$DEPLOY_DIR/.env" ]; then
    export DISCORD_WEBHOOK_URL=$(grep '^DISCORD_WEBHOOK_URL=' "$DEPLOY_DIR/.env" | cut -d= -f2-)
    export DISCORD_MONITOR_WEBHOOK_URL=$(grep '^DISCORD_MONITOR_WEBHOOK_URL=' "$DEPLOY_DIR/.env" | cut -d= -f2-)
fi

if [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
    source "$WORK_DIR/deploy/shared/discord-notify.sh"
fi

if [ -f "$STATUS_FILE" ]; then
    source "$STATUS_FILE"
else
    HEALTH_STATUS="up"
    HEALTH_FAILURE_COUNT=0
    HEALTH_DOWN_SINCE=""
    CONTAINER_WARNING_SENT=false
fi

NGINX_UPSTREAM_FILE="/etc/nginx/conf.d/upstream-didit.conf"
if [ -f "$NGINX_UPSTREAM_FILE" ]; then
    ACTIVE_PORT=$(grep -oE '127\.0\.0\.1:[0-9]+' "$NGINX_UPSTREAM_FILE" | cut -d: -f2 | head -n 1 || true)
else
    ACTIVE_PORT=""
fi

case "$ACTIVE_PORT" in
    8081)
        HEALTH_CHECK_URL="http://localhost:8081/actuator/health"
        CONTAINER_NAME="didit-api-blue"
        ;;
    8082)
        HEALTH_CHECK_URL="http://localhost:8082/actuator/health"
        CONTAINER_NAME="didit-api-green"
        ;;
    *)
        echo "[WARN] nginx upstream에서 active 포트를 읽지 못했습니다. blue(8081)로 fallback합니다."
        HEALTH_CHECK_URL="http://localhost:8081/actuator/health"
        CONTAINER_NAME="didit-api-blue"
        ;;
esac

if curl -f -s --max-time 5 $HEALTH_CHECK_URL > /dev/null 2>&1; then
    if [ "$HEALTH_STATUS" = "down" ]; then
        if [ ! -z "$HEALTH_DOWN_SINCE" ]; then
            DOWN_TIMESTAMP=$(date -d "$HEALTH_DOWN_SINCE" +%s)
            UP_TIMESTAMP=$(date +%s)
            DOWN_DURATION=$(( UP_TIMESTAMP - DOWN_TIMESTAMP ))
            DOWNTIME_MIN=$(( DOWN_DURATION / 60 ))
            notify_server_up "$ENV" "${DOWNTIME_MIN}분"
        fi
        HEALTH_STATUS="up"
        HEALTH_FAILURE_COUNT=0
        HEALTH_DOWN_SINCE=""
    else
        HEALTH_FAILURE_COUNT=0
    fi
else
    HEALTH_FAILURE_COUNT=$((HEALTH_FAILURE_COUNT + 1))
    if [ $HEALTH_FAILURE_COUNT -ge $MAX_HEALTH_FAILURES ] && [ "$HEALTH_STATUS" = "up" ]; then
        HEALTH_STATUS="down"
        HEALTH_DOWN_SINCE=$(date '+%Y-%m-%d %H:%M:%S')
        notify_server_down "$ENV"
    fi
fi

CONTAINER_RUNNING=$(docker ps --filter "name=$CONTAINER_NAME" --filter "status=running" -q)
if [ -z "$CONTAINER_RUNNING" ]; then
    if [ "$CONTAINER_WARNING_SENT" = "false" ]; then
        notify_container_stopped "$ENV" "$CONTAINER_NAME"
        CONTAINER_WARNING_SENT=true
    fi
else
    CONTAINER_WARNING_SENT=false
fi

TEMP_STATUS_FILE=$(mktemp "${STATUS_FILE}.tmp.XXXXXX")
cat > "$TEMP_STATUS_FILE" << EOF
HEALTH_STATUS="$HEALTH_STATUS"
HEALTH_FAILURE_COUNT=$HEALTH_FAILURE_COUNT
HEALTH_DOWN_SINCE="$HEALTH_DOWN_SINCE"
CONTAINER_WARNING_SENT=$CONTAINER_WARNING_SENT
EOF
mv "$TEMP_STATUS_FILE" "$STATUS_FILE"

