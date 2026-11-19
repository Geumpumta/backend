[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('mysql', 'redis')][string]$Mode,
    [Parameter(Mandatory)][ValidateRange(100, 1000)][int]$Rate,
    [Parameter(Mandatory)][uri]$BaseUrl,
    [uri]$PrometheusUrl = 'http://127.0.0.1:9090',
    [ValidateSet(1000, 10000, 50000)][int]$Users = 10000,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '../../build/rank-read')
)

$ErrorActionPreference = 'Stop'
$k6 = Get-Command k6 -ErrorAction Stop
foreach ($origin in @($BaseUrl, $PrometheusUrl)) {
    if (!$origin.IsAbsoluteUri -or $origin.Scheme -notin @('http', 'https') -or
        $origin.UserInfo -or $origin.Query -or $origin.Fragment -or $origin.AbsolutePath -ne '/') {
        throw 'Supply an HTTP or HTTPS origin without a path, query, fragment or user information.'
    }
}
if (!$PrometheusUrl.IsLoopback) { throw 'Use a local Prometheus origin or an SSH tunnel.' }
$ready = [uri]::new($PrometheusUrl, '/-/ready')
Invoke-WebRequest -Uri $ready -UseBasicParsing -TimeoutSec 5 | Out-Null

$runId = "$Mode-$Users-$Rate-$([datetime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ'))"
$output = $OutputDirectory
New-Item -ItemType Directory -Force -Path $output | Out-Null
$report = Join-Path (Resolve-Path -LiteralPath $output).Path "$runId.json"
$settings = @{
    RANK_BASE_URL = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
    RANK_MODE = $Mode
    RANK_USERS = $Users
    RANK_RATE = $Rate
    RANK_RUN_ID = $runId
    RANK_REPORT = $report
    K6_PROMETHEUS_RW_SERVER_URL = [uri]::new($PrometheusUrl, '/api/v1/write').AbsoluteUri
    K6_PROMETHEUS_RW_TREND_STATS = 'avg,p(99)'
    K6_PROMETHEUS_RW_PUSH_INTERVAL = '5s'
    K6_PROMETHEUS_RW_STALE_MARKERS = 'true'
}

if (-not $env:RANK_TOKEN) {
    $token = Read-Host 'Test user access token' -AsSecureString
    $settings.RANK_TOKEN = [System.Net.NetworkCredential]::new('', $token).Password
}

$previous = @{}
try {
    foreach ($key in $settings.Keys) {
        $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, [string]$settings[$key], 'Process')
    }
    & $k6 run -o experimental-prometheus-rw --tag "run_id=$runId" --tag "mode=$Mode" --tag "target_rps=$Rate" (Join-Path $PSScriptRoot 'rank.js')
    $runExit = $LASTEXITCODE
    if (Test-Path -LiteralPath $report) {
        Write-Host "Report saved to $report"
    }
    if ($runExit -ne 0) {
        throw "k6 exited with code $runExit. Check the summary and k6 output."
    }
} finally {
    foreach ($key in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
    }
}
