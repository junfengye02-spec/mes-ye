import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { RecheckRequestVO, RecheckRequestDTO, RecheckRequestQuery } from '@/types/quality'

const BASE = '/quality/recheck'

export const recheckRequestApi = {
  page: (params: RecheckRequestQuery) => request.get<PageResult<RecheckRequestVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<RecheckRequestVO>(`${BASE}/${id}`),
  create: (data: RecheckRequestDTO) => request.post<number>(BASE, data),
  update: (id: number, data: RecheckRequestDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
