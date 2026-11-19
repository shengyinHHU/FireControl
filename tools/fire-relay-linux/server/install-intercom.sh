#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
DEST=/opt/firecontrol-relay-receiver
if [ "$(id -u)" -ne 0 ] || [ -z "${1:-}" ]; then
    printf '%s\n' '用法：sudo sh install-intercom.sh 后端运行用户名' >&2; exit 1
fi
APP_GROUP=$(id -gn "$1")
install -d -m 750 -o root -g "$APP_GROUP" "$DEST"
if [ -e "$DEST/fire-intercom.properties" ] && ! cmp -s "$APP_DIR/fire-intercom.properties" "$DEST/fire-intercom.properties"; then
    printf '%s\n' '已存在不同的对讲映射配置，请核对后合并；未覆盖现有配置。' >&2; exit 1
fi
install -m 640 -o root -g "$APP_GROUP" "$APP_DIR/fire-intercom.properties" "$DEST/fire-intercom.properties"
printf '%s\n' '对讲映射已安装，未重启后端或修改摄像机绑定。'
printf '%s\n' '更新JAR后，将此配置文件追加到原启动参数spring.config.additional-location中：'
printf '%s\n' 'optional:file:/opt/firecontrol-relay-receiver/fire-intercom.properties'
