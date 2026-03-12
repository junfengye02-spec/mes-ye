import type { BaseEntity, PageQuery } from './common'

export interface StorageInventoryVO extends BaseEntity {
  factory?: string
  inventoryOrg?: string
  warehouse?: string
  storageLocation?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  unrestrictedStock?: number
  qualityStock?: number
  frozenStock?: number
  unit?: string
  teamId?: number
}

export interface InventoryQuery extends PageQuery {
  warehouse?: string
  storageLocation?: string
  materialCode?: string
  materialName?: string
}

export interface RequisitionVO extends BaseEntity {
  requisitionNo?: string
  workOrderId?: number
  workOrderNo?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  issuedQty?: number
  qtyUnit?: string
  status?: string
  remark?: string
}

export interface RequisitionDTO {
  workOrderId?: number
  materialId?: number
  requiredQty?: number
  qtyUnit?: string
  remark?: string
}

export interface RequisitionQuery extends PageQuery {
  requisitionNo?: string
  workOrderNo?: string
  materialCode?: string
  status?: string
}

export interface RequisitionOrderVO extends BaseEntity {
  orderNo?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  totalQty?: number
  issuedQty?: number
  qtyUnit?: string
  status?: string
  remark?: string
}

export interface RequisitionOrderDTO {
  materialId?: number
  totalQty?: number
  qtyUnit?: string
  remark?: string
}

export interface RequisitionOrderQuery extends PageQuery {
  orderNo?: string
  materialCode?: string
  status?: string
}

export interface ReceiptVO extends BaseEntity {
  receiptNo?: string
  workOrderId?: number
  workOrderNo?: string
  productCode?: string
  productName?: string
  receiptType?: string
  receiptQty?: number
  qtyUnit?: string
  storageLocation?: string
  status?: string
  remark?: string
}

export interface ReceiptDTO {
  workOrderId?: number
  receiptType?: string
  receiptQty?: number
  qtyUnit?: string
  storageLocation?: string
  remark?: string
}

export interface ReceiptQuery extends PageQuery {
  receiptNo?: string
  workOrderNo?: string
  receiptType?: string
  status?: string
}

export interface ReceiptRequestVO extends BaseEntity {
  requestNo?: string
  workOrderId?: number
  workOrderNo?: string
  productCode?: string
  productName?: string
  requestQty?: number
  qtyUnit?: string
  requestType?: string
  status?: string
  remark?: string
}

export interface ReceiptRequestDTO {
  workOrderId?: number
  requestQty?: number
  qtyUnit?: string
  requestType?: string
  remark?: string
}

export interface ReceiptRequestQuery extends PageQuery {
  requestNo?: string
  workOrderNo?: string
  requestType?: string
  status?: string
}

export interface MaterialReturnVO extends BaseEntity {
  returnNo?: string
  workOrderId?: number
  workOrderNo?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  returnQty?: number
  qtyUnit?: string
  returnReason?: string
  status?: string
  remark?: string
}

export interface MaterialReturnDTO {
  workOrderId?: number
  materialId?: number
  returnQty?: number
  qtyUnit?: string
  returnReason?: string
  remark?: string
}

export interface MaterialReturnQuery extends PageQuery {
  returnNo?: string
  workOrderNo?: string
  materialCode?: string
  status?: string
}

export interface DeliverySignVO extends BaseEntity {
  deliveryNo?: string
  orderNo?: string
  productCode?: string
  productName?: string
  deliveryQty?: number
  qtyUnit?: string
  deliveryDate?: string
  signDate?: string
  signPerson?: string
  status?: string
  remark?: string
}

export interface DeliverySignDTO {
  orderNo?: string
  productCode?: string
  productName?: string
  deliveryQty?: number
  qtyUnit?: string
  deliveryDate?: string
  remark?: string
}

export interface DeliverySignQuery extends PageQuery {
  deliveryNo?: string
  orderNo?: string
  productCode?: string
  status?: string
}
