param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Users = 10,
    [int]$Iterations = 10
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

if (-not (Test-Path "run-logs\tokens.json")) {
    throw "run-logs\\tokens.json not found"
}

$tokens = Get-Content "run-logs\tokens.json" -Raw | ConvertFrom-Json
$adminToken = $tokens.admin.token

function Invoke-Scenario {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers
    )

    $jobs = @()
    $startAt = [System.Diagnostics.Stopwatch]::StartNew()

    for ($u = 0; $u -lt $Users; $u++) {
        $jobs += Start-Job -ScriptBlock {
            param($Method, $Url, $Headers, $Iterations)
            $localResults = @()
            for ($i = 0; $i -lt $Iterations; $i++) {
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                try {
                    if ($Headers -and $Headers.Count -gt 0) {
                        $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -TimeoutSec 8
                    } else {
                        $response = Invoke-WebRequest -Uri $Url -Method $Method -TimeoutSec 8
                    }
                    $body = $response.Content
                    $sw.Stop()
                    $localResults += [pscustomobject]@{
                        elapsedMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                        ok = ($body -like '*"code":200*')
                        statusCode = [int]$response.StatusCode
                    }
                } catch {
                    $sw.Stop()
                    $localResults += [pscustomobject]@{
                        elapsedMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                        ok = $false
                        statusCode = -1
                    }
                }
            }
            return $localResults
        } -ArgumentList $Method, $Url, $Headers, $Iterations
    }

    Wait-Job -Job $jobs | Out-Null
    $startAt.Stop()
    $items = @($jobs | Receive-Job)
    $jobs | Remove-Job -Force | Out-Null
    $total = $items.Count
    $success = @($items | Where-Object { $_.ok }).Count
    $failed = $total - $success
    $avg = if ($total -gt 0) { [math]::Round((($items | Measure-Object -Property elapsedMs -Average).Average), 2) } else { 0 }
    $min = if ($total -gt 0) { [math]::Round((($items | Measure-Object -Property elapsedMs -Minimum).Minimum), 2) } else { 0 }
    $max = if ($total -gt 0) { [math]::Round((($items | Measure-Object -Property elapsedMs -Maximum).Maximum), 2) } else { 0 }
    $qps = if ($startAt.Elapsed.TotalSeconds -gt 0) { [math]::Round($total / $startAt.Elapsed.TotalSeconds, 2) } else { 0 }

    return [pscustomobject]@{
        scenario = $Name
        users = $Users
        iterationsPerUser = $Iterations
        totalRequests = $total
        successRequests = $success
        failedRequests = $failed
        successRate = if ($total -gt 0) { [math]::Round(($success * 100.0 / $total), 2) } else { 0 }
        avgMs = $avg
        minMs = $min
        maxMs = $max
        qps = $qps
    }
}

$report = @()
$report += Invoke-Scenario -Name "public_article_page" -Method "GET" -Url "$BaseUrl/api/article/page?pageNum=1&pageSize=10" -Headers @{}
$report += Invoke-Scenario -Name "public_article_detail" -Method "GET" -Url "$BaseUrl/api/article/detail/1" -Headers @{}
$report += Invoke-Scenario -Name "auth_user_me" -Method "GET" -Url "$BaseUrl/api/user/me" -Headers @{ Authorization = "Bearer $adminToken" }

$report | ConvertTo-Json -Depth 6
