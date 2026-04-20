import type { BaseEntity, PageQuery } from './common'

export interface ApsSyncConfigVO extends BaseEntity {
  configKey: string
  configValue: string
  configDesc?: string
  enabled?: number
}

export interface ApsSyncConfigDTO {
  configKey: string
  configValue: string
  configDesc?: string
  enabled?: number
}

export interface ApsSyncConfigQuery extends PageQuery {
  configKey?: string
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
  durationMs?: number
}

export interface ApsSyncLogQuery extends PageQuery {
  syncDirection?: string
  syncType?: string
  status?: string
  startTimeFrom?: string
  startTimeTo?: string
}

export interface ApsDataMappingVO extends BaseEntity {
  mappingType?: string
  mesCode?: string
  mesName?: string
  apsCode?: string
  apsName?: string
  enabled?: number
}

export interface ApsDataMappingDTO {
  mappingType?: string
  mesCode?: string
  mesName?: string
  apsCode?: string
  apsName?: string
  enabled?: number
}

export interface ApsDataMappingQuery extends PageQuery {
  mappingType?: string
  mesCode?: string
  apsCode?: string
  enabled?: number
}

export interface ApsSyncStatusVO {
  apsAvailable: boolean
  circuitBreakerState: string
  pendingUpstreamCount: number
  pendingCompensationCount?: number
}
