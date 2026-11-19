#!/bin/sh
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
if command -v systemctl >/dev/null 2>&1 && systemctl is-active --quiet firecontrol-relay.service; then
    printf '%s\n' '转发后台服务正在运行，不再启动第二个实例。'
    systemctl status firecontrol-relay.service --no-pager --full
    printf '\n%s\n' '查看详细日志：sudo journalctl -u firecontrol-relay -n 50 --no-pager'
    printf '%s' '按回车关闭窗口：'; read -r ignored || true
    exit 0
fi
sh "$APP_DIR/run.sh"
result=$?
if [ "$result" -ne 0 ]; then
    printf '\n%s\n' '启动失败，请检查 config.json、本机10.5.90.50地址、端口占用及网络。'
    printf '%s' '按回车关闭窗口：'; read -r ignored || true
fi
exit "$result"
