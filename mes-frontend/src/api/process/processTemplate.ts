import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ProcessTemplateVO, ProcessTemplateDTO, ProcessTemplateQuery } from '@/types/process'

const BASE = '/process/process-template'

export const processTemplateApi = {
  page: (params: ProcessTemplateQuery) => request.get<PageResult<ProcessTemplateVO>>(`${BASE}/page`, params),
  tree: () => request.get<ProcessTemplateVO[]>(`${BASE}/tree`),
  getDetail: (id: number) => request.get<ProcessTemplateVO>(`${BASE}/${id}`),
  create: (data: ProcessTemplateDTO) => request.post<number>(BASE, data),
  update: (id: number, data: ProcessTemplateDTO) => request.put(`${BASE}/${id}`, data),
  delete: (id: number) => request.delete(`${BASE}/${id}`),
}
