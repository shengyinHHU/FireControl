#!/bin/sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
LOGIN_USER=${1:-root}
if [ "$(id -u)" -ne 0 ]; then printf '%s\n' '请使用 sudo sh authorize-ssh.sh [SSH用户名]。' >&2; exit 1; fi
USER_HOME=$(getent passwd "$LOGIN_USER" | cut -d: -f6)
USER_GROUP=$(id -gn "$LOGIN_USER")
case "$USER_HOME" in /*) ;; *) printf '%s\n' '找不到有效的用户home目录。' >&2; exit 1 ;; esac
SSH_DIR="$USER_HOME/.ssh"
install -d -m 700 -o "$LOGIN_USER" -g "$USER_GROUP" "$SSH_DIR"
AUTH="$SSH_DIR/authorized_keys"
if [ ! -e "$AUTH" ]; then install -m 600 -o "$LOGIN_USER" -g "$USER_GROUP" /dev/null "$AUTH"; fi
# Add one narrowly restricted forwarding key; preserve all existing login keys.
KEY=$(cat "$APP_DIR/relay-forwarding.pub")
if ! grep -F -q -- "$KEY" "$AUTH"; then
    printf '\nrestrict,port-forwarding,permitopen="127.0.0.1:22000",command="/bin/false" %s\n' "$KEY" >> "$AUTH"
fi
chown "$LOGIN_USER:$USER_GROUP" "$AUTH"
chmod 600 "$AUTH"
printf '已为 %s 添加仅允许转发到127.0.0.1:22000的独立公钥，未修改sshd配置。\n' "$LOGIN_USER"
printf '%s\n' '若使用的用户名不是root，请同步修改麒麟config.json中production的ssh.username。'
