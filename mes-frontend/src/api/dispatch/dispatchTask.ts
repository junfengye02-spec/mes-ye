import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { DispatchTaskVO, DispatchTaskQuery, DispatchAssignmentVO, PersonAssignDTO, DeviceAssignDTO, TeamAssignDTO } from '@/types/dispatch'

const BASE = '/dispatch'

export const dispatchTaskApi = {
  page: (params: DispatchTaskQuery) => request.get<PageResult<DispatchTaskVO>>(`${BASE}/task/page`, params),
  getDetail: (id: number) => request.get<DispatchTaskVO>(`${BASE}/task/${id}`),
  generate: (workOrderId: number) => request.post(`${BASE}/task/generate/${workOrderId}`),
  assignPerson: (taskId: number, data: PersonAssignDTO) => request.post(`${BASE}/assignment/person/${taskId}`, data),
  assignDevice: (taskId: number, data: DeviceAssignDTO) => request.post(`${BASE}/assignment/device/${taskId}`, data),
  assignTeam: (taskId: number, data: TeamAssignDTO) => request.post(`${BASE}/assignment/team/${taskId}`, data),
  revokeAssignment: (assignmentId: number, reason: string) => request.post(`${BASE}/assignment/revoke/${assignmentId}`, null, { params: { reason } }),
  getAssignments: (taskId: number) => request.get<DispatchAssignmentVO[]>(`${BASE}/assignment/list/${taskId}`),
}
