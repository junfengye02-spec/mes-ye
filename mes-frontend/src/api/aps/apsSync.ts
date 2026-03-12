import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ApsSyncConfigVO, ApsSyncConfigDTO, ApsSyncConfigQuery, ApsSyncLogVO, ApsSyncLogQuery, ApsDataMappingVO, ApsDataMappingDTO, ApsDataMappingQuery, ApsSyncStatusVO } from '@/types/aps'

export const apsSyncConfigApi = {
  page: (params: ApsSyncConfigQuery) => request.get<PageResult<ApsSyncConfigVO>>('/aps/config/page', params),
  list: () => request.get<ApsSyncConfigVO[]>('/aps/config/list'),
  getDetail: (id: number) => request.get<ApsSyncConfigVO>(`/aps/config/${id}`),
  create: (data: ApsSyncConfigDTO) => request.post<number>('/aps/config', data),
  update: (id: number, data: ApsSyncConfigDTO) => request.put(`/aps/config/${id}`, data),
  delete: (id: number) => request.delete(`/aps/config/${id}`),
}

export const apsSyncLogApi = {
  page: (params: ApsSyncLogQuery) => request.get<PageResult<ApsSyncLogVO>>('/aps/log/page', params),
  getDetail: (id: number) => request.get<ApsSyncLogVO>(`/aps/log/${id}`),
}

export const apsDataMappingApi = {
  page: (params: ApsDataMappingQuery) => request.get<PageResult<ApsDataMappingVO>>('/aps/mapping/page', params),
  getDetail: (id: number) => request.get<ApsDataMappingVO>(`/aps/mapping/${id}`),
  create: (data: ApsDataMappingDTO) => request.post<number>('/aps/mapping', data),
  update: (id: number, data: ApsDataMappingDTO) => request.put(`/aps/mapping/${id}`, data),
  delete: (id: number) => request.delete(`/aps/mapping/${id}`),
}

export const apsSyncApi = {
  triggerDownstream: () => request.post('/aps/sync/downstream'),
  triggerDownstreamByType: (syncType: string) => request.post(`/aps/sync/downstream/${syncType}`),
  triggerUpstream: () => request.post('/aps/sync/upstream'),
  triggerCompensate: () => request.post('/aps/sync/compensate'),
  getStatus: () => request.get<ApsSyncStatusVO>('/aps/sync/status'),
  healthCheck: () => request.get('/aps/sync/health'),
}
