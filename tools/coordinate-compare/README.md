# 高德官方坐标转换与本地算法对照

独立测试工具，不连接业务数据库，不修改项目配置。读取已有119样本时只提取经纬度；官方只接收坐标。

使用Node及Playwright、已安装的Chrome。运行前在进程环境中设置 `AMAP_COMPARE_KEY` 和 `AMAP_COMPARE_SECURITY`，然后执行：

```powershell
node tools/coordinate-compare/compare.cjs
```

密钥不写入代码或报告。每批最多40对坐标，SDK加载25秒超时，每批转换15秒超时，无无限重试；结束关闭浏览器和临时本地HTTP服务。报告位于 `docs/verification/119-coordinate-comparison.{json,csv}`，人工解读见同目录 `.md`。

样本含25个附近WGS84格点、33个顶点附近合成点、33个边线中点附近合成点、132个边线两侧约2米/10米合成点，以及现有result.json中附近最多25条独立坐标。合成点使用本地反算再取6位小数，不是独立测量控制点。误差用球面距离计算，边界距离在大厂附近采用局部米制近似。测试统计不能证明实际定位精度或全局误差上限。

参考：[高德官方JS转换](https://lbs.amap.com/api/javascript-api-v2/guide/transform/convertfrom)、[常见本地公式](https://github.com/hujiulong/gcoord/blob/master/src/crs/GCJ02.ts)。
