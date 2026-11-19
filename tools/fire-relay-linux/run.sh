#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
case "$(uname -s):$(uname -m)" in
    Linux:aarch64|Linux:arm64) ;;
    *) printf '%s\n' '此部署包用于麒麟Linux ARM64/aarch64，当前系统或CPU架构不匹配。' >&2; exit 1 ;;
esac
if [ ! -x "$APP_DIR/fire-relay" ]; then
    printf '%s\n' '缺少可执行文件或执行权限。请在Linux用 tar -xzf 解压tar.gz部署包。' >&2
    exit 1
fi
exec "$APP_DIR/fire-relay" --config "$APP_DIR/config.json" "$@"
