import { ApiClient, LoginOptions } from './api-client'

/**
 * E2E seed —— 业务回归前预创建整套基础数据。
 *
 * 原则：
 *   1. 所有对象都带**唯一前缀** `e2e_<ts>_<rand>_`，保证与共享库数据不冲突
 *   2. 提供 setup()/teardown()；失败时 teardown 尽力而为（捕获单项异常）
 *   3. 能降级：后端不可达时 setup() 抛 SeedUnavailableError，spec 侧根据类型判断 skip
 *
 * 覆盖对象：
 *   - 租户（可选；若管理端账号本就绑定现有租户则复用）
 *   - 管理员+普通用户（RBAC 测试需要）
 *   - 物料、工作中心
 *   - 生产工单（供 workorder/dispatch 链路测试使用，以唯一 code 保证）
 *
 * 用法：
 *   const seed = new E2ESeed()
 *   const data = await seed.setup()
 *   // ...跑测试...
 *   await seed.teardown()
 */

export class SeedUnavailableError extends Error {
  constructor(msg: string) {
    super(msg)
    this.name = 'SeedUnavailableError'
  }
}

export interface SeedUser {
  id: number
  username: string
  password: string
  realName: string
  roleIds?: number[]
}

export interface SeedMaterial {
  id: number
  code: string
  name: string
}

export interface SeedWorkCenter {
  id: number
  code: string
  name: string
}

export interface SeedWorkOrder {
  id: number
  code: string
  orderNo: string
  materialId: number
  materialCode: string
  materialName: string
  workCenterId: number
  quantity: number
}

export interface SeedData {
  prefix: string
  tenantId?: number
  tenantCode?: string
  admin: SeedUser
  operator: SeedUser
  materials: SeedMaterial[]
  workCenters: SeedWorkCenter[]
  workOrders: SeedWorkOrder[]
  createdAt: number
}

export interface SeedOptions {
  /** 复用现有管理员账号直接登录（默认 admin/admin123） */
  admin?: LoginOptions
  /** 是否尝试创建独立租户；默认 false（直接用管理员登录后的租户上下文） */
  createTenant?: boolean
  /** 物料 / 工作中心数量 */
  materialCount?: number
  workCenterCount?: number
  /** 生产工单数量 */
  workOrderCount?: number
  /** 普通用户初始密码 */
  operatorPassword?: string
}

const DEFAULT_ADMIN: LoginOptions = {
  username: process.env.E2E_USER || 'admin',
  password: process.env.E2E_PASS || 'admin123',
  loginClient: 'ADMIN',
  tenantCode: process.env.E2E_TENANT || undefined,
}

function toLocalDateTime(value: Date): string {
  return value.toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, '')
}

export class E2ESeed {
  readonly prefix: string
  readonly client: ApiClient
  private data: SeedData | null = null

  constructor(client?: ApiClient, prefix?: string) {
    this.prefix = prefix || `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    this.client = client!
  }

  static async create(opts?: { prefix?: string; base?: string }): Promise<E2ESeed> {
    const client = await ApiClient.create(opts?.base)
    return new E2ESeed(client, opts?.prefix)
  }

  get seedData(): SeedData | null {
    return this.data
  }

  async setup(opts: SeedOptions = {}): Promise<SeedData> {
    const alive = await this.client.ping()
    if (!alive) {
      throw new SeedUnavailableError(
        `后端 ${this.client.base} 不可达，seed 跳过。设置 E2E_BACKEND_BASE 指向可用 MES 后端。`,
      )
    }

    const adminLogin = opts.admin || DEFAULT_ADMIN
    try {
      await this.client.login(adminLogin)
    } catch (e: any) {
      throw new SeedUnavailableError(`管理员登录失败，seed 跳过: ${e?.message || e}`)
    }

    const matCount = opts.materialCount ?? 2
    const wcCount = opts.workCenterCount ?? 2
    const woCount = opts.workOrderCount ?? 1

    const operator: SeedUser = await this.createOperatorUser(opts.operatorPassword || 'Qwer1234!')
    const materials: SeedMaterial[] = []
    for (let i = 0; i < matCount; i++) {
      materials.push(await this.createMaterial(i))
    }
    const workCenters: SeedWorkCenter[] = []
    for (let i = 0; i < wcCount; i++) {
      workCenters.push(await this.createWorkCenter(i))
    }
    const workOrders: SeedWorkOrder[] = []
    for (let i = 0; i < woCount; i++) {
      workOrders.push(await this.createWorkOrder(i, materials[0], workCenters[0].id))
    }

    this.data = {
      prefix: this.prefix,
      tenantCode: adminLogin.tenantCode,
      admin: {
        id: 0,
        username: adminLogin.username,
        password: adminLogin.password,
        realName: 'admin',
      },
      operator,
      materials,
      workCenters,
      workOrders,
      createdAt: Date.now(),
    }
    return this.data
  }

  /**
   * 尽力而为的清理：按创建逆序删除；单项失败不影响其他项。
   */
  async teardown(): Promise<void> {
    if (!this.data) {
      await this.client.dispose().catch(() => undefined)
      return
    }
    const d = this.data
    const swallow = async (fn: () => Promise<any>, tag: string) => {
      try {
        await fn()
      } catch (e: any) {
        console.warn(`[seed-teardown] ${tag} 失败（忽略）: ${e?.message || e}`)
      }
    }

    for (const wo of d.workOrders) {
      await swallow(() => this.client.delete(`/workorder/work-order/${wo.id}`), `workOrder#${wo.id}`)
    }
    for (const wc of d.workCenters) {
      await swallow(() => this.client.delete(`/basic/work-center/${wc.id}`), `workCenter#${wc.id}`)
    }
    for (const m of d.materials) {
      await swallow(() => this.client.delete(`/basic/material/${m.id}`), `material#${m.id}`)
    }
    if (d.operator.id) {
      await swallow(() => this.client.delete(`/system/user/${d.operator.id}`), `user#${d.operator.id}`)
    }
    await this.client.dispose().catch(() => undefined)
    this.data = null
  }

  private async createOperatorUser(password: string): Promise<SeedUser> {
    const username = `${this.prefix}_op`
    const dto = {
      username,
      password,
      realName: `E2E Operator ${this.prefix}`,
      enabled: true,
      accountType: 'STAFF',
      roleIds: [] as number[],
    }
    const id = await this.client.post<number>('/system/user', dto)
    return { id, username, password, realName: dto.realName }
  }

  private async createMaterial(idx: number): Promise<SeedMaterial> {
    const code = `${this.prefix}_M${idx}`
    const dto = {
      materialCode: code,
      materialName: `E2E物料-${this.prefix}-${idx}`,
      materialType: 'FG',
      categoryLevel1: 'E2E',
      factory: 'E2E',
      baseUnit: 'PCS',
      traceMode: 'QUANTITY',
      needInspection: 0,
    }
    const id = await this.client.post<number>('/basic/material', dto)
    return { id, code, name: dto.materialName }
  }

  private async createWorkCenter(idx: number): Promise<SeedWorkCenter> {
    const code = `${this.prefix}_WC${idx}`
    const dto = {
      workCenterCode: code,
      workCenterName: `E2E工作中心-${this.prefix}-${idx}`,
      workCenterCategory: 'MACHINE',
      businessUnit: 'E2E',
      workCalendar: 'STANDARD',
      resourceOrder: idx + 1,
      usageQty: 1,
      usageUnit: 'H',
      batchQty: 1,
      efficiency: 1,
      resourceType: 'DEVICE',
      resourceCapacity: 1,
      processNoInterrupt: 0,
      processNoCrossDay: 0,
      fixedTaktProduction: 0,
    }
    const id = await this.client.post<number>('/basic/work-center', dto)
    return { id, code, name: dto.workCenterName }
  }

  private async createWorkOrder(idx: number, material: SeedMaterial, workCenterId: number): Promise<SeedWorkOrder> {
    const code = `${this.prefix}_WO${idx}`
    const orderNo = `${this.prefix}_ORD${idx}`
    const quantity = 100
    const dto = {
      workOrderNo: code,
      workOrderType: 'E2E',
      orderNo,
      productCode: material.code,
      productName: material.name,
      projectName: 'E2E',
      planQty: quantity,
      qtyUnit: 'PCS',
      planWorkCenterId: workCenterId,
      specifiedWorkCenterId: workCenterId,
      factoryOrg: 'E2E',
      planOrg: 'E2E',
      mainOrg: 'E2E',
      planStartTime: toLocalDateTime(new Date(Date.now() + 3600_000)),
      planEndTime: toLocalDateTime(new Date(Date.now() + 2 * 3600_000)),
      remark: `e2e workorder ${this.prefix}-${idx}`,
      tasks: [
        {
          taskNo: `${this.prefix}_T${idx}_10`,
          taskName: `E2E工序-${idx}`,
          planWorkCenterId: workCenterId,
          planQty: quantity,
          qtyUnit: 'PCS',
          sequenceNo: 10,
          projectName: 'E2E',
        },
      ],
    }
    const id = await this.client.post<number>('/workorder/work-order', dto)
    return {
      id,
      code,
      orderNo,
      materialId: material.id,
      materialCode: material.code,
      materialName: material.name,
      workCenterId,
      quantity,
    }
  }
}

/**
 * 允许在 describe 级别复用：
 *   const seedHolder = shareSeed()
 *   test.beforeAll(async () => await seedHolder.ensure())
 *   test.afterAll(async () => await seedHolder.release())
 */
export function shareSeed(opts?: { options?: SeedOptions; base?: string }): {
  ensure: () => Promise<{ seed: E2ESeed; data: SeedData } | { skipped: true; reason: string }>
  release: () => Promise<void>
  current: () => E2ESeed | null
} {
  let seed: E2ESeed | null = null
  let data: SeedData | null = null
  let skipReason: string | null = null

  return {
    async ensure() {
      if (skipReason) return { skipped: true, reason: skipReason }
      if (seed && data) return { seed, data }
      try {
        seed = await E2ESeed.create({ base: opts?.base })
        data = await seed.setup(opts?.options || {})
        return { seed, data }
      } catch (e: any) {
        skipReason = e?.message || String(e)
        // seed.setup 失败需要释放
        await seed?.teardown().catch(() => undefined)
        seed = null
        data = null
        return { skipped: true, reason: skipReason }
      }
    },
    async release() {
      await seed?.teardown().catch(() => undefined)
      seed = null
      data = null
    },
    current() {
      return seed
    },
  }
}
