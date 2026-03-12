import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { AbnormalContactVO, AbnormalContactDTO, AbnormalContactQuery } from '@/types/abnormal'

const BASE = '/abnormal/contact'

export const abnormalContactApi = {
  page: (params: AbnormalContactQuery) => request.get<PageResult<AbnormalContactVO>>(`${BASE}/page`, params),
  getDetail: (id: number) => request.get<AbnormalContactVO>(`${BASE}/${id}`),
  create: (data: AbnormalContactDTO) => request.post<number>(BASE, data),
  update: (id: number, data: AbnormalContactDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
  submit: (id: number) => request.post(`${BASE}/${id}/submit`),
  process: (id: number) => request.post(`${BASE}/${id}/process`),
  close: (id: number) => request.post(`${BASE}/${id}/close`),
  getAttachments: (contactId: number) => request.get(`${BASE}/${contactId}/attachments`),
  addAttachment: (contactId: number, data: any) => request.post(`${BASE}/${contactId}/attachments`, data),
  deleteAttachment: (attachmentId: number) => request.delete(`${BASE}/attachments/${attachmentId}`),
  signAttachment: (attachmentId: number) => request.post(`${BASE}/attachments/${attachmentId}/sign`),
}
