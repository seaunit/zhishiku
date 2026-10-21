#!/usr/bin/env bash
set -Eeuo pipefail

fixture="${1:-}"
if [[ -z "$fixture" || ! -f "$fixture" ]]; then
  printf '[preflight] 图片规范化测试夹具不存在：%s\n' "${fixture:-<未提供>}" >&2
  exit 1
fi

if ! command -v vips >/dev/null 2>&1; then
  printf '[preflight] 缺少 libvips；请先安装 libvips-tools。\n' >&2
  exit 1
fi
if ! command -v base64 >/dev/null 2>&1; then
  printf '[preflight] 缺少 base64 命令，无法读取图片测试夹具。\n' >&2
  exit 1
fi

probe_dir="$(mktemp -d)"
cleanup() {
  rm -rf "$probe_dir"
}
trap cleanup EXIT

base64 -d "$fixture" > "$probe_dir/source.png"
if ! vips autorot "$probe_dir/source.png" "$probe_dir/normalized.png[strip]" \
  >/dev/null 2>&1; then
  printf '[preflight] libvips 无法规范化测试图片，请检查安装和运行权限。\n' >&2
  exit 1
fi
if [[ ! -s "$probe_dir/normalized.png" ]]; then
  printf '[preflight] libvips 未生成规范化测试图片。\n' >&2
  exit 1
fi

printf '[preflight] libvips 图片规范化检查通过：%s\n' "$(vips --version)"
