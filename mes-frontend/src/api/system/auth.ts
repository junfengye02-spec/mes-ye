import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  factoryCode?: string
  roles: string[]
  permissions: string[]
}

export const authApi = {
  login: (data: LoginParams) => request.post<LoginResult>('/auth/login', data),
  refresh: (refreshToken: string) => request.post<LoginResult>('/auth/refresh', { refreshToken }),
  logout: () => request.post('/auth/logout'),
  getUserInfo: () => request.get<UserInfo>('/auth/user-info'),
}
