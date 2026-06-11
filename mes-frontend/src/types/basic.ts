import type { BaseEntity, PageQuery } from './common'

export interface MaterialVO extends BaseEntity {
  materialCode: string
  materialName: string
  materialType?: string
  categoryLevel1?: string
  categoryLevel2?: string
  gCode?: string
  productType?: string
  productCategory?: string
  machineModel?: string
  partName?: string
  factory?: string
  baseUnit?: string
  traceMode?: string
  serialGenerator?: string
  batchGenerator?: string
  barcodeType?: string
  needInspection?: number
  drawingNo?: string
  materialBrand?: string
  productImage?: string
}

export interface MaterialDTO {
  materialCode: string
  materialName: string
  materialType?: string
  categoryLevel1?: string
  categoryLevel2?: string
  gCode?: string
  productType?: string
  productCategory?: string
  machineModel?: string
  partName?: string
  factory?: string
  baseUnit?: string
  traceMode?: string
  serialGenerator?: string
  batchGenerator?: string
  barcodeType?: string
  needInspection?: number
  drawingNo?: string
  materialBrand?: string
  productImage?: string
}

export interface MaterialQuery extends PageQuery {
  materialCode?: string
  materialName?: string
  materialType?: string
  factory?: string
}

export interface MaterialPriceVO extends BaseEntity {
  materialId: number
  materialCode?: string
  materialName?: string
  unitPrice?: number
  unit?: string
}

export interface MaterialPriceDTO {
  materialId: number
  unitPrice?: number
  unit?: string
}

export interface MaterialPriceQuery extends PageQuery {
  materialCode?: string
  materialName?: string
}

export interface WorkCenterVO extends BaseEntity {
  workCenterCode: string
  workCenterName: string
  workCenterCategory?: string
  businessUnit?: string
  workCalendar?: string
  resourceOrder?: number
  usageQty?: number
  usageUnit?: string
  batchQty?: number
  efficiency?: number
  resourceType?: string
  resourceSubtype?: string
  resourceCapacity?: number
  processNoInterrupt?: number
  processNoCrossDay?: number
  fixedTaktProduction?: number
}

export interface WorkCenterDTO {
  workCenterCode: string
  workCenterName: string
  workCenterCategory?: string
  businessUnit?: string
  workCalendar?: string
  resourceOrder?: number
  usageQty?: number
  usageUnit?: string
  batchQty?: number
  efficiency?: number
  resourceType?: string
  resourceSubtype?: string
  resourceCapacity?: number
  processNoInterrupt?: number
  processNoCrossDay?: number
  fixedTaktProduction?: number
}

export interface WorkCenterQuery extends PageQuery {
  workCenterCode?: string
  workCenterName?: string
  workCenterCategory?: string
}
