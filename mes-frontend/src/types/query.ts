import type { BaseEntity, PageQuery } from './common'

export interface WorkStatusViewVO extends BaseEntity {
  workOrderNo?: string
  productCode?: string
  productName?: string
  planQty?: number
  qtyUnit?: string
  status?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  workCenterName?: string
  assignedPerson?: string
}

export interface WorkStatusQuery extends PageQuery {
  status?: string
  workOrderNo?: string
  productCode?: string
}

export interface ProductionWorkVO extends BaseEntity {
  workOrderNo?: string
  taskNo?: string
  processName?: string
  productCode?: string
  productName?: string
  planQty?: number
  completedQty?: number
  status?: string
  assignedPerson?: string
  workCenterName?: string
}

export interface ProductionWorkQuery extends PageQuery {
  workOrderNo?: string
  processName?: string
  status?: string
}

export interface InspectionWorkVO extends BaseEntity {
  inspectionNo?: string
  workOrderNo?: string
  productCode?: string
  productName?: string
  inspectionType?: string
  inspector?: string
  inspectionDate?: string
  result?: string
  remark?: string
}

export interface InspectionWorkQuery extends PageQuery {
  workOrderNo?: string
  inspectionType?: string
  result?: string
}
