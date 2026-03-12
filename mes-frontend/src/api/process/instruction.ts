import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { InstructionVO, InstructionDTO, InstructionQuery } from '@/types/process'

const BASE = '/process/instruction'

export const instructionApi = {
  page: (params: InstructionQuery) => request.get<PageResult<InstructionVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<InstructionVO>(`${BASE}/${id}`),
  create: (data: InstructionDTO) => request.post<number>(BASE, data),
  update: (id: number, data: InstructionDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  upgrade: (id: number) => request.post(`${BASE}/${id}/upgrade`),
  getFlowLogs: (id: number) => request.get(`${BASE}/${id}/flow-logs`),
}
