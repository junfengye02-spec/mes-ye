import request from '@/utils/request'
import type { PageResult } from '@/types/common'

/** 租户状态常量：与后端 SysTenant.status 保持一致 */
export const TENANT_STATUS = {
  PENDING: 0,
  ACTIVE: 1,
  PROVISIONING: 2,
  SUSPENDED: 3,
  ARCHIVED: 4,
} as const

export type TenantStatus = (typeof TENANT_STATUS)[keyof typeof TENANT_STATUS]

export const TENANT_STATUS_LABEL: Record<number, string> = {
  0: 'PENDING',
  1: 'ACTIVE',
  2: 'PROVISIONING',
  3: 'SUSPENDED',
  4: 'ARCHIVED',
}

export type TenantStatusTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined

export const TENANT_STATUS_TAG_TYPE: Record<number, TenantStatusTagType> = {
  0: 'info',
  1: 'success',
  2: 'warning',
  3: 'danger',
  4: undefined,
}

/** 租户视图对象（对应 PlatformTenantController.TenantVO） */
export interface TenantVO {
  id: number
  tenantCode: string
  tenantName: string
  status: number
  schemaMode?: string
  dataRegion?: string
  quotaUsers?: number
  quotaStorageMb?: number
  quotaQps?: number
  expireAt?: string
  contactName?: string
  contactEmail?: string
  createdTime?: string
  updatedTime?: string
}

/** 租户注册入参（对应后端 TenantRegisterDTO） */
export interface TenantRegisterDTO {
  tenantCode: string
  tenantName: string
  contactName?: string
  contactEmail?: string
  initialAdminUsername?: string
  initialAdminPassword?: string
}

export interface TenantQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number | ''
}

export const platformTenantApi = {
  list: (params: TenantQuery) =>
    request.get<PageResult<TenantVO>>('/platform/tenants', params),
  getDetail: (id: number) =>
    request.get<TenantVO>(`/platform/tenants/${id}`),
  register: (data: TenantRegisterDTO) =>
    request.post<void>('/platform/tenants/register', data),
  suspend: (id: number) =>
    request.post<void>(`/platform/tenants/${id}/suspend`),
  resume: (id: number) =>
    request.post<void>(`/platform/tenants/${id}/resume`),
  archive: (id: number) =>
    request.post<void>(`/platform/tenants/${id}/archive`),
  reprovision: (id: number) =>
    request.post<void>(`/platform/tenants/${id}/reprovision`),
}

/** 便捷别名（便于按需引用） */
export const listTenants = platformTenantApi.list
export const getTenantDetail = platformTenantApi.getDetail
export const registerTenant = platformTenantApi.register
export const suspendTenant = platformTenantApi.suspend
export const resumeTenant = platformTenantApi.resume
export const archiveTenant = platformTenantApi.archive
export const reprovisionTenant = platformTenantApi.reprovision
