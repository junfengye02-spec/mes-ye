import type { BaseEntity, PageQuery } from './common'

export interface RecheckRequestVO extends BaseEntity {
  recheckNo?: string
  orderNo?: string
  productCode?: string
  productName?: string
  serialNo?: string
  recheckReason?: string
  status?: string
  remark?: string
}

export interface RecheckRequestDTO {
  orderNo?: string
  productCode?: string
  productName?: string
  serialNo?: string
  recheckReason?: string
  remark?: string
}

export interface RecheckRequestQuery extends PageQuery {
  recheckNo?: string
  orderNo?: string
  productCode?: string
  status?: string
}

export interface WorkStartCheckVO extends BaseEntity {
  checkNo?: string
  workOrderId?: number
  workOrderNo?: string
  checkDate?: string
  checker?: string
  checkResult?: string
  remark?: string
}

export interface WorkStartCheckDTO {
  workOrderId?: number
  checkDate?: string
  checker?: string
  checkResult?: string
  remark?: string
}

export interface WorkStartCheckQuery extends PageQuery {
  workOrderNo?: string
  checker?: string
  checkResult?: string
}

export interface OrderStartCheckVO extends BaseEntity {
  checkNo?: string
  workOrderId?: number
  workOrderNo?: string
  checkDate?: string
  checker?: string
  checkResult?: string
  remark?: string
}

export interface OrderStartCheckDTO {
  workOrderId?: number
  checkDate?: string
  checker?: string
  checkResult?: string
  remark?: string
}

export interface OrderStartCheckQuery extends PageQuery {
  workOrderNo?: string
  checker?: string
  checkResult?: string
}

export interface ShiftHandoverVO extends BaseEntity {
  handoverNo?: string
  workOrderId?: number
  workOrderNo?: string
  handoverPerson?: string
  receivePerson?: string
  handoverTime?: string
  receiveTime?: string
  content?: string
  status?: string
  remark?: string
}

export interface ShiftHandoverDTO {
  workOrderId?: number
  handoverPerson?: string
  content?: string
  remark?: string
}

export interface ShiftHandoverQuery extends PageQuery {
  workOrderNo?: string
  handoverPerson?: string
  status?: string
}
