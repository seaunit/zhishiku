import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..', '..')
const deploy = readFileSync(resolve(root, 'deploy/scripts/deploy.sh'), 'utf8')
const provision = readFileSync(resolve(root, 'deploy/scripts/provision-ubuntu.sh'), 'utf8')
const failures = []

if (!provision.includes('libvips-tools')) {
  failures.push('provision-ubuntu.sh must install libvips-tools')
}
if (!deploy.includes('systemctl vips')) {
  failures.push('deploy.sh must require the vips command')
}
if (
  !deploy.includes('check-image-normalizer.sh') ||
  !deploy.includes('pixel.png.base64')
) {
  failures.push('deploy.sh must run the real PNG normalization smoke test')
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
