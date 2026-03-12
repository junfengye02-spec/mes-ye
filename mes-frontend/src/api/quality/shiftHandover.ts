import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ShiftHandoverVO, ShiftHandoverDTO, ShiftHandoverQuery } from '@/types/quality'

const BASE = '/quality/shift-handover'

export const shiftHandoverApi = {
  page: (params: ShiftHandoverQuery) => request.get<PageResult<ShiftHandoverVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ShiftHandoverVO>(`${BASE}/${id}`),
  create: (data: ShiftHandoverDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ShiftHandoverDTO) => request.put(`${BASE}/${id}`, data),
  receive: (id: number) => request.post(`${BASE}/${id}/receive`),
}
