import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SysUserVO {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  enabled: boolean
  factoryCode?: string
  tenantId?: number
  accountType?: string
  roles?: { id: number; roleName: string; roleCode: string }[]
  createdTime?: string
  updatedTime?: string
}

export interface SysUserDTO {
  username: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  enabled?: boolean
  factoryCode?: string
  accountType?: 'ADMIN' | 'STAFF'
  roleIds?: number[]
}

export interface SysUserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  realName?: string
  enabled?: boolean | string
}

export const sysUserApi = {
  page: (params: SysUserQuery) => request.get<PageResult<SysUserVO>>('/system/user/page', params),
  getDetail: (id: number) => request.get<SysUserVO>(`/system/user/${id}`),
  create: (data: SysUserDTO) => request.post<number>('/system/user', data),
  update: (id: number, data: SysUserDTO) => request.put(`/system/user/${id}`, data),
  delete: (id: number) => request.delete(`/system/user/${id}`),
  resetPassword: (id: number) => request.put(`/system/user/${id}/reset-password`),
}
