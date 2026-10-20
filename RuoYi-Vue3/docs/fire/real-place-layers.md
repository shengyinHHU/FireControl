# 真实场所地图图层

四类标签通过 fire_place.place_type 匹配：重点企业 industry、高层建筑 highrise、综合商业体 commercial、养老院 elderly。

## 使用

重新启动后端，并启动前端 npm run dev。即使消防其他业务仍是演示模式，场所地图、侧栏、列表、详情也使用真实数据库。先通过若依登录页面登录；资源图层未登录时会提供登录入口。超级管理员可直接查询。普通账号需要 fire:places:list 权限，可执行 SQL/fire_place_map_permission.sql 增量注册，再在角色管理中配置授权和数据范围，重新登录生效。

打开资源图层，点击四类标签添加对应点位，再次点击移除。初始四类均未选中。多选支持叠加，面板显示当前点位数。待核验 review 坐标也显示，不要求先改 confirmed。点位详情复用若依弹窗，并显示真实类别、地址、风险源、经纬度和定位状态。

## 接口

- GET /fire/places/map：仅正常、未删除、有有效GCJ02坐标的四类场所。最多5000，额外标记 truncated。
- GET /fire/places/list：若依分页场所列表，可按 placeType/name 查询，包含未定位记录。
- GET /fire/places/{placeId}：同样权限与数据范围内的只读档案。

Controller → Service（DataScope）→ Mapper XML，沿用若依 token 和 fire:places:list。部门及仅本人数据范围分别按 dept_id、supervisor_user_id 过滤；导入时为空的记录不会自动分配给普通账号。管理员不受范围限制。

API将placeId/gridId序列化为字符串；前端规范化为id/gridId，longitude/latitude映射lng/lat。图标颜色和“企/楼/商/养”区分四类，名称通过textContent显示。

## 真实数据优先

src/api/fire/sourcePolicy.js 明确登记已接入真实数据库的资源，目前为places。空结果表示真实无数据，不拿模拟记录填补；接口错误、无权限或未登录时显示对应错误/登录/重试。大屏场所侧栏和分类统计也由真实列表替换演示值。现有模拟关联人员、器材、视频不拼接到真实档案上。

本次为只读地图接入，尚未实现真实场所新增、编辑、删除；对应入口隐藏，防止写入演示副本。其余尚无真实接口的消防业务保留既有模式，后续接入时继续按真实数据替代模拟数据的规则实施。

## 验证范围

- Java：mvn -o -pl ruoyi-admin -am -DskipTests compile。
- Vue：vite build --mode firemock。
- 单元/集成逻辑：node --test tests/fire/*.test.mjs。
- 浏览器：map-layers.browser.cjs，真实Vue+高德SDK，后端返回使用101条清单坐标的固定HTTP响应；不连接业务数据库。

后端源码已编译。当前离线 package 的 Spring Boot 打包插件依赖未完整缓存，若用jar部署需要先在依赖可用的环境正常完成 Maven package；开发环境可用IDE启动源码。此处不修改若依认证策略或数据库连接配置来绕过环境限制。