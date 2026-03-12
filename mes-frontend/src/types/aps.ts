import type { BaseEntity, PageQuery } from './common'

export interface ApsSyncConfigVO extends BaseEntity {
  configKey: string
  configValue: string
  description?: string
  enabled?: number
}

export interface ApsSyncConfigDTO {
  configKey: string
  configValue: string
  description?: string
  enabled?: number
}

export interface ApsSyncConfigQuery extends PageQuery {
  configKey?: string
  description?: string
  enabled?: number
}

export interface ApsSyncLogVO extends BaseEntity {
  batchId?: string
  syncDirection?: string
  syncType?: string
  totalCount?: number
  successCount?: number
  failCount?: number
  status?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
}

export interface ApsSyncLogQuery extends PageQuery {
  syncDirection?: string
  syncType?: string
  status?: string
  startTime?: string
  endTime?: string
}

export interface ApsDataMappingVO extends BaseEntity {
  mappingType?: string
  mesField?: string
  apsField?: string
  mesValue?: string
  apsValue?: string
  description?: string
  enabled?: number
}

export interface ApsDataMappingDTO {
  mappingType?: string
  mesField?: string
  apsField?: string
  mesValue?: string
  apsValue?: string
  description?: string
  enabled?: number
}

export interface ApsDataMappingQuery extends PageQuery {
  mappingType?: string
  mesField?: string
  enabled?: number
}

export interface ApsSyncStatusVO {
  apsAvailable: boolean
  circuitBreakerState: string
  pendingQueueCount: number
  lastSyncTime?: string
}
