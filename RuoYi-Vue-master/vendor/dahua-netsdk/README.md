# 大华 NetSDK 项目运行依赖

已接入 ruoyi-system Maven 资源与普通依赖。SDK按需初始化，未配置设备或未开始对讲时不登录摄像机。

| 路径 | 用途 |
| --- | --- |
| maven/com/dahua/netsdk-api/3.060.0000003/ | 可随仓库构建的文件Maven仓库，含SDK接口JAR及POM |
| java/dahua-netsdk-api-3.060.0000003.jar | 原始提取接口包，与Maven仓库副本相同 |
| java/jna-5.13.0.jar | 厂商原始JNA配套备份；应用统一使用已有OSHI依赖的jna-jpms 5.19.1，避免重复类 |
| native/linux64/ | 官方Linux x86-64的7个动态库 |
| native/win64/ | 官方Windows x86-64的11个动态库 |
| config/dynamic-lib-load.xml | 官方资源加载配置 |
| ReadMe_Linux64.txt、ReadMe_Win64.txt | 两份SDK原始README |
| manifest.json | 原始复制文件的来源、长度与SHA-256 |

Java接口包从已测试的官方Windows demo JAR提取，class内容保持不变。包含com/netsdk/lib及其ToolKits所需ErrorCode、PaintPanel、LoginModule支持类；业务不调用demo全局登录类。Windows/Linux两套3653份Java接口源码逐文件一致。原始150MB完整demo JAR未整体引入。

config与native资源由Maven放入ruoyi-system运行JAR，LibraryLoad按操作系统提取到fire.video.dahua.extract-directory/process-PID目录。Linux部署还应复制native/linux64作为外部依赖目录，并在启动Java前设置LD_LIBRARY_PATH；不覆盖系统SSL库。全部原生文件约201MiB，包含双平台配套，不适用于ARM64。

Windows资源加载与CLIENT_Init/CLIENT_Cleanup已验证通过，没有登录摄像机或发送音频。Linux库无法在Windows执行，需在目标服务器检查uname -m、ldd和现场网络后联调。

完整步骤：[部署说明](../../../docs/dahua-netsdk-deployment.md)。本目录没有摄像机密码、录音或本机DPAPI文件。
