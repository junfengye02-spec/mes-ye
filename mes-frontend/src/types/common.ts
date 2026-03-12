export interface BaseEntity {
  id: number
  createdBy?: string
  createdTime?: string
  updatedBy?: string
  updatedTime?: string
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}
