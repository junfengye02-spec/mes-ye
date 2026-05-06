# MES 生产级就绪整改计划

更新时间：2026-04-29
当前结论：本地功能联调基本跑通，处于预发候选前半段；不建议直接上生产。
成熟度估计：65%-70%。

## 1. 评估依据

### 已通过的本地验证

- 前端本地页面可打开，登录流程已修复。
- 后端健康检查可返回 UP。
- 前端 `npm run build` 已通过。
- 后端 `mvn -pl mes-admin -am -DskipTests compile` 已通过。
- 基础 API 冒烟：42/42 通过。
- 页面路由冒烟：42/42 通过。
- Playwright 登录 E2E：Chromium 下 3/3 通过。

### 未通过或未闭环的验证

- 后端完整测试门禁未通过：`mes-dispatch` 单模块测试失败。
- 前端生产依赖安全审计未通过：`npm audit --omit=dev --audit-level=high` 报 7 个漏洞。
- 干净数据库初始化、完整迁移、容器化部署、全链路 E2E 尚未一次性闭环验证。
- 监控、告警、备份恢复、压测、灰度发布、回滚流程尚未完成生产级演练。

### 参考基线

- OWASP ASVS：认证、会话、访问控制、日志、安全配置等 Web 应用安全验证基线。
- OWASP Logging Cheat Sheet：生产审计日志需要能定位用户、操作、结果和上下文。
- OWASP Docker Security Cheat Sheet：容器最小权限、非 root、镜像安全等要求。
- Spring Boot Actuator 官方文档：生产端点暴露需要显式控制和鉴权。
- Vite 官方文档：`VITE_*` 环境变量会进入客户端包，不能放服务端秘密。
- Kubernetes Secrets 官方文档：Secret 需要结合加密、RBAC 和外部密钥管理使用。
- GitHub Dependabot Alerts：依赖漏洞需要持续发现、升级和验证。

## 2. P0 阻断项

P0 必须在进入生产或准生产压测前完成。

### P0-01 修复后端测试门禁

现象：

- 命令：`mvn -B -ntp -pl mes-dispatch test -DskipITs`
- 结果：`DispatchModuleTest.testGenerateFromWorkOrder_Success` 失败。
- 失败原因：`DispatchTaskServiceImpl.generateFromWorkOrder` 调用 `TenantContextHolder.requireTenantId()`，测试未设置租户上下文。

涉及文件：

- `mes-backend/mes-dispatch/src/test/java/com/mes/dispatch/DispatchModuleTest.java`
- `mes-backend/mes-dispatch/src/main/java/com/mes/dispatch/service/impl/DispatchTaskServiceImpl.java`

修复动作：

- 在相关单测中显式设置并清理 `TenantContext`。
- 检查非 HTTP 调用链、定时任务、MQ 消费、内部服务调用是否也有租户上下文。
- 重新跑全量后端测试：`mvn -B -ntp test -DskipITs`。
- CI 中保持后端测试为阻断项。

验收标准：

- `mes-dispatch` 单模块测试通过。
- 后端全模块测试通过。
- 不引入跨租户数据写入风险。

### P0-02 升级前端漏洞依赖

现象：

- `npm audit --omit=dev --audit-level=high` 报 7 个漏洞。
- high：`lodash`、`lodash-es`。
- moderate：`axios`、`follow-redirects`、`vue-i18n/@intlify/core-base`、`postcss`。

修复动作：

- 执行受控升级，不直接盲目提交 `npm audit fix --force`。
- 优先升级 `axios`、`vue-i18n`、`postcss`、`lodash/lodash-es` 到修复版本。
- 更新 `package-lock.json`。
- 跑 `npm run build`、Playwright 登录 E2E、核心页面/API 冒烟。

验收标准：

- `npm audit --omit=dev --audit-level=high` 无 high。
- 若 moderate 因上游暂未修复无法清零，需要记录风险接受说明。
- 前端构建和关键 E2E 通过。

### P0-03 验证干净环境一键部署

现状：

- 本地数据库曾手工补过迁移，不能代表生产可复现。
- `docker-compose.yml` 已有 MySQL、Redis、MinIO、后端、前端编排，但还需要用空环境完整验证。

修复动作：

- 新建空库或清理独立测试卷。
- 从 `sql/` 初始化完整 schema 和种子数据。
- 使用生产 profile 启动后端。
- 使用 Docker Compose 或目标 K8s 配置启动全栈。
- 验证登录、菜单、基础数据、工艺模板、工单、派工、质量、异常、物料等核心链路。

验收标准：

- 空环境部署不需要手工改库。
- 所有服务健康检查通过。
- 初始化脚本幂等性和失败恢复策略明确。

### P0-04 生产密钥和配置强校验

现状：

- `ProdEnvValidator` 已经对数据库密码、RabbitMQ 密码、JWT、APS API Key、APS HMAC 做生产校验，这是好的基础。
- 但生产部署仍必须证明所有 secret 都来自 CI/CD 或密钥管理系统，而不是默认值。

修复动作：

- 明确生产 secret 来源：云 KMS、Vault、K8s Secret + etcd encryption，或等效方案。
- 在部署脚本中检查 `SPRING_DATASOURCE_PASSWORD`、`SPRING_RABBITMQ_PASSWORD`、`MES_JWT_SECRET`、`MES_APS_API_KEY`、`MES_APS_HMAC_KEY`。
- 禁止把任何生产 secret 写入 `.env`、文档、镜像层、前端 `VITE_*` 变量。

验收标准：

- prod profile 缺少关键 secret 时应用启动失败。
- 镜像和前端构建产物中不包含生产 secret。
- 密钥轮换流程有文档和演练记录。

## 3. P1 高优先级

P1 应在预发验收前完成。

### P1-01 改造前端 token 存储策略

现状：

- `mes-frontend/src/stores/auth.ts` 使用 `localStorage` 保存 `token` 和 `refreshToken`。
- 一旦发生 XSS，token 容易被窃取。

建议动作：

- 优先改为 HttpOnly、Secure、SameSite Cookie。
- 若短期无法改 Cookie，至少补 CSP、输入输出转义、依赖升级、刷新 token 轮换和异常登录检测。
- 梳理 CSRF 策略；如果改 Cookie，需要重新开启或补充 CSRF 防护。

验收标准：

- refresh token 不再暴露给 JavaScript。
- 登录、刷新、退出、跨标签页状态一致性通过 E2E。

### P1-02 完整 E2E 和业务回归

现状：

- 已跑通登录 E2E 和基础冒烟，但还不是生产级回归。

建议动作：

- 建立测试数据库 seed。
- 覆盖核心链路：登录、权限、租户、物料、工作中心、工艺模板、制造 BOM、工单、派工、质量、异常、库存、APS 同步。
- 在 CI 中增加 E2E job，可先只跑 Chromium，稳定后扩展 WebKit。

验收标准：

- 关键业务 E2E 全绿。
- 失败时保留 trace、screenshot、video。
- E2E 数据可重复执行，不污染开发库。

### P1-03 收紧网关 CORS 和 Actuator 暴露

现状：

- `mes-backend/mes-gateway/src/main/resources/application.yml` 允许 localhost origin，并暴露 `health,info,metrics,prometheus`。
- `allow-credentials: true` 要求生产 origin 更严格。

建议动作：

- prod profile 只允许明确 HTTPS 域名。
- `MES_CORS_ALLOWED_ORIGIN` 为空时 prod 启动失败。
- Actuator metrics/prometheus 仅内网、监控系统或认证用户可访问。
- 网关或 Ingress 层限制 `/actuator/**`。

验收标准：

- 公网无法访问敏感 Actuator 端点。
- 跨域只允许生产域名。

### P1-04 补齐业务引用检查和审计字段

现状：

- 删除物料、工作中心、班组等存在 TODO，需要检查是否被工单、BOM、工序、派工引用。
- 多个日志/审批/状态流转位置仍用 `"system"` 作为操作人。

建议动作：

- 删除操作前增加引用校验。
- 统一使用 `SecurityUtils.getCurrentUsername()` 或审计上下文。
- 对系统任务和人工操作做区分。

验收标准：

- 被引用主数据无法被误删。
- 审计日志能准确定位操作人。

### P1-05 文件存储生产化

现状：

- 默认 `MES_FILE_STORAGE_TYPE=local`。
- MinIO 支持已存在，但生产还需要策略化配置。

建议动作：

- 生产使用 MinIO 集群或云 OSS。
- 配置 bucket 权限、生命周期、备份、跨区容灾。
- 补上传大小、MIME、扩展名、病毒扫描或异步安全扫描策略。

验收标准：

- 业务附件不依赖单机磁盘。
- 文件访问权限和过期策略可控。

## 4. P2 完善项

P2 不一定阻塞预发，但会影响长期维护和生产稳定性。

### P2-01 清理前端 `@ts-nocheck`

现状：

- 多个列表页面使用 `@ts-nocheck`，容易掩盖字段错配。

建议动作：

- 逐页移除 `@ts-nocheck`。
- 为 API DTO、表格行、表单模型补准确类型。
- 增加 `lint` 脚本和 CI 校验。

验收标准：

- 业务页面不依赖 `@ts-nocheck`。
- `npm run build` 和类型检查持续通过。

### P2-02 让安全扫描真正阻断发布

现状：

- `.github/workflows/ci.yml` 中 Trivy 配置 `exit-code: '0'` 且 `continue-on-error: true`，发现高危漏洞也不会失败。

建议动作：

- 对 HIGH/CRITICAL 漏洞设置阻断。
- 增加 npm audit、Maven dependency check、镜像扫描、secret scanning。
- 建立漏洞例外审批机制。

验收标准：

- 未审批的 HIGH/CRITICAL 漏洞不能进入 main/tag 发布。

### P2-03 性能和容量验证

现状：

- 前端构建存在较大 chunk 警告。
- 后端已有部分压测文档，但需要与当前版本重新对齐。

建议动作：

- 前端拆分 `element-vendor` 等大包。
- 后端做登录、列表查询、工单、派工、库存、APS 同步压测。
- 建立容量模型：并发用户、订单量、库存流水量、附件容量、MQ 堆积阈值。

验收标准：

- 有基准压测报告。
- 有明确扩容和降级策略。

### P2-04 运维闭环演练

建议动作：

- 备份恢复演练。
- 灰度发布和回滚演练。
- 日志留存和检索。
- 告警规则：服务不可用、错误率、慢 SQL、MQ 堆积、Redis/MySQL 资源、磁盘容量。

验收标准：

- 至少完成一次演练并记录结果。
- 值班人员能按 runbook 独立处理常见故障。

### P2-05 工作区清理和提交规范

现状：

- 当前工作区有未提交修改。
- `mes-frontend/playwright-report/` 未被 `.gitignore` 忽略。

建议动作：

- 将本次启动和联调修复按主题提交。
- `.gitignore` 增加 `**/playwright-report/` 和 `**/test-results/`。
- 清理生成物后再提交。

验收标准：

- `git status --short` 只剩预期变更。
- 生成物不会误提交。

## 5. 建议推进顺序

### 第 1 阶段：恢复质量门禁

目标：让项目可以稳定进入 CI。

任务：

1. 修复 `mes-dispatch` 租户上下文测试。
2. 跑后端全量单测。
3. 升级前端漏洞依赖。
4. 跑前端构建和登录 E2E。
5. 清理 Playwright 生成物并补 `.gitignore`。

完成标准：

- 后端测试全绿。
- 前端 audit 无 high。
- 构建和登录 E2E 全绿。

### 第 2 阶段：证明干净部署可复现

目标：从空环境启动完整系统。

任务：

1. 清理或新建独立 MySQL/Redis/MinIO 测试环境。
2. 跑完整 SQL 初始化。
3. 用 prod profile 启动后端。
4. 用前端生产构建访问。
5. 执行核心业务冒烟。

完成标准：

- 不手工改库也能启动。
- 健康检查全部通过。
- 核心业务链路通过。

### 第 3 阶段：安全和运维硬化

目标：达到可预发验收状态。

任务：

1. 收紧 CORS、Actuator、JWT、APS、文件上传。
2. 改造 token 存储或补短期防护。
3. 补齐审计字段和引用校验。
4. 建立监控、告警、日志、备份恢复。
5. 完成一次压测和恢复演练。

完成标准：

- 安全配置有生产验收记录。
- 监控和告警可用。
- 备份恢复演练通过。

### 第 4 阶段：生产发布准备

目标：形成可发布版本。

任务：

1. 冻结版本。
2. 生成发布说明和回滚方案。
3. 打 tag，构建镜像。
4. 镜像扫描通过。
5. 预发环境业务验收通过。

完成标准：

- 所有 P0 关闭。
- P1 无未接受风险。
- 发布、回滚、备份、监控均有负责人和文档。

## 6. 生产上线 Go/No-Go 清单

上线前必须全部为 Yes：

- 后端全量测试是否通过？
- 前端构建和 E2E 是否通过？
- `npm audit --omit=dev --audit-level=high` 是否无 high？
- 镜像扫描 HIGH/CRITICAL 是否已清零或审批？
- 空环境部署是否完成过一次？
- 数据库迁移是否可重复执行？
- 生产 secret 是否全部由密钥管理系统注入？
- CORS 是否只允许生产域名？
- Actuator 是否已做内网或鉴权限制？
- 日志、指标、告警是否可用？
- 备份恢复是否演练通过？
- 回滚方案是否验证？
- 核心业务负责人是否完成验收？

## 7. 外部参考

- OWASP ASVS：https://owasp.org/www-project-application-security-verification-standard/
- OWASP Logging Cheat Sheet：https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
- OWASP Docker Security Cheat Sheet：https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html
- Spring Boot Actuator Endpoints：https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Vite Env Variables：https://vite.dev/guide/env-and-mode
- Kubernetes Secrets：https://kubernetes.io/docs/concepts/configuration/secret/
- GitHub Dependabot Alerts：https://docs.github.com/en/code-security/concepts/supply-chain-security/about-dependabot-alerts
