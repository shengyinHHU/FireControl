#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
LOGIN_USER=${1:-root}
SOURCE_IP=${2:-127.0.0.1}
if [ "$(id -u)" -ne 0 ]; then printf '%s\n' '请使用sudo执行此脚本。' >&2; exit 1; fi
case "$LOGIN_USER" in ''|*[!A-Za-z0-9_.-]*) printf '%s\n' 'SSH用户名格式无效。' >&2; exit 1 ;; esac
case "$SOURCE_IP" in ''|*[!0-9.]*) printf '%s\n' '来源IPv4格式无效。' >&2; exit 1 ;; esac
SSHD=$(command -v sshd || printf /usr/sbin/sshd)
# GatewayPorts=yes overrides the requested loopback bind with a wildcard listener.
# Refuse that configuration instead of silently exposing a camera port.
EFFECTIVE=$("$SSHD" -T -C "user=$LOGIN_USER,host=relay,addr=$SOURCE_IP")
if printf '%s\n' "$EFFECTIVE" | grep -q '^gatewayports yes$'; then
    printf '%s\n' 'GatewayPorts=yes会强制公网监听，未添加公钥。请管理员改为no或clientspecified后重试。' >&2; exit 1
fi
if printf '%s\n' "$EFFECTIVE" | grep -Eq '^(disableforwarding yes|allowtcpforwarding (no|local))$'; then
    printf '%s\n' '当前sshd策略不允许反向TCP转发，未添加公钥。请管理员核对该账号策略。' >&2; exit 1
fi
USER_HOME=$(getent passwd "$LOGIN_USER" | cut -d: -f6)
USER_GROUP=$(id -gn "$LOGIN_USER")
case "$USER_HOME" in /*) ;; *) printf '%s\n' '找不到SSH用户home目录。' >&2; exit 1 ;; esac
SSH_DIR="$USER_HOME/.ssh"
install -d -m 700 -o "$LOGIN_USER" -g "$USER_GROUP" "$SSH_DIR"
AUTH="$SSH_DIR/authorized_keys"
if [ ! -e "$AUTH" ]; then install -m 600 -o "$LOGIN_USER" -g "$USER_GROUP" /dev/null "$AUTH"; fi
LINE=$(cat "$APP_DIR/intercom-authorized-key.txt")
KEY=$(awk '{print $2}' "$APP_DIR/intercom-forwarding.pub")
if grep -F -q -- "$KEY" "$AUTH"; then
    if ! grep -F -x -q -- "$LINE" "$AUTH"; then
        printf '%s\n' '此对讲公钥已存在且限制不同，请核对现有公钥行；未追加重复授权。' >&2; exit 1
    fi
else
    cp -p "$AUTH" "$AUTH.before-fire-intercom"
    printf '\n%s\n' "$LINE" >> "$AUTH"
fi
chown "$LOGIN_USER:$USER_GROUP" "$AUTH"
chmod 600 "$AUTH"
printf '%s\n' '已添加独立对讲公钥，服务器反向监听限制为包内指定的127.0.0.1端口。未修改sshd配置。'
printf '%s\n' '若sshd使用Match Address策略，第二个参数应填写服务器实际观察到的麒麟来源IPv4后复查。'
