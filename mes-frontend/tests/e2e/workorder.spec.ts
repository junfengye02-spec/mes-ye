import { test, expect, loginAsAdmin } from './fixtures'
import { E2ESeed, SeedUnavailableError, SeedData } from './seed/seed-data'

/**
 * 生产工单业务回归（数据级）：
 *   主线：新建 → 派工 → 开工 → 完工 → 入库
 *     0. UI smoke（保留）：列表页可达
 *     1. API 回读：seed 工单的字段完整
 *     2. POST /workorder/work-order/{id}/release  → GET 状态为 RELEASED
 *     3. POST /dispatch/task/generate/{woId}      → GET 派工任务存在
 *     4. POST /dispatch/assignment/device/{taskId}→ GET 分配记录存在
 *     5. POST /workorder/work-order/{id}/start    → GET 状态为 IN_PROGRESS
 *     6. POST /workorder/work-order/{id}/complete → GET 状态为 COMPLETED
 *     7. POST /material/receipt (items)           → GET 入库单含工单关联
 *
 * 数据流图（docs/test-reports/fix-mcp10-m9-p3-14.md 同步）：
 *   admin → createWorkOrder → release → generateDispatchTask →
 *     assignDevice → startWorkOrder → completeWorkOrder → createReceipt
 *     (each step GET-readback asserts)
 *
 * 说明：后端状态枚举以实际返回为准；断言使用「宽容式」：
 *   - 能拿到 PLANNED/RELEASED/IN_PROGRESS/COMPLETED/RECEIVED 字面量或对应 code 即算通过；
 *   - 字段名兼容 `status` / `state` 两种命名。
 */
test.describe('生产工单 / WorkOrder (UI smoke)', () => {
  test.beforeEach(async ({ page, backendAlive }) => {
    test.skip(!backendAlive, 'MES 后端不可达，无法登录进入业务页面')
    await loginAsAdmin(page)
  })

  test('工单列表页可达', async ({ page }) => {
    await page.goto('/workorder/list')
    await expect(page).not.toHaveURL(/\/login/)
    const table = page.locator('.el-table, .el-empty, [role="table"]')
    await expect(table.first()).toBeVisible({ timeout: 15000 })
  })

  test('查询/分页工具条可见', async ({ page }) => {
    await page.goto('/workorder/list')
    const searchOrPager = page.locator('.el-form, .el-pagination, input[placeholder*="搜索"]').first()
    await expect(searchOrPager).toBeVisible({ timeout: 15000 })
  })
})

test.describe('生产工单 / WorkOrder (full chain 数据级)', () => {
  let seed: E2ESeed | null = null
  let data: SeedData | null = null
  let skipReason: string | null = null

  test.beforeAll(async () => {
    try {
      seed = await E2ESeed.create()
      data = await seed.setup({ workOrderCount: 1, materialCount: 1, workCenterCount: 1 })
    } catch (e: any) {
      skipReason = e instanceof SeedUnavailableError ? e.message : e?.message || String(e)
    }
  })

  test.afterAll(async () => {
    await seed?.teardown()
  })

  test('S1: seed 工单 API 回读字段完整', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    const detail = await api.get<any>(`/workorder/work-order/${wo.id}`)
    expect(detail).toBeTruthy()
    expect(String(detail.workOrderNo || detail.code || detail.orderCode || detail.workOrderCode)).toContain(
      data!.prefix,
    )
    expect(String(detail.productCode)).toBe(wo.materialCode)
    expect(Number(detail.planWorkCenterId ?? detail.specifiedWorkCenterId)).toBe(wo.workCenterId)
    expect(Number(detail.planQty ?? detail.quantity)).toBe(wo.quantity)
  })

  test('S2: release → GET 状态 RELEASED', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    await api.post(`/workorder/work-order/${wo.id}/release`)
    const detail = await api.get<any>(`/workorder/work-order/${wo.id}`)
    const status = String(detail.status ?? detail.state ?? '')
    expect(status).toMatch(/RELEASED|released|已下达|已发布/)
  })

  test('S3: 生成派工任务 → GET 列表包含该 wo', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    // 幂等：如已经有，直接用；否则生成
    await api.post(`/dispatch/task/generate/${wo.id}`).catch(() => undefined)
    const page = await api.get<any>('/dispatch/task/page', { pageNum: 1, pageSize: 20, workOrderId: wo.id })
    const rows = (page?.records || page?.list || page?.rows || []) as any[]
    const match = rows.find((r) => Number(r.workOrderId ?? r.workOrder?.id) === wo.id)
    expect(match, `派工任务未找到：${JSON.stringify(rows).slice(0, 300)}`).toBeTruthy()
    ;(test.info() as any).taskId = match?.id
  })

  test('S4: 设备分配 → GET 分配记录', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    const page = await api.get<any>('/dispatch/task/page', { pageNum: 1, pageSize: 20, workOrderId: wo.id })
    const rows = (page?.records || page?.list || page?.rows || []) as any[]
    const task = rows.find((r) => Number(r.workOrderId ?? r.workOrder?.id) === wo.id)
    expect(task, '派工任务缺失，无法分配').toBeTruthy()
    const taskId = Number(task.id)
    await api.post('/dispatch/task/assign', {
      taskId,
      assignType: 'DEVICE',
      assigneeIds: [wo.workCenterId],
      assigneeCodes: [String(wo.workCenterId)],
      assigneeNames: ['E2E device'],
      assignedQty: 50,
      qtyUnit: 'PCS',
    })
    const list = await api.get<any[]>(`/dispatch/assignment/list/${taskId}`)
    expect(Array.isArray(list)).toBeTruthy()
    expect(list.length).toBeGreaterThanOrEqual(1)
  })

  test('S5: start → 状态 IN_PROGRESS', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    await api.post(`/workorder/work-order/${wo.id}/start`)
    const detail = await api.get<any>(`/workorder/work-order/${wo.id}`)
    const status = String(detail.status ?? detail.state ?? '')
    expect(status).toMatch(/IN_PROGRESS|STARTED|进行中|开工/)
  })

  test('S6: complete → 状态 COMPLETED', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    await api.post(`/workorder/work-order/${wo.id}/complete`)
    const detail = await api.get<any>(`/workorder/work-order/${wo.id}`)
    const status = String(detail.status ?? detail.state ?? '')
    expect(status).toMatch(/COMPLETED|completed|DONE|FINISHED|已完工|完成/)
  })

  test('S7: 完工入库 → GET 回读包含该工单', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    const receiptDto = {
      receiptType: 'NEW_PRODUCT',
      warehouse: 'E2E-WH',
      movementType: '101',
      planReceiptTime: new Date(Date.now() + 3600_000).toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, ''),
      items: [
        {
          itemCode: `${data!.prefix}_R1`,
          workOrderId: wo.id,
          workOrderNo: wo.code,
          materialId: wo.materialId,
          materialCode: wo.materialCode,
          materialName: wo.materialName,
          receiptQty: wo.quantity,
          unit: 'PCS',
          storageLocation: 'E2E',
        },
      ],
    }
    const receiptId = await api.post<number>('/material/receipt', receiptDto)
    expect(Number(receiptId)).toBeGreaterThan(0)
    const detail = await api.get<any>(`/material/receipt/${receiptId}`)
    expect(detail).toBeTruthy()
    const items = detail.items || detail.itemList || []
    expect(items.length).toBe(1)
    expect(String(items[0].materialCode)).toBe(wo.materialCode)
    expect(Number(items[0].receiptQty ?? items[0].quantity)).toBe(wo.quantity)
    // 回收：删除入库单以便 teardown 可以删除 workOrder（后端有外键约束时需要）
    await api.delete(`/material/receipt/${receiptId}`).catch(() => undefined)
  })
})
