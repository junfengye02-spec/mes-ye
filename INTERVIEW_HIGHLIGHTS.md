# MES 制造执行系统 — 面试难点与亮点

## 一、项目概述（面试开场介绍）

> 这是一套面向离散制造行业的 MES（制造执行系统），覆盖订单计划、生产工单、派工调度、质量管控、物料管理以及 APS 外部系统集成等完整生产执行流程。前端基于 Vue 3 + TypeScript + Element Plus，后端基于 Spring Boot 3 + MyBatis Plus 的多模块架构，共 14 个 Maven 模块，12+ 个业务子系统，40+ 个页面。

---

## 二、架构设计亮点

### 2.1 后端多模块分层架构

**亮点描述：**

后端采用 Maven 多模块架构，将 14 个模块按职责严格分层：

```
mes-common   → 基础类、异常、工具（零业务依赖）
mes-framework → 技术框架（MyBatis配置、Security、Redis、JWT）
mes-basic / mes-team / mes-process / ...  → 各业务模块（互不依赖）
mes-admin    → 启动模块（聚合所有业务模块）
```

**面试话术：**

> 后端采用 Maven 多模块设计，`mes-common` 提供基础类和异常定义，`mes-framework` 封装技术基础设施（MyBatis Plus 配置、Spring Security 安全框架、Redis 缓存、JWT 认证），各业务模块（工单、派工、质量等）只依赖 `common` 和 `framework`，业务模块之间互不耦合。`mes-admin` 作为启动模块聚合所有依赖。这种设计使得每个模块职责清晰，可以独立开发测试，也为后续拆分微服务提供了基础。

**可能追问：**

- Q：为什么不直接用微服务？
- A：当前阶段业务量不需要分布式部署，多模块单体是最经济的选择。模块间通过依赖管理隔离，如果将来需要拆分，每个模块已经具备独立的 Controller/Service/Mapper 分层，迁移成本很低。

---

### 2.2 前端领域驱动的目录组织

**亮点描述：**

前端 API 层、路由、页面组件都按业务域（basic/team/process/workorder/dispatch/quality/material-mgmt/aps/system）组织，而非按技术类型组织，与后端模块一一对应。

```
api/
├── basic/          → 对应 mes-basic 模块
├── workorder/      → 对应 mes-workorder 模块
├── dispatch/       → 对应 mes-dispatch 模块
└── system/         → 对应用户/角色/菜单
```

**面试话术：**

> 前端的 API、路由、视图都按业务域而非技术类型来组织。例如 `api/workorder/`、`router/modules/workorder.ts`、`views/workorder/` 三者对应后端的 `mes-workorder` 模块。这种方式让前后端开发人员基于同一业务语言协作，添加新模块时只需在对应域下新增文件，不会影响其他模块。

---

### 2.3 统一的后端分层模型

**亮点描述：**

每个后端业务模块都严格遵循 Controller → Service → Mapper 三层架构，数据模型分为四层：

| 层 | 类型 | 职责 |
|----|------|------|
| Controller | DTO (Data Transfer Object) | 接收前端请求参数 |
| Controller | Query | 封装分页查询条件 |
| Service | Entity | 数据库表映射实体 |
| Controller | VO (View Object) | 返回给前端的视图对象 |

使用 MapStruct 实现 DTO ↔ Entity ↔ VO 的自动映射，避免手动 get/set 代码。

**面试话术：**

> 我们严格区分了 DTO、Entity、VO、Query 四种数据模型。DTO 用于接收前端参数，Entity 映射数据库表结构，VO 作为返回给前端的视图对象，Query 封装分页和过滤条件。模型间的转换通过 MapStruct 编译期生成代码自动完成，既避免了手动 BeanUtils.copyProperties 的运行时反射开销，也能在编译时发现字段不匹配的问题。

---

## 三、核心技术难点

### 3.1 JWT 双令牌 + 无感刷新机制（高频考点）

**难点描述：**

系统采用 Access Token + Refresh Token 双令牌认证方案，Access Token 有效期 2 小时，Refresh Token 有效期 7 天。核心难点在于：当 Access Token 过期（401）时，需要自动用 Refresh Token 换取新令牌，同时保证并发请求不会重复刷新。

**实现要点：**

```
1. 请求拦截器：每个请求自动携带 Authorization: Bearer <token>
2. 响应拦截器检测 401：
   ├── 如果没有 Refresh Token → 直接跳转登录页
   ├── 如果已经在刷新中（isRefreshing = true）→ 将当前请求加入 pendingRequests 队列
   └── 如果未在刷新 → 标记 isRefreshing = true，调用刷新接口
       ├── 刷新成功 → 更新令牌，逐个执行 pendingRequests 队列中的回调
       └── 刷新失败 → 清除登录状态，跳转登录页
3. _retried 标记防止无限重试循环
```

**关键代码逻辑（request.ts 中）：**

- `isRefreshing` 全局标志位确保同一时刻只有一个刷新请求
- `pendingRequests` 数组作为请求队列，存储等待令牌刷新的回调函数
- `_retried` 标记在请求 config 上，防止刷新后重试再次触发刷新

**面试话术：**

> 我们实现了无感 Token 刷新机制。当任意请求收到 401 响应时，拦截器会检查是否已有刷新操作进行中。如果没有，就用 Refresh Token 去换取新的 Access Token；如果正在刷新，就把当前请求放入等待队列。刷新完成后，队列中所有请求会带上新 Token 统一重试。同时在每个请求的 config 上打 `_retried` 标记防止无限循环。这个方案的关键是用 `isRefreshing` 作为互斥锁 + `pendingRequests` 作为请求缓冲队列，本质上是一个简化版的"令牌锁"模式。

**可能追问：**

- Q：如果刷新 Token 也过期了怎么办？
- A：catch 中直接清除本地存储并跳转登录页，让用户重新登录。
- Q：为什么不用拦截器而用 Pinia Store 来做刷新？
- A：Store 中也有一层 `refreshPromise` 防重机制，双重保证。动态 `import()` Store 是为了避免循环依赖（request.ts 和 store 互相引用）。

---

### 3.2 动态菜单与权限控制

**难点描述：**

系统菜单不是写死的，而是登录后从后端接口 `/system/menu/user-tree` 动态获取，根据用户角色返回不同的菜单树。

**实现要点：**

```
1. 用户登录成功后，MainLayout.vue 的 onMounted 中调用 permissionStore.loadUserMenus()
2. 后端返回树形菜单数据（包含 menuType: D-目录 / M-菜单 / B-按钮）
3. 前端过滤掉 menuType === 'B' 的按钮节点，只保留目录和菜单
4. 递归将后端 SysMenuVO 映射为前端 MenuItem 结构
5. SidebarMenu 组件通过 useActiveMenuList() 优先使用动态菜单，降级为静态菜单
```

**面试话术：**

> 系统菜单支持动态配置。后端基于 RBAC 模型，用户 → 角色 → 菜单三层关联，登录后调用接口返回当前用户有权限访问的菜单树。前端在 Permission Store 中缓存菜单数据，过滤掉按钮类型节点后递归映射为前端路由结构。侧边栏优先渲染动态菜单，当接口异常时降级为前端静态配置，保证系统可用性。

**可能追问：**

- Q：菜单和路由是怎么对应的？
- A：前端路由是静态注册的（全部写在 router/modules 里），动态菜单只控制侧边栏的显示/隐藏。没有动态添加路由（addRoute），这样更简单可靠，避免了刷新丢失路由等问题。
- Q：按钮级权限怎么做？
- A：后端菜单表中 menuType='B' 的节点代表按钮权限，当前前端尚未实现 `v-permission` 指令，后续可扩展。

---

### 3.3 Axios 请求封装与统一错误处理

**难点描述：**

对 Axios 进行了二次封装，统一处理请求头、响应格式、错误提示和 Token 管理。

**实现要点：**

```typescript
// 统一响应格式
interface R<T> {
  code: number    // 200 表示成功
  message: string
  data: T
}

// 请求封装提供 5 种方法
request.get<T>(url, params)
request.post<T>(url, data)
request.put<T>(url, data)
request.delete<T>(url, params)
request.upload<T>(url, file, directory)
```

**设计决策：**
- 响应拦截器直接返回 `res.data`（解包），业务代码不需要写 `.data.data`
- 非 200 状态码统一 `ElMessage.error` 提示
- 文件上传自动切换 `Content-Type` 为 `multipart/form-data`
- 泛型 `<T>` 支持让调用方获得完整类型推断

**面试话术：**

> 我封装了一个 Axios 工具模块，在请求拦截器中自动注入 JWT Token，在响应拦截器中统一处理业务状态码和 HTTP 错误。对于 401 错误会触发 Token 刷新机制，其他错误统一用 Element Plus 的 Message 组件提示。封装了 get/post/put/delete/upload 五个方法，响应拦截器中直接解包 `data` 字段，结合 TypeScript 泛型让调用方获得完整的类型推断。

---

### 3.4 工单状态机与生命周期管理

**难点描述：**

生产工单是 MES 系统的核心实体，具有复杂的状态流转逻辑：

```
已创建(CREATED) → 已下发(RELEASED) → 执行中(IN_PROGRESS) → 已完工(COMPLETED)
                                                           → 强制完工(FORCE_COMPLETED)
                                    → 已关闭(CLOSED)
```

**实现要点：**
- 前端使用字典工具 `dict.ts` 统一管理状态枚举，每个状态对应不同的 Tag 颜色（info/primary/warning/success/danger）
- 工单详情页使用 Tabs 组件展示 7 个子维度：工作清单、输入物料、输出物料、检验项目、约束关系、供应计划、文档附件
- 不同状态下操作按钮的可见性不同（如"已创建"只能"下发"，"执行中"可以"完工"或"强制完工"）
- 后端通过状态模式或条件判断确保状态流转的合法性

**面试话术：**

> 生产工单有 6 种状态，从创建到下发、执行、完工形成完整的生命周期。前端通过数据字典工具统一管理状态的显示文本和颜色标签，工单详情页用 Tab 切换展示 7 个维度的子数据。后端在状态变更时做合法性校验，比如只有"已下发"的工单才能"开工"，防止非法状态跳转。

---

### 3.5 生产派工的多维度资源分配

**难点描述：**

派工模块是系统中交互最复杂的页面，采用左右分栏布局：

```
┌─────────────────┬──────────────────────────┐
│   资源选择区     │     派工任务列表           │
│ ┌─────────────┐ │ ┌──────────────────────┐ │
│ │ [人员] [设备]│ │ │ 查询表单              │ │
│ │ [班组]      │ │ │ 任务表格              │ │
│ │             │ │ │ [派人员][派设备][派班组]│ │
│ │  资源表格    │ │ │ [查看派工]            │ │
│ └─────────────┘ │ └──────────────────────┘ │
└─────────────────┴──────────────────────────┘
```

**实现要点：**
- 左侧资源区通过 Tabs 切换人员/设备/班组三种资源类型，各自独立的 selection 状态
- 右侧任务列表支持按任务号、工单号、状态筛选
- 每个任务行有 4 种操作：派人员、派设备、派班组、查看派工
- 撤销派工需要通过 `ElMessageBox.prompt` 弹窗强制输入撤销原因
- 多个 Dialog 管理（派人员/派设备/派班组/派工明细/撤销确认）

**面试话术：**

> 派工页面是系统中交互最复杂的模块。左右分栏布局，左侧是资源选择区，通过 Tab 切换人员、设备、班组三种资源维度；右侧是派工任务列表。每个任务可以分别指派人员、设备或班组。撤销派工时使用 `ElMessageBox.prompt` 强制用户填写原因，保证操作可追溯。页面同时管理 5 个 Dialog 的状态，通过合理的响应式变量命名和职责划分保持代码可维护性。

---

### 3.6 通用组件抽象与复用

**难点描述：**

系统有 40+ 个 CRUD 页面，如果每个页面都写完整的表格、分页、搜索、导入逻辑，代码重复度极高。

**解决方案 — 抽象 5 个公共组件：**

| 组件 | 核心设计 |
|------|----------|
| **DataTable** | 封装 `el-table` + `el-pagination`，通过 props 控制序号列、多选列、分页显示，通过 slot 注入列定义和工具栏，emit `page-change` 事件 |
| **SearchForm** | 封装 `el-form` + 查询/重置按钮，重置时遍历 `modelValue` 清空字段但保留 `pageNum`/`pageSize`，然后自动触发 `search` 事件 |
| **ImportDialog** | 拖拽上传 + 模板下载 + 错误回显，通过 `defineExpose` 暴露 `setErrors` 方法供父组件传入后端校验错误 |
| **FileUpload** | 自动携带 Token、支持文件大小限制与上传目录 |
| **BatchEdit** | 批量编辑弹窗，对选中行应用统一修改 |

**面试话术：**

> 项目有 40 多个 CRUD 页面，我抽象了 5 个公共组件来减少重复。其中 `DataTable` 封装了表格和分页，通过 slot 注入列定义保持灵活性；`SearchForm` 封装了查询/重置逻辑，重置时智能保留分页参数；`ImportDialog` 实现了拖拽上传+模板下载+后端校验错误回显，通过 `defineExpose` 暴露方法让父组件可以注入服务端返回的具体行级错误。这些组件让新增一个 CRUD 页面的开发时间从 2 小时缩短到约 30 分钟。

**可能追问：**

- Q：SearchForm 重置时为什么保留 pageNum 和 pageSize？
- A：因为搜索条件和分页参数通常放在同一个 reactive 对象中传给 API，重置后 pageNum 应该回到 1，而 pageSize 应该保持用户之前的选择。如果不保留，会导致分页混乱。
- Q：ImportDialog 的错误怎么从后端传到组件内部？
- A：使用 `defineExpose` 暴露 `setErrors` 方法，父组件调用 import API 后拿到错误信息，通过 ref 调用 `importDialogRef.value.setErrors(errors)` 将错误注入。

---

### 3.7 前端路由守卫与登录跳转

**难点描述：**

需要实现：未登录用户访问任何页面自动跳转到登录页，登录后跳回原始访问路径。

**实现要点：**

```
router.beforeEach:
├── 目标是公开路径（/login）→ 已登录则跳转首页，未登录放行
└── 目标是受保护路径 →
    ├── 有 Token → 放行
    └── 无 Token → 跳转 /login?redirect=原始路径
```

同时在 request.ts 的 `handleLogout` 函数中也做了同样的逻辑：401 且无法刷新时，携带当前路径跳转到登录页。

**面试话术：**

> 路由守卫通过 `beforeEach` 钩子在每次导航前检查用户登录状态。未登录时会将目标路径编码后作为 `redirect` 参数传给登录页，登录成功后从 query 中读取并跳转回原始页面。已登录用户访问登录页则直接重定向到首页，避免重复登录。

---

### 3.8 keep-alive 路由缓存策略

**难点描述：**

系统有大量的列表页面，用户从列表进入详情页再返回时，期望保留之前的搜索条件、分页状态和滚动位置。

**实现要点：**

```html
<router-view v-slot="{ Component }">
  <transition name="fade" mode="out-in">
    <keep-alive :max="15">
      <component :is="Component" />
    </keep-alive>
  </transition>
</router-view>
```

- 使用 `keep-alive` 缓存路由组件，设置 `max="15"` 限制最大缓存数量（LRU 淘汰策略）
- 配合 `transition` 组件实现页面切换的淡入淡出动画

**面试话术：**

> 我在主布局的 `router-view` 外层包裹了 `keep-alive`，并设置了 `max=15` 的 LRU 缓存上限。这样用户从列表页进入详情再返回时，列表的搜索条件和分页状态都会保留。`max` 限制防止缓存过多组件导致内存溢出，超过上限时自动淘汰最久未访问的组件。

---

## 四、数据库设计亮点

### 4.1 Flyway 风格的版本化数据库迁移

SQL 脚本采用 `V{版本号}__{描述}.sql` 的 Flyway 命名规范：

```
V1.0__basic_data.sql          → 基础数据表
V1.1__team_management.sql     → 班组管理
V1.2__process_management.sql  → 工艺管理
...
V1.11__auth_rbac.sql          → RBAC 权限表
V1.12__add_missing_deleted_columns.sql → 补丁迁移
```

**面试话术：**

> 数据库脚本采用 Flyway 风格的版本化命名，从 V1.0 到 V1.12 共 12 个迁移文件，每个文件对应一个业务模块。这种方式确保了数据库变更的可追溯性和可重复执行性，新成员只需按版本号顺序执行即可初始化完整数据库。

### 4.2 逻辑删除设计

数据库表采用逻辑删除策略（`deleted` 字段），MyBatis Plus 全局配置逻辑删除，查询时自动拼接 `WHERE deleted = 0`，删除操作实际执行 `UPDATE SET deleted = 1`。

---

## 五、工程化亮点

### 5.1 Vite 构建优化

```typescript
// 自动导入 — 减少大量 import 语句
AutoImport({ imports: ['vue', 'vue-router', 'pinia'] })

// Element Plus 组件按需自动注册 — 减少打包体积
Components({ resolvers: [ElementPlusResolver()] })

// 路径别名 — @/ 映射到 src/
resolve: { alias: { '@': resolve(__dirname, 'src') } }
```

### 5.2 路由懒加载

所有页面组件都使用 `import()` 动态导入，实现按需加载：

```typescript
component: () => import('@/views/workorder/WorkOrderList.vue')
```

配合 Vite 构建，每个路由对应一个独立的 chunk，首屏只加载必要资源。

### 5.3 TypeScript 严格模式

- `tsconfig.json` 开启 `strict: true`
- 完整的类型定义：API 返回类型、组件 Props/Emits 类型、Store 状态类型
- `withDefaults` + `defineProps<T>()` 的组合实现类型安全的 Props 默认值

### 5.4 E2E 测试

集成 Playwright 进行端到端测试：

```bash
npm run test:e2e        # 运行测试
npm run test:e2e:ui     # UI 模式调试
```

---

## 六、性能优化点

| 优化项 | 具体做法 |
|--------|----------|
| 路由懒加载 | 所有页面 `() => import()` 动态导入，按需加载 |
| 组件缓存 | `keep-alive` + `max=15` LRU 策略 |
| 组件按需导入 | Element Plus 通过 unplugin 自动按需注册 |
| API 自动导入 | Vue/Router/Pinia API 编译时自动引入 |
| 图标 shallowRef | 登录页等图标组件使用 `shallowRef` 减少不必要的深层响应式 |
| 分页查询 | 所有列表接口均支持分页，避免一次加载全量数据 |
| Token 本地持久化 | 登录状态存 `localStorage`，刷新页面不丢失 |
| 路由切换动画 | `transition` + `mode="out-in"` 平滑过渡 |

---

## 七、可能的面试追问及回答

### Q1：为什么选择 Vue 3 + Composition API 而不是 Options API？

> Composition API 在逻辑复用和代码组织方面更有优势。Options API 在复杂组件中容易出现"选项碎片化"问题——同一个功能的响应式数据、计算属性、方法分散在不同选项中。Composition API 允许按功能而非选项类型组织代码，配合 `<script setup>` 语法糖减少了大量模板代码。TypeScript 支持也更好，类型推断更自然。

### Q2：为什么用 Pinia 而不是 Vuex？

> Pinia 是 Vue 3 官方推荐的状态管理方案。相比 Vuex 4，它去掉了 mutations 的概念，减少了样板代码；支持 setup store 写法，与 Composition API 风格一致；对 TypeScript 的类型推断更友好，不需要额外的类型声明；同时体积更小。

### Q3：Token 刷新时并发请求是怎么处理的？

> 核心是 `isRefreshing` 全局标志 + `pendingRequests` 请求队列。第一个触发 401 的请求负责发起 refresh 并设置 `isRefreshing = true`，后续请求发现正在刷新就不再发起新的 refresh，而是把自己包装成 Promise 推入队列等待。refresh 成功后遍历队列，给每个等待中的请求注入新 Token 并重发。这是一个经典的"请求合并"（request deduplication）模式。

### Q4：组件间通信用了哪些方式？

> - Props/Emits：父子组件间的标准通信
> - Pinia Store：全局状态共享（auth、permission）
> - defineExpose：子组件暴露方法（如 ImportDialog 的 setErrors）
> - defineModel：双向绑定语法糖（如 ImportDialog 的 visible）
> - provide/inject：布局组件下发共享数据（有需要时可用）

### Q5：如果数据量很大，表格会怎么优化？

> 当前用分页来控制单次渲染量（默认 20 条/页）。如果单页需要展示更多数据，可以引入虚拟滚动（如 `el-table-v2` 或 `vue-virtual-scroller`），只渲染可视区域内的行。后端配合 MyBatis Plus 的分页插件，数据库层面使用 `LIMIT/OFFSET` 保证查询性能。

### Q6：你在这个项目中遇到的最大挑战是什么？

> 最大挑战是工单模块的复杂度。一个工单涉及 7 个子维度数据（工作清单、输入/输出物料、检验项目、约束关系、供应计划、文档附件），状态流转有 6 种状态和多种合法转换路径。需要在前端做好多 Tab 数据加载和状态同步，在后端做好状态机校验防止非法操作。另外 Token 无感刷新在并发场景下的边界情况也花了不少时间调试。

### Q7：这个项目的不足之处和后续改进计划？

> 1. 按钮级权限：目前前端没有 `v-permission` 指令，后续需要实现
> 2. 数据可视化：缺少生产看板和图表，可以引入 ECharts
> 3. WebSocket：车间实时数据采集可以用 WebSocket 替代轮询
> 4. 国际化：当前只支持中文，可以引入 vue-i18n
> 5. 单元测试：后端和前端的单元测试覆盖率需要提升

---

## 八、项目数据总结（用于面试量化描述）

| 维度 | 数据 |
|------|------|
| 业务模块数 | 12 个（基础数据、班组、工艺、计划、工单、派工、异常、质量、查询、物料、APS、系统） |
| 前端页面数 | 40+ 个 |
| 后端 Maven 模块数 | 14 个 |
| 数据库迁移脚本 | 12 个版本 |
| 公共组件 | 5 个（DataTable、SearchForm、BatchEdit、FileUpload、ImportDialog） |
| API 模块 | 12 个域（与后端一一对应） |
| Pinia Store | 2 个（auth、permission） |
| 状态枚举数 | 14 种（工单、计划、异常、同步、BOM、领料、入库、退料等） |
