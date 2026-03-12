import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { MaterialReturnVO, MaterialReturnDTO, MaterialReturnQuery } from '@/types/material-mgmt'

const BASE = '/material/return'

export const materialReturnApi = {
  page: (params: MaterialReturnQuery) => request.get<PageResult<MaterialReturnVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<MaterialReturnVO>(`${BASE}/${id}`),
  create: (data: MaterialReturnDTO) => request.post<number>(BASE, data),
  update: (id: number, data: MaterialReturnDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
