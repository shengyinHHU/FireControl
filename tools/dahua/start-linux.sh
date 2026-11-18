#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
native_dir="${FIRE_DAHUA_NATIVE_DIR:-$project_root/RuoYi-Vue-master/vendor/dahua-netsdk/native/linux64}"
app_jar="${FIRE_APP_JAR:-$project_root/RuoYi-Vue-master/ruoyi-admin/target/ruoyi-admin.jar}"
if [[ "$(uname -m)" != x86_64 ]]; then echo '当前SDK需要Linux x86_64；ARM64需替换官方SDK。' >&2; exit 1; fi
if [[ ! -f "$native_dir/libdhnetsdk.so" || ! -f "$app_jar" ]]; then echo '请配置FIRE_DAHUA_NATIVE_DIR和FIRE_APP_JAR。' >&2; exit 1; fi
: "${FIRE_DAHUA_PASSWORD:?请通过服务环境设置摄像机密码FIRE_DAHUA_PASSWORD}"
export LD_LIBRARY_PATH="$native_dir${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec java -Dspring.devtools.restart.enabled=false -jar "$app_jar" "$@"
