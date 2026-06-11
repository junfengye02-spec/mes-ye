import type { BaseEntity, PageQuery } from './common'

export interface DispatchTaskVO extends BaseEntity {
  workOrderId?: number
  workOrderTaskId?: number
  orderNo?: string
  processNo?: string
  workName?: string
  planWorkCenterId?: number
  serialNo?: string
  projectName?: string
  planQty?: number
  qtyUnit?: string
  dispatchStatus?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  actualQty?: number
  qualityResult?: string
  cancelReason?: string
  assignments?: DispatchAssignmentVO[]
}

export interface DispatchTaskQuery extends PageQuery {
  workOrderId?: number
  orderNo?: string
  processNo?: string
  dispatchStatus?: string
}

export interface DispatchAssignmentVO extends BaseEntity {
  dispatchTaskId?: number
  assignType?: string
  assigneeId?: number
  assigneeCode?: string
  assigneeName?: string
  assignedQty?: number
  qtyUnit?: string
  status?: string
  assignedBy?: string
  assignedTime?: string
  revokedBy?: string
  revokedTime?: string
}

export interface DispatchAssignDTO {
  assigneeId: number
  assigneeCode: string
  assigneeName?: string
  assignedQty?: number
  qtyUnit?: string
}

export interface DispatchTaskCreateDTO {
  workOrderId: number
  workOrderTaskId?: number
  orderNo: string
  processNo: string
  workName: string
  planWorkCenterId?: number
  serialNo?: string
  projectName?: string
  planQty: number
  qtyUnit: string
  planStartTime?: string
  planEndTime?: string
}

export interface DispatchTaskUpdateDTO {
  id: number
  orderNo?: string
  processNo?: string
  workName?: string
  planWorkCenterId?: number
  serialNo?: string
  projectName?: string
  planQty?: number
  qtyUnit?: string
  planStartTime?: string
  planEndTime?: string
}

export interface DispatchTaskAssignBatchDTO {
  taskId: number
  assignType: 'PERSON' | 'EQUIPMENT' | 'DEVICE' | 'TEAM'
  assigneeIds: number[]
  assigneeCodes?: string[]
  assigneeNames?: string[]
  assignedQty?: number
  qtyUnit?: string
}

export interface DispatchTaskCompleteDTO {
  actualStartTime?: string
  actualEndTime: string
  actualQty: number
  qualityResult: 'PASS' | 'FAIL' | 'NA'
  remark?: string
}
