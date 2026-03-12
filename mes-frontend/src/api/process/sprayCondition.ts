import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SprayConditionVO, SprayConditionDTO, SprayConditionQuery } from '@/types/process'

const BASE = '/process/spray-condition'

export const sprayConditionApi = {
  page: (params: SprayConditionQuery) => request.get<PageResult<SprayConditionVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<SprayConditionVO>(`${BASE}/${id}`),
  create: (data: SprayConditionDTO) => request.post<number>(BASE, data),
  update: (id: number, data: SprayConditionDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
