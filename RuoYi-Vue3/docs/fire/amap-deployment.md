# 高德地图接入

已启用高德JS API 2.0深蓝底图，本地运行 yarn dev 即可加载。Java后端和数据库未修改。

本机凭据位于工程根目录 .env.local，该文件已被 *.local 忽略规则排除，不加入源码包。VITE_FIRE_AMAP_KEY是浏览器公开Key；AMAP_SECURITY_JSCODE仅由Vite服务端读取。请勿将安全密钥改为VITE_前缀。

开发与 vite preview 通过 /_AMapService 代理，服务端追加安全密钥；生产静态站点必须配置同路径代理，不能只上传dist就省略代理。按高德官方文档配置Nginx（替换下方占位符，仅保存在服务器）：

```nginx
location /_AMapService/v4/map/styles {
    set $args "$args&jscode=SERVER_SECURITY_CODE";
    proxy_ssl_server_name on;
    proxy_set_header Host webapi.amap.com;
    proxy_pass https://webapi.amap.com/v4/map/styles;
}
location /_AMapService/ {
    set $args "$args&jscode=SERVER_SECURITY_CODE";
    proxy_ssl_server_name on;
    proxy_set_header Host restapi.amap.com;
    proxy_pass https://restapi.amap.com/;
}
```

官方参考：https://lbs.amap.com/api/javascript-api-v2/guide/abc/jscode

2026-10-19浏览器验证：真实道路、地名与高德标识显示正常；点击火警打开右侧详情并定位。场所、网格、警情依旧来自本地演示业务数据，真实底图不表示这些业务数据也已接入真实系统。部署域名需符合该Key在高德控制台中的授权设置。
