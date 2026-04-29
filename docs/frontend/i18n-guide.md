# MES 前端 i18n 开发指南

> 本文说明 MES 前端如何使用 `vue-i18n@10`，以及如何为新页面补充翻译。
>
> 本项目已内置：
> - zh-CN / en-US 两种语言
> - Element Plus 内置文案（分页 / 日期选择等）与应用 locale 联动
> - 语言切换 Header 下拉菜单（MainLayout 与 Login 页右上角）
> - `localStorage` 持久化 + 浏览器语言自动探测

---

## 目录结构

```
src/locales/
├─ index.ts            # i18n 入口（createI18n + loadElementPlusLocale + SUPPORTED_LOCALES）
├─ zh-CN/
│  ├─ index.ts         # 聚合导出
│  ├─ common.ts        # 通用（home/status/remark/language 等）
│  ├─ buttons.ts       # 按钮（add/edit/delete/search/...）
│  ├─ messages.ts      # 操作提示（成功/失败/删除确认）
│  ├─ menu.ts          # 菜单标题
│  ├─ login.ts         # 登录页
│  ├─ workorder.ts     # 工单模块
│  ├─ dispatch.ts      # 派工模块
│  ├─ material.ts      # 物料管理模块
│  └─ system.ts        # 系统管理（user/role/menu）
└─ en-US/              # 结构完全对齐 zh-CN
```

`src/stores/locale.ts` 是 Pinia store：
- `current`   当前语言 key
- `elLocale`  Element Plus 语言包（供 `<el-config-provider :locale>`）
- `setLocale(next)`  切换语言，同步 vue-i18n + ElementPlus + localStorage + `<html lang>`

---

## 在组件中使用翻译

### 组合式 API（推荐）

```ts
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t, te } = useI18n()
</script>

<template>
  <h1>{{ t('workorder.listTitle') }}</h1>
  <el-button>{{ t('buttons.add') }}</el-button>
  <span v-if="te('workorder.fields.dueDate')">{{ t('workorder.fields.dueDate') }}</span>
</template>
```

### 全局注入（`globalInjection: true`）

不用 `useI18n` 也可以在模板里直接用 `$t`：

```vue
<template>
  <el-button>{{ $t('buttons.save') }}</el-button>
</template>
```

但在 `<script>` 里使用翻译字符串，仍需显式 `useI18n()`。

### 带变量的翻译

```ts
t('common.total', { total: 123 })            // zh-CN: 共 123 条
t('login.tenantBadge', { code: 'ACME' })     // zh-CN: 租户：ACME
```

### 复数（根据需要启用）

vue-i18n 支持 `@:` / `|` 分隔符，本项目暂未开启复数规则。

---

## 切换语言

三种方式：

1. **UI 下拉菜单**（Header 右上角 / Login 页右上角）
2. **代码触发**：

```ts
import { useLocaleStore } from '@/stores/locale'
const localeStore = useLocaleStore()
await localeStore.setLocale('en-US')
```

3. **直接访问**：把 `mes.locale` 写进 `localStorage`（刷新生效）

---

## 为新页面补充翻译（3 步法）

以"成品质量 / 复检申请页 (RecheckRequestList)" 为例：

### 步骤 1：建立 key（zh-CN）

在 `src/locales/zh-CN/` 下新增 `quality.ts`：

```ts
export default {
  title: '成品质量',
  recheck: {
    title: '复检申请',
    fields: {
      requestNo: '申请单号',
      reason: '复检原因',
      result: '复检结果',
    },
    statuses: {
      PENDING: '待处理',
      APPROVED: '已通过',
      REJECTED: '已拒绝',
    },
  },
}
```

在 `src/locales/zh-CN/index.ts` 引入：

```ts
import quality from './quality'
export default {
  // ... existing
  quality,
}
```

### 步骤 2：同步 en-US

在 `src/locales/en-US/` 下创建结构完全一致的 `quality.ts`，用英文覆盖。
（**缺失的 key 会 fallback 到 zh-CN**，所以短期可以先填占位）

### 步骤 3：在组件中使用

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <el-card>
    <template #header>
      <span>{{ t('quality.recheck.title') }}</span>
    </template>
    <el-table :data="list">
      <el-table-column :label="t('quality.recheck.fields.requestNo')" prop="requestNo" />
    </el-table>
  </el-card>
</template>
```

---

## 菜单 / 路由翻译

菜单标题写在 `src/layout/menuConfig.ts`，每个菜单项都有：

```ts
{
  path: '/workorder',
  title: '生产工单',          // 中文 fallback
  i18nKey: 'menu.workorder._title',  // 翻译 key
  icon: 'Document',
  children: [...]
}
```

渲染时由 `SidebarMenu.vue / BreadcrumbNav.vue` 的 `renderTitle` 函数统一处理：
```ts
function renderTitle(item: MenuItem): string {
  if (item.i18nKey && te(item.i18nKey)) return t(item.i18nKey)
  return item.title   // fallback
}
```

**新增菜单时**：
1. 在 `menuConfig.ts` 填 `title`（中文）+ `i18nKey`
2. 在 `src/locales/zh-CN/menu.ts` 和 `en-US/menu.ts` 补对应 key

---

## Element Plus 组件内置文案

Element Plus 的日期选择器、分页组件等内置文案通过 `<el-config-provider :locale>` 联动：

- `App.vue` 已接入 `<el-config-provider :locale="localeStore.elLocale">`
- `useLocaleStore.setLocale()` 会动态 `import('element-plus/es/locale/lang/en')` 加载对应语言包

无需手动处理。

---

## 校验规则（表单 rules）

把 `rules` 写成 `computed<FormRules>` 而非 `const`，确保切换语言后错误消息实时更新：

```ts
const { t } = useI18n()

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('login.rules.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.rules.passwordRequired'), trigger: 'blur' }],
}))
```

---

## 常见问题 FAQ

### Q1：切换语言后部分文字没变？

检查：
1. 是否直接写了中文字符串而未用 `t()`？
2. key 是否存在于 `zh-CN/index.ts`（用 `te(key)` 验证）？
3. 组件是否在 `keep-alive` 里缓存了渲染？重新激活即可，或绑定 `computed` 触发响应式。

### Q2：en-US 还没翻译，先留空会怎样？

`createI18n` 配置了 `fallbackLocale: 'zh-CN'` + `missingWarn: false`，未翻译的 key 会自动回落到中文，不会报错。

### Q3：怎么在后端返回的字符串上翻译？

后端返回枚举值（如 `DRAFT / RELEASED`），前端用 key 翻译：

```vue
<el-tag>{{ t(`workorder.statuses.${row.status}`) }}</el-tag>
```

不要让后端返回已翻译的中文/英文字符串，否则切语言时会不一致。

### Q4：字段 label 里有标点符号（如 "用户名："）怎么办？

把标点放在 key 中，两种语言分别控制：
- zh-CN: `username: '用户名'` + `<el-form-item :label="t('system.user.fields.username') + '：'" />`
- 或在 zh-CN 里包含 `用户名：`、en-US 里包含 `Username:`

---

## 后续扩展

- 新增第三语言（如 `ja-JP`）：
  1. 复制 `src/locales/zh-CN/` 到 `src/locales/ja-JP/`
  2. 在 `src/locales/index.ts` 的 `messages` 追加、`SUPPORTED_LOCALES` 追加
  3. 在 `loadElementPlusLocale` 追加对应 `await import('element-plus/es/locale/lang/ja')`
- 按需懒加载语言包（减少主包体积）：
  改 `createI18n({ messages: {} })` + `i18n.global.setLocaleMessage(locale, await import(...))`
- 拉取远端语言包（热更新文案）：
  `axios.get('/api/i18n/zh-CN')` 后调用 `i18n.global.setLocaleMessage`

---

## 维护清单

- [ ] 新增文案前确认是否已有通用 key（如 "保存/取消/删除" 都在 `buttons.ts`）
- [ ] zh-CN 与 en-US 结构保持一致（可借 `assert-i18n-keys.ts` 脚本校验）
- [ ] 组件里避免使用 `<span>中文硬编码</span>`，全部走 `t()`
- [ ] Header / 按钮 / 消息提示 / 表单 label / 占位符 / 表格 label 六处是最常见遗漏
- [ ] 每次发布前跑一次 `npm run build`，vue-tsc 会顺带检测未闭合标签等问题
