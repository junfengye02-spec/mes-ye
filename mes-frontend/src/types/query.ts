import type { BaseEntity, PageQuery } from './common'

export interface WorkStatusViewVO extends BaseEntity {
  workNo?: string
  sequenceNo?: number
  processNo?: string
  workName?: string
  isOutput?: boolean
  processForm?: string
  processDrawing?: string
  status?: string
  description?: string
  furnaceNo?: string
  belongProcess?: string
  factory?: string
  businessOrg?: string
  planWorkCenterName?: string
  specifiedWorkCenterName?: string
  planTeamName?: string
  planShift?: string
  sourceNo?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  issued?: boolean
}

export interface WorkStatusQuery extends PageQuery {
  status?: string
  workNo?: string
  workName?: string
  factory?: string
  businessOrg?: string
}

export interface ProductionWorkVO extends BaseEntity {
  workNo?: string
  workName?: string
  workOrderId?: number
  workOrderNo?: string
  productMaterial?: string
  productionFactory?: string
  productionOrg?: string
  actualStartTime?: string
  actualEndTime?: string
  planStartTime?: string
  planEndTime?: string
  actualProcessTime?: number
  timeUnit?: string
  isReportPoint?: boolean
  isCheckPoint?: boolean
  isHandoverPoint?: boolean
  remark?: string
}

export interface ProductionWorkQuery extends PageQuery {
  workNo?: string
  workName?: string
  workOrderNo?: string
  workOrderId?: number
}

export interface InspectionWorkVO extends BaseEntity {
  workNo?: string
  workName?: string
  planInspectQty?: number
  inspectedQty?: number
  qualifiedQty?: number
  unqualifiedQty?: number
  judgment?: string
  isCheckPoint?: boolean
  dispatchStatus?: string
  workStatus?: string
  inspectType?: string
  inspectCategory?: string
  qcOrg?: string
  inspectFactory?: string
  planTeamLab?: string
  actualStartTime?: string
  actualEndTime?: string
  isReportPoint?: boolean
  workOrderId?: number
  workOrderNo?: string
  orderStatus?: string
  description?: string
}

export interface InspectionWorkQuery extends PageQuery {
  workNo?: string
  workName?: string
  workStatus?: string
  workOrderId?: number
  inspectCategory?: string
}
