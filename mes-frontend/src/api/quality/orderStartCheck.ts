import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { OrderStartCheckVO, OrderStartCheckDTO, OrderStartCheckQuery } from '@/types/quality'

const BASE = '/quality/order-start-check'

export const orderStartCheckApi = {
  page: (params: OrderStartCheckQuery) => request.get<PageResult<OrderStartCheckVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<OrderStartCheckVO>(`${BASE}/${id}`),
  create: (data: OrderStartCheckDTO) => request.post<number>(BASE, data),
  update: (id: number, data: OrderStartCheckDTO) => request.put(`${BASE}/${id}`, data),
}
