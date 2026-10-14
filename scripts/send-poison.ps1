<#
.SYNOPSIS
    Publica UMA mensagem com erro no messages.exchange via API HTTP de gerenciamento do RabbitMQ
    (POST /api/exchanges/%2F/messages.exchange/publish) para demonstrar a DLQ.

.EXAMPLE
    .\scripts\send-poison.ps1                         # review + __POISON__ (padrão)
    .\scripts\send-poison.ps1 -Target support         # support + __POISON__
    .\scripts\send-poison.ps1 -Kind invalid-json      # corpo que não é JSON
    .\scripts\send-poison.ps1 -Target support -Kind missing-field

.NOTES
    Credenciais: variáveis RABBITMQ_USER / RABBITMQ_PASS, ou lidas do .env, ou padrão admin/admin123.
    Se a execução de scripts estiver bloqueada:  powershell -ExecutionPolicy Bypass -File .\scripts\send-poison.ps1
#>
param(
    [ValidateSet("review", "support")]
    [string]$Target = "review",

    [ValidateSet("poison", "invalid-json", "missing-field")]
    [string]$Kind = "poison",

    [string]$ManagementUrl = ""
)

$ErrorActionPreference = "Stop"

function Read-EnvValue([string]$Name) {
    $envFile = Join-Path $PSScriptRoot "..\.env"
    if (Test-Path $envFile) {
        $line = Get-Content $envFile | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
        if ($line) { return ($line -split "=", 2)[1].Trim() }
    }
    return $null
}

function First-NonEmpty([string[]]$Values) {
    foreach ($v in $Values) { if (-not [string]::IsNullOrWhiteSpace($v)) { return $v } }
    return $null
}

$user = First-NonEmpty @($env:RABBITMQ_USER, (Read-EnvValue "RABBITMQ_USER"), "admin")
$pass = First-NonEmpty @($env:RABBITMQ_PASS, (Read-EnvValue "RABBITMQ_PASS"), "admin123")
$port = First-NonEmpty @((Read-EnvValue "RABBITMQ_MANAGEMENT_PORT"), "15672")
$baseUrl = First-NonEmpty @($ManagementUrl, $env:RABBITMQ_MANAGEMENT_URL, "http://localhost:$port")

if ($Target -eq "review") {
    $routingKey = "review.product"; $type = "review"; $dlq = "review.dlq"
} else {
    $routingKey = "support.request"; $type = "support"; $dlq = "support.dlq"
}

$id = [guid]::NewGuid().ToString()
$now = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.fffzzz")

switch ($Kind) {
    "poison"        { $body = (@{ id = $id; timestamp = $now; type = $type; content = "__POISON__" } | ConvertTo-Json -Compress) }
    "invalid-json"  { $body = "{`"id`":`"$id`",`"type`":`"$type`",`"content`": isto nao e um JSON valido" }
    "missing-field" { $body = (@{ id = $id; timestamp = $now; type = $type } | ConvertTo-Json -Compress) }
}

$request = @{
    properties       = @{ delivery_mode = 2; content_type = "application/json"; message_id = $id; app_id = "send-poison" }
    routing_key      = $routingKey
    payload          = $body
    payload_encoding = "string"
} | ConvertTo-Json -Compress

$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${user}:${pass}"))
$headers = @{ Authorization = "Basic $auth" }

Write-Host "Publicando mensagem com erro ($Kind) em messages.exchange com routing key '$routingKey'..."
Write-Host "  id   : $id"
Write-Host "  corpo: $body"

$response = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/exchanges/%2F/messages.exchange/publish" `
    -Headers $headers -ContentType "application/json" -Body ([Text.Encoding]::UTF8.GetBytes($request))

Write-Host "Resposta da API: routed=$($response.routed)"
if ($response.routed) {
    Write-Host "OK! O consumidor vai rejeitar a mensagem (basicNack requeue=false) e ela aparecera na fila '$dlq'." -ForegroundColor Green
    Write-Host "No painel: Queues > $dlq > Get messages (veja o header x-death)." -ForegroundColor Green
} else {
    Write-Error "A mensagem NAO foi roteada. Verifique se o ambiente esta no ar e as credenciais."
}
