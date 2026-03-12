import type { BaseEntity, PageQuery } from './common'

export interface WorkOrderVO extends BaseEntity {
  workOrderNo: string
  workOrderType?: string
  productionPlanNo?: string
  orderPlanNo?: string
  orderNo?: string
  productCode?: string
  productName?: string
  mainProduct?: string
  machineModel?: string
  productCategory?: string
  productType?: string
  bomCode?: string
  projectName?: string
  wbsElement?: string
  newOrRepairType?: string
  workType?: string
  planQty?: number
  qtyUnit?: string
  factoryOrg?: string
  planOrg?: string
  mainOrg?: string
  planWorkCenterId?: number
  specifiedWorkCenterId?: number
  status?: string
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  serialNo?: string
  specialStockFlag?: string
  deliveryLocation?: string
  remark?: string
  tasks?: WorkOrderTaskVO[]
  inputMaterials?: WorkOrderInputMaterialVO[]
  outputMaterials?: WorkOrderOutputMaterialVO[]
  qualityItems?: WorkOrderQualityItemVO[]
  constraints?: WorkOrderConstraintVO[]
  supplyPlans?: WorkOrderSupplyPlanVO[]
  attachments?: WorkOrderAttachmentVO[]
}

export interface WorkOrderTaskVO extends BaseEntity {
  workOrderId: number
  taskNo: string
  taskName?: string
  planWorkCenterId?: number
  planQty?: number
  qtyUnit?: string
  status?: string
  sequenceNo?: number
  serialNo?: string
  projectName?: string
}

export interface WorkOrderInputMaterialVO extends BaseEntity {
  workOrderId: number
  materialId?: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  issuedQty?: number
  qtyUnit?: string
  batchNo?: string
  serialNo?: string
}

export interface WorkOrderOutputMaterialVO extends BaseEntity {
  workOrderId: number
  materialId?: number
  materialCode?: string
  materialName?: string
  outputQty?: number
  qtyUnit?: string
}

export interface WorkOrderQualityItemVO extends BaseEntity {
  workOrderId: number
  qualityItemCode?: string
  qualityItemName?: string
  requirement?: string
  status?: string
}

export interface WorkOrderConstraintVO extends BaseEntity {
  workOrderId: number
  constraintType?: string
  relatedWorkOrderId?: number
  relatedTaskId?: number
  remark?: string
}

export interface WorkOrderSupplyPlanVO extends BaseEntity {
  workOrderId: number
  demandPlanNo?: string
  supplyPlanNo?: string
  supplyQty?: number
  qtyUnit?: string
  planOrg?: string
  completedQty?: number
  code?: string
}

export interface WorkOrderAttachmentVO extends BaseEntity {
  workOrderId: number
  fileName?: string
  fileType?: string
  fileSizeKb?: number
  fileUrl?: string
  fileModifiedTime?: string
  modifiedBy?: string
  modifiedTime?: string
}

export interface WorkOrderDTO {
  workOrderNo: string
  workOrderType?: string
  productionPlanNo?: string
  orderPlanNo?: string
  productCode?: string
  productName?: string
  bomCode?: string
  planQty?: number
  qtyUnit?: string
  factoryOrg?: string
  planOrg?: string
  mainOrg?: string
  planWorkCenterId?: number
  planStartTime?: string
  planEndTime?: string
  remark?: string
}

export interface WorkOrderQuery extends PageQuery {
  workOrderNo?: string
  productCode?: string
  productName?: string
  status?: string
  planStartTime?: string
  planEndTime?: string
}
