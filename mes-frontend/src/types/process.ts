import type { BaseEntity, PageQuery } from './common'

export interface InstructionVO extends BaseEntity {
  instructionNo: string
  version?: string
  status?: string
  upgradeFromId?: number
  projectNo?: string
  wbs?: string
  newOrRepairType?: string
  mainType?: string
  extensionData?: Record<string, unknown>
  gtType?: string
  productCategory?: string
  productType?: string
  partName?: string
  workOrderNo?: string
  workInstructionId?: number
  finishDate?: string
  qty?: number
  issueDate?: string
  finalDeliveryDate?: string
  checkSubmitDate?: string
  drawingNo?: string
  repairGuideDrawing?: string
  assignee?: string
  processingStatus?: string
  rawMaterialArrivalDate?: string
  rawMaterialPurchaseName?: string
  purchaseRequestNo?: string
  receiveTime?: string
  remark?: string
  stages?: InstructionStageVO[]
  serials?: InstructionSerialVO[]
}

export interface InstructionStageVO {
  id?: number
  instructionId?: number
  stage?: string
  role?: string
  content?: string
  requiredDate?: string
  actualDate?: string
}

export interface InstructionSerialVO {
  id?: number
  instructionId?: number
  productType?: string
  qty?: number
  receiveKgCode?: string
  sendGCode?: string
  scheduledCheckTime?: string
  receiveTime?: string
  remark?: string
}

export interface InstructionDTO {
  instructionNo: string
  projectNo?: string
  wbs?: string
  newOrRepairType?: string
  mainType?: string
  extensionData?: Record<string, unknown>
  gtType?: string
  productCategory?: string
  productType?: string
  partName?: string
  workOrderNo?: string
  workInstructionId?: number
  finishDate?: string
  qty?: number
  issueDate?: string
  finalDeliveryDate?: string
  checkSubmitDate?: string
  drawingNo?: string
  repairGuideDrawing?: string
  assignee?: string
  processingStatus?: string
  rawMaterialArrivalDate?: string
  rawMaterialPurchaseName?: string
  purchaseRequestNo?: string
  receiveTime?: string
  remark?: string
  stages?: InstructionStageDTO[]
  serials?: InstructionSerialDTO[]
}

export interface InstructionStageDTO {
  stage?: string
  role?: string
  content?: string
  requiredDate?: string
  actualDate?: string
}

export interface InstructionSerialDTO {
  productType?: string
  qty?: number
  receiveKgCode?: string
  sendGCode?: string
  scheduledCheckTime?: string
  receiveTime?: string
  remark?: string
}

export interface InstructionQuery extends PageQuery {
  instructionNo?: string
  version?: string
  status?: string
  productCategory?: string
  productType?: string
}

export interface ProcessTemplateVO extends BaseEntity {
  processNo: string
  processName?: string
  parentProcessNo?: string
  processType?: string
  description?: string
  children?: ProcessTemplateVO[]
}

export interface ProcessTemplateDTO {
  processNo: string
  processName?: string
  parentProcessNo?: string
  processType?: string
  description?: string
}

export interface ProcessTemplateQuery extends PageQuery {
  processNo?: string
  processName?: string
  processType?: string
}

export interface ProcessInfoVO extends BaseEntity {
  processNo?: string
  processName?: string
  processCode?: string
  product?: string
  gCode?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  processDrawing?: string
  processForm?: string
  processTemplateId?: number
  processType?: string
  factory?: string
  businessOrg?: string
  workCenterId?: number
  workshopArea?: string
  teamId?: number
  needStrip?: number
  handleTime?: number
  disassembleTime?: number
  installTime?: number
  remark?: string
}

export interface ProcessInfoDTO {
  processNo?: string
  processName?: string
  processCode?: string
  product?: string
  gCode?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  processDrawing?: string
  processForm?: string
  processTemplateId?: number
  processType?: string
  factory?: string
  businessOrg?: string
  workCenterId?: number
  workshopArea?: string
  teamId?: number
  needStrip?: number
  handleTime?: number
  disassembleTime?: number
  installTime?: number
  remark?: string
}

export interface ProcessInfoQuery extends PageQuery {
  processNo?: string
  processName?: string
  productCategory?: string
  processType?: string
  workCenterId?: number
}

export interface WorkInstructionVO extends BaseEntity {
  instructionCode: string
  instructionName: string
  processId?: number
  processName?: string
  version?: string
  content?: string
  remark?: string
  level?: string
  status?: string
  persons?: WorkInstructionPersonVO[]
}

export interface WorkInstructionPersonVO {
  id?: number
  workInstructionId?: number
  personCode?: string
  personName?: string
  personCategory?: string
  gender?: string
  birthDate?: string
  phone?: string
  email?: string
}

export interface WorkInstructionDTO {
  instructionCode: string
  instructionName: string
  processId?: number
  version?: string
  content?: string
  remark?: string
  level?: string
  status?: string
  persons?: WorkInstructionPersonDTO[]
}

export interface WorkInstructionPersonDTO {
  personCode?: string
  personName?: string
  personCategory?: string
  gender?: string
  birthDate?: string
  phone?: string
  email?: string
}

export interface WorkInstructionQuery extends PageQuery {
  instructionCode?: string
  instructionName?: string
  processId?: number
  level?: string
  status?: string
}

export interface SprayConditionVO extends BaseEntity {
  conditionNo: string
  ministerApprover?: string
  ministerApproveTime?: string
  sectionApprover?: string
  sectionApproveTime?: string
  leaderApprover?: string
  leaderApproveTime?: string
  powderFeedRate?: number
  sprayDistance?: number
  sprayGunModel?: string
  faiReport?: string
  faiGuide?: string
  powderFeeder?: string
  powderFeederSpeed?: number
  oxygenScfh?: number
  keroseneGph?: number
  combustionPressure?: number
  carrierGas?: string
  equipment?: string
  powderType?: string
}

export interface SprayConditionDTO {
  conditionNo: string
  ministerApprover?: string
  sectionApprover?: string
  leaderApprover?: string
  powderFeedRate?: number
  sprayDistance?: number
  sprayGunModel?: string
  faiReport?: string
  faiGuide?: string
  powderFeeder?: string
  powderFeederSpeed?: number
  oxygenScfh?: number
  keroseneGph?: number
  combustionPressure?: number
  carrierGas?: string
  equipment?: string
  powderType?: string
}

export interface SprayConditionQuery extends PageQuery {
  conditionNo?: string
  sprayGunModel?: string
  equipment?: string
}

export interface MachiningProgramVO extends BaseEntity {
  gCode: string
  programTable?: string
  productName?: string
}

export interface MachiningProgramDTO {
  gCode: string
  programTable?: string
  productName?: string
}

export interface MachiningProgramQuery extends PageQuery {
  gCode?: string
  productName?: string
}

export interface ManufacturingBomVO extends BaseEntity {
  bomCode: string
  bomName?: string
  productId?: number
  productCode?: string
  productName?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  newOrRepairType?: string
  bomVersion?: string
  status?: string
  effectiveDate?: string
  expiryDate?: string
  factoryOrg?: string
  upgradeFromId?: number
  remark?: string
  items?: ManufacturingBomItemVO[]
}

export interface ManufacturingBomItemVO extends BaseEntity {
  bomId: number
  parentItemId?: number
  level?: number
  materialId: number
  materialCode?: string
  materialName?: string
  materialSpec?: string
  materialType?: string
  quantity: number
  lossRate?: number
  unit?: string
  supplyType?: string
  routeStepId?: number
  processId?: number
  processNo?: string
  isSubstitute?: number
  substituteGroup?: string
  isKeyPart?: number
  sequenceNo?: number
  remark?: string
  children?: ManufacturingBomItemVO[]
}

export interface ManufacturingBomDTO {
  bomCode: string
  bomName?: string
  productId?: number
  productCode?: string
  productName?: string
  productCategory?: string
  machineModel?: string
  productType?: string
  newOrRepairType?: string
  effectiveDate?: string
  expiryDate?: string
  factoryOrg?: string
  remark?: string
  items?: ManufacturingBomItemDTO[]
}

export interface ManufacturingBomItemDTO {
  materialId?: number
  materialCode?: string
  materialName?: string
  materialSpec?: string
  materialType?: string
  quantity?: number
  lossRate?: number
  unit?: string
  supplyType?: string
  routeStepId?: number
  processId?: number
  processNo?: string
  isSubstitute?: number
  substituteGroup?: string
  isKeyPart?: number
  sequenceNo?: number
  remark?: string
  children?: ManufacturingBomItemDTO[]
}

export interface ManufacturingBomQuery extends PageQuery {
  bomCode?: string
  bomName?: string
  productCode?: string
  productCategory?: string
  status?: string
}
