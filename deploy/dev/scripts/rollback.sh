#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

REGISTRY="ghcr.io/ddiddit/didit-backend"
WORK_DIR="/home/didit-dev/didit"
DEPLOY_DIR="$WORK_DIR/deploy/dev"
APP_COMPOSE="docker-compose.app.yaml"
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
MAX_RETRY=6
RETRY_INTERVAL=10

if [ -f "$DEPLOY_DIR/.env" ]; then
  DISCORD_WEBHOOK_URL="$(grep -E '^DISCORD_WEBHOOK_URL=' "$DEPLOY_DIR/.env" | tail -n 1 | cut -d= -f2- || true)"
  export DISCORD_WEBHOOK_URL
fi

echo -e "${YELLOW}[1/6] 롤백용 이미지 확인${NC}"
PREVIOUS_IMAGE=$(docker images ${REGISTRY}:previous -q || true)
if [ -z "${PREVIOUS_IMAGE:-}" ]; then
  echo -e "${RED}[ERROR] 롤백할 이전 이미지가 없습니다.${NC}"
  exit 1
fi

echo -e "${YELLOW}[2/6] 현재 APP 컨테이너 중지${NC}"
cd "$DEPLOY_DIR" || exit 1
docker compose -f "$APP_COMPOSE" down 2>/dev/null || docker rm -f didit-api 2>/dev/null || true

echo -e "${YELLOW}[3/6] 이전 버전으로 태그 변경${NC}"
docker tag ${REGISTRY}:previous ${REGISTRY}:latest

echo -e "${YELLOW}[4/6] 이전 버전 APP 컨테이너 시작${NC}"
docker compose -f "$APP_COMPOSE" up -d

echo -e "${YELLOW}[5/6] 시작 대기${NC}"
sleep 10

echo -e "${YELLOW}[6/6] 헬스체크${NC}"
RETRY_COUNT=0
HEALTH_OK=false
while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
  if curl -f -s --max-time 5 "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
    HEALTH_OK=true
    break
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep $RETRY_INTERVAL
  fi
done

if [ "$HEALTH_OK" = true ]; then
  echo -e "${GREEN}  롤백 성공${NC}"
  if [ ! -z "${DISCORD_WEBHOOK_URL:-}" ] && [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
    source "$WORK_DIR/deploy/shared/discord-notify.sh"
    notify_rollback_success "dev"
  fi
else
  echo -e "${RED}  롤백 실패 - 수동 복구 필요${NC}"
  if [ ! -z "${DISCORD_WEBHOOK_URL:-}" ] && [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
    source "$WORK_DIR/deploy/shared/discord-notify.sh"
    notify_rollback_failed "dev"
  fi
  exit 1
fi