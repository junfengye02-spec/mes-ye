import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ReceiptVO, ReceiptDTO, ReceiptQuery, ReceiptRequestVO, ReceiptRequestDTO, ReceiptRequestQuery } from '@/types/material-mgmt'

const BASE = '/material/receipt'

export const receiptApi = {
  page: (params: ReceiptQuery) => request.get<PageResult<ReceiptVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<ReceiptVO>(`${BASE}/${id}`),
  create: (data: ReceiptDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ReceiptDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}

export const receiptRequestApi = {
  page: (params: ReceiptRequestQuery) => request.get<PageResult<ReceiptRequestVO>>(`${BASE}/request/page`, params),
  getDetail: (id: number) => request.get<ReceiptRequestVO>(`${BASE}/request/${id}`),
  create: (data: ReceiptRequestDTO) => request.post<number>(`${BASE}/request`, data),
  update: (id: number, data: ReceiptRequestDTO) => request.put(`${BASE}/request/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/request/${id}`),
}
