import type { BaseEntity, PageQuery } from './common'

export interface ProductionTeamVO extends BaseEntity {
  teamCode: string
  teamName: string
  orgId?: number
  orgCode?: string
  orgName?: string
  enabled?: number
  description?: string
}

export interface ProductionTeamDTO {
  teamCode: string
  teamName: string
  orgId?: number
  orgCode?: string
  orgName?: string
  description?: string
}

export interface ProductionTeamQuery extends PageQuery {
  teamCode?: string
  teamName?: string
  enabled?: number
}
