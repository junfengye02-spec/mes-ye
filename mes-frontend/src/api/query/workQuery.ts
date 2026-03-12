import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkStatusViewVO, WorkStatusQuery, ProductionWorkVO, ProductionWorkQuery, InspectionWorkVO, InspectionWorkQuery } from '@/types/query'

const BASE = '/query'

export const workQueryApi = {
  workStatusPage: (params: WorkStatusQuery) => request.get<PageResult<WorkStatusViewVO>>(`${BASE}/work-status-view/page`, params),
  productionWorkPage: (params: ProductionWorkQuery) => request.get<PageResult<ProductionWorkVO>>(`${BASE}/production-work/page`, params),
  productionWorkDetail: (id: number) => request.get<ProductionWorkVO>(`${BASE}/production-work/${id}`),
  inspectionWorkPage: (params: InspectionWorkQuery) => request.get<PageResult<InspectionWorkVO>>(`${BASE}/inspection-work/page`, params),
  inspectionWorkDetail: (id: number) => request.get<InspectionWorkVO>(`${BASE}/inspection-work/${id}`),
}

export const shiftHandoverAttachmentApi = {
  list: (handoverId: number) => request.get(`${BASE}/shift-handover-attachment/list/${handoverId}`),
  create: (data: any) => request.post(`${BASE}/shift-handover-attachment`, data),
  delete: (id: number) => request.delete(`${BASE}/shift-handover-attachment/${id}`),
}
