[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$runtimeRoot = Join-Path $repoRoot ".runtime\local-test"
$logRoot = Join-Path $runtimeRoot "logs"
$pidFile = Join-Path $runtimeRoot "pids.json"
$envFile = Join-Path $repoRoot ".env.local-test"

function Import-LocalEnvFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
            continue
        }
        $name, $value = $trimmed.Split("=", 2)
        $name = $name.Trim()
        $value = $value.Trim().Trim("'").Trim('"')
        if ($name) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

function Set-DefaultEnv {
    param(
        [string]$Name,
        [string]$Value
    )

    if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }
}

function Assert-PortAvailable {
    param([int]$Port)

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) {
        throw "Port $Port is already in use. Stop the existing process before starting the local test stack."
    }
}

function Wait-Url {
    param(
        [string]$Url,
        [int]$Attempts = 45
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt += 1) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Timed out waiting for $Url"
}

function Wait-CloudflareUrl {
    param(
        [string]$LogPath,
        [int]$Attempts = 30
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt += 1) {
        if (Test-Path -LiteralPath $LogPath) {
            $match = Select-String `
                -LiteralPath $LogPath `
                -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' `
                -AllMatches `
                -ErrorAction SilentlyContinue |
                ForEach-Object { $_.Matches.Value } |
                Select-Object -Last 1
            if ($match) {
                return $match.TrimEnd("/")
            }
        }
        Start-Sleep -Seconds 1
    }
    if (Test-Path -LiteralPath $LogPath) {
        Get-Content -LiteralPath $LogPath -Tail 40
    }
    throw "Timed out waiting for the Cloudflare quick tunnel URL."
}

function Stop-StartedProcesses {
    param([int[]]$ProcessIds)

    foreach ($processId in $ProcessIds) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

if (Test-Path -LiteralPath $pidFile) {
    $existing = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
    $running = @($existing.PSObject.Properties.Value | Where-Object {
        Get-Process -Id $_ -ErrorAction SilentlyContinue
    })
    if ($running.Count -gt 0) {
        throw "The local test stack appears to be running. Run stop.ps1 first."
    }
}

Import-LocalEnvFile -Path $envFile

$env:PGPASSWORD = $env:SHIHAI_DATABASE_PASSWORD
if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = "postgres"
}
$dbHost = "127.0.0.1"
$dbPort = "5432"
$dbUser = $env:SHIHAI_DATABASE_USERNAME
if (-not $dbUser) {
    $dbUser = "postgres"
}
$dbName = "zhishiku_test"
$dbExists = psql -h $dbHost -p $dbPort -U $dbUser -d postgres -Atc "select 1 from pg_database where datname='$dbName'"
if ($dbExists -ne "1") {
    createdb -h $dbHost -p $dbPort -U $dbUser -O $dbUser $dbName
}

Assert-PortAvailable -Port 8082
Assert-PortAvailable -Port 8091
Assert-PortAvailable -Port 4173

New-Item -ItemType Directory -Force $runtimeRoot, $logRoot | Out-Null

$env:SPRING_PROFILES_ACTIVE = "local,local-test"
$env:SHIHAI_APP_ENV = "test"
$env:SHIHAI_BILLING_DB_ENV = "test"
$env:SHIHAI_RUNTIME_ENV = "test"
$env:SHIHAI_BILLING_ENABLED = "true"
$env:SHIHAI_SERVER_PORT = "8082"
$env:SHIHAI_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/zhishiku_test"
$env:SHIHAI_DATABASE_USERNAME = $dbUser
$env:SHIHAI_DATABASE_PASSWORD = $env:PGPASSWORD
$env:SHIHAI_SESSION_COOKIE_SECURE = "false"
$env:SHIHAI_STORAGE_ROOT = (Join-Path $repoRoot ".data\local-test\uploads")
$env:SHIHAI_BROWSER_WORKER_JAR = (Join-Path $repoRoot "backend\kb-browser-worker\target\kb-browser-worker-0.1.0-SNAPSHOT.jar")
$env:SHIHAI_INTERNAL_EVENT_URL = "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events"
$env:SHIHAI_WAFFO_ENV = "test"
$env:SHIHAI_WAFFO_CURRENCY = "CNY"
$env:SHIHAI_WAFFO_BRIDGE_PORT = "8091"
$env:SHIHAI_WAFFO_SUCCESS_URL_ALLOWLIST = "http://127.0.0.1:4173/kb/billing/return"
Set-DefaultEnv "SHIHAI_APP_TO_BRIDGE_SECRET" "local-test-app-to-bridge-secret"
Set-DefaultEnv "SHIHAI_BRIDGE_TO_APP_SECRET" "local-test-bridge-to-app-secret"
Set-DefaultEnv "SHIHAI_WAFFO_STORE_ID" "STO_1234567890123456789012"
Set-DefaultEnv "SHIHAI_WAFFO_MERCHANT_ID" "MER_1234567890123456789012"
Set-DefaultEnv "SHIHAI_WAFFO_PRODUCT_MAP" "kb_pack_9=TEST_PACK_9,kb_pack_29=TEST_PACK_29,kb_pack_99=TEST_PACK_99"
Set-DefaultEnv "SHIHAI_WAFFO_DRY_RUN" "true"

if ($env:SHIHAI_WAFFO_PRIVATE_KEY_FILE) {
    if (-not (Test-Path -LiteralPath $env:SHIHAI_WAFFO_PRIVATE_KEY_FILE)) {
        throw "SHIHAI_WAFFO_PRIVATE_KEY_FILE does not exist."
    }
    $pem = Get-Content -LiteralPath $env:SHIHAI_WAFFO_PRIVATE_KEY_FILE -Raw
    $env:SHIHAI_WAFFO_PRIVATE_KEY_BASE64 = [Convert]::ToBase64String(
        [Text.Encoding]::UTF8.GetBytes($pem)
    )
    if ($env:SHIHAI_WAFFO_DRY_RUN -eq "false") {
        if (-not $env:SHIHAI_WAFFO_PRODUCT_MAP -or $env:SHIHAI_WAFFO_PRODUCT_MAP -like "*TEST_PACK*") {
            throw "Real test checkout requires the real Waffo test product map in .env.local-test."
        }
    }
}

Push-Location (Join-Path $repoRoot "backend")
try {
    & .\mvnw.cmd -q -pl kb-app,kb-browser-worker -am package -DskipTests
} finally {
    Pop-Location
}

Push-Location (Join-Path $repoRoot "bridges\waffo")
try {
    & pnpm.cmd build
} finally {
    Pop-Location
}

$started = @()
try {
    $backend = Start-Process `
        -FilePath "java" `
        -ArgumentList @("-jar", (Join-Path $repoRoot "backend\kb-app\target\kb-app-0.1.0-SNAPSHOT.jar")) `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logRoot "backend.out.log") `
        -RedirectStandardError (Join-Path $logRoot "backend.err.log") `
        -PassThru
    $started += $backend.Id

    $apiHealth = "http://127.0.0.1:8082/kb/api/actuator/health"
    Wait-Url -Url $apiHealth

    $bridge = Start-Process `
        -FilePath "node" `
        -ArgumentList @("dist/server.js") `
        -WorkingDirectory (Join-Path $repoRoot "bridges\waffo") `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logRoot "bridge.out.log") `
        -RedirectStandardError (Join-Path $logRoot "bridge.err.log") `
        -PassThru
    $started += $bridge.Id

    Wait-Url -Url "http://127.0.0.1:8091/health"

    $cloudflared = $null
    if ($env:SHIHAI_WAFFO_DRY_RUN -ne "true") {
        if ($env:SHIHAI_WAFFO_ENV -ne "test") {
            throw "Automatic callback setup is only supported for the Waffo test environment."
        }
        $cloudflaredCommand = Get-Command "cloudflared.exe" -ErrorAction SilentlyContinue
        if (-not $cloudflaredCommand) {
            throw "cloudflared is required for real Waffo test payments. Install it or keep SHIHAI_WAFFO_DRY_RUN=true."
        }
        $cloudflaredLog = Join-Path $logRoot "cloudflared.log"
        Clear-Content -LiteralPath $cloudflaredLog -ErrorAction SilentlyContinue
        $cloudflared = Start-Process `
            -FilePath $cloudflaredCommand.Source `
            -ArgumentList @(
                "tunnel",
                "--no-autoupdate",
                "--url",
                "http://127.0.0.1:8091",
                "--logfile",
                $cloudflaredLog,
                "--loglevel",
                "info"
            ) `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logRoot "cloudflared.out.log") `
            -RedirectStandardError (Join-Path $logRoot "cloudflared.err.log") `
            -PassThru
        $started += $cloudflared.Id
        $tunnelBaseUrl = Wait-CloudflareUrl -LogPath $cloudflaredLog
        Wait-Url -Url "$tunnelBaseUrl/health"
        $env:SHIHAI_WAFFO_TEST_WEBHOOK_URL = "$tunnelBaseUrl/webhooks/waffo"
        Push-Location (Join-Path $repoRoot "bridges\waffo")
        try {
            & node "scripts/configure-test-webhook.mjs"
            if ($LASTEXITCODE -ne 0) {
                throw "Waffo test webhook configuration failed."
            }
        } finally {
            Pop-Location
        }
    }

    $frontend = Start-Process `
        -FilePath "node" `
        -ArgumentList @("node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "4173") `
        -WorkingDirectory (Join-Path $repoRoot "frontend\kb-web") `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logRoot "frontend.out.log") `
        -RedirectStandardError (Join-Path $logRoot "frontend.err.log") `
        -PassThru
    $started += $frontend.Id

    Wait-Url -Url "http://127.0.0.1:4173/kb/login"

    @{
        backend = $backend.Id
        bridge = $bridge.Id
        cloudflared = if ($cloudflared) { $cloudflared.Id } else { $null }
        frontend = $frontend.Id
    } | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding utf8

    Write-Host "Local test environment is ready."
    Write-Host "App: http://127.0.0.1:4173/kb/"
    Write-Host "API: http://127.0.0.1:8082/kb/api/actuator/health"
    Write-Host "Bridge: http://127.0.0.1:8091/health"
    Write-Host "Database: jdbc:postgresql://127.0.0.1:5432/zhishiku_test"
    $waffoMode = if ($env:SHIHAI_WAFFO_DRY_RUN -eq "true") { "dry-run" } else { "test checkout" }
    Write-Host "Waffo: $waffoMode"
    if ($env:SHIHAI_WAFFO_TEST_WEBHOOK_URL) {
        Write-Host "Waffo test webhook: $env:SHIHAI_WAFFO_TEST_WEBHOOK_URL"
    }
    Write-Host "Registration email: local mock; verification code is returned by the local API."
} catch {
    Stop-StartedProcesses -ProcessIds $started
    Write-Host "Local test startup failed. Logs: $logRoot"
    throw
}
