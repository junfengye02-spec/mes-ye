/**
 * 租户编码识别工具：
 *   - 子域名形式 {tenantCode}.mes.example.com 时，自动把子域作为 tenantCode；
 *   - 排除 www / localhost / 127.0.0.1 / IP 地址等场景；
 *   - 否则返回 null，交给登录页展示租户编码输入框。
 *
 * 生产环境建议：统一主域（如 mes.example.com）作为平台入口（强制手动输入 tenantCode），
 * 子域（如 factory-a.mes.example.com）作为租户入口（自动识别）。
 */
const RESERVED_SUBDOMAINS = new Set(['www', 'admin', 'app', 'api', 'gateway', 'portal'])
const IP_REGEX = /^\d{1,3}(?:\.\d{1,3}){3}$/

export function resolveTenantCodeFromHost(host = window.location.hostname): string | null {
  if (!host) return null
  if (host === 'localhost' || host === '127.0.0.1' || IP_REGEX.test(host)) return null

  const parts = host.split('.')
  if (parts.length < 3) return null

  const first = parts[0].trim().toLowerCase()
  if (!first || RESERVED_SUBDOMAINS.has(first)) return null
  // 合法租户编码：字母开头，包含字母数字短横线，长度 2~64
  if (!/^[a-z][a-z0-9-]{1,63}$/.test(first)) return null
  return first
}

/**
 * 一个轻量的"租户编码"上下文：登录前由 Login.vue 初始化到 localStorage，
 * 其他地方（例如刷新 token 时）可以读它决定是否展示租户编码。
 */
const STORAGE_KEY = 'mes.tenantCode'

export function getStoredTenantCode(): string | null {
  const fromDomain = resolveTenantCodeFromHost()
  if (fromDomain) return fromDomain
  return localStorage.getItem(STORAGE_KEY)
}

export function setStoredTenantCode(code: string | null | undefined) {
  if (!code) {
    localStorage.removeItem(STORAGE_KEY)
  } else {
    localStorage.setItem(STORAGE_KEY, code)
  }
}
