import request from '@/utils/request'

export interface SysMenuVO {
  id: number
  parentId: number
  menuName: string
  path?: string
  component?: string
  menuType: string
  permission?: string
  icon?: string
  sortOrder: number
  visible: boolean
  children?: SysMenuVO[]
}

export interface SysMenuDTO {
  parentId?: number
  menuName: string
  path?: string
  component?: string
  menuType: string
  permission?: string
  icon?: string
  sortOrder?: number
  visible?: boolean
}

export const sysMenuApi = {
  getTree: () => request.get<SysMenuVO[]>('/system/menu/tree'),
  getUserTree: () => request.get<SysMenuVO[]>('/system/menu/user-tree'),
  getDetail: (id: number) => request.get<SysMenuVO>(`/system/menu/${id}`),
  create: (data: SysMenuDTO) => request.post<number>('/system/menu', data),
  update: (id: number, data: SysMenuDTO) => request.put(`/system/menu/${id}`, data),
  delete: (id: number) => request.delete(`/system/menu/${id}`),
}
