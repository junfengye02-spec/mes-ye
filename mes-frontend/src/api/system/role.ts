import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SysRoleVO {
  id: number
  roleName: string
  roleCode: string
  description?: string
  enabled: boolean
  createdTime?: string
}

export interface SysRoleDTO {
  roleName: string
  roleCode: string
  description?: string
  enabled?: boolean
  menuIds?: number[]
}

export interface SysRoleQuery {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleCode?: string
  enabled?: boolean | string
}

export const sysRoleApi = {
  page: (params: SysRoleQuery) => request.get<PageResult<SysRoleVO>>('/system/role/page', params),
  list: () => request.get<SysRoleVO[]>('/system/role/list'),
  getDetail: (id: number) => request.get<SysRoleVO>(`/system/role/${id}`),
  create: (data: SysRoleDTO) => request.post<number>('/system/role', data),
  update: (id: number, data: SysRoleDTO) => request.put(`/system/role/${id}`, data),
  delete: (id: number) => request.delete(`/system/role/${id}`),
  assignMenus: (id: number, menuIds: number[]) => request.put(`/system/role/${id}/menus`, menuIds),
  getRoleMenuIds: (id: number) => request.get<number[]>(`/system/role/${id}/menus`),
}
