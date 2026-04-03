#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

REGISTRY="ghcr.io/ddiddit/didit-backend"
WORK_DIR="/home/didit-prod/didit"
DEPLOY_DIR="$WORK_DIR/deploy/prod"
DB_COMPOSE="docker-compose.db.yaml"
BLUE_COMPOSE="docker-compose.blue.yaml"
GREEN_COMPOSE="docker-compose.green.yaml"
NGINX_UPSTREAM="/etc/nginx/conf.d/upstream-didit.conf"
MAX_RETRY=6
RETRY_INTERVAL=10

ENV_FILE="$DEPLOY_DIR/.env"
if [ -f "$ENV_FILE" ]; then
  while IFS='=' read -r key value; do
    case "$key" in
      DISCORD_WEBHOOK_URL|COMMIT_MESSAGE|DEPLOYER|GIT_SHA)
        export "$key=$value"
        ;;
    esac
  done < "$ENV_FILE"
fi

cd "$WORK_DIR" || exit 1

# 현재 활성 컨테이너 확인
if docker ps --format '{{.Names}}' | grep -q '^didit-api-blue$'; then
  CURRENT="blue"
  CURRENT_COMPOSE="$BLUE_COMPOSE"
  NEXT="green"
  NEXT_COMPOSE="$GREEN_COMPOSE"
  NEXT_PORT=8082
else
  CURRENT="green"
  CURRENT_COMPOSE="$GREEN_COMPOSE"
  NEXT="blue"
  NEXT_COMPOSE="$BLUE_COMPOSE"
  NEXT_PORT=8081
fi

HEALTH_CHECK_URL="http://localhost:${NEXT_PORT}/actuator/health"

echo -e "${YELLOW}[현재: $CURRENT → 배포: $NEXT]${NC}"

echo -e "${YELLOW}[1/8] 오래된 이미지 정리${NC}"
OLD_IMAGES=$(docker images ${REGISTRY} -q | tail -n +3 || true)
if [ ! -z "${OLD_IMAGES:-}" ]; then
  echo "$OLD_IMAGES" | xargs docker rmi -f 2>/dev/null || true
fi

# 1. git-sha 태그로 pull
echo -e "${YELLOW}[2/8] 최신 이미지 다운로드${NC}"
docker pull ${REGISTRY}:${GIT_SHA}
docker tag ${REGISTRY}:${GIT_SHA} ${REGISTRY}:latest
NEW_IMAGE=$(docker images ${REGISTRY}:latest -q || true)
echo -e "${GREEN}[SUCCESS] 새 이미지: ${NEW_IMAGE} (sha: ${GIT_SHA})${NC}"

cd "$DEPLOY_DIR" || exit 1

echo -e "${YELLOW}[3/8] DB 컨테이너 확인${NC}"
if ! docker ps --format '{{.Names}}' | grep -q '^didit-prod-db$'; then
  docker compose -f "$DB_COMPOSE" up -d
  echo -e "${GREEN}[SUCCESS] DB 컨테이너 시작 완료${NC}"
else
  echo -e "${GREEN}[SUCCESS] DB 컨테이너 이미 실행 중${NC}"
fi

# 2. set -e 우회해서 $? 체크
echo -e "${YELLOW}[4/8] 새 컨테이너($NEXT) 시작${NC}"
set +e
docker compose -f "$NEXT_COMPOSE" up -d
COMPOSE_EXIT=$?
set -e
if [ $COMPOSE_EXIT -ne 0 ]; then
  echo -e "${RED}[ERROR] 컨테이너 시작 실패${NC}"
  docker compose -f "$NEXT_COMPOSE" down 2>/dev/null || true
  if [ ! -z "${DISCORD_WEBHOOK_URL:-}" ] && [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
    source "$WORK_DIR/deploy/shared/discord-notify.sh"
    notify_rollback_failed
  fi
  exit 1
fi

echo -e "${YELLOW}[5/8] 애플리케이션 시작 대기${NC}"
sleep 15

echo -e "${YELLOW}[6/8] 헬스체크 ($NEXT)${NC}"
RETRY_COUNT=0
HEALTH_OK=false
while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
  if curl -f -s --max-time 5 "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
    HEALTH_OK=true
    break
  else
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -e "${YELLOW}[RETRY] (${RETRY_COUNT}/${MAX_RETRY})${NC}"
    [ $RETRY_COUNT -eq 3 ] && docker logs --tail 30 didit-api-${NEXT} || true
    sleep $RETRY_INTERVAL
  fi
done

if [ "$HEALTH_OK" = false ]; then
  echo -e "${RED}[ERROR] 헬스체크 실패 - 새 컨테이너 종료 (이전 컨테이너 유지)${NC}"
  docker logs --tail 100 didit-api-${NEXT} || true
  docker compose -f "$NEXT_COMPOSE" down 2>/dev/null || true
  if [ ! -z "${DISCORD_WEBHOOK_URL:-}" ] && [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
    source "$WORK_DIR/deploy/shared/discord-notify.sh"
    notify_rollback_failed
  fi
  exit 1
fi

echo -e "${YELLOW}[7/8] Nginx upstream 전환 ($CURRENT → $NEXT)${NC}"
cat > "$NGINX_UPSTREAM" << EOF
upstream didit-api {
    server 127.0.0.1:${NEXT_PORT};
}
EOF
nginx -s reload
echo -e "${GREEN}[SUCCESS] Nginx upstream 전환 완료${NC}"

echo -e "${YELLOW}[8/8] 이전 컨테이너($CURRENT) 종료 및 정리${NC}"
docker compose -f "$CURRENT_COMPOSE" down 2>/dev/null || true
docker image prune -f || true

if [ ! -z "${DISCORD_WEBHOOK_URL:-}" ] && [ -f "$WORK_DIR/deploy/shared/discord-notify.sh" ]; then
  source "$WORK_DIR/deploy/shared/discord-notify.sh"
  notify_deploy_success "$NEW_IMAGE" "$COMMIT_MESSAGE" "$DEPLOYER"
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  배포 완료! ($NEXT 활성)${NC}"
echo -e "${GREEN}========================================${NC}"
