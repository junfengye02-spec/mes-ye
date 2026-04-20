import type { BaseEntity, PageQuery } from './common'

export interface RecheckRequestVO extends BaseEntity {
  projectCode?: string
  projectName?: string
  materialCode?: string
  materialName?: string
  productionOrderNo?: string
  recheckRequirement?: string
  recheckReason?: string
  recheckProposer?: string
  recheckProposeTime?: string
  requiredDeliveryTime?: string
  isReasonable?: boolean
  reviewer?: string
  reviewDate?: string
  status?: string
  remark?: string
}

export interface RecheckRequestDTO {
  projectCode?: string
  projectName?: string
  materialCode?: string
  materialName?: string
  productionOrderNo?: string
  recheckRequirement?: string
  recheckReason?: string
  recheckProposer?: string
  recheckProposeTime?: string
  requiredDeliveryTime?: string
}

export interface RecheckRequestQuery extends PageQuery {
  projectCode?: string
  materialCode?: string
  productionOrderNo?: string
  status?: string
}

export interface WorkStartCheckVO extends BaseEntity {
  workNo?: string
  workOrderTaskId?: number
  workOrderId?: number
  workOrderNo?: string
  checkItem?: string
  checkResult?: string
  checkStatus?: string
  checkRemark?: string
  checker?: string
  checkTime?: string
  remark?: string
}

export interface WorkStartCheckDTO {
  workNo?: string
  workOrderTaskId?: number
  workOrderId?: number
  workOrderNo?: string
  checkItem: string
  checkResult?: string
  checkStatus: string
  checkRemark?: string
  remark?: string
}

export interface WorkStartCheckQuery extends PageQuery {
  workOrderId?: number
  workOrderNo?: string
  checkStatus?: string
}

export interface OrderStartCheckVO extends BaseEntity {
  workNo?: string
  workOrderTaskId?: number
  workOrderId?: number
  workOrderNo?: string
  checkItem?: string
  checkResult?: string
  checkStatus?: string
  checkRemark?: string
  checker?: string
  checkTime?: string
  remark?: string
}

export interface OrderStartCheckDTO {
  workOrderId?: number
  workOrderNo?: string
  checkItem: string
  checkResult?: string
  checkStatus: string
  checkRemark?: string
  remark?: string
}

export interface OrderStartCheckQuery extends PageQuery {
  workOrderId?: number
  workOrderNo?: string
  checkStatus?: string
}

export interface ShiftHandoverVO extends BaseEntity {
  projectName?: string
  productSerialNo?: string
  processContent?: string
  handoverDate?: string
  handoverWeekday?: string
  handoverTime?: string
  handoverTeamId?: number
  handoverTeamName?: string
  handoverShift?: string
  takeoverShift?: string
  takeoverTeamId?: number
  takeoverTeamName?: string
  handoverPerson?: string
  takeoverPerson?: string
  teamLeader?: string
  planQty?: number
  actualQty?: number
  gapAnalysis?: string
  handoverContent?: string
  otherMatters?: string
  status?: string
}

export interface ShiftHandoverDTO {
  projectName?: string
  productSerialNo?: string
  processContent?: string
  handoverDate?: string
  handoverWeekday?: string
  handoverTime?: string
  handoverTeamId?: number
  handoverTeamName?: string
  handoverShift?: string
  takeoverShift?: string
  takeoverTeamId?: number
  takeoverTeamName?: string
  handoverPerson?: string
  takeoverPerson?: string
  teamLeader?: string
  planQty?: number
  actualQty?: number
  gapAnalysis?: string
  handoverContent?: string
  otherMatters?: string
}

export interface ShiftHandoverQuery extends PageQuery {
  projectName?: string
  handoverDate?: string
  handoverTeamName?: string
  status?: string
}
