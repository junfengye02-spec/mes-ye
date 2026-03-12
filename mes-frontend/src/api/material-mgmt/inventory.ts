import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { StorageInventoryVO, InventoryQuery } from '@/types/material-mgmt'

const BASE = '/material/inventory'

export const inventoryApi = {
  page: (params: InventoryQuery) => request.get<PageResult<StorageInventoryVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<StorageInventoryVO>(`${BASE}/${id}`),
  create: (data: any) => request.post(BASE, data),
  update: (id: number, data: any) => request.put(`${BASE}/${id}`, data),
}
