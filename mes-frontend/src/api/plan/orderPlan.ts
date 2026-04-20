import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { OrderPlanVO, OrderPlanDTO, OrderPlanQuery } from '@/types/plan'

const BASE = '/plan/order-plan'

export const orderPlanApi = {
  page: (params: OrderPlanQuery) => request.get<PageResult<OrderPlanVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<OrderPlanVO>(`${BASE}/${id}`),
  create: (data: OrderPlanDTO) => request.post<number>(BASE, data),
  update: (id: number, data: OrderPlanDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  release: (id: number) => request.post(`${BASE}/${id}/release`),
  complete: (id: number) => request.post(`${BASE}/${id}/complete`),
  terminate: (id: number, reason: string) => request.post(`${BASE}/${id}/terminate`, null, { params: { reason } }),
  expand: (id: number) => request.post(`${BASE}/${id}/expand`),
  getStatusLogs: (id: number) => request.get(`${BASE}/${id}/status-logs`),
}
