import { APIRequestContext, request as pwRequest } from '@playwright/test'

/**
 * authed API client —— 给 seed 和回归断言共用。
 * 直接使用 Playwright 的 APIRequestContext，绕开浏览器，走后端真实 REST：
 *   - baseURL: E2E_BACKEND_BASE (默认 http://localhost:9091)
 *   - 自动注入 Authorization: Bearer <token>
 *   - 统一解包 {code, message, data} 信封；code!==200 抛错
 *   - 提供 raw() 用于拿到原始响应（用于 403/冲突等需要看 HTTP code 的断言）
 */

export interface LoginResult {
  accessToken: string
  refreshToken: string
  userInfo: {
    id: number
    username: string
    accountType?: string
    roles?: string[]
    permissions?: string[]
    [k: string]: any
  }
}

export interface Envelope<T> {
  code: number
  message: string
  data: T
}

export const BACKEND_BASE = process.env.E2E_BACKEND_BASE || 'http://localhost:9091'

export interface LoginOptions {
  username: string
  password: string
  loginClient?: 'ADMIN' | 'USER'
  tenantCode?: string
}

export class ApiClient {
  private token: string | null = null
  private refreshToken: string | null = null
  readonly base: string

  constructor(private ctx: APIRequestContext, base?: string) {
    this.base = base || BACKEND_BASE
  }

  static async create(base?: string): Promise<ApiClient> {
    const ctx = await pwRequest.newContext({ baseURL: base || BACKEND_BASE, ignoreHTTPSErrors: true })
    return new ApiClient(ctx, base)
  }

  async dispose(): Promise<void> {
    await this.ctx.dispose().catch(() => undefined)
  }

  private headers(): Record<string, string> {
    const h: Record<string, string> = { 'Content-Type': 'application/json;charset=UTF-8' }
    if (this.token) h['Authorization'] = `Bearer ${this.token}`
    return h
  }

  setToken(token: string, refresh?: string): void {
    this.token = token
    if (refresh) this.refreshToken = refresh
  }

  getToken(): string | null {
    return this.token
  }

  getRefreshToken(): string | null {
    return this.refreshToken
  }

  async login(opts: LoginOptions): Promise<LoginResult> {
    const res = await this.ctx.post('/api/auth/login', {
      data: {
        username: opts.username,
        password: opts.password,
        loginClient: opts.loginClient || 'ADMIN',
        tenantCode: opts.tenantCode,
      },
      failOnStatusCode: false,
      timeout: 8000,
    })
    if (!res.ok()) {
      throw new Error(`login http ${res.status()}: ${await res.text()}`)
    }
    const body = (await res.json()) as Envelope<LoginResult>
    if (body.code !== 200 || !body.data?.accessToken) {
      throw new Error(`login failed: code=${body.code} msg=${body.message}`)
    }
    this.token = body.data.accessToken
    this.refreshToken = body.data.refreshToken
    return body.data
  }

  /**
   * 健康探活：判断 `${base}/api` 下是否是**目标 MES 后端**（而不是随便一个返回 404 的服务）。
   * 判定策略：
   *   1. 能建立连接（不抛 network error）
   *   2. /api/auth/login 以伪造密码返回时，body 应是 JSON 且含 `code` 字段（MES 统一 envelope）
   *      —— 404 纯文本 / 503 html 等都会被判定为「不可达」
   */
  async ping(): Promise<boolean> {
    try {
      const res = await this.ctx.post('/api/auth/login', {
        data: { username: '__ping__', password: '__ping__', loginClient: 'ADMIN' },
        failOnStatusCode: false,
        timeout: 3000,
      })
      const text = await res.text()
      if (!text) return false
      try {
        const body = JSON.parse(text)
        // 只要响应是 {code, message, data} 任一形态即认为是 MES envelope
        return typeof body === 'object' && body !== null && 'code' in body
      } catch {
        return false
      }
    } catch {
      return false
    }
  }

  /** 解包 envelope；code!==200 抛错 */
  async get<T = any>(url: string, params?: Record<string, any>): Promise<T> {
    const res = await this.ctx.get(url.startsWith('/api') ? url : `/api${url}`, {
      params,
      headers: this.headers(),
      failOnStatusCode: false,
      timeout: 15000,
    })
    return this.unwrap<T>(res, `GET ${url}`)
  }

  async post<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
    const res = await this.ctx.post(url.startsWith('/api') ? url : `/api${url}`, {
      data,
      params,
      headers: this.headers(),
      failOnStatusCode: false,
      timeout: 15000,
    })
    return this.unwrap<T>(res, `POST ${url}`)
  }

  async put<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
    const res = await this.ctx.put(url.startsWith('/api') ? url : `/api${url}`, {
      data,
      params,
      headers: this.headers(),
      failOnStatusCode: false,
      timeout: 15000,
    })
    return this.unwrap<T>(res, `PUT ${url}`)
  }

  async delete<T = any>(url: string, params?: Record<string, any>): Promise<T> {
    const res = await this.ctx.delete(url.startsWith('/api') ? url : `/api${url}`, {
      params,
      headers: this.headers(),
      failOnStatusCode: false,
      timeout: 15000,
    })
    return this.unwrap<T>(res, `DELETE ${url}`)
  }

  /** 不做 envelope 解包，返回原始状态/JSON；用于权限越界 / 冲突校验 */
  async raw(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any,
    params?: Record<string, any>,
  ): Promise<{ status: number; body: any; text: string }> {
    const fullUrl = url.startsWith('/api') ? url : `/api${url}`
    const common = { headers: this.headers(), failOnStatusCode: false, timeout: 15000 }
    let res
    switch (method) {
      case 'GET':
        res = await this.ctx.get(fullUrl, { params, ...common })
        break
      case 'POST':
        res = await this.ctx.post(fullUrl, { data, params, ...common })
        break
      case 'PUT':
        res = await this.ctx.put(fullUrl, { data, params, ...common })
        break
      case 'DELETE':
        res = await this.ctx.delete(fullUrl, { params, ...common })
        break
    }
    const text = await res.text()
    let body: any = null
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      body = null
    }
    return { status: res.status(), body, text }
  }

  private async unwrap<T>(res: Awaited<ReturnType<APIRequestContext['get']>>, tag: string): Promise<T> {
    const text = await res.text()
    if (!res.ok()) {
      throw new Error(`${tag} http ${res.status()}: ${text}`)
    }
    let body: Envelope<T>
    try {
      body = text ? JSON.parse(text) : ({ code: 0, message: 'empty', data: null as any } as Envelope<T>)
    } catch (e) {
      throw new Error(`${tag} non-json response: ${text}`)
    }
    if (body.code !== 200) {
      throw new Error(`${tag} biz-error code=${body.code} msg=${body.message}`)
    }
    return body.data
  }
}
