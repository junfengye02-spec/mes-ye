import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ManufacturingBomVO, ManufacturingBomDTO, ManufacturingBomQuery, ManufacturingBomItemVO } from '@/types/process'

const BASE = '/process/manufacturing-bom'

export const manufacturingBomApi = {
  page: (params: ManufacturingBomQuery) => request.get<PageResult<ManufacturingBomVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ManufacturingBomVO>(`${BASE}/${id}`),
  create: (data: ManufacturingBomDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ManufacturingBomDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  upgrade: (id: number) => request.post(`${BASE}/${id}/upgrade`),
  publish: (id: number) => request.post(`${BASE}/${id}/publish`),
  disable: (id: number) => request.post(`${BASE}/${id}/disable`),
  getItemsTree: (id: number) => request.get<ManufacturingBomItemVO[]>(`${BASE}/${id}/items/tree`),
}
