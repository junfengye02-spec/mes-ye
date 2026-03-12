import type { BaseEntity, PageQuery } from './common'

export interface OrderPlanVO extends BaseEntity {
  orderNo: string
  customerName?: string
  productCode?: string
  productName?: string
  planQty: number
  qtyUnit?: string
  deliveryDate?: string
  status?: string
  priority?: number
  remark?: string
  apsOrderId?: string
  apsSyncStatus?: string
}

export interface OrderPlanDTO {
  orderNo: string
  customerName?: string
  productCode?: string
  productName?: string
  planQty: number
  qtyUnit?: string
  deliveryDate?: string
  priority?: number
  remark?: string
}

export interface OrderPlanQuery extends PageQuery {
  orderNo?: string
  customerName?: string
  productCode?: string
  status?: string
}

export interface ProductionPlanVO extends BaseEntity {
  planNo: string
  orderPlanId?: number
  orderNo?: string
  productCode?: string
  productName?: string
  planQty: number
  qtyUnit?: string
  planStartDate?: string
  planEndDate?: string
  status?: string
  remark?: string
}

export interface ProductionPlanDTO {
  planNo: string
  orderPlanId?: number
  productCode?: string
  productName?: string
  planQty: number
  qtyUnit?: string
  planStartDate?: string
  planEndDate?: string
  remark?: string
}

export interface ProductionPlanQuery extends PageQuery {
  planNo?: string
  orderNo?: string
  productCode?: string
  status?: string
}
