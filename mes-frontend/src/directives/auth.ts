import type { App, Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 超级管理员通配符：permissions 列表中包含该值时拥有全部权限
 *
 * 约定来源：ruoyi/vue-element-admin 社区通用标识，后端可选择在登录时
 * 对平台级超管（tenantId=0 且 ADMIN 角色）注入该值。即便后端没注入，
 * {@link #hasAnyPermission} 还会兜底判断 roles 是否包含 ADMIN 通配角色码。
 */
const SUPER_ADMIN_FLAG = '*:*:*'

/**
 * 被视作"无限制"的兜底角色编码列表。
 *
 * 出现在该列表中的角色会直接跳过 permission 细粒度校验。
 * 与后端 V1.11__auth_rbac.sql 中的 ADMIN 角色编码保持一致。
 */
const SUPER_ADMIN_ROLE_CODES: readonly string[] = ['ADMIN', 'SUPER_ADMIN']

/**
 * 判断当前登录用户是否拥有任一指定权限码。
 *
 * 命中优先级：
 * 1. permissions 里含 '*:*:*' 通配 → 通过
 * 2. roles 里含 ADMIN / SUPER_ADMIN → 通过（租户内超管兜底）
 * 3. 指定的任一权限码出现在 permissions 列表 → 通过
 * 其余情况一律拒绝。
 *
 * @param requiredPermissions 按钮需要的权限码列表，命中任一即返回 true
 * @returns true 表示可以展示按钮；false 表示应隐藏
 */
export function hasAnyPermission(requiredPermissions: string[]): boolean {
  if (!Array.isArray(requiredPermissions) || requiredPermissions.length === 0) {
    return true
  }
  const authStore = useAuthStore()
  const owned = authStore.userInfo?.permissions ?? []
  const ownedRoles = authStore.userInfo?.roles ?? []
  if (Array.isArray(owned) && owned.includes(SUPER_ADMIN_FLAG)) {
    return true
  }
  if (Array.isArray(ownedRoles) && ownedRoles.some((r) => SUPER_ADMIN_ROLE_CODES.includes(r))) {
    return true
  }
  if (!Array.isArray(owned) || owned.length === 0) {
    return false
  }
  return requiredPermissions.some((code) => owned.includes(code))
}

/**
 * 校验指令绑定的参数合法性。
 *
 * @param binding 指令绑定
 * @returns 规范化后的权限码数组
 */
function normalizeBindingValue(binding: DirectiveBinding): string[] {
  const value = binding.value
  if (typeof value === 'string' && value.trim().length > 0) {
    return [value.trim()]
  }
  if (Array.isArray(value)) {
    return value.filter((v): v is string => typeof v === 'string' && v.trim().length > 0)
  }
  return []
}

/**
 * 根据权限校验结果应用隐藏策略：
 * 1. 优先从 DOM 树移除（与 ruoyi v-hasPermi 一致），保证无法通过样式复活
 * 2. 特殊场景：如果希望保留占位，可通过 modifier `v-auth.keep` 走 display:none
 *
 * @param el 指令作用的元素
 * @param binding 指令绑定
 */
function applyAuth(el: HTMLElement, binding: DirectiveBinding): void {
  const required = normalizeBindingValue(binding)
  if (required.length === 0) {
    console.warn('[v-auth] 需要传入权限码，如 v-auth="[\'sys:user:add\']"')
    return
  }
  if (hasAnyPermission(required)) {
    return
  }
  if (binding.modifiers?.keep) {
    el.style.display = 'none'
    return
  }
  el.parentNode?.removeChild(el)
}

/**
 * v-auth 按钮级权限指令
 *
 * 用法示例：
 *   <el-button v-auth="['sys:user:add']">新增</el-button>
 *   <el-button v-auth="['sys:user:edit', 'sys:user:audit']">编辑或审核</el-button>
 *   <el-button v-auth.keep="['sys:role:remove']">删除（保留占位）</el-button>
 *
 * 约束：
 * - 数据源：Pinia useAuthStore().userInfo.permissions
 * - '*:*:*' 视为超级管理员，直接通过
 * - mounted 时判定一次并从 DOM 移除；permissions 变更需要刷新页面
 *   （与后端 JWT 续期同步，避免 Vue 响应式依赖导致的安全漏洞）
 */
export const authDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    applyAuth(el, binding)
  },
}

/**
 * 全局注册 v-auth 指令。
 *
 * @param app Vue 应用实例
 */
export function setupAuthDirective(app: App): void {
  app.directive('auth', authDirective)
}

/**
 * 兼容旧代码的 hasPermi 判断函数（如果有模板/脚本中直接 js 判断的需要）。
 *
 * @param permissions 权限码列表
 * @returns 是否拥有权限
 */
export const hasPermi = hasAnyPermission
