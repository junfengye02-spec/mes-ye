import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { MaterialPriceVO, MaterialPriceDTO, MaterialPriceQuery } from '@/types/basic'

const BASE = '/basic/material-price'

export const materialPriceApi = {
  page: (params: MaterialPriceQuery) => request.get<PageResult<MaterialPriceVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<MaterialPriceVO>(`${BASE}/${id}`),
  create: (data: MaterialPriceDTO) => request.post<number>(BASE, data),
  update: (id: number, data: MaterialPriceDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
