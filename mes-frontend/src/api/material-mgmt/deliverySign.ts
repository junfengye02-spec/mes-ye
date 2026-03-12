import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { DeliverySignVO, DeliverySignDTO, DeliverySignQuery } from '@/types/material-mgmt'

const BASE = '/material/delivery-sign'

export const deliverySignApi = {
  page: (params: DeliverySignQuery) => request.get<PageResult<DeliverySignVO>>(`${BASE}/page`, params),
  create: (data: DeliverySignDTO) => request.post<number>(BASE, data),
  confirm: (id: number) => request.post(`${BASE}/${id}/confirm`),
}
