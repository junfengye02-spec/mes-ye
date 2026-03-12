import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { MachiningProgramVO, MachiningProgramDTO, MachiningProgramQuery } from '@/types/process'

const BASE = '/process/machining-program'

export const machiningProgramApi = {
  page: (params: MachiningProgramQuery) => request.get<PageResult<MachiningProgramVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<MachiningProgramVO>(`${BASE}/${id}`),
  create: (data: MachiningProgramDTO) => request.post<number>(BASE, data),
  update: (id: number, data: MachiningProgramDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
