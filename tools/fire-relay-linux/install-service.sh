#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
DEST=/opt/firecontrol-relay
UNIT=/etc/systemd/system/firecontrol-relay.service
if [ "$(id -u)" -ne 0 ]; then
    printf '%s\n' '请执行 sudo sh install-service.sh；此脚本只安装程序及开机服务，不修改网络或防火墙。' >&2
    exit 1
fi
if ! command -v systemctl >/dev/null 2>&1; then
    printf '%s\n' '当前系统没有systemd，请改用 sh run.sh 手动运行。' >&2; exit 1
fi
if [ "${1:-}" = '--remove' ]; then
    systemctl disable --now firecontrol-relay.service 2>/dev/null || true
    rm -f /etc/systemd/system/firecontrol-relay.service
    systemctl daemon-reload
    printf '%s\n' '已移除开机服务；/opt/firecontrol-relay 中的程序、配置、密钥和日志均保留。'
    exit 0
fi
sh "$APP_DIR/run.sh" --check-config
if [ -e "$DEST/config.json" ] && [ ! -f "$DEST/relay.secret" ]; then
    printf '%s\n' '已有安装缺少relay.secret，停止安装；请先核对原配置。' >&2; exit 1
fi
if [ -e "$DEST/relay.secret" ] && [ ! -f "$DEST/config.json" ]; then
    printf '%s\n' '已有安装缺少config.json，停止安装；请先核对原配置。' >&2; exit 1
fi
if ! getent group fire-relay >/dev/null 2>&1; then groupadd --system fire-relay; fi
if ! id fire-relay >/dev/null 2>&1; then
    NOLOGIN=$(command -v nologin || printf '/bin/false')
    useradd --system --gid fire-relay --home-dir "$DEST" --shell "$NOLOGIN" fire-relay
fi
install -d -m 755 -o root -g root "$DEST"
if [ "$APP_DIR" != "$DEST" ]; then
    install -m 755 -o root -g root "$APP_DIR/fire-relay" "$DEST/fire-relay.new"
    mv -f "$DEST/fire-relay.new" "$DEST/fire-relay"
    for name in run.sh launch.sh create-desktop.sh install-service.sh; do
        install -m 755 -o root -g root "$APP_DIR/$name" "$DEST/$name"
    done
    if [ -f "$APP_DIR/switch-target.sh" ]; then
        install -m 755 -o root -g root "$APP_DIR/switch-target.sh" "$DEST/switch-target.sh"
    fi
    install -m 644 -o root -g root "$APP_DIR/README.md" "$DEST/README.md"
    install -m 644 -o root -g root "$APP_DIR/firecontrol-relay.service" "$DEST/firecontrol-relay.service"
fi
if [ ! -e "$DEST/config.json" ]; then
    install -m 640 -o root -g fire-relay "$APP_DIR/config.json" "$DEST/config.json"
    install -m 640 -o root -g fire-relay "$APP_DIR/relay.secret" "$DEST/relay.secret"
else
    printf '%s\n' '已保留原有config.json和relay.secret，不覆盖配对信息。'
fi
chown root:fire-relay "$DEST/config.json" "$DEST/relay.secret"
chmod 640 "$DEST/config.json" "$DEST/relay.secret"
if [ "$APP_DIR" != "$DEST" ] && [ -f "$APP_DIR/production-relay.secret" ]; then
    if [ -e "$DEST/production-relay.secret" ] && ! cmp -s "$APP_DIR/production-relay.secret" "$DEST/production-relay.secret"; then
        printf '%s\n' '已有生产密钥与新包不同，停止升级，避免覆盖现有生产配对。' >&2; exit 1
    fi
    install -m 640 -o root -g fire-relay "$APP_DIR/production-relay.secret" "$DEST/production-relay.secret"
    install -d -m 750 -o root -g fire-relay "$DEST/ssh"
    for name in production_ed25519 production_known_hosts; do
        if [ -e "$DEST/ssh/$name" ] && ! cmp -s "$APP_DIR/ssh/$name" "$DEST/ssh/$name"; then
            printf '已有SSH文件不同，停止升级：%s\n' "$DEST/ssh/$name" >&2; exit 1
        fi
        if [ "$name" = production_ed25519 ]; then
            install -m 400 -o fire-relay -g fire-relay "$APP_DIR/ssh/$name" "$DEST/ssh/$name"
        else
            install -m 640 -o root -g fire-relay "$APP_DIR/ssh/$name" "$DEST/ssh/$name"
        fi
    done
    if [ -f "$APP_DIR/ssh/intercom_ed25519" ]; then
        if [ -e "$DEST/ssh/intercom_ed25519" ] && ! cmp -s "$APP_DIR/ssh/intercom_ed25519" "$DEST/ssh/intercom_ed25519"; then
            printf '%s\n' '已有对讲SSH私钥不同，停止升级，未覆盖对讲身份。' >&2; exit 1
        fi
        install -m 400 -o fire-relay -g fire-relay "$APP_DIR/ssh/intercom_ed25519" "$DEST/ssh/intercom_ed25519"
    fi
    "$DEST/fire-relay" --config "$DEST/config.json" --import-targets "$APP_DIR/config.json"
fi
install -d -m 700 -o fire-relay -g fire-relay "$DEST/logs"
if command -v runuser >/dev/null 2>&1; then
    runuser -u fire-relay -- "$DEST/fire-relay" --config "$DEST/config.json" --check-config
else
    printf '%s\n' '缺少runuser，无法验证后台账号读取配置；请补齐系统工具后重试。' >&2; exit 1
fi
install -m 644 -o root -g root "$APP_DIR/firecontrol-relay.service" "$UNIT"
systemctl daemon-reload
systemctl enable firecontrol-relay.service
systemctl restart firecontrol-relay.service
systemctl status firecontrol-relay.service --no-pager --full
printf '\n%s\n' '服务已安装。查看日志：sudo journalctl -u firecontrol-relay -n 50 --no-pager'
printf '%s\n' '安装桌面状态图标（以普通用户执行）：sh /opt/firecontrol-relay/create-desktop.sh'
