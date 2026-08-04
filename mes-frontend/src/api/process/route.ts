import request from '@/utils/request'
import type { PageResult } from '@/types/common'

const BASE = '/process/route'

export interface RouteStepVO {
  id?: number
  routeId?: number
  sequenceNo?: number
  processId?: number
  processNo?: string
  processName?: string
  workCenterId?: number
  handleTime?: number
  predecessorStepId?: number
  parallelFlag?: number
  optionalFlag?: number
  remark?: string
  createdTime?: string
  updatedTime?: string
}

export interface RouteVO {
  id: number
  routeCode: string
  routeName?: string
  productCode?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  status?: string
  effectiveDate?: string
  expiryDate?: string
  remark?: string
  steps?: RouteStepVO[]
  createdBy?: string
  createdTime?: string
  updatedBy?: string
  updatedTime?: string
}

export interface RouteDTO {
  routeCode: string
  routeName?: string
  productCode?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  effectiveDate?: string
  expiryDate?: string
  remark?: string
  steps?: RouteStepVO[]
}

export interface RouteQuery {
  pageNum?: number
  pageSize?: number
  routeCode?: string
  routeName?: string
  productCode?: string
  productCategory?: string
  machineModel?: string
  status?: string
}

export const routeApi = {
  page: (params: RouteQuery) => request.get<PageResult<RouteVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<RouteVO>(`${BASE}/${id}`),
  create: (data: RouteDTO) => request.post<number>(BASE, data),
  update: (id: number, data: RouteDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  activate: (id: number) => request.post(`${BASE}/activate/${id}`),
  disable: (id: number) => request.post(`${BASE}/disable/${id}`),
}
