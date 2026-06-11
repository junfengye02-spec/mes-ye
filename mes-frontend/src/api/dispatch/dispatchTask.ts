import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  DispatchTaskVO,
  DispatchTaskQuery,
  DispatchAssignmentVO,
  DispatchAssignDTO,
  DispatchTaskCreateDTO,
  DispatchTaskUpdateDTO,
  DispatchTaskAssignBatchDTO,
  DispatchTaskCompleteDTO,
} from '@/types/dispatch'

const BASE = '/dispatch'

export const dispatchTaskApi = {
  page: (params: DispatchTaskQuery) => request.get<PageResult<DispatchTaskVO>>(`${BASE}/task/page`, params),
  getDetail: (id: number) => request.get<DispatchTaskVO>(`${BASE}/task/${id}`),
  generate: (workOrderId: number) => request.post(`${BASE}/task/generate/${workOrderId}`),
  create: (data: DispatchTaskCreateDTO) => request.post<number>(`${BASE}/task/create`, data),
  update: (data: DispatchTaskUpdateDTO) => request.put(`${BASE}/task/update`, data),
  cancel: (id: number, cancelReason: string) => request.post(`${BASE}/task/cancel/${id}`, null, { params: { cancelReason } }),
  assign: (data: DispatchTaskAssignBatchDTO) => request.post(`${BASE}/task/assign`, data),
  unassign: (id: number, reason: string) => request.post(`${BASE}/task/unassign/${id}`, null, { params: { reason } }),
  start: (id: number) => request.post(`${BASE}/task/start/${id}`),
  complete: (id: number, data: DispatchTaskCompleteDTO) => request.post(`${BASE}/task/complete/${id}`, data),
  assignPerson: (taskId: number, data: DispatchAssignDTO) => request.post(`${BASE}/assignment/person/${taskId}`, data),
  assignDevice: (taskId: number, data: DispatchAssignDTO) => request.post(`${BASE}/assignment/device/${taskId}`, data),
  assignTeam: (taskId: number, data: DispatchAssignDTO) => request.post(`${BASE}/assignment/team/${taskId}`, data),
  revokeAssignment: (assignmentId: number, reason: string) => request.post(`${BASE}/assignment/revoke/${assignmentId}`, null, { params: { reason } }),
  getAssignments: (taskId: number) => request.get<DispatchAssignmentVO[]>(`${BASE}/assignment/list/${taskId}`),
}
