import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkStatusViewVO, WorkStatusQuery, ProductionWorkVO, ProductionWorkQuery, InspectionWorkVO, InspectionWorkQuery } from '@/types/query'
import type { DispatchTaskVO, DispatchTaskQuery } from '@/types/dispatch'
import type {
  OrderStartCheckQuery,
  OrderStartCheckVO,
  ShiftHandoverQuery,
  ShiftHandoverVO,
  WorkStartCheckQuery,
  WorkStartCheckVO,
} from '@/types/quality'
import type { WorkOrderQuery, WorkOrderVO } from '@/types/workorder'

const BASE = '/query'

export const workQueryApi = {
  workStatusPage: (params: WorkStatusQuery) => request.get<PageResult<WorkStatusViewVO>>(`${BASE}/work-status-view/page`, params),
  productionWorkPage: (params: ProductionWorkQuery) => request.get<PageResult<ProductionWorkVO>>(`${BASE}/production-work/page`, params),
  productionWorkDetail: (id: number) => request.get<ProductionWorkVO>(`${BASE}/production-work/${id}`),
  inspectionWorkPage: (params: InspectionWorkQuery) => request.get<PageResult<InspectionWorkVO>>(`${BASE}/inspection-work/page`, params),
  inspectionWorkDetail: (id: number) => request.get<InspectionWorkVO>(`${BASE}/inspection-work/${id}`),
  workStartCheckPage: (params: WorkStartCheckQuery) => request.get<PageResult<WorkStartCheckVO>>('/quality/work-start-check/page', params),
  workStartCheckDetail: (id: number) => request.get<WorkStartCheckVO>(`/quality/work-start-check/${id}`),
  orderStartCheckPage: (params: OrderStartCheckQuery) => request.get<PageResult<OrderStartCheckVO>>('/quality/order-start-check/page', params),
  orderStartCheckDetail: (id: number) => request.get<OrderStartCheckVO>(`/quality/order-start-check/${id}`),
  shiftHandoverPage: (params: ShiftHandoverQuery) => request.get<PageResult<ShiftHandoverVO>>('/quality/shift-handover/page', params),
  shiftHandoverDetail: (id: number) => request.get<ShiftHandoverVO>(`/quality/shift-handover/${id}`),
  workOrderPage: (params: WorkOrderQuery) => request.get<PageResult<WorkOrderVO>>('/workorder/work-order/page', params),
  workOrderDetail: (id: number) => request.get<WorkOrderVO>(`/workorder/work-order/${id}`),
  dispatchWorkPage: (params: DispatchTaskQuery) => request.get<PageResult<DispatchTaskVO>>('/dispatch/task/page', params),
  dispatchWorkDetail: (id: number) => request.get<DispatchTaskVO>(`/dispatch/task/${id}`),
}

export const shiftHandoverAttachmentApi = {
  list: (handoverId: number) => request.get(`${BASE}/shift-handover-attachment/list/${handoverId}`),
  create: (data: any) => request.post(`${BASE}/shift-handover-attachment`, data),
  delete: (id: number) => request.delete(`${BASE}/shift-handover-attachment/${id}`),
}
