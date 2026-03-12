import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ProcessInfoVO, ProcessInfoDTO, ProcessInfoQuery } from '@/types/process'

const BASE = '/process/process-info'

export const processInfoApi = {
  page: (params: ProcessInfoQuery) => request.get<PageResult<ProcessInfoVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ProcessInfoVO>(`${BASE}/${id}`),
  create: (data: ProcessInfoDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ProcessInfoDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  batchUpdate: (data: ProcessInfoDTO[]) => request.put(`${BASE}/batch`, data),
}
