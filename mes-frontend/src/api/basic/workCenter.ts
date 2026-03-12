import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkCenterVO, WorkCenterDTO, WorkCenterQuery } from '@/types/basic'

const BASE = '/basic/work-center'

export const workCenterApi = {
  page: (params: WorkCenterQuery) => request.get<PageResult<WorkCenterVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<WorkCenterVO>(`${BASE}/${id}`),
  create: (data: WorkCenterDTO) => request.post<number>(BASE, data),
  update: (id: number, data: WorkCenterDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  batchUpdate: (data: WorkCenterDTO[]) => request.put(`${BASE}/batch`, data),
}
