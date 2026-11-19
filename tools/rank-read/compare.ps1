[CmdletBinding()]
param(
    [Parameter(Mandatory)][string[]]$Report
)

$ErrorActionPreference = 'Stop'
$results = @($Report | ForEach-Object {
    Get-Content -LiteralPath $_ -Raw | ConvertFrom-Json
})

foreach ($group in ($results | Group-Object users,targetRps)) {
    $mysql = @($group.Group | Where-Object mode -eq 'mysql')
    $redis = @($group.Group | Where-Object mode -eq 'redis')
    $sample = $group.Group[0]

    $improvement = $null
    if ($mysql.Count -eq 1 -and $redis.Count -eq 1 -and
        $mysql[0].verdict -in @('pass', 'fail') -and $redis[0].verdict -in @('pass', 'fail') -and
        $mysql[0].p99Ms -gt 0 -and $null -ne $redis[0].p99Ms) {
        $improvement = [math]::Round(100 * ($mysql[0].p99Ms - $redis[0].p99Ms) / $mysql[0].p99Ms, 2)
    }

    [pscustomobject]@{
        users = $sample.users
        targetRps = $sample.targetRps
        p99ImprovementPercent = $improvement
        mysql = $mysql
        redis = $redis
    }
}
