$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "run-logs"
$envFile = Join-Path $root ".env"
$syncScript = Join-Path $PSScriptRoot "sync-runtime-config.ps1"

function Import-DotEnv {
    param([string]$Path)

    $envMap = @{}
    if (!(Test-Path $Path)) {
        return $envMap
    }

    foreach ($line in Get-Content $Path) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }
        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }
        $envMap[$parts[0].Trim()] = $parts[1].Trim()
    }

    return $envMap
}

function Get-ServiceJarPath {
    param(
        [string]$ModulePath,
        [string]$ModuleName
    )

    $targetDir = Join-Path $ModulePath "target"
    if (!(Test-Path $targetDir)) {
        return $null
    }

    $jar = Get-ChildItem -Path $targetDir -Filter "$ModuleName-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*original*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        return $null
    }

    return $jar.FullName
}

$envMap = Import-DotEnv -Path $envFile
if (!$envMap.ContainsKey("NACOS_HTTP_PORT")) { $envMap["NACOS_HTTP_PORT"] = "8948" }
if (!$envMap.ContainsKey("NACOS_SERVER_ADDR")) { $envMap["NACOS_SERVER_ADDR"] = "127.0.0.1:8948" }
if (!$envMap.ContainsKey("REDIS_HOST")) { $envMap["REDIS_HOST"] = "127.0.0.1" }
if (!$envMap.ContainsKey("REDIS_PORT")) { $envMap["REDIS_PORT"] = "6379" }
if (!$envMap.ContainsKey("RABBITMQ_HOST")) { $envMap["RABBITMQ_HOST"] = "127.0.0.1" }
if (!$envMap.ContainsKey("RABBITMQ_PORT")) { $envMap["RABBITMQ_PORT"] = "35672" }
if (!$envMap.ContainsKey("RABBITMQ_USERNAME")) { $envMap["RABBITMQ_USERNAME"] = "guest" }
if (!$envMap.ContainsKey("RABBITMQ_PASSWORD")) { $envMap["RABBITMQ_PASSWORD"] = "guest" }
if (!$envMap.ContainsKey("MYSQL_URL")) { $envMap["MYSQL_URL"] = "jdbc:mysql://127.0.0.1:3306/blog_cloud?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai" }
if (!$envMap.ContainsKey("MYSQL_USERNAME")) { $envMap["MYSQL_USERNAME"] = "root" }
if (!$envMap.ContainsKey("MYSQL_PASSWORD")) { $envMap["MYSQL_PASSWORD"] = "032581" }
if (!$envMap.ContainsKey("MYSQL_TEST_URL")) { $envMap["MYSQL_TEST_URL"] = "jdbc:mysql://127.0.0.1:3306/blog_cloud_test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai" }
if (!$envMap.ContainsKey("MYSQL_TEST_USERNAME")) { $envMap["MYSQL_TEST_USERNAME"] = $envMap["MYSQL_USERNAME"] }
if (!$envMap.ContainsKey("MYSQL_TEST_PASSWORD")) { $envMap["MYSQL_TEST_PASSWORD"] = $envMap["MYSQL_PASSWORD"] }
if (!$envMap.ContainsKey("SPRING_PROFILES_ACTIVE")) { $envMap["SPRING_PROFILES_ACTIVE"] = "test" }
if (!$envMap.ContainsKey("GATEWAY_PORT")) { $envMap["GATEWAY_PORT"] = "18080" }

& $syncScript

if (!(Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$services = @(
    @{ Name = "user-service"; Module = "user-service" },
    @{ Name = "article-service"; Module = "article-service" },
    @{ Name = "comment-service"; Module = "comment-service" },
    @{ Name = "notify-service"; Module = "notify-service" },
    @{ Name = "blog-gateway"; Module = "blog-gateway" }
)

foreach ($service in $services) {
    $modulePath = Join-Path $root $service.Module
    $outFile = Join-Path $logDir "$($service.Name).out.log"
    $errFile = Join-Path $logDir "$($service.Name).err.log"
    $jarPath = Get-ServiceJarPath -ModulePath $modulePath -ModuleName $service.Module

    if ($null -eq $jarPath) {
        Write-Host "Packaging $($service.Name) because no runnable jar was found ..." -ForegroundColor Yellow
        & mvn -q -DskipTests package -f (Join-Path $modulePath "pom.xml")
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to package $($service.Name)"
        }
        $jarPath = Get-ServiceJarPath -ModulePath $modulePath -ModuleName $service.Module
        if ($null -eq $jarPath) {
            throw "Runnable jar still not found for $($service.Name)"
        }
    }

    $envAssignments = @(
        foreach ($entry in $envMap.GetEnumerator()) {
            '$env:{0}=''{1}''' -f $entry.Key, ($entry.Value -replace "'", "''")
        }
    ) -join "; "
    $startupCommand = "$envAssignments; & java '-jar' '$jarPath' '--spring.profiles.active=$($envMap['SPRING_PROFILES_ACTIVE'])'"

    Write-Host "Starting $($service.Name) ..." -ForegroundColor Cyan

    Start-Process `
        -FilePath "powershell.exe" `
        -WorkingDirectory $modulePath `
        -ArgumentList @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-Command",
            $startupCommand
        ) `
        -RedirectStandardOutput $outFile `
        -RedirectStandardError $errFile
}

Write-Host ""
Write-Host "All services are starting in background." -ForegroundColor Green
Write-Host "Logs directory: $logDir" -ForegroundColor Yellow
Write-Host "Recommended check order:" -ForegroundColor Yellow
Write-Host "1. Open Nacos: http://localhost:$($envMap['NACOS_HTTP_PORT'])/nacos"
Write-Host "2. Confirm 5 services are registered"
Write-Host "3. Access gateway: http://localhost:$($envMap['GATEWAY_PORT'])"
