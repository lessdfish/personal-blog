$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
$syncScript = Join-Path $PSScriptRoot "sync-runtime-config.ps1"
$script:ExcludedPortRanges = $null

function Get-ExcludedPortRanges {
    if ($null -ne $script:ExcludedPortRanges) {
        return $script:ExcludedPortRanges
    }

    $ranges = @()
    $output = netsh interface ipv4 show excludedportrange protocol=tcp 2>$null
    foreach ($line in $output) {
        if ($line -match '^\s*(\d+)\s+(\d+)\s*(\*)?\s*$') {
            $ranges += [pscustomobject]@{
                Start = [int]$matches[1]
                End = [int]$matches[2]
            }
        }
    }

    $script:ExcludedPortRanges = $ranges
    return $script:ExcludedPortRanges
}

function Test-PortExcluded {
    param([int]$Port)

    foreach ($range in (Get-ExcludedPortRanges)) {
        if ($Port -ge $range.Start -and $Port -le $range.End) {
            return $true
        }
    }

    return $false
}

function Test-PortAvailable {
    param([int]$Port)

    if (Test-PortExcluded -Port $Port) {
        return $false
    }

    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
        $listener.Start()
        $listener.Stop()
        return $true
    } catch {
        return $false
    }
}

function Get-AvailablePort {
    param(
        [int]$PreferredPort,
        [int]$SearchStart = $PreferredPort,
        [int]$SearchEnd = ($PreferredPort + 200)
    )

    if (Test-PortAvailable -Port $PreferredPort) {
        return $PreferredPort
    }

    for ($port = $SearchStart; $port -le $SearchEnd; $port++) {
        if (Test-PortAvailable -Port $port) {
            return $port
        }
    }

    throw "No available port found in range $SearchStart-$SearchEnd"
}

function Get-AvailableNacosBasePort {
    param(
        [int]$PreferredPort,
        [int]$SearchStart = $PreferredPort,
        [int]$SearchEnd = ($PreferredPort + 500)
    )

    for ($basePort = $SearchStart; $basePort -le $SearchEnd; $basePort++) {
        $grpcPort = $basePort + 1000
        $raftPort = $basePort + 1001

        if (
            (Test-PortAvailable -Port $basePort) -and
            (Test-PortAvailable -Port $grpcPort) -and
            (Test-PortAvailable -Port $raftPort)
        ) {
            return $basePort
        }
    }

    throw "No available Nacos port set found in range $SearchStart-$SearchEnd"
}

function Get-ContainerPortMapping {
    param(
        [string]$ContainerName,
        [string]$ContainerPort
    )

    $inspectJson = docker inspect $ContainerName 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($inspectJson)) {
        return $null
    }

    $inspect = $inspectJson | ConvertFrom-Json
    if ($inspect.Count -eq 0) {
        return $null
    }

    $bindingKey = "$ContainerPort/tcp"
    $bindings = $inspect[0].HostConfig.PortBindings
    if ($null -eq $bindings -or -not $bindings.PSObject.Properties.Name.Contains($bindingKey)) {
        return $null
    }

    $portBindings = $bindings.$bindingKey
    if ($null -eq $portBindings -or $portBindings.Count -eq 0) {
        return $null
    }

    $hostPort = $portBindings[0].HostPort
    if ([string]::IsNullOrWhiteSpace($hostPort)) {
        return $null
    }

    return [int]$hostPort
}

function Get-ResolvedPort {
    param(
        [string]$ContainerName,
        [string]$ContainerPort,
        [int]$PreferredPort,
        [int]$SearchStart,
        [int]$SearchEnd
    )

    $existingPort = Get-ContainerPortMapping -ContainerName $ContainerName -ContainerPort $ContainerPort
    if ($null -ne $existingPort -and (Test-PortAvailable -Port $existingPort)) {
        return $existingPort
    }

    return Get-AvailablePort -PreferredPort $PreferredPort -SearchStart $SearchStart -SearchEnd $SearchEnd
}

function Start-Or-CreateContainer {
    param(
        [string]$ContainerName,
        [string]$ComposeServiceName,
        [hashtable]$DesiredPortMappings = @{}
    )

    $containerId = docker ps -a --filter "name=^/${ContainerName}$" --format "{{.ID}}"
    if (![string]::IsNullOrWhiteSpace($containerId)) {
        $shouldRecreate = $false
        foreach ($containerPort in $DesiredPortMappings.Keys) {
            $currentHostPort = Get-ContainerPortMapping -ContainerName $ContainerName -ContainerPort $containerPort
            if ($currentHostPort -ne [int]$DesiredPortMappings[$containerPort]) {
                $shouldRecreate = $true
                break
            }
        }

        if ($shouldRecreate) {
            docker rm -f $ContainerName | Out-Null
            docker compose up -d $ComposeServiceName | Out-Null
            return
        }

        docker start $ContainerName | Out-Null
        return
    }

    docker compose up -d $ComposeServiceName | Out-Null
}

$redisPort = Get-ResolvedPort -ContainerName "blog-redis" -ContainerPort "6379" -PreferredPort 6379 -SearchStart 6379 -SearchEnd 6479
$rabbitMqPort = Get-ResolvedPort -ContainerName "blog-rabbitmq" -ContainerPort "5672" -PreferredPort 35672 -SearchStart 35672 -SearchEnd 35772
$rabbitMqManagementPort = Get-ResolvedPort -ContainerName "blog-rabbitmq" -ContainerPort "15672" -PreferredPort 15672 -SearchStart 15672 -SearchEnd 15772
$gatewayPort = Get-AvailablePort -PreferredPort 18080 -SearchStart 18080 -SearchEnd 18180
$existingNacosHttpPort = Get-ContainerPortMapping -ContainerName "blog-nacos" -ContainerPort "8848"
$existingNacosGrpcPort = Get-ContainerPortMapping -ContainerName "blog-nacos" -ContainerPort "9848"
$existingNacosRaftPort = Get-ContainerPortMapping -ContainerName "blog-nacos" -ContainerPort "9849"

$reuseNacosPorts = (
    $null -ne $existingNacosHttpPort -and
    $null -ne $existingNacosGrpcPort -and
    $null -ne $existingNacosRaftPort -and
    $existingNacosGrpcPort -eq ($existingNacosHttpPort + 1000) -and
    $existingNacosRaftPort -eq ($existingNacosHttpPort + 1001) -and
    -not (Test-PortExcluded -Port $existingNacosHttpPort) -and
    -not (Test-PortExcluded -Port $existingNacosGrpcPort) -and
    -not (Test-PortExcluded -Port $existingNacosRaftPort)
)

if ($reuseNacosPorts) {
    $nacosHttpPort = $existingNacosHttpPort
} else {
    $nacosHttpPort = Get-AvailableNacosBasePort -PreferredPort 8948 -SearchStart 8948 -SearchEnd 9448
}

$nacosGrpcPort = $nacosHttpPort + 1000
$nacosRaftPort = $nacosHttpPort + 1001

$envContent = @(
    "REDIS_HOST=127.0.0.1"
    "REDIS_PORT=$redisPort"
    "REDIS_HOST_PORT=$redisPort"
    "RABBITMQ_HOST=127.0.0.1"
    "RABBITMQ_PORT=$rabbitMqPort"
    "RABBITMQ_HOST_PORT=$rabbitMqPort"
    "RABBITMQ_MANAGEMENT_PORT=$rabbitMqManagementPort"
    "GATEWAY_PORT=$gatewayPort"
    "NACOS_HTTP_PORT=$nacosHttpPort"
    "NACOS_GRPC_PORT=$nacosGrpcPort"
    "NACOS_RAFT_PORT=$nacosRaftPort"
    "NACOS_SERVER_ADDR=127.0.0.1:$nacosHttpPort"
) -join [Environment]::NewLine

Set-Content -Path $envFile -Value $envContent -Encoding UTF8

& $syncScript

Write-Host "Generated .env with available ports:" -ForegroundColor Cyan
Write-Host "  Redis:            127.0.0.1:$redisPort"
Write-Host "  RabbitMQ AMQP:    127.0.0.1:$rabbitMqPort"
Write-Host "  RabbitMQ Console: http://127.0.0.1:$rabbitMqManagementPort"
Write-Host "  Gateway:          http://127.0.0.1:$gatewayPort"
Write-Host "  Nacos Console:    http://127.0.0.1:$nacosHttpPort/nacos"
Write-Host "  Nacos gRPC:       127.0.0.1:$nacosGrpcPort"
Write-Host "  Nacos raft:       127.0.0.1:$nacosRaftPort"

Start-Or-CreateContainer -ContainerName "blog-redis" -ComposeServiceName "redis" -DesiredPortMappings @{
    "6379" = $redisPort
}
Start-Or-CreateContainer -ContainerName "blog-rabbitmq" -ComposeServiceName "rabbitmq" -DesiredPortMappings @{
    "5672" = $rabbitMqPort
    "15672" = $rabbitMqManagementPort
}
Start-Or-CreateContainer -ContainerName "blog-nacos" -ComposeServiceName "nacos" -DesiredPortMappings @{
    "8848" = $nacosHttpPort
    "9848" = $nacosGrpcPort
    "9849" = $nacosRaftPort
}

Write-Host ""
Write-Host "Infrastructure is starting." -ForegroundColor Green
Write-Host "Compose variables loaded from: $envFile" -ForegroundColor Yellow
