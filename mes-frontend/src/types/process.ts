import type { BaseEntity, PageQuery } from './common'

export interface InstructionVO extends BaseEntity {
  instructionCode: string
  instructionName: string
  version?: string
  status?: string
  productCode?: string
  productName?: string
  remark?: string
  stages?: InstructionStageVO[]
  serialNos?: InstructionSerialNoVO[]
}

export interface InstructionStageVO extends BaseEntity {
  instructionId: number
  stageName: string
  stageOrder: number
  description?: string
}

export interface InstructionSerialNoVO extends BaseEntity {
  instructionId: number
  serialNo: string
}

export interface InstructionDTO {
  instructionCode: string
  instructionName: string
  version?: string
  productCode?: string
  productName?: string
  remark?: string
  stages?: { stageName: string; stageOrder: number; description?: string }[]
  serialNos?: { serialNo: string }[]
}

export interface InstructionQuery extends PageQuery {
  instructionCode?: string
  instructionName?: string
  productCode?: string
  status?: string
}

export interface ProcessTemplateVO extends BaseEntity {
  templateCode: string
  templateName: string
  parentId?: number
  processType?: string
  description?: string
  children?: ProcessTemplateVO[]
}

export interface ProcessTemplateDTO {
  templateCode: string
  templateName: string
  parentId?: number
  processType?: string
  description?: string
}

export interface ProcessTemplateQuery extends PageQuery {
  templateCode?: string
  templateName?: string
  processType?: string
}

export interface ProcessInfoVO extends BaseEntity {
  processCode: string
  processName: string
  processType?: string
  workCenterId?: number
  workCenterName?: string
  standardTime?: number
  timeUnit?: string
  description?: string
}

export interface ProcessInfoDTO {
  processCode: string
  processName: string
  processType?: string
  workCenterId?: number
  standardTime?: number
  timeUnit?: string
  description?: string
}

export interface ProcessInfoQuery extends PageQuery {
  processCode?: string
  processName?: string
  processType?: string
}

export interface WorkInstructionVO extends BaseEntity {
  instructionCode: string
  instructionName: string
  processId?: number
  processName?: string
  version?: string
  content?: string
  remark?: string
  personnel?: WorkInstructionPersonnelVO[]
}

export interface WorkInstructionPersonnelVO extends BaseEntity {
  workInstructionId: number
  personnelName: string
  role?: string
}

export interface WorkInstructionDTO {
  instructionCode: string
  instructionName: string
  processId?: number
  version?: string
  content?: string
  remark?: string
  personnel?: { personnelName: string; role?: string }[]
}

export interface WorkInstructionQuery extends PageQuery {
  instructionCode?: string
  instructionName?: string
}

export interface SprayConditionVO extends BaseEntity {
  conditionCode: string
  conditionName: string
  temperature?: string
  humidity?: string
  paintType?: string
  thickness?: string
  remark?: string
}

export interface SprayConditionDTO {
  conditionCode: string
  conditionName: string
  temperature?: string
  humidity?: string
  paintType?: string
  thickness?: string
  remark?: string
}

export interface SprayConditionQuery extends PageQuery {
  conditionCode?: string
  conditionName?: string
}

export interface MachiningProgramVO extends BaseEntity {
  programCode: string
  programName: string
  machineType?: string
  programContent?: string
  version?: string
  remark?: string
}

export interface MachiningProgramDTO {
  programCode: string
  programName: string
  machineType?: string
  programContent?: string
  version?: string
  remark?: string
}

export interface MachiningProgramQuery extends PageQuery {
  programCode?: string
  programName?: string
  machineType?: string
}

export interface ManufacturingBomVO extends BaseEntity {
  bomCode: string
  bomName: string
  version?: string
  productCode?: string
  productName?: string
  status?: string
  remark?: string
  items?: ManufacturingBomItemVO[]
}

export interface ManufacturingBomItemVO extends BaseEntity {
  bomId: number
  materialId: number
  materialCode?: string
  materialName?: string
  quantity: number
  unit?: string
  parentItemId?: number
  level?: number
  children?: ManufacturingBomItemVO[]
}

export interface ManufacturingBomDTO {
  bomCode: string
  bomName: string
  version?: string
  productCode?: string
  productName?: string
  remark?: string
}

export interface ManufacturingBomQuery extends PageQuery {
  bomCode?: string
  bomName?: string
  productCode?: string
  status?: string
}
