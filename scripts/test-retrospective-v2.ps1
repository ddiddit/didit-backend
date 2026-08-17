param(
    [Parameter(Mandatory = $true)]
    [string]$AccessToken,

    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [string[]]$Messages = @(
        "오늘 배포 자동화 작업을 마쳤는데 예상보다 설정 충돌이 많았습니다.",
        "로그를 단계별로 확인해서 환경 변수 우선순위가 원인이라는 걸 찾았습니다."
    ),

    [switch]$Finish
)

$ErrorActionPreference = "Stop"
$headers = @{ Authorization = "Bearer $AccessToken" }
$jsonHeaders = $headers + @{ "Content-Type" = "application/json" }

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
