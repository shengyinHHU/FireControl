#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
if command -v xdg-user-dir >/dev/null 2>&1; then
    DESKTOP_DIR=$(xdg-user-dir DESKTOP)
else
    DESKTOP_DIR="$HOME/Desktop"
fi
mkdir -p "$DESKTOP_DIR"
# Desktop Entry Exec uses its own quoting rules; no shell interpolation or eval.
ESCAPED_SCRIPT=$(printf '%s' "$APP_DIR/launch.sh" | sed 's/\\/\\\\/g; s/"/\\"/g; s/`/\\`/g; s/\$/\\$/g; s/%/%%/g')
ENTRY="$DESKTOP_DIR/FireControl-Relay.desktop"
cat > "$ENTRY" <<EOF
[Desktop Entry]
Type=Application
Name=用传设备转发
Comment=启动转发程序或查看后台运行状态
Exec=/bin/sh "$ESCAPED_SCRIPT"
Icon=network-transmit-receive
Terminal=true
Categories=Network;
StartupNotify=false
EOF
chmod 755 "$ENTRY"
if command -v gio >/dev/null 2>&1; then
    gio set "$ENTRY" metadata::trusted true 2>/dev/null || true
fi
printf '已创建桌面图标：%s\n' "$ENTRY"
printf '%s\n' '桌面环境首次提示时，请选择“允许启动/信任”。随后可双击运行。'
