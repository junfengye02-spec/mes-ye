import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { MaterialVO, MaterialDTO, MaterialQuery } from '@/types/basic'

const BASE = '/basic/material'

export const materialApi = {
  page: (params: MaterialQuery) => request.get<PageResult<MaterialVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<MaterialVO>(`${BASE}/${id}`),
  create: (data: MaterialDTO) => request.post<number>(BASE, data),
  update: (id: number, data: MaterialDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
