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

// 工作清单 DTO（对齐后端 WorkOrderTaskDTO）
export interface WorkOrderTaskDTO {
  taskNo?: string
  taskName?: string
  planWorkCenterId?: number
  planQty?: number
  qtyUnit?: string
  sequenceNo?: number
  serialNo?: string
  projectName?: string
}

// 输入物料 DTO（对齐后端 WorkOrderInputMaterialDTO）
export interface WorkOrderInputMaterialDTO {
  materialId?: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  qtyUnit?: string
  batchNo?: string
  serialNo?: string
}

// 输出物料 DTO（对齐后端 WorkOrderOutputMaterialDTO）
export interface WorkOrderOutputMaterialDTO {
  materialId?: number
  materialCode?: string
  materialName?: string
  outputQty?: number
  qtyUnit?: string
}

// 检验项目 DTO（对齐后端 WorkOrderQualityItemDTO）
export interface WorkOrderQualityItemDTO {
  qualityItemCode?: string
  qualityItemName?: string
  requirement?: string
}

// 约束关系 DTO（对齐后端 WorkOrderConstraintDTO）
export interface WorkOrderConstraintDTO {
  constraintType?: string
  relatedWorkOrderId?: number
  relatedTaskId?: number
  remark?: string
}

// 供应计划 DTO（对齐后端 WorkOrderSupplyPlanDTO）
export interface WorkOrderSupplyPlanDTO {
  demandPlanNo?: string
  supplyPlanNo?: string
  supplyQty?: number
  qtyUnit?: string
  planOrg?: string
  code?: string
}

// 工单主表 DTO（对齐后端 WorkOrderDTO，34 字段 + 6 子表）
export interface WorkOrderDTO {
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
  serialNo?: string
  specialStockFlag?: string
  deliveryLocation?: string
  remark?: string
  planStartTime?: string
  planEndTime?: string
  tasks?: WorkOrderTaskDTO[]
  inputMaterials?: WorkOrderInputMaterialDTO[]
  outputMaterials?: WorkOrderOutputMaterialDTO[]
  qualityItems?: WorkOrderQualityItemDTO[]
  constraints?: WorkOrderConstraintDTO[]
  supplyPlans?: WorkOrderSupplyPlanDTO[]
}

export interface WorkOrderQuery extends PageQuery {
  workOrderNo?: string
  productCode?: string
  productName?: string
  status?: string
  planStartTime?: string
  planEndTime?: string
}
