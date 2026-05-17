param(
    [string]$OutputDir = "deploy/backup/output",
    [switch]$SkipContainerDumps
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $outputRoot = [System.IO.Path]::GetFullPath($OutputDir)
} else {
    $outputRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDir))
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupDir = Join-Path $outputRoot $stamp
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

function Copy-IfExists {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path -LiteralPath $Source) {
        Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
    }
}

$manifest = @(
    "backup_time=$((Get-Date).ToString("o"))"
    "repo_root=$repoRoot"
    "skip_container_dumps=$SkipContainerDumps"
)
Set-Content -LiteralPath (Join-Path $backupDir "backup-manifest.txt") -Value $manifest -Encoding UTF8

Copy-IfExists -Source (Join-Path $repoRoot "deploy\nacos") -Destination (Join-Path $backupDir "nacos-configs")
Copy-IfExists -Source (Join-Path $repoRoot "deploy\prometheus") -Destination (Join-Path $backupDir "prometheus-config")
Copy-IfExists -Source (Join-Path $repoRoot "deploy\grafana\provisioning") -Destination (Join-Path $backupDir "grafana-provisioning")
Copy-IfExists -Source (Join-Path $repoRoot "docker-compose.yml") -Destination (Join-Path $backupDir "docker-compose.yml")

if ($SkipContainerDumps) {
    Set-Content -LiteralPath (Join-Path $backupDir "container-dumps-skipped.txt") -Value "Container dumps were skipped by request." -Encoding UTF8
    Write-Output "Created backup metadata snapshot: $backupDir"
    exit 0
}

try {
    & docker version --format "{{.Server.Version}}" | Out-Null
} catch {
    throw "Docker is not available. Re-run with -SkipContainerDumps for a metadata-only backup, or start Docker and retry."
}

$mysqlDump = Join-Path $backupDir "mysql-blog-cloud.sql"
& docker exec blog-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases "$MYSQL_DATABASE"' | Set-Content -LiteralPath $mysqlDump -Encoding UTF8
if ($LASTEXITCODE -ne 0) {
    throw "MySQL dump failed."
}

$redisInfo = Join-Path $backupDir "redis-persistence.txt"
& docker exec blog-redis sh -c 'redis-cli ${REDIS_PASSWORD:+-a "$REDIS_PASSWORD"} BGSAVE && redis-cli ${REDIS_PASSWORD:+-a "$REDIS_PASSWORD"} LASTSAVE' | Set-Content -LiteralPath $redisInfo -Encoding UTF8

$rabbitmqStatus = Join-Path $backupDir "rabbitmq-status.txt"
& docker exec blog-rabbitmq rabbitmq-diagnostics status | Set-Content -LiteralPath $rabbitmqStatus -Encoding UTF8

$esIndices = Join-Path $backupDir "elasticsearch-indices.txt"
& docker exec blog-elasticsearch sh -c 'curl -fsS http://127.0.0.1:9200/_cat/indices?v' | Set-Content -LiteralPath $esIndices -Encoding UTF8

Write-Output "Created backup snapshot: $backupDir"
