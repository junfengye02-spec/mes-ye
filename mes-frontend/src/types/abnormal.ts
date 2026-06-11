import type { BaseEntity, PageQuery } from './common'

export interface AbnormalContactVO extends BaseEntity {
  contactNo: string
  workOrderId?: number
  dispatchTaskId?: number
  subject?: string
  occurStage?: string
  eventCategory?: string
  productDivision?: string
  orderNo?: string
  customerProject?: string
  initiateDept?: string
  productModel?: string
  productType?: string
  productName?: string
  initiateProcess?: string
  qty?: number
  storageLocation?: string
  discoveryDate?: string
  abnormalDesc?: string
  status?: string
  affectSchedule?: number
  publishTime?: string
  attachments?: AbnormalContactAttachmentVO[]
  logs?: AbnormalContactLogVO[]
}

export interface AbnormalContactAttachmentVO extends BaseEntity {
  contactId: number
  fileNo?: string
  fileName?: string
  fileUrl?: string
  fileType?: string
  responsiblePerson?: string
  team?: string
  publishTime?: string
  submitTime?: string
  signatureProvider?: string
  signatureStatus?: string
  signed?: number
}

export interface AbnormalContactLogVO extends BaseEntity {
  contactId: number
  fromStatus?: string
  toStatus?: string
  action?: string
  operator?: string
  operatedTime?: string
  remark?: string
}

export interface AbnormalContactDTO {
  workOrderId?: number
  dispatchTaskId?: number
  subject?: string
  occurStage?: string
  eventCategory?: string
  productDivision?: string
  orderNo?: string
  customerProject?: string
  initiateDept?: string
  productModel?: string
  productType?: string
  productName?: string
  initiateProcess?: string
  qty?: number
  storageLocation?: string
  discoveryDate?: string
  abnormalDesc?: string
  affectSchedule?: number
}

export interface AbnormalContactQuery extends PageQuery {
  contactNo?: string
  subject?: string
  status?: string
  eventCategory?: string
  discoveryDate?: string
}
