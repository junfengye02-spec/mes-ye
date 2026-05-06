import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  /** ADMIN=管理端 USER=现场端，与后端校验账号类型 */
  loginClient?: 'ADMIN' | 'USER'
  /** 租户编码（子域名自动识别；同名账号跨租户时必须显式传） */
  tenantCode?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken?: string | null
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  factoryCode?: string
  tenantId?: number
  tenantCode?: string
  /** ADMIN=管理端+现场端 STAFF=仅现场端 */
  accountType?: string
  roles: string[]
  permissions: string[]
}

export const authApi = {
  login: (data: LoginParams) => request.post<LoginResult>('/auth/login', data),
  refresh: () => request.post<LoginResult>('/auth/refresh', {}),
  logout: () => request.post('/auth/logout'),
  getUserInfo: () => request.get<UserInfo>('/auth/user-info'),
}
