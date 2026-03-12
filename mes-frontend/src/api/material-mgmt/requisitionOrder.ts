import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { RequisitionOrderVO, RequisitionOrderDTO, RequisitionOrderQuery } from '@/types/material-mgmt'

const BASE = '/material/requisition-order'

export const requisitionOrderApi = {
  page: (params: RequisitionOrderQuery) => request.get<PageResult<RequisitionOrderVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<RequisitionOrderVO>(`${BASE}/${id}`),
  create: (data: RequisitionOrderDTO) => request.post<number>(BASE, data),
  update: (id: number, data: RequisitionOrderDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
