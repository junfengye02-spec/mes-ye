import type { BaseEntity, PageQuery } from './common'

export interface DispatchTaskVO extends BaseEntity {
  taskNo: string
  workOrderId: number
  workOrderNo?: string
  taskName?: string
  processName?: string
  planQty?: number
  qtyUnit?: string
  status?: string
  planStartTime?: string
  planEndTime?: string
  assignedPersonName?: string
  assignedDeviceName?: string
  assignedTeamName?: string
}

export interface DispatchTaskQuery extends PageQuery {
  taskNo?: string
  workOrderNo?: string
  status?: string
}

export interface DispatchAssignmentVO extends BaseEntity {
  taskId: number
  assignmentType: string
  resourceId: number
  resourceName?: string
  resourceCode?: string
  status?: string
}

export interface PersonAssignDTO {
  personId: number
  personName?: string
}

export interface DeviceAssignDTO {
  deviceId: number
  deviceName?: string
}

export interface TeamAssignDTO {
  teamId: number
  teamName?: string
}
