param(
    [int]$Iterations = 30,
    [int]$Warmup = 5,
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = $scriptDir
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

function Assert-AppRunning {
    param([string]$Url)
    try {
        Invoke-WebRequest -Uri "$Url/api-docs" -UseBasicParsing -TimeoutSec 3 | Out-Null
    } catch {
        throw "Application is not reachable at $Url. Start the app with preloaded MariaDB data and Lucene index, then rerun."
    }
}

function Invoke-PostJson {
    param(
        [string]$Url,
        [object]$Body
    )
    $json = $Body | ConvertTo-Json -Depth 10
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-RestMethod -Method Post -Uri $Url -ContentType "application/json" -Body $json
    $sw.Stop()
    return [PSCustomObject]@{
        elapsedMs = [Math]::Round($sw.Elapsed.TotalMilliseconds, 3)
        response = $response
    }
}

function Percentile {
    param([double[]]$Values, [double]$P)
    if ($Values.Count -eq 0) { return 0.0 }
    $sorted = $Values | Sort-Object
    $rank = [Math]::Ceiling(($P / 100.0) * $sorted.Count) - 1
    if ($rank -lt 0) { $rank = 0 }
    if ($rank -ge $sorted.Count) { $rank = $sorted.Count - 1 }
    return [Math]::Round([double]$sorted[$rank], 3)
}

Assert-AppRunning -Url $BaseUrl
Write-Host "Running benchmark using preloaded MariaDB data and existing Lucene index."

$scenarios = @(
    [PSCustomObject]@{
        name = "baseline_natural_light"
        body = @{
            queryText = "Natural Light"
            city = "Nuremberg"
            propertyType = "APARTMENT"
            limit = 20
        }
    },
    [PSCustomObject]@{
        name = "modern_area_berlin"
        body = @{
            queryText = "modern"
            city = "Berlin"
            minAreaInSquareMeter = 50
            maxAreaInSquareMeter = 150
            limit = 20
        }
    }
)

$rows = New-Object System.Collections.Generic.List[object]

foreach ($scenario in $scenarios) {
    foreach ($engine in @("lucene", "mariadb-fts")) {
        $url = "$BaseUrl/api/$engine/search"
        Write-Host "Warmup: scenario=$($scenario.name), engine=$engine"
        for ($i = 0; $i -lt $Warmup; $i++) {
            Invoke-PostJson -Url $url -Body $scenario.body | Out-Null
        }

        Write-Host "Benchmark: scenario=$($scenario.name), engine=$engine"
        $samples = New-Object System.Collections.Generic.List[double]
        $hitCounts = New-Object System.Collections.Generic.List[int]
        for ($i = 1; $i -le $Iterations; $i++) {
            $result = Invoke-PostJson -Url $url -Body $scenario.body
            $samples.Add([double]$result.elapsedMs)
            $hitCounts.Add([int]$result.response.totalHits)
        }

        $avg = [Math]::Round((($samples | Measure-Object -Average).Average), 3)
        $p50 = Percentile -Values $samples.ToArray() -P 50
        $p95 = Percentile -Values $samples.ToArray() -P 95
        $min = [Math]::Round((($samples | Measure-Object -Minimum).Minimum), 3)
        $max = [Math]::Round((($samples | Measure-Object -Maximum).Maximum), 3)
        $hits = [int](($hitCounts | Measure-Object -Maximum).Maximum)

        $rows.Add([PSCustomObject]@{
            scenario = $scenario.name
            engine = $engine
            iterations = $Iterations
            warmup = $Warmup
            avgMs = $avg
            p50Ms = $p50
            p95Ms = $p95
            minMs = $min
            maxMs = $max
            totalHits = $hits
            measuredAt = (Get-Date).ToString("s")
        }) | Out-Null
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$csvPath = Join-Path $outDir "benchmark-$timestamp.csv"
$jsonPath = Join-Path $outDir "benchmark-$timestamp.json"
$rows | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
$rows | ConvertTo-Json -Depth 5 | Out-File -FilePath $jsonPath -Encoding UTF8

Write-Host ""
Write-Host "Benchmark complete"
Write-Host "CSV: $csvPath"
Write-Host "JSON: $jsonPath"
$rows | Format-Table -AutoSize
