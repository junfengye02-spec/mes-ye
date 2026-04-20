# MES 前端全量小功能深度测试报告
任务 ID: mes-frontend-deep-test
审计人: cursor-sr-18
审计范围: 仅审计，未修改任何代码 (files_changed=[])

================================================================
① 基础构建校验
================================================================

环境: Node v22.22.0, npm 11.9.0，pnpm 未安装，使用 npm/npx。
注意: package.json 只定义 dev/build/preview/test:e2e 四个脚本，**无独立 lint 和 type-check 脚本**，也无 ESLint 配置文件；build 已内置 vue-tsc。

vue-tsc --noEmit 结果：
- exit code = 0，**TypeScript 错误 0 条**
- 耗时 ~149 秒

vite build 结果：
- exit code = 0，构建成功，耗时 30 s
- 1827 modules transformed
- 产物目录 dist/，共 200+ chunk (主 entry + element-plus 按需拆分 + 每个路由懒加载)

⚠️ 构建告警 (3 条 warning，均不影响构建):
1. **Chunk size warning**: dist/assets/index-BJVkcysH.js = 1232.88 kB (gzip 400.17 kB)，超过 vite 默认 500 kB 警告阈值。主入口 chunk 塞得过大，建议在 vite.config.ts 用 manualChunks 把 element-plus/axios/pinia/vue-router 等拆到 vendor chunk。
2. `src/stores/permission.ts` 被 `stores/auth.ts` 动态导入，同时被 `layout/MainLayout.vue` / `layout/useActiveMenuList.ts` 静态导入 → dynamic import 失效、不会被切分到独立 chunk。
3. `src/stores/auth.ts` 被 `utils/request.ts` 动态导入 (response 拦截器里 refresh token 用)，但同时被 5 个文件静态导入 → 同问题。
修复建议: request.ts 里的动态 import 可以删除（改回静态 import），因为 auth.ts 在 5 个入口都静态导入过，循环依赖风险可忽略；或者保留动态 import 但把 auth/permission 也手动分到同一个 vendor chunk。

无 lint 工具。若要补齐 lint，推荐添加 eslint + @vue/eslint-config-typescript + eslint-plugin-vue。

================================================================
② 路由与菜单校验
================================================================

**路由合计 47 条**（/login 和 /app/login + /app/* 下 4 条 + 根 / 下 13 个模块路由）。
**所有 component: () => import('@/views/...') 的目标文件均存在**，无 404 路由。

孤儿组件 / 死文件检查 → src/views 目录下 47 个 .vue 文件全部被路由或父组件引用：
- views/platform/TenantDetail.vue 被 TenantList.vue 引用（详情抽屉，非独立路由，正确）
- views/platform/TenantRegisterDialog.vue 被 TenantList.vue 引用（表单对话框，正确）
- 其他 45 个 .vue 文件每个都对应一条路由（通过 1:1 匹配验证）

**路由/菜单一致性问题**:
1. **缺少 404 兜底路由**: router/index.ts 无 `{ path: '/:pathMatch(.*)*', component: NotFound }`。用户直接访问不存在的 URL 时会白屏或停在空 MainLayout。
2. **菜单 path 与路由一一对应**（menuConfig.ts 手工维护的 staticMenuList 的 10 大类 × 叶子菜单 ↔ router/modules/*.ts），比对结果：
   - 菜单项全部能对应到实际路由
   - 实际路由比菜单多：
     * /workorder/detail/:id — 工单详情页（详情入口在列表内，无需菜单）
     * /app/* — 现场端独立入口，不走管理端菜单（由 UserPortalLayout 处理）
     * /platform/tenants — 仅 tenantId=0 的平台超管可见（platformOnly=true），由 useActiveMenuList.ts 的 filterPlatformOnly 过滤，逻辑正确
3. **动态菜单 vs 静态菜单混用**: permission store 加载 sysMenuApi.getUserTree() 得到 SysMenuVO[] 并做 mapToMenuItem。该映射丢弃 SysMenuVO 的 component、permission、sortOrder 等字段，只保留 path/menuName/icon/children，未按 sortOrder 排序——如果后端菜单未按 sortOrder asc 返回，菜单顺序可能与预期不一致。
4. **后端 SysMenuVO.visible=false 的菜单未被前端过滤掉**: permission.ts 只 filter `menuType !== 'B'`（按钮），不看 visible 字段。需要后端自己保证 getUserTree 不返回 visible=false 的条目，否则会出现"隐藏菜单"。
5. **权限过滤维度不足**: router/index.ts 的全局守卫只做 STAFF 重定向 + platformOnly 拦截，不根据用户实际权限拒绝访问某个路由。只要登录即可直接打开 /system/user 等任意页面；后端会返回 403，但前端体验不佳。后端 SysMenu 有 permission 字段、后端 API 有 @PreAuthorize，但前端缺少路由级权限校验（没有 meta.permissions 比对逻辑）。

================================================================
③ API 契约差异（重点）
================================================================

按后端 mes-backend 的 Controller/VO/DTO 依序比对前端 src/api/**, src/types/**：

**🔴 严重不匹配（会导致功能无法使用）**:

### 3.1 完工入库 ReceiptVO / ReceiptDTO ↔ 后端 FinishedGoodsReceiptVO/DTO
后端 FinishedGoodsReceiptController 路由 /material/receipt/{page,create,update,...}，返回 FinishedGoodsReceiptVO / 收 FinishedGoodsReceiptDTO：
  VO 字段: `id, receiptNo, receiptType, warehouse, movementType, planReceiptTime, actualReceiptTime, status, items[], createdBy, createdTime`
  DTO 字段: `receiptNo, receiptType, warehouse, movementType, planReceiptTime, items[FinishedGoodsReceiptItemDTO]`

前端 types/material-mgmt.ts 里的 ReceiptVO / ReceiptDTO:
  VO 字段: `receiptNo, workOrderId, workOrderNo, productCode, productName, receiptQty, qtyUnit, storageLocation, status, remark`
  DTO 字段: `workOrderId, receiptType, receiptQty, qtyUnit, storageLocation, remark`

**问题**:
 - 前端列表（ReceiptList.vue）按 workOrderNo/productCode/productName/receiptQty/qtyUnit/storageLocation 渲染列，这些字段后端根本不返回 → **列全部空白**。
 - 前端新增表单填 workOrderId + receiptQty + qtyUnit + storageLocation + remark 发给后端，后端 DTO 没有这些字段 (Jackson 默认忽略未知) → **实际落库字段全空**，核心的 warehouse/movementType/planReceiptTime/items 前端根本未提供，**功能完全不可用**。
 - 前端缺失 FinishedGoodsReceiptItemVO/DTO (入库明细) 的类型，整个子表功能未实现。

### 3.2 完工入库申请 ReceiptRequestVO / ReceiptRequestDTO ↔ 后端
后端 ReceiptRequestVO: `id, requestNo, receiptType, workOrderId, workOrderNo, projectName, materialCode, materialName, serialNo, qty, qualifiedQty, unqualifiedQty, pendingReceiptQty, status, createdBy, createdTime`
后端 ReceiptRequestDTO: `requestNo, receiptType, workOrderId, workOrderNo, projectName, wbsElement, materialId, materialCode, materialName, serialNo, qty, qualifiedQty, unqualifiedQty, unit, description, planReceiptTime`

前端 ReceiptRequestVO: `requestNo, workOrderId, workOrderNo, productCode, productName, requestQty, qtyUnit, requestType, status, remark`
前端 ReceiptRequestDTO: `workOrderId, requestQty, qtyUnit, requestType, remark`

**问题**（字段命名严重不一致）:
 - 前端 `requestType` ≠ 后端 `receiptType` (不同字段名)
 - 前端 `requestQty` ≠ 后端 `qty`
 - 前端 `qtyUnit` ≠ 后端 `unit`
 - 前端 `remark` ≠ 后端 `description`
 - 后端返回的 materialCode/materialName/serialNo/qualifiedQty/unqualifiedQty/pendingReceiptQty/projectName 前端完全未展示
 - 前端 productCode/productName 字段后端不存在
 - DTO 同样错位 → **申请入库功能无法正确创建**

**🟡 字段缺失/不完整（部分可用）**:

### 3.3 WorkOrderDTO ↔ 后端
后端 DTO 包含 34 个字段 + 7 个子表 list (tasks/inputMaterials/outputMaterials/qualityItems/constraints/supplyPlans)。
前端 types/workorder.ts 的 WorkOrderDTO 只有 16 个字段。

**前端 DTO 缺少字段**:
 - 普通字段: orderNo, mainProduct, machineModel, productCategory, productType, projectName, wbsElement, newOrRepairType, workType, specifiedWorkCenterId, serialNo, specialStockFlag, deliveryLocation (13 个)
 - 子表完全缺失: tasks, inputMaterials, outputMaterials, qualityItems, constraints, supplyPlans (6 个子表)

后果: WorkOrderList.vue 新增 / 编辑表单只提供 15 个字段输入，子表（工作清单、输入物料、输出物料、检验项目、约束、供应计划）完全无法编辑。WorkOrderDetail.vue 能看到所有子表数据（因为 VO 有完整字段），但创建时无法录入——必须靠后端自动生成或靠 APS 同步进来。

### 3.4 ShiftHandoverDTO 字段类型
后端 `handoverWeekday: Integer` (星期几用数字 1-7 表示)，但前端 ShiftHandoverDTO/VO 里 `handoverWeekday?: string`，**类型不一致**。若前端主动填字符串 "Monday" 发给后端，Jackson 会解析失败。虽然当前 ShiftHandoverList.vue 完全不提供该字段 UI，不会马上暴露，但如果未来要加 UI 会踩坑。

### 3.5 ShiftHandoverList 表单只提供 8 个字段输入
后端 ShiftHandoverDTO 有 20 个字段（processContent, handoverWeekday, handoverTime, handoverTeamId, handoverShift, takeoverShift, takeoverTeamId, takeoverTeamName, teamLeader, planQty, actualQty, gapAnalysis, otherMatters 等），前端新增对话框只给出 projectName, productSerialNo, handoverDate, handoverPerson, takeoverPerson, handoverTeamName, handoverContent, status（且 status 在提交前被剔除） 8 个字段，业务核心字段（handoverTeamId, takeoverTeamId, planQty, actualQty, gapAnalysis 等）无法录入。

### 3.6 ApsSyncConfigVO
前端 types/aps.ts 里 ApsSyncConfigVO extends BaseEntity (所以有 createdBy/createdTime/updatedBy/updatedTime 全部)，后端只有 updatedTime（无 createdBy/createdTime/updatedBy）。前端试图读取 createdBy 会永远为 undefined。

**🟢 基本匹配（无问题或可接受）**:

### 3.7 PlatformTenantController.TenantVO ↔ 前端 tenant.ts TenantVO
所有 12 个字段都对齐: id, tenantCode, tenantName, status, schemaMode, dataRegion, quotaUsers, quotaStorageMb, quotaQps, expireAt, contactName, contactEmail, createdTime, updatedTime。✅

### 3.8 SysUserVO / DTO ↔ 前端
基本完全匹配。前端 roles 类型 `{id, roleName, roleCode}[]` 对应后端 SysRoleVO（未完整验证字段，但 UserList.vue 里只用到这 3 个字段，可用）。✅

### 3.9 MaterialVO / DTO ↔ 前端
22 个字段完全匹配 + BaseEntity 的 created/updated 时间字段。✅

### 3.10 OrderPlanVO / DTO ↔ 前端
匹配。前端 VO 多了 completionStatus（后端 VO 也有），apsOrderId/apsSyncBatchId/apsSyncStatus 三字段前端声明，后端 VO 同样包含。✅

### 3.11 DispatchTaskVO / AssignDTO ↔ 前端
基本匹配。DispatchAssignmentVO 前端定义的 assignedTime/assignedBy/revokedBy/revokedTime 后端返回是否一致需运行时验证；assignType/assigneeId/assigneeCode/assigneeName/assignedQty/status 对应。✅

### 3.12 ApsDataMappingVO / DTO ↔ 前端
匹配。✅

### 3.13 ApsSyncStatusVO
后端 /aps/sync/status 返回 Map<String, Object>，key 正好是前端声明的 4 个 key（apsAvailable, circuitBreakerState, pendingUpstreamCount, pendingCompensationCount）。✅

### 3.14 SysMenuVO ↔ 前端
11 个字段完全匹配。✅

### 3.15 AbnormalContactVO/DTO ↔ 后端
AbnormalContactDTO 字段匹配，api 方法全覆盖（submit/process/close/附件增删查签）。✅

### 3.16 request.ts 响应拦截器
- 正确解构 {code, message, data}：code=200 返回 data，否则报错
- 401 带 refresh token 的情况：用队列 (pendingRequests) + isRefreshing 标志防止多次并发刷新 → ✅ 实现正确
- 401 刷新失败 / 刷新接口本身 401 → handleLogout()：清 token，判断 /app 前缀跳 /app/login，否则跳 /login 并保留 redirect → ✅ 实现正确

================================================================
④ 表单校验问题清单（按页面）
================================================================

### 4.1 租户注册 views/platform/TenantRegisterDialog.vue ✅ 校验最完整
- 必填: tenantCode / tenantName
- 正则: tenantCode `/^[a-z][a-z0-9-]{1,63}$/`（对齐后端 Pattern）
- 邮箱: type='email'
- 密码: min 8 / max 128（对齐后端 @Size(min=8)）
- 提交 loading ✅、空字段删除再发送 ✅
- 唯一缺点: 无"保存中"期间的页面遮罩；重复点击依赖 submitting 标志防重（已有，OK）

### 4.2 租户管理列表 views/platform/TenantList.vue ✅
- 关键操作均有二次确认（suspend/resume/archive/reprovision 都 ElMessageBox.confirm）
- 搜索 status 为空时自动 delete → 避免空字符串传给后端 Integer 报 400 ✅

### 4.3 用户新增/编辑 views/system/UserList.vue ⚠️
**问题**:
 - 只校验 username 必填；password / phone / email / accountType / roleIds 均无校验
 - email 无 type='email' 校验；phone 无手机号正则
 - 编辑时 form 里 password 保留空字符串发给后端，后端如果没做"空字符串不更新"处理会把用户密码覆盖成空（需后端配合）
 - **重置密码无二次确认**: `handleResetPassword` 直接调 API，应该 ElMessageBox.confirm 包一层
 - **handleSubmit 无 submitLoading 保护**: 重复点击"确定"按钮会重复发请求
 - 删除 popconfirm 内没 loading 态
 - 角色下拉 `sysRoleApi.list()` 失败无兜底、无错误提示（会静默失败）

### 4.4 工单新增 views/workorder/WorkOrderList.vue ⚠️
- 只校验 workOrderNo 必填
- planStartTime < planEndTime 无约束
- planQty 无上限
- 工单创建无法录入子表（tasks/inputMaterials/outputMaterials/qualityItems/constraints/supplyPlans）——见 3.3
- **状态流转（下发/开工/完工/强制完工）** ✅ 都有 ElMessageBox.confirm 二次确认
- 强制完工 reason 必填 ✅
- submitLoading ✅

### 4.5 计划新增 views/plan/OrderPlanList.vue ⚠️
- 仅查看到前 100 行，校验推测不多（后面的实现需实际运行确认）
- 状态流转按钮（释放/完成/终止）根据 status 显示合理 ✅

### 4.6 生产派工 views/dispatch/DispatchTask.vue 🔴 **最严重**
- **人员 / 设备 / 班组全部是硬编码 mock 数据**：
  ```ts
  const personnelList = ref([{ id: 1, code: 'P001', name: '张三' }, { id:2, code:'P002', name: '李四' }...])
  const deviceList = ref([{ id:1, code:'DEV001', name: '数控机床A' }...])
  const teamList = ref([{ id:1, code:'T001', name: '一班' }...])
  ```
  没有调用 sysUserApi/productionTeamApi 获取真实数据，所以生产派工本质上**只能给这 4 个虚构的 demo 资源做派工**，线上完全不可用
- **Tab 多选功能无用**：selectedPersonnel/selectedDevices/selectedTeams 定义了但从未在任何 handler 被读取 → 死代码
- 打开派工对话框后只能单选（从 `el-select` 选 1 个），和左侧 Table 的多选 UI 不一致
- 撤销派工时 prompt 原因填写有 inputPattern:/\S+/ 校验 ✅

### 4.7 生产领料 / 按物料领料 views/material-mgmt/Requisition*.vue
- (未全量读，接口侧 api/material-mgmt/requisition.ts 和 requisitionOrder.ts 存在，VO/DTO 字段数量合理)

### 4.8 完工入库 views/material-mgmt/ReceiptList.vue 🔴
- 除 3.1 里说的 VO/DTO 跟后端完全错位外，表单只校验 workOrderId/receiptType/receiptQty 必填；qtyUnit/storageLocation 无校验；receiptQty 最小值 0.0001，但最大值未设上限
- 删除 ✅ 二次确认

### 4.9 完工入库申请 views/material-mgmt/ReceiptRequestList.vue 🔴
- 契约错位见 3.2
- 只校验 workOrderId、requestQty 必填
- 删除 ✅ 二次确认

### 4.10 质量异常 views/abnormal/AbnormalContactList.vue ⚠️
- 仅校验 subject 必填；其他 15 个字段全无校验（qty 无上限、discoveryDate 无必填、abnormalDesc 无长度上限）
- **提交 / 处理 / 关闭 操作无二次确认**：handleSubmit / handleProcess / handleClose 直接调 API
- 删除 ✅ 二次确认
- 详情抽屉加载附件的回退逻辑 OK ✅

### 4.11 交班记录 views/quality/ShiftHandoverList.vue ⚠️
- 仅校验 handoverPerson 必填
- UI 提供 status 字段选择器，但提交前剔除（前端伪字段），误导用户以为可以控制状态
- 关键字段（接班人、交接日期、交接班组、交接内容）无必填校验
- **"接收"操作无二次确认**：handleReceive 直接调 API
- query.handoverDate 为空字符串时会发给后端 LocalDate 参数 → 后端可能解析失败 400（未验证是否有空串防护）

### 4.12 BOM 编辑 views/process/ManufacturingBomList.vue ⚠️
- 仅校验 bomCode / bomName 必填
- **BOM 明细（items）仅查看不可编辑**：itemsDialog 只读展示，不能新增/删除/修改明细项
- **发布 / 停用 / 升级版本 无二次确认**：handlePublish/handleDisable/handleUpgrade 直接调 API
- 删除 ✅ 二次确认
- 使用了 `// @ts-nocheck` 关闭了整个组件的 TS 检查，降低了类型安全

### 4.13 物料档案 views/basic/MaterialList.vue ✅ / ⚠️
- 校验 materialCode/materialName 必填
- 编辑时 materialCode :disabled ✅
- 删除二次确认 ✅
- 仍缺字段级格式校验

### 4.14 登录 views/login/Login.vue 和 AppLogin.vue ✅
- 校验 username/password 必填
- 登录 loading ✅
- redirect 回跳有安全校验 (startsWith('/') && !startsWith('//'))
- 多租户: 域名解析 resolveTenantCodeFromHost 或手动输入 tenantCode
- 错误 catch ✅
- 账号类型锁定: ADMIN 端登录用 loginClient: 'ADMIN'；现场端用 'USER'

================================================================
⑤ 列表页功能校验
================================================================

抽样 5 个高频列表：WorkOrderList / OrderPlanList / UserList / MaterialList / AbnormalContactList

### 5.1 分页 ✅
全部使用 el-pagination 双向绑定 query.pageNum/pageSize，page-sizes 默认 [10, 20, 50, 100]，页码/页大小变化触发 loadList，pageNum/pageSize 正确传给后端 query 对象。

### 5.2 搜索/筛选 ✅
- 所有列表均支持关键字 clearable 搜索 + 状态下拉
- 搜索时 handleSearch 统一把 pageNum 重置为 1（防止在 N 页改条件后停留在空页）
- 重置按钮 handleReset 清空所有字段再 fetch

### 5.3 排序 ⚠️
- 所有列表表格都**没有 sortable / sort-change 事件**，即无客户端或服务端排序
- 列表默认按什么排序完全依赖后端实现（实际 PlatformTenantController 是 createdTime DESC）
- 建议：对 createTime/状态字段开启 sort-change 并传给后端

### 5.4 导出 ❌
- 全系统**无导出功能**，无 Excel/CSV 按钮；业务常见的导出报表未实现

### 5.5 批量选择/批量删除 ❌
- 所有列表表格**无 type="selection" 多选列**，无批量操作 bar
- DataTable 组件虽有 `showSelection` prop，但没有任何列表用到

### 5.6 表格状态/加载 ✅
- 统一 v-loading="loading"
- 空状态统一 el-empty "暂无数据"

================================================================
⑥ 认证 / 权限 / 租户切换
================================================================

### 6.1 登录 ✅
- 表单校验、loading、错误提示齐全
- accessToken + refreshToken 都存 localStorage（pinia store 同步）
- 两套入口：/login (ADMIN) + /app/login (USER)

### 6.2 退出登录 ✅
- 清 accessToken/refreshToken/userInfo，重置 permissionStore
- 后端 authApi.logout() 调用（fire-and-forget，失败不影响清理）

### 6.3 401 自动刷新 ✅
- request.ts 拦截器识别 401 → doRefreshToken()，并发请求加入 pendingRequests 队列
- refresh 接口本身 401 或无 refreshToken 时直接 handleLogout，避免死循环
- 刷新成功后重放原请求（携带新 token）

### 6.4 租户切换 ⚠️
- Login 页支持 tenantCode 输入（多租户同名账号时必填）
- 域名自动识别租户（resolveTenantCodeFromHost）
- **但登录成功后无"切换租户"入口**，MainLayout 的用户下拉菜单只有"退出登录"，没有换租户/个人中心等常用项

### 6.5 权限 ⚠️
- 路由守卫只做：登录校验、STAFF 账号锁到 /app/**、platformOnly 拦截
- 菜单: 后端 getUserTree() 返回用户可见菜单 → permissionStore.loadUserMenus → 动态渲染
- **无路由级权限校验**: 即使菜单隐藏，用户手输 URL 仍能打开页面（由后端 403 兜底）
- 按钮级权限: 完全未实现（UserList 的"重置密码""删除"对所有登录用户可见）

================================================================
⑦ 其他细小功能
================================================================

### 7.1 全局错误处理 ⚠️
- 无 `app.config.errorHandler`（main.ts 没设）
- 无 window.onerror / unhandledrejection 监听
- 仅依赖 axios 拦截器统一显示 ElMessage.error
- 组件内 render 错误或 async 未 catch 会进 devtools 的 console，用户无感知
- **建议**: 在 main.ts 加 `app.config.errorHandler = (err) => { console.error(err); ElMessage.error('系统异常') }` 做兜底

### 7.2 国际化 (i18n) ❌
- 无 locales/ 目录，无 vue-i18n 依赖
- 仅引入 element-plus/es/locale/lang/zh-cn 做 EP 自身翻译
- 所有文案均硬编码中文
- 对海外用户 / 多语言客户不友好（如果需求里不要求多语言则 OK）

### 7.3 dayjs / 日期格式化 ⚠️
- 未引入 dayjs
- 时间字符串直接渲染，依赖后端返回的 ISO8601 格式（LocalDateTime 默认 "yyyy-MM-dd'T'HH:mm:ss"）
- 表格里 createdTime/planStartTime 等字段如果后端带 'T' 会很丑
- 建议引入 dayjs 统一 formatDate

### 7.4 数字格式化
- planQty 等 BigDecimal 字段前端直接显示数字，**无千分位分隔符**
- 建议对数量字段统一 toLocaleString('zh-CN')

### 7.5 图标与响应式布局
- 图标使用 @element-plus/icons-vue 全量注册（main.ts 里 for 循环注册所有 icon）→ tree-shaking 失效，浪费 ~50 kB gzip
- 布局只针对桌面端，el-col :span 硬编码，移动端布局可能挤压变形
- 建议按需导入图标、或用 unplugin-icons

### 7.6 按钮 disabled 状态
- 所有表单"确定"按钮都用了 :loading 防重
- 关键按钮根据 status 条件渲染（v-if）而非 disabled，较好
- 删除按钮无"执行中"灰态（删除接口慢时可能重复点击）

### 7.7 Store / Pinia 兜底值
- auth.ts: userInfo 默认 null，accessToken/refreshToken 从 localStorage 读空串
- permission.ts: dynamicMenus 默认 []，loadUserMenus 失败只把 loaded=false，无错误提示
- **permission 加载失败时**: useActiveMenuList 回退到 staticMenuList（源代码写死的 menuList），菜单不会丢 → ✅ 容灾设计合理

### 7.8 keep-alive
- MainLayout router-view 使用 `<keep-alive :max="15">` 缓存 15 个页面
- WorkOrderDetail 做了 watch route.params + onActivated 刷新，处理得当 ✅

================================================================
总结：紧急修复优先级
================================================================

P0（阻塞功能，必须修）:
  1. ReceiptList / ReceiptRequestList 与后端 FinishedGoodsReceipt(Request) 契约完全错位，完工入库和申请功能**目前不可用**
  2. DispatchTask 的人员/设备/班组是硬编码 mock 数据，真实派工**无法使用**
  3. WorkOrderDTO 缺子表 (tasks/inputMaterials/outputMaterials/qualityItems/constraints/supplyPlans)，工单无法从前端创建完整数据

P1（严重 UX 问题，需尽快修）:
  4. ShiftHandover 表单只开放 8/20 字段
  5. 提交/处理/关闭/发布/停用 类关键动作缺二次确认（AbnormalContact 3 个、ShiftHandover 1 个、BOM 3 个、UserList 的重置密码 1 个）
  6. 主 chunk 1.23 MB 未拆分，首屏加载慢
  7. UserList 缺 phone/email/password 校验，重置密码无 loading 保护

P2（优化建议）:
  8. 新增 404 兜底路由
  9. 按需加载 element-plus icon
  10. 引入 dayjs 格式化时间
  11. 补齐 ESLint 配置
  12. 列表增加排序/导出/批量操作
  13. 加 app.config.errorHandler 全局错误兜底
  14. 用户下拉菜单加"修改密码 / 个人中心 / 切换租户"入口
