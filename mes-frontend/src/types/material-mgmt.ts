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

// 完工入库明细 VO（对齐后端 FinishedGoodsReceiptItemVO）
export interface ReceiptItemVO {
  id?: number
  receiptId?: number
  itemCode?: string
  workOrderId?: number
  workOrderNo?: string
  materialCode?: string
  materialName?: string
  receiptQty?: number
  unit?: string
  storageLocation?: string
  varianceQty?: number
  varianceReason?: string
}

// 完工入库明细 DTO（对齐后端 FinishedGoodsReceiptItemDTO）
export interface ReceiptItemDTO {
  itemCode?: string
  workOrderId?: number
  workOrderNo?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  receiptQty?: number
  unit?: string
  storageLocation?: string
  varianceQty?: number
  varianceReason?: string
}

// 完工入库主表 VO（对齐后端 FinishedGoodsReceiptVO）
export interface ReceiptVO extends BaseEntity {
  receiptNo?: string
  receiptType?: string
  warehouse?: string
  movementType?: string
  planReceiptTime?: string
  actualReceiptTime?: string
  status?: string
  items?: ReceiptItemVO[]
}

// 完工入库主表 DTO（对齐后端 FinishedGoodsReceiptDTO）
export interface ReceiptDTO {
  receiptNo?: string
  receiptType?: string
  warehouse?: string
  movementType?: string
  planReceiptTime?: string
  items?: ReceiptItemDTO[]
}

// 完工入库查询（对齐后端 FinishedGoodsReceiptQuery）
export interface ReceiptQuery extends PageQuery {
  receiptNo?: string
  receiptType?: string
  status?: string
}

// 完工入库申请 VO（对齐后端 ReceiptRequestVO）
export interface ReceiptRequestVO extends BaseEntity {
  requestNo?: string
  receiptType?: string
  workOrderId?: number
  workOrderNo?: string
  projectName?: string
  materialCode?: string
  materialName?: string
  serialNo?: string
  qty?: number
  qualifiedQty?: number
  unqualifiedQty?: number
  pendingReceiptQty?: number
  status?: string
}

// 完工入库申请 DTO（对齐后端 ReceiptRequestDTO）
export interface ReceiptRequestDTO {
  requestNo?: string
  receiptType?: string
  workOrderId?: number
  workOrderNo?: string
  projectName?: string
  wbsElement?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  serialNo?: string
  qty?: number
  qualifiedQty?: number
  unqualifiedQty?: number
  unit?: string
  description?: string
  planReceiptTime?: string
}

// 完工入库申请查询（对齐后端 ReceiptRequestQuery）
export interface ReceiptRequestQuery extends PageQuery {
  requestNo?: string
  receiptType?: string
  workOrderId?: number
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
