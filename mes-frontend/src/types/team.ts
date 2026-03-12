import type { BaseEntity, PageQuery } from './common'

export interface ProductionTeamVO extends BaseEntity {
  teamCode: string
  teamName: string
  teamType?: string
  leaderId?: number
  leaderName?: string
  factory?: string
  workCenterId?: number
  workCenterName?: string
  memberCount?: number
  enabled?: number
  remark?: string
}

export interface ProductionTeamDTO {
  teamCode: string
  teamName: string
  teamType?: string
  leaderId?: number
  factory?: string
  workCenterId?: number
  enabled?: number
  remark?: string
}

export interface ProductionTeamQuery extends PageQuery {
  teamCode?: string
  teamName?: string
  teamType?: string
  factory?: string
  enabled?: number
}
