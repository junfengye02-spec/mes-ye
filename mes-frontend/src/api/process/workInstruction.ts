import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { WorkInstructionVO, WorkInstructionDTO, WorkInstructionQuery } from '@/types/process'

const BASE = '/process/work-instruction'

export const workInstructionApi = {
  page: (params: WorkInstructionQuery) => request.get<PageResult<WorkInstructionVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<WorkInstructionVO>(`${BASE}/${id}`),
  create: (data: WorkInstructionDTO) => request.post<number>(BASE, data),
  update: (id: number, data: WorkInstructionDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
