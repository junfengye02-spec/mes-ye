import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ProductionTeamVO, ProductionTeamDTO, ProductionTeamQuery } from '@/types/team'

const BASE = '/team/production-team'

export const productionTeamApi = {
  page: (params: ProductionTeamQuery) => request.get<PageResult<ProductionTeamVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ProductionTeamVO>(`${BASE}/${id}`),
  create: (data: ProductionTeamDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ProductionTeamDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  toggleEnabled: (id: number) => request.put(`${BASE}/${id}/toggle-enabled`),
}
