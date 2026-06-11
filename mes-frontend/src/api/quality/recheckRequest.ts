import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  RecheckRequestVO,
  RecheckRequestDTO,
  RecheckRequestQuery,
  RecheckReviewDTO,
  RecheckApproveDTO,
} from '@/types/quality'

const BASE = '/quality/recheck'

export const recheckRequestApi = {
  page: (params: RecheckRequestQuery) => request.get<PageResult<RecheckRequestVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<RecheckRequestVO>(`${BASE}/${id}`),
  create: (data: RecheckRequestDTO) => request.post<number>(BASE, data),
  update: (id: number, data: RecheckRequestDTO) => request.put(`${BASE}/${id}`, data),
  submit: (id: number) => request.post(`${BASE}/${id}/submit`),
  review: (id: number, data: RecheckReviewDTO) => request.post(`${BASE}/${id}/review`, data),
  approve: (id: number, data: RecheckApproveDTO) => request.post(`${BASE}/${id}/approve`, data),
  complete: (id: number) => request.post(`${BASE}/${id}/complete`),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
