import type { BaseEntity, PageQuery } from './common'

export interface OrderPlanVO extends BaseEntity {
  orderNo?: string
  productCode?: string
  productName?: string
  projectName?: string
  wbsElement?: string
  newOrRepairType?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  productType?: string
  planQty?: number
  qtyUnit?: string
  factoryOrg?: string
  planOrg?: string
  mainOrg?: string
  planWorkCenterId?: number
  status?: string
  flowStatus?: string
  expandStatus?: string
  completionStatus?: string
  isOrder?: boolean
  flowCode?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  dataSource?: string
  apsOrderId?: number
  apsSyncBatchId?: string
  apsSyncStatus?: string
}

export interface OrderPlanDTO {
  orderNo: string
  productCode?: string
  productName?: string
  projectName?: string
  wbsElement?: string
  newOrRepairType?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  productType?: string
  planQty?: number
  qtyUnit?: string
  factoryOrg?: string
  planOrg?: string
  mainOrg?: string
  planWorkCenterId?: number
  isOrder?: boolean
  flowCode?: string
  planStartTime?: string
  planEndTime?: string
  dataSource?: string
}

export interface OrderPlanQuery extends PageQuery {
  orderNo?: string
  productCode?: string
  productName?: string
  status?: string
  flowStatus?: string
  expandStatus?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  dataSource?: string
}

export interface ProductionPlanVO extends BaseEntity {
  orderPlanId?: number
  orderNo?: string
  productCode?: string
  productName?: string
  newOrRepairType?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  productType?: string
  wbsElement?: string
  workOrderType?: string
  planOrg?: string
  planQty?: number
  qtyUnit?: string
  completedQty?: number
  status?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
}

export interface ProductionPlanDTO {
  orderPlanId: number
  orderNo?: string
  productCode?: string
  productName?: string
  newOrRepairType?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  productType?: string
  wbsElement?: string
  workOrderType?: string
  planOrg?: string
  planQty?: number
  qtyUnit?: string
  planStartTime?: string
  planEndTime?: string
}

export interface ProductionPlanQuery extends PageQuery {
  orderNo?: string
  productCode?: string
  productName?: string
  status?: string
  businessType?: string
  workType?: string
  machineModel?: string
  productCategory?: string
  orderPlanId?: number
}
