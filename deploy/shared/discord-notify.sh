#!/bin/bash

COLOR_RED=15158332
COLOR_GREEN=3066993
COLOR_YELLOW=16776960

notify_deploy_success() {
    local image_id=$1
    local commit_message=${2:-""}
    local deployer=${3:-""}
    local env=${4:-"dev"}
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    [ -z "$DISCORD_WEBHOOK_URL" ] && return

    PAYLOAD=$(jq -n \
      --arg image_id "$image_id" \
      --arg msg "$commit_message" \
      --arg ts "$timestamp" \
      --arg deployer "$deployer" \
      --arg env "$env" \
      --argjson color "$COLOR_GREEN" \
      '{username: "Didit Bot", embeds: [{title: ("배포 완료 [" + $env + "]"), description: $msg, color: $color, fields: [{name: "담당자", value: $deployer, inline: true}, {name: "시간", value: $ts, inline: true}, {name: "이미지", value: $image_id, inline: false}]}]}')
    curl -s -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$DISCORD_WEBHOOK_URL" > /dev/null
}

notify_rollback_success() {
    local env=${1:-"dev"}
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    [ -z "$DISCORD_WEBHOOK_URL" ] && return

    PAYLOAD=$(jq -n --arg ts "$timestamp" --arg env "$env" --argjson color "$COLOR_GREEN" \
      '{username: "Didit Bot", embeds: [{title: ("롤백 성공 [" + $env + "]"), description: "이전 버전으로 복구되었습니다.", color: $color, fields: [{name: "시간", value: $ts, inline: true}]}]}')
    curl -s -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$DISCORD_WEBHOOK_URL" > /dev/null
}

notify_rollback_failed() {
    local env=${1:-"dev"}
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    [ -z "$DISCORD_WEBHOOK_URL" ] && return

    PAYLOAD=$(jq -n --arg ts "$timestamp" --arg env "$env" --argjson color "$COLOR_RED" \
      '{username: "Didit Bot", content: "@here 롤백 실패 - 수동 복구 필요", embeds: [{title: ("롤백 실패 [" + $env + "]"), color: $color, fields: [{name: "시간", value: $ts, inline: true}]}]}')
    curl -s -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$DISCORD_WEBHOOK_URL" > /dev/null
}

notify_server_down() {
    local env=${1:-"dev"}
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    [ -z "$DISCORD_WEBHOOK_URL" ] && return

    PAYLOAD=$(jq -n --arg ts "$timestamp" --arg env "$env" --argjson color "$COLOR_RED" \
      '{username: "Didit Bot", content: "@here 서버 다운", embeds: [{title: ("서버 다운 [" + $env + "]"), color: $color, fields: [{name: "시간", value: $ts, inline: true}]}]}')
    curl -s -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$DISCORD_WEBHOOK_URL" > /dev/null
}

notify_server_up() {
    local downtime=${1:-""}
    local env=${2:-"dev"}
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    [ -z "$DISCORD_WEBHOOK_URL" ] && return

    PAYLOAD=$(jq -n --arg ts "$timestamp" --arg dt "$downtime" --arg env "$env" --argjson color "$COLOR_GREEN" \
      '{username: "Didit Bot", embeds: [{title: ("서버 복구 [" + $env + "]"), color: $color, fields: [{name: "다운타임", value: $dt, inline: true}, {name: "복구 시간", value: $ts, inline: true}]}]}')
    curl -s -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$DISCORD_WEBHOOK_URL" > /dev/null
}