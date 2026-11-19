#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
case "${1:-}" in
    development|production) ;;
    *) printf '%s\n' '用法：sh switch-target.sh development 或 sh switch-target.sh production' >&2; exit 1 ;;
esac
sh "$APP_DIR/run.sh" --select-target "$1"
printf '%s\n' '手动运行：先关闭旧实例，再执行 sh run.sh。'
printf '%s\n' '开机服务：在 /opt/firecontrol-relay 中切换后，执行 sudo systemctl restart firecontrol-relay。'
