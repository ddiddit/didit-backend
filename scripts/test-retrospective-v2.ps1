param(
    [Parameter(Mandatory = $true)]
    [string]$AccessToken,

    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [ValidateSet("Default", "LongAnswer", "Decline", "ActionGuide")]
    [string]$Scenario = "Default",

    [string[]]$Messages = @(),

    [switch]$Finish
)

$ErrorActionPreference = "Stop"
$headers = @{ Authorization = "Bearer $AccessToken" }
$jsonHeaders = $headers + @{ "Content-Type" = "application/json" }

if ($Messages.Count -eq 0) {
    $Messages = switch ($Scenario) {
        "LongAnswer" {
            @(
                "오늘 배포 준비와 개발 서버 검증을 진행했습니다. 테스트 범위가 예상보다 넓어 시간이 오래 걸렸지만 팀원과 실패 로그를 나눠 확인했고, 환경 변수 우선순위 문제를 찾아 수정한 뒤 배포 준비를 마쳤습니다.",
                "로그를 단계별로 좁혀간 방식이 효과적이었고 다음에도 같은 방식을 유지하고 싶습니다."
            )
        }
        "Decline" {
            @(
                "오늘 Spring CS 면접 공부를 하면서 멱등성과 멱등키를 정리했습니다.",
                "어려웠던 점은 딱히 없어요.",
                "GPT가 만든 퀴즈를 풀면서 프로젝트 사례와 연결해 복습했습니다."
            )
        }
        "ActionGuide" {
            @(
                "오늘 Spring CS 면접 공부를 했고 멱등키로 중복 요청을 막는 방법을 정리했습니다.",
                "노션에 제 방식대로 요약하고 GPT 퀴즈로 답변을 확인했습니다.",
                "다음 행동을 어떻게 정하면 좋을지 예시를 보고 싶어요."
            )
        }
        default {
            @(
                "오늘 배포 자동화 작업을 마쳤는데 예상보다 설정 충돌이 많았습니다.",
                "로그를 단계별로 확인해서 환경 변수 우선순위가 원인이라는 걸 찾았습니다."
            )
        }
    }
}

function Write-Response {
    param(
        [string]$Label,
        [object]$Response
    )

    Write-Host "`n[$Label]"
    $Response | ConvertTo-Json -Depth 10
}

$started = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v2/retrospectives" -Headers $headers
$retrospectiveId = $started.data.retrospectiveId
Write-Response -Label "START $retrospectiveId" -Response $started
Write-Host "Scenario: $Scenario"

foreach ($message in $Messages) {
    $body = @{
        clientMessageId = [guid]::NewGuid().ToString()
        content = $message
    } | ConvertTo-Json

    $reply = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/v2/retrospectives/$retrospectiveId/messages" `
        -Headers $jsonHeaders `
        -Body $body
    Write-Response -Label "MESSAGE" -Response $reply
    $guide = $reply.data.assistantMessage.answerGuide
    Write-Host "Observed action=$($reply.data.action), guideMode=$($guide.mode), exampleCount=$($guide.examples.Count)"
}

$conversation = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/v2/retrospectives/$retrospectiveId/conversation" `
    -Headers $headers
Write-Response -Label "CONVERSATION" -Response $conversation

if ($Finish) {
    $finished = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/v2/retrospectives/$retrospectiveId/finish" `
        -Headers $headers
    Write-Response -Label "FINISH" -Response $finished
}

Write-Host "`nRetrospective ID: $retrospectiveId"
