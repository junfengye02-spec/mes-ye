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
  priceType?: string
  price?: number
  currency?: string
  effectiveDate?: string
  expirationDate?: string
  supplier?: string
  remark?: string
}

export interface MaterialPriceDTO {
  materialId: number
  priceType?: string
  price?: number
  currency?: string
  effectiveDate?: string
  expirationDate?: string
  supplier?: string
  remark?: string
}

export interface MaterialPriceQuery extends PageQuery {
  materialCode?: string
  materialName?: string
  priceType?: string
  supplier?: string
}

export interface WorkCenterVO extends BaseEntity {
  workCenterCode: string
  workCenterName: string
  workCenterType?: string
  factory?: string
  planOrg?: string
  capacity?: number
  capacityUnit?: string
  enabled?: number
  remark?: string
}

export interface WorkCenterDTO {
  workCenterCode: string
  workCenterName: string
  workCenterType?: string
  factory?: string
  planOrg?: string
  capacity?: number
  capacityUnit?: string
  enabled?: number
  remark?: string
}

export interface WorkCenterQuery extends PageQuery {
  workCenterCode?: string
  workCenterName?: string
  workCenterType?: string
  factory?: string
}
