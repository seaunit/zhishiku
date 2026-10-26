param(
  [string]$ProxyContainer = "shihai-dify-api-proxy"
)

$ErrorActionPreference = "Stop"

$existing = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ProxyContainer }
if (-not $existing) {
  Write-Output "Dify local API proxy is not running."
  exit 0
}

docker rm -f $ProxyContainer | Out-Null
Write-Output "Dify local API proxy removed."
