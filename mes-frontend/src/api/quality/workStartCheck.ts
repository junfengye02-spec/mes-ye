import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkStartCheckVO, WorkStartCheckDTO, WorkStartCheckQuery } from '@/types/quality'

const BASE = '/quality/work-start-check'

export const workStartCheckApi = {
  page: (params: WorkStartCheckQuery) => request.get<PageResult<WorkStartCheckVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<WorkStartCheckVO>(`${BASE}/${id}`),
  create: (data: WorkStartCheckDTO) => request.post<number>(BASE, data),
  update: (id: number, data: WorkStartCheckDTO) => request.put(`${BASE}/${id}`, data),
}
