param(
  [string]$DifyNginxContainer = "docker-nginx-1",
  [string]$ProxyContainer = "shihai-dify-api-proxy",
  [int]$Port = 8502
)

$ErrorActionPreference = "Stop"

if (-not (docker inspect $DifyNginxContainer 2>$null)) {
  throw "Dify Nginx container not found: $DifyNginxContainer"
}

$existing = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ProxyContainer }
if ($existing) {
  docker rm -f $ProxyContainer | Out-Null
}

docker run -d `
  --name $ProxyContainer `
  --restart unless-stopped `
  --network docker_default `
  --volumes-from $DifyNginxContainer `
  -e CERTBOT_DOMAIN=your_domain.com `
  -e NGINX_PROXY_READ_TIMEOUT=3600s `
  -e NGINX_SSL_PORT=443 `
  -e NGINX_WORKER_PROCESSES=auto `
  -e NGINX_SERVER_NAME=_ `
  -e NGINX_KEEPALIVE_TIMEOUT=65 `
  -e NGINX_CLIENT_MAX_BODY_SIZE=100M `
  -e NGINX_PROXY_SEND_TIMEOUT=3600s `
  -e NGINX_HTTPS_ENABLED=false `
  -e NGINX_PORT=80 `
  -e NGINX_SSL_CERT_KEY_FILENAME=dify.key `
  -e NGINX_ENABLE_CERTBOT_CHALLENGE=false `
  -e NGINX_SSL_PROTOCOLS="TLSv1.2 TLSv1.3" `
  -e NGINX_SSL_CERT_FILENAME=dify.crt `
  -p "127.0.0.1:${Port}:80" `
  --entrypoint sh `
  nginx:latest `
  -c "cp /docker-entrypoint-mount.sh /docker-entrypoint.sh && sed -i 's/\r`$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh && exec /docker-entrypoint.sh" | Out-Null

Start-Sleep -Seconds 3

$status = docker inspect $ProxyContainer --format "{{.State.Status}}"
if ($status -ne "running") {
  docker logs --tail 50 $ProxyContainer
  throw "Dify local API proxy failed to start."
}

curl.exe -4 --noproxy "*" -fsS --max-time 10 "http://127.0.0.1:${Port}/console/api/setup" | Out-Null
Write-Output "Dify console: http://localhost:${Port}"
Write-Output "Dify local API: http://127.0.0.1:${Port}/v1"
