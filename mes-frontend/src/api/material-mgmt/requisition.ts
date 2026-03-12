import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { RequisitionVO, RequisitionDTO, RequisitionQuery } from '@/types/material-mgmt'

const BASE = '/material/requisition'

export const requisitionApi = {
  page: (params: RequisitionQuery) => request.get<PageResult<RequisitionVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<RequisitionVO>(`${BASE}/${id}`),
  create: (data: RequisitionDTO) => request.post<number>(BASE, data),
  update: (id: number, data: RequisitionDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
