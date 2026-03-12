import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkOrderVO, WorkOrderDTO, WorkOrderQuery } from '@/types/workorder'

const BASE = '/workorder/work-order'

export const workOrderApi = {
  page: (params: WorkOrderQuery) => request.get<PageResult<WorkOrderVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<WorkOrderVO>(`${BASE}/${id}`),
  create: (data: WorkOrderDTO) => request.post<number>(BASE, data),
  update: (id: number, data: WorkOrderDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  release: (id: number) => request.post(`${BASE}/${id}/release`),
  start: (id: number) => request.post(`${BASE}/${id}/start`),
  complete: (id: number) => request.post(`${BASE}/${id}/complete`),
  forceComplete: (id: number, data: { reason: string }) => request.post(`${BASE}/${id}/force-complete`, data),
  getStatusLogs: (id: number) => request.get(`${BASE}/${id}/status-logs`),
}

export const workOrderAttachmentApi = {
  list: (workOrderId: number) => request.get('/workorder/attachment/list', { workOrderId }),
  create: (workOrderId: number, data: any) => request.post('/workorder/attachment', data, { params: { workOrderId } }),
  delete: (id: number) => request.delete(`/workorder/attachment/${id}`),
}
