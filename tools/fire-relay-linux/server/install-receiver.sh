#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
DEST=/opt/firecontrol-relay-receiver
if [ "$(id -u)" -ne 0 ] || [ -z "${1:-}" ]; then
    printf '%s\n' '用法：sudo sh install-receiver.sh 后端运行用户名，例如 root 或 firecontrol' >&2; exit 1
fi
APP_USER=$1
APP_GROUP=$(id -gn "$APP_USER")
install -d -m 750 -o root -g "$APP_GROUP" "$DEST"
for name in receiver.p12 keystore-password.secret relay.secret; do
    if [ -e "$DEST/$name" ] && ! cmp -s "$DEST/$name" "$APP_DIR/receiver-private/$name"; then
        printf '已有接收端配对文件不同，停止安装：%s\n' "$DEST/$name" >&2; exit 1
    fi
done
for name in receiver.p12 keystore-password.secret relay.secret; do
    install -m 640 -o root -g "$APP_GROUP" "$APP_DIR/receiver-private/$name" "$DEST/$name"
done
cat > "$DEST/fire-relay-local.properties" <<EOF
fire.relay.enabled=true
fire.relay.bind-address=127.0.0.1
fire.relay.port=22000
fire.relay.node-id=site-01
fire.relay.allowed-source-cidrs=10.5.89.0/24,10.5.90.0/24
fire.relay.key-store=$DEST/receiver.p12
fire.relay.key-store-password-file=$DEST/keystore-password.secret
fire.relay.secret-file=$DEST/relay.secret
EOF
chown root:"$APP_GROUP" "$DEST/fire-relay-local.properties"
chmod 640 "$DEST/fire-relay-local.properties"
printf '%s\n' '接收端配对配置已安装，未替换JAR或重启生产服务。'
printf '%s\n' '更新JAR后，给原启动命令增加程序参数（放在JAR文件名后）：'
printf '%s\n' '--spring.config.additional-location=optional:file:/opt/firecontrol-relay-receiver/fire-relay-local.properties'
printf '%s\n' '若原命令已有spring.config.additional-location，请将上面的文件追加到原值，用逗号分隔。'
