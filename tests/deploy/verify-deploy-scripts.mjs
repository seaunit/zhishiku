import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..', '..')
const deploy = readFileSync(resolve(root, 'deploy/scripts/deploy.sh'), 'utf8')
const provision = readFileSync(resolve(root, 'deploy/scripts/provision-ubuntu.sh'), 'utf8')
const nginx = readFileSync(resolve(root, 'deploy/nginx/zhishiku-kb.conf'), 'utf8')
const failures = []

if (!provision.includes('libvips-tools')) {
  failures.push('provision-ubuntu.sh must install libvips-tools')
}
if (!/for command in[^;]*\bvips\b[^;]*; do/.test(deploy)) {
  failures.push('deploy.sh must require the vips command')
}
if (!/for command in[^;]*\btesseract\b[^;]*; do/.test(deploy)) {
  failures.push('deploy.sh must require the tesseract command')
}
if (!deploy.includes('chi_sim') || !deploy.includes('eng')) {
  failures.push('deploy.sh must require Tesseract chi_sim and eng language data')
}
if (!deploy.includes('kb-browser-worker') || !deploy.includes('--install-browser')) {
  failures.push('deploy.sh must build and install the Playwright browser worker')
}
if (!deploy.includes('PLAYWRIGHT_BROWSERS_PATH')) {
  failures.push('deploy.sh must configure the Playwright browser path')
}
if (!provision.includes("KB_WEB_IMPORT_ALLOW_UNBILLED_AI='false'")) {
  failures.push('production provisioning must keep unbilled webpage AI disabled')
}
if (!/location \^~ \/kb\/api\/ \{[\s\S]*client_max_body_size 26m;/.test(nginx)) {
  failures.push('the /kb/api location must allow 26 MiB document uploads')
}
if (
  !deploy.includes('check-image-normalizer.sh') ||
  !deploy.includes('pixel.png.base64')
) {
  failures.push('deploy.sh must run the real PNG normalization smoke test')
}
if (
  !deploy.includes('bridges/waffo') ||
  !deploy.includes('zhishiku-waffo-bridge.service') ||
  !deploy.includes('http://127.0.0.1:8091/health')
) {
  failures.push('deploy.sh must build, install, start, and health-check the Waffo bridge')
}
if (
  !deploy.includes('waffo-bridge/node_modules') ||
  !deploy.includes('waffo-bridge/scripts') ||
  !deploy.includes('waffo-bridge/package.json')
) {
  failures.push('deploy.sh must ship Waffo runtime dependencies and operator scripts')
}
if (
  !nginx.includes('location = /kb/pay/webhooks/waffo') ||
  !nginx.includes('proxy_pass http://127.0.0.1:8091/webhooks/waffo')
) {
  failures.push('Nginx must route only the exact zhishiku Waffo webhook to the bridge')
}
if (!provision.includes('waffo.env') || !provision.includes('Waffo 私钥 Base64')) {
  failures.push('provision-ubuntu.sh must interactively configure the isolated Waffo environment')
}
if (
  !provision.includes('KB_RUNTIME_ENV') ||
  !provision.includes('WAFFO_ENVIRONMENT') ||
  provision.includes('WAFFO_ENV=production')
) {
  failures.push('provision-ubuntu.sh must keep billing and Waffo environments aligned')
}
if (!provision.includes('Waffo 店铺 ID')) {
  failures.push('provision-ubuntu.sh must preserve the Waffo store id for catalog setup')
}
if (
  !provision.includes('Existing WAFFO_ENV') ||
  !provision.includes('Existing BILLING_DB_ENV')
) {
  failures.push('provision-ubuntu.sh must reject conflicting existing billing environments')
}
if (
  !deploy.includes('ensure_environment_consistency') ||
  !deploy.includes('read_waffo_env_value') ||
  !deploy.includes('WAFFO_ENV and APP_ENV')
) {
  failures.push('deploy.sh must verify APP_ENV, BILLING_DB_ENV, and WAFFO_ENV before release')
}
if (!provision.includes("BILLING_ENABLED_VALUE=\"${BILLING_ENABLED:-false}\"")) {
  failures.push('production billing must require an explicit BILLING_ENABLED choice')
}
if (!provision.includes('BILLING_ADMIN_EMAILS=')) {
  failures.push('production provisioning must configure at least one billing administrator')
}
if (!provision.includes('APP_TO_BRIDGE_SECRET and BRIDGE_TO_APP_SECRET must be different')) {
  failures.push('provision-ubuntu.sh must reject identical internal bridge secrets')
}
if (!deploy.includes('pg_dump') || !deploy.includes('predeploy-$RELEASE_ID.sql.gz')) {
  failures.push('deploy.sh must create a PostgreSQL backup before switching releases')
}
if (
  !deploy.includes('BRIDGE_UNIT_BACKUP') ||
  !deploy.includes('NGINX_SNIPPET_BACKUP') ||
  !deploy.includes('systemctl restart zhishiku-waffo-bridge.service')
) {
  failures.push('deploy.sh rollback must restore bridge and Nginx snippet configuration')
}

const unsafeNetworkPipe = /curl[^\n]*(?:\\\s*\n\s*)?[^\n]*\|[^\n]*grep\s+-q/
if (unsafeNetworkPipe.test(deploy)) {
  failures.push('deploy.sh must not pipe curl into grep -q under pipefail')
}

if (failures.length > 0) {
  console.error(failures.map((failure) => `FAIL: ${failure}`).join('\n'))
  process.exit(1)
}

console.log('Deploy script dependency and network checks passed.')
