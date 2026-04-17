$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
$runDir = Join-Path $root ".run"
$webEnvFile = Join-Path $root "blog-web\.env.local"

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

function Set-RunConfigEnvValue {
    param(
        [xml]$ConfigXml,
        [string]$EnvName,
        [string]$EnvValue
    )

    $envsNode = $ConfigXml.component.configuration.envs
    if ($null -eq $envsNode) {
        return
    }

    $envNode = @($envsNode.env) | Where-Object { $_.name -eq $EnvName } | Select-Object -First 1
    if ($null -eq $envNode) {
        $envNode = $ConfigXml.CreateElement("env")
        $null = $envNode.SetAttribute("name", $EnvName)
        $null = $envNode.SetAttribute("value", $EnvValue)
        $null = $envsNode.AppendChild($envNode)
        return
    }

    $null = $envNode.SetAttribute("value", $EnvValue)
}

$envMap = Import-DotEnv -Path $envFile
if ($envMap.Count -eq 0) {
    throw "No runtime environment found at $envFile"
}

$runConfigFiles = Get-ChildItem -Path $runDir -Filter "*.xml" -ErrorAction SilentlyContinue
foreach ($file in $runConfigFiles) {
    [xml]$xml = Get-Content -Path $file.FullName
    if ($file.Name -eq "06_AllServices.run.xml") {
        continue
    }

    if ($envMap.ContainsKey("NACOS_SERVER_ADDR")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "NACOS_SERVER_ADDR" -EnvValue $envMap["NACOS_SERVER_ADDR"]
    }
    if ($envMap.ContainsKey("REDIS_HOST")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "REDIS_HOST" -EnvValue $envMap["REDIS_HOST"]
    }
    if ($envMap.ContainsKey("REDIS_PORT")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "REDIS_PORT" -EnvValue $envMap["REDIS_PORT"]
    }
    if ($envMap.ContainsKey("RABBITMQ_HOST")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "RABBITMQ_HOST" -EnvValue $envMap["RABBITMQ_HOST"]
    }
    if ($envMap.ContainsKey("RABBITMQ_PORT")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "RABBITMQ_PORT" -EnvValue $envMap["RABBITMQ_PORT"]
    }
    if ($envMap.ContainsKey("RABBITMQ_USERNAME")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "RABBITMQ_USERNAME" -EnvValue $envMap["RABBITMQ_USERNAME"]
    }
    if ($envMap.ContainsKey("RABBITMQ_PASSWORD")) {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "RABBITMQ_PASSWORD" -EnvValue $envMap["RABBITMQ_PASSWORD"]
    }
    if ($envMap.ContainsKey("GATEWAY_PORT") -and $file.Name -eq "05_BlogGateway.run.xml") {
        Set-RunConfigEnvValue -ConfigXml $xml -EnvName "GATEWAY_PORT" -EnvValue $envMap["GATEWAY_PORT"]
    }

    $xml.Save($file.FullName)
}

$gatewayPort = if ($envMap.ContainsKey("GATEWAY_PORT")) { $envMap["GATEWAY_PORT"] } else { "18080" }
$webEnvContent = @(
    "VITE_GATEWAY_TARGET=http://127.0.0.1:$gatewayPort"
) -join [Environment]::NewLine
Set-Content -Path $webEnvFile -Value $webEnvContent -Encoding UTF8

Write-Host "Runtime config synchronized from .env" -ForegroundColor Green
Write-Host "  IDEA run configs: $runDir" -ForegroundColor Yellow
Write-Host "  Frontend env file: $webEnvFile" -ForegroundColor Yellow
