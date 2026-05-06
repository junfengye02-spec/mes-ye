import { Page, expect, test as base } from '@playwright/test'
import { ApiClient, BACKEND_BASE } from './seed/api-client'
import { E2ESeed, SeedData, SeedOptions, SeedUnavailableError } from './seed/seed-data'

/**
 * E2E fixture 中枢：
 * - 封装登录（后端优先，UI 降级）
 * - 注入 authed `api` client（Playwright APIRequestContext 实现）
 * - 可选 `seed` fixture：按 describe 级别共享数据
 *
 * 历史兼容：保留 `loginAsAdmin` / `logout` 导出函数，原 5 个 spec 继续可用。
 */

export const E2E_USER = process.env.E2E_USER || 'admin'
export const E2E_PASS = process.env.E2E_PASS || 'admin123'
export const E2E_TENANT = process.env.E2E_TENANT || ''

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'

/**
 * 登录：走真实登录页表单，保持路由守卫、Pinia 状态和浏览器存储一致。
 */
export async function loginAsAdmin(page: Page): Promise<string | null> {
  await page.goto('/login')
  if (E2E_TENANT) {
    await page
      .locator('input[placeholder*="租户"], input[placeholder*="Tenant"]')
      .first()
      .fill(E2E_TENANT)
      .catch(() => undefined)
  }

  await page.fill(LOGIN_USERNAME_SELECTOR, E2E_USER)
  await page.fill(LOGIN_PASSWORD_SELECTOR, E2E_PASS)
  await page.click(LOGIN_SUBMIT_SELECTOR)
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15000 })
  return page.evaluate(() => window.localStorage.getItem('token'))
}

/**
 * 登录页选择器 —— 统一导出，供 spec 共享；优先使用标准 HTML 属性 `autocomplete`，
 * 同时兼容中英文 placeholder，保证在不同 locale / 不同样式版本下都能命中。
 */
export const LOGIN_USERNAME_SELECTOR =
  'input[autocomplete="username"], input[name="username"], input[placeholder*="用户"], input[placeholder*="账号"], input[placeholder*="Username"], input[placeholder*="Account"]'

export const LOGIN_PASSWORD_SELECTOR =
  'input[autocomplete="current-password"], input[type="password"], input[placeholder*="密码"], input[placeholder*="Password"]'

export const LOGIN_SUBMIT_SELECTOR =
  'button:has-text("登录"), button:has-text("登 录"), button:has-text("Log In"), button:has-text("Login"), button:has-text("Sign in"), button[type="submit"]'

/** 用指定账号登录（RBAC 测试用） */
export async function loginAs(
  page: Page,
  opts: { username: string; password: string; loginClient?: 'ADMIN' | 'USER'; tenantCode?: string },
): Promise<string | null> {
  await page.goto('/login')
  const res = await page.request.post(`${BACKEND_BASE}/api/auth/login`, {
    data: {
      username: opts.username,
      password: opts.password,
      loginClient: opts.loginClient || 'ADMIN',
      tenantCode: opts.tenantCode,
    },
    failOnStatusCode: false,
    timeout: 5000,
  })
  if (!res.ok()) return null
  const body = await res.json()
  const token = body?.data?.accessToken
  const refreshToken = body?.data?.refreshToken
  if (!token) return null
  await page.evaluate(
    ([t, r]) => {
      window.localStorage.setItem('token', t)
      if (r) window.localStorage.setItem('refreshToken', r)
    },
    [token, refreshToken] as const,
  )
  await page.goto('/')
  return token
}

export async function logout(page: Page): Promise<void> {
  await page.evaluate(
    ([tk, rk]) => {
      window.localStorage.removeItem(tk)
      window.localStorage.removeItem(rk)
    },
    [TOKEN_KEY, REFRESH_KEY],
  )
  await page.goto('/login')
  await expect(page).toHaveURL(/\/login/)
}

/**
 * 扩展 test：提供 `api`（已登录管理员 API client）与 `backendAlive` 标志。
 * 数据级测试 spec 使用 `test.skip(!backendAlive, '...')` 进行优雅降级。
 */
interface Fixtures {
  api: ApiClient
  backendAlive: boolean
  adminUsername: string
  adminPassword: string
}

export const test = base.extend<Fixtures>({
  adminUsername: [E2E_USER, { option: true }],
  adminPassword: [E2E_PASS, { option: true }],
  backendAlive: [
    async ({}, use) => {
      const probe = await ApiClient.create()
      const alive = await probe.ping()
      await probe.dispose()
      await use(alive)
    },
    { scope: 'worker' },
  ],
  api: async ({ adminUsername, adminPassword }, use) => {
    const client = await ApiClient.create()
    try {
      await client.login({
        username: adminUsername,
        password: adminPassword,
        loginClient: 'ADMIN',
        tenantCode: E2E_TENANT || undefined,
      })
    } catch {
      // 登录失败也要把 client 暴露出去；spec 侧通常会先 test.skip(!backendAlive)
    }
    await use(client)
    await client.dispose()
  },
})

export { expect, ApiClient, E2ESeed }
export type { SeedData, SeedOptions }
export { SeedUnavailableError }
