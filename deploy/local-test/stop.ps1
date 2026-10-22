[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pidFile = Join-Path $repoRoot ".runtime\local-test\pids.json"

if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host "No local test PID file found."
    exit 0
}

$pids = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
$signatures = @{
    backend = "kb-app-0.1.0-SNAPSHOT.jar"
    bridge = "dist/server.js"
    cloudflared = "cloudflared"
    frontend = "node_modules/vite/bin/vite.js"
}
foreach ($property in $pids.PSObject.Properties) {
    $processId = [int]$property.Value
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
    if (-not $process) {
        continue
    }
    $signature = $signatures[$property.Name]
    if (-not $signature -or -not $process.CommandLine -or -not $process.CommandLine.Contains($signature)) {
        throw "Refusing to stop PID $processId because it does not belong to this workspace."
    }
    Stop-Process -Id $processId -Force
}

Remove-Item -LiteralPath $pidFile
Write-Host "Local test environment stopped."
