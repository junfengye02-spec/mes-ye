# MES 制造执行系统

## 项目简介

MES（Manufacturing Execution System，制造执行系统）是一套面向离散制造行业的生产管理平台，覆盖从订单计划、生产工单、派工调度、质量管控到物料管理的完整生产执行流程。系统采用前后端分离架构，前端基于 Vue 3 + TypeScript + Element Plus，后端基于 Spring Boot 3 + MyBatis Plus，支持多模块业务扩展和 APS 外部系统集成。

## 技术栈

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | ^3.4.0 | 渐进式前端框架（Composition API + `<script setup>`） |
| TypeScript | ^5.4.0 | 类型安全 |
| Vite | ^5.4.0 | 构建工具与开发服务器 |
| Vue Router | ^4.3.0 | 路由管理（History 模式） |
| Pinia | ^2.1.0 | 状态管理 |
| Element Plus | ^2.7.0 | UI 组件库 |
| Axios | ^1.7.0 | HTTP 客户端 |
| Playwright | ^1.58.2 | E2E 端到端测试 |
| unplugin-auto-import | ^0.17.0 | Vue/Router/Pinia API 自动引入 |
| unplugin-vue-components | ^0.27.0 | Element Plus 组件按需自动导入 |

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| Java | 17 | 运行环境 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0.33 | 关系型数据库 |
| Druid | 1.2.21 | 数据库连接池与监控 |
| Spring Security | (Boot 内置) | 安全框架 |
| JWT (jjwt) | 0.12.5 | 令牌认证 |
| Redis | (Boot 内置) | 缓存与会话管理 |
| Knife4j | 4.3.0 | OpenAPI 3 接口文档 |
| Hutool | 5.8.25 | Java 工具类库 |
| MapStruct | 1.5.5.Final | 对象映射（DTO/VO/Entity 转换） |
| EasyExcel | 3.3.3 | Excel 导入导出 |
| Lombok | (Boot 内置) | 消除样板代码 |

## 项目结构

```
mes/
├── docs/                          # 产品需求与设计文档
│   ├── 06-生产工单模块/
│   ├── 07-生产派工模块/
│   ├── 08-计划管理模块/
│   ├── 09-异常联络单管理模块/
│   ├── 10-成品质量管理模块/
│   ├── 11-工作查询模块/
│   ├── 12-物料管理模块/
│   ├── 13-APS集成模块/
│   └── 14-实施计划.md
│
├── mes-frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                   # 按业务域组织的 API 模块
│   │   │   ├── basic/             # 基础数据（物料、物料价格、工作中心）
│   │   │   ├── team/              # 班组管理
│   │   │   ├── process/           # 工艺管理（工艺信息、模板、指导书、BOM等）
│   │   │   ├── plan/              # 计划管理（订单计划、生产计划）
│   │   │   ├── workorder/         # 工单管理
│   │   │   ├── dispatch/          # 派工管理
│   │   │   ├── abnormal/          # 异常管理
│   │   │   ├── quality/           # 质量管理
│   │   │   ├── query/             # 工作查询
│   │   │   ├── material-mgmt/     # 物料管理
│   │   │   ├── aps/               # APS 集成
│   │   │   └── system/            # 系统管理（用户、角色、菜单、认证）
│   │   ├── components/            # 公共组件
│   │   │   ├── DataTable/         # 通用数据表格（带分页）
│   │   │   ├── SearchForm/        # 通用搜索表单
│   │   │   ├── BatchEdit/         # 批量编辑对话框
│   │   │   ├── FileUpload/        # 文件上传组件
│   │   │   └── ImportDialog/      # 数据导入对话框（拖拽上传+模板下载）
│   │   ├── layout/                # 页面布局
│   │   │   ├── MainLayout.vue     # 主布局（侧边栏+头部+内容区）
│   │   │   ├── SidebarMenu.vue    # 侧边导航菜单
│   │   │   ├── BreadcrumbNav.vue  # 面包屑导航
│   │   │   └── menuConfig.ts      # 静态菜单配置
│   │   ├── router/                # 路由配置
│   │   │   ├── index.ts           # 路由入口与全局守卫
│   │   │   └── modules/           # 按业务模块拆分的路由
│   │   ├── stores/                # Pinia 状态管理
│   │   │   ├── auth.ts            # 认证状态（登录、登出、Token 管理）
│   │   │   └── permission.ts      # 权限状态（动态菜单）
│   │   ├── types/                 # TypeScript 类型定义
│   │   ├── utils/                 # 工具函数
│   │   │   ├── request.ts         # Axios 封装（拦截器、Token 刷新）
│   │   │   └── dict.ts            # 数据字典工具
│   │   └── views/                 # 页面组件（按业务域组织）
│   ├── .env.development           # 开发环境变量
│   ├── .env.production            # 生产环境变量
│   ├── vite.config.ts             # Vite 配置
│   └── tsconfig.json              # TypeScript 配置
│
├── mes-backend/                    # 后端项目（Maven 多模块）
│   ├── pom.xml                    # 父 POM（统一依赖版本管理）
│   ├── mes-common/                # 公共模块（基础类、异常、工具）
│   ├── mes-framework/             # 框架模块（MyBatis、Web、Security、Redis、JWT）
│   ├── mes-basic/                 # 基础数据模块
│   ├── mes-team/                  # 班组管理模块
│   ├── mes-process/               # 工艺管理模块
│   ├── mes-plan/                  # 计划管理模块
│   ├── mes-workorder/             # 工单管理模块
│   ├── mes-dispatch/              # 派工管理模块
│   ├── mes-abnormal/              # 异常联络单模块
│   ├── mes-quality/               # 质量管理模块
│   ├── mes-query/                 # 工作查询模块
│   ├── mes-material/              # 物料管理模块
│   ├── mes-aps/                   # APS 集成模块
│   └── mes-admin/                 # 启动模块（聚合所有业务模块）
│
└── sql/                            # 数据库脚本
    ├── V1.00__basic_data.sql      # 基础数据表（两位序号保证 Docker init 字典序正确）
    ├── V1.01__team_management.sql # 班组管理表
    ├── V1.02__process_management.sql
    ├── V1.03__plan_management.sql
    ├── V1.04__work_order.sql
    ├── V1.05__dispatch.sql
    ├── V1.06__abnormal_contact.sql
    ├── V1.07__quality_management.sql
    ├── V1.08__work_query.sql
    ├── V1.09__material_management.sql
    ├── V1.10__aps_integration.sql
    ├── V1.11__auth_rbac.sql       # 用户/角色/菜单/RBAC
    ├── V1.12__add_missing_deleted_columns.sql
    └── seed_test_data.sql         # 测试数据
```

## 功能模块

### 1. 基础数据管理
- **物料管理**：物料编码、名称、规格、单位、分类等基础信息维护
- **物料价格管理**：物料对应价格信息维护
- **工作中心管理**：生产车间/工位信息维护

### 2. 班组管理
- 生产班组信息维护、班组成员管理

### 3. 工艺管理
- **工艺信息管理**：工艺路线定义与维护
- **工艺模板管理**：标准化工艺模板
- **工艺指导书管理**：作业指导文档
- **作业指导书管理**：现场操作指导
- **喷涂条件管理**：喷涂工艺参数配置
- **加工程序管理**：数控加工程序管理
- **制造 BOM 管理**：产品制造 BOM 维护（支持草稿/发布/停用状态流转）

### 4. 计划管理
- **订单计划管理**：客户订单转化为生产订单，支持创建 → 下达 → 完成 → 终止的状态流转
- **生产计划管理**：生产排产计划制定与下达

### 5. 生产工单管理
- 工单创建、下发、开工、完工、强制完工的完整生命周期管理
- 工单详情包含：工作清单、输入物料、输出物料、检验项目、约束关系、供应计划、文档附件等 7 大子页签

### 6. 生产派工管理
- 左右分栏布局：左侧资源选择（人员/设备/班组三维度），右侧任务列表
- 支持按人员派工、按设备派工、按班组派工
- 派工明细查看与撤销（需填写撤销原因）

### 7. 异常联络单管理
- 生产异常上报、流转与关闭（草稿 → 已提交 → 处理中 → 已关闭）

### 8. 质量管理
- **复检申请管理**：产品复检流程
- **开工检查管理**：工序开工前检查
- **订单开工检查**：订单级别开工检查
- **交接班管理**：班次交接信息记录

### 9. 工作查询模块
- 工作状态查询、生产工作查询、检验工作查询、开工检查查询、交接班查询、工单查询、派工查询等 8 个维度查询视图

### 10. 物料管理
- **库存管理**：物料库存台账查询
- **领料管理**：领料单与领料申请
- **入库管理**：入库请求与入库操作
- **退料管理**：生产退料处理
- **配送签收管理**：物料配送签收确认

### 11. APS 集成模块
- **同步配置管理**：与外部 APS 系统的数据同步策略配置
- **同步日志管理**：同步执行记录与状态追踪
- **数据映射管理**：MES 与 APS 系统间的数据字段映射

### 12. 系统管理
- **用户管理**：系统用户 CRUD
- **角色管理**：RBAC 角色定义与权限分配
- **菜单管理**：动态菜单树管理（支持目录/菜单/按钮三种类型）

## 环境要求

| 依赖 | 版本要求 |
|------|----------|
| Node.js | >= 18.0 |
| Java | 17 |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Maven | 3.8+ |

## 快速启动

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE mes DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 按顺序执行 SQL 脚本
mysql -u root -p mes < sql/V1.00__basic_data.sql
mysql -u root -p mes < sql/V1.01__team_management.sql
# ... 依次执行到 V1.12
mysql -u root -p mes < sql/V1.11__auth_rbac.sql
mysql -u root -p mes < sql/V1.12__add_missing_deleted_columns.sql

# 导入测试数据（可选）
mysql -u root -p mes < sql/seed_test_data.sql
```

### 2. 启动后端

```bash
cd mes-backend

# 修改数据库/Redis 连接信息
# 编辑 mes-admin/src/main/resources/application-dev.yml

# 编译并启动
mvn clean install -DskipTests
cd mes-admin
mvn spring-boot:run
```

后端启动后访问：
- API 地址：`http://localhost:9091/api`
- 接口文档：`http://localhost:9091/api/doc.html`（Knife4j）
- Druid 监控：`http://localhost:9091/api/druid`

### 3. 启动前端

```bash
cd mes-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端开发服务器启动后访问：`http://localhost:3000`

开发环境下，Vite 会自动将 `/api` 路径代理到 `http://localhost:9091`。

### 4. 其他命令

```bash
# 前端构建生产版本
npm run build

# 前端预览生产构建
npm run preview

# 运行 E2E 测试
npm run test:e2e

# 运行 E2E 测试（UI 模式）
npm run test:e2e:ui
```

## 后端模块结构

每个业务模块遵循统一的分层结构：

```
mes-{module}/
├── src/main/java/com/mes/{module}/
│   ├── controller/        # REST 控制器
│   ├── domain/
│   │   ├── dto/           # 数据传输对象（接收前端参数）
│   │   ├── entity/        # 数据库实体
│   │   ├── query/         # 查询参数对象
│   │   └── vo/            # 视图对象（返回前端数据）
│   ├── enums/             # 枚举定义
│   ├── mapper/            # MyBatis Mapper 接口
│   └── service/           # 业务逻辑层
└── src/main/resources/mapper/{module}/  # MyBatis XML 映射文件
```

## 前端公共组件

| 组件 | 说明 |
|------|------|
| `DataTable` | 通用数据表格，封装 `el-table` + `el-pagination`，支持序号列、多选列、分页事件 |
| `SearchForm` | 通用搜索表单，封装查询/重置逻辑，重置时保留分页参数并自动触发搜索 |
| `BatchEdit` | 批量编辑对话框，支持对选中行进行批量修改 |
| `FileUpload` | 文件上传组件，自动携带 Token、支持文件大小限制与目录指定 |
| `ImportDialog` | 数据导入对话框，支持拖拽上传 xlsx/xls/csv 文件，提供模板下载与错误回显 |

## 认证与鉴权

- 认证方式：JWT（Access Token + Refresh Token 双令牌机制）
- Access Token 有效期：2 小时
- Refresh Token 有效期：7 天
- 401 时自动刷新令牌，刷新期间并发请求会排队等待，刷新完成后统一重试
- 前端路由守卫拦截未登录用户，自动跳转登录页并记录来源路径
- 后端基于 Spring Security + RBAC（用户-角色-菜单）模型
- 前端动态菜单：登录后从后端获取用户菜单树，动态渲染侧边栏

## 配置说明

### 后端配置（application.yml）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 9091 | 服务端口 |
| `server.servlet.context-path` | /api | 接口路径前缀 |
| `spring.datasource.url` | localhost:3306/mes | MySQL 连接 |
| `spring.data.redis.host` | localhost | Redis 地址 |
| `mes.jwt.access-token-expire` | 2h | Access Token 过期时间 |
| `mes.jwt.refresh-token-expire` | 7d | Refresh Token 过期时间 |
| `mes.file.upload-path` | ./uploads | 文件上传存储路径 |

### 前端环境变量

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | API 基础路径（默认 `/api`） |
| `VITE_APP_TITLE` | 系统标题 |
