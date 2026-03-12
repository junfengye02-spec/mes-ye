import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ProductionPlanVO, ProductionPlanDTO, ProductionPlanQuery } from '@/types/plan'

const BASE = '/plan/production-plan'

export const productionPlanApi = {
  page: (params: ProductionPlanQuery) => request.get<PageResult<ProductionPlanVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ProductionPlanVO>(`${BASE}/${id}`),
  create: (data: ProductionPlanDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ProductionPlanDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  release: (id: number) => request.post(`${BASE}/${id}/release`),
  getStatusLogs: (id: number) => request.get(`${BASE}/${id}/status-logs`),
}
