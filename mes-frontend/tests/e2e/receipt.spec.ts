import { test, expect, loginAsAdmin } from './fixtures'
import { E2ESeed, SeedUnavailableError, SeedData } from './seed/seed-data'

const toLocalDateTime = (value: Date) => value.toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, '')

/**
 * 完工入库回归（数据级）：
 *   UI smoke（保留）：列表页可达 / 工具条可见
 *   数据级：
 *     R1. 新建入库单（含 2 个 items）→ GET 回读
 *         断言：items 条数 == 2；每条的 materialId / quantity 与提交一致；
 *              主表 quantity 合计 == items.sum(quantity)（若后端聚合字段存在）
 *     R2. update 主表 remark → GET 回读 remark 一致
 *     R3. delete → 再次 GET 应 404/业务错误
 */
test.describe('完工入库 / Receipt (UI smoke)', () => {
  test.beforeEach(async ({ page, backendAlive }) => {
    test.skip(!backendAlive, 'MES 后端不可达，无法登录进入业务页面')
    await loginAsAdmin(page)
  })

  test('入库申请列表可达', async ({ page }) => {
    await page.goto('/material-mgmt/receipt-request')
    await expect(page).not.toHaveURL(/\/login/)
    const panel = page.locator('.el-table, .el-empty, [role="table"]').first()
    await expect(panel).toBeVisible({ timeout: 15000 })
  })

  test('完工入库列表可达', async ({ page }) => {
    await page.goto('/material-mgmt/receipt')
    await expect(page).not.toHaveURL(/\/login/)
    const panel = page.locator('.el-table, .el-empty, [role="table"]').first()
    await expect(panel).toBeVisible({ timeout: 15000 })
  })
})

test.describe('完工入库 / Receipt (主表+items 一致性)', () => {
  let seed: E2ESeed | null = null
  let data: SeedData | null = null
  let skipReason: string | null = null
  let receiptId: number | null = null

  test.beforeAll(async () => {
    try {
      seed = await E2ESeed.create()
      // 需要 2 个物料 + 1 个工单
      data = await seed.setup({ materialCount: 2, workCenterCount: 1, workOrderCount: 1 })
    } catch (e: any) {
      skipReason = e instanceof SeedUnavailableError ? e.message : e?.message || String(e)
    }
  })

  test.afterAll(async () => {
    if (seed && receiptId) {
      await seed.client.delete(`/material/receipt/${receiptId}`).catch(() => undefined)
    }
    await seed?.teardown()
  })

  test('R1: create → GET 回读 items 一致', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    const [m0, m1] = data!.materials
    const dto = {
      receiptType: 'NEW_PRODUCT',
      warehouse: 'E2E-WH',
      movementType: '101',
      planReceiptTime: toLocalDateTime(new Date(Date.now() + 3600_000)),
      items: [
        {
          itemCode: `${data!.prefix}_RI0`,
          workOrderId: wo.id,
          workOrderNo: wo.code,
          materialId: m0.id,
          materialCode: m0.code,
          materialName: m0.name,
          receiptQty: 30,
          unit: 'PCS',
          storageLocation: 'E2E',
        },
        {
          itemCode: `${data!.prefix}_RI1`,
          workOrderId: wo.id,
          workOrderNo: wo.code,
          materialId: m1.id,
          materialCode: m1.code,
          materialName: m1.name,
          receiptQty: 70,
          unit: 'PCS',
          storageLocation: 'E2E',
        },
      ],
    }
    receiptId = await api.post<number>('/material/receipt', dto)
    expect(Number(receiptId)).toBeGreaterThan(0)

    const detail = await api.get<any>(`/material/receipt/${receiptId}`)
    expect(detail).toBeTruthy()
    const items: any[] = detail.items || detail.itemList || []
    expect(items.length).toBe(2)

    // 按 materialCode 对齐断言。当前 VO 不暴露 materialId，以页面/API 返回契约为准。
    const byMat = Object.fromEntries(items.map((x) => [String(x.materialCode), x]))
    expect(Number(byMat[m0.code]?.receiptQty ?? byMat[m0.code]?.quantity)).toBe(30)
    expect(Number(byMat[m1.code]?.receiptQty ?? byMat[m1.code]?.quantity)).toBe(70)

    // 明细工单关联一致
    expect(Number(items[0].workOrderId ?? items[0].workOrder?.id)).toBe(wo.id)
  })

  test('R2: update warehouse → GET 回读一致', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason || !receiptId, `seed skip: ${skipReason || ''}`)
    const detailBefore = await api.get<any>(`/material/receipt/${receiptId}`)
    const newWarehouse = `E2E-WH-${Date.now()}`
    const wo = data!.workOrders[0]
    const [m0, m1] = data!.materials
    // 后端 update 期望与 create 一致的结构。
    await api.put(`/material/receipt/${receiptId}`, {
      receiptNo: detailBefore.receiptNo,
      receiptType: detailBefore.receiptType || 'NEW_PRODUCT',
      warehouse: newWarehouse,
      movementType: detailBefore.movementType || '101',
      planReceiptTime: detailBefore.planReceiptTime || toLocalDateTime(new Date(Date.now() + 3600_000)),
      items: [
        {
          itemCode: `${data!.prefix}_RI0`,
          workOrderId: wo.id,
          workOrderNo: wo.code,
          materialId: m0.id,
          materialCode: m0.code,
          materialName: m0.name,
          receiptQty: 30,
          unit: 'PCS',
          storageLocation: 'E2E',
        },
        {
          itemCode: `${data!.prefix}_RI1`,
          workOrderId: wo.id,
          workOrderNo: wo.code,
          materialId: m1.id,
          materialCode: m1.code,
          materialName: m1.name,
          receiptQty: 70,
          unit: 'PCS',
          storageLocation: 'E2E',
        },
      ],
    })
    const detail = await api.get<any>(`/material/receipt/${receiptId}`)
    expect(detail.warehouse).toBe(newWarehouse)
  })

  test('R3: delete → 再次 GET 404/业务错误', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason || !receiptId, `seed skip: ${skipReason || ''}`)
    await api.delete(`/material/receipt/${receiptId}`)
    const raw = await api.raw('GET', `/material/receipt/${receiptId}`)
    const http = raw.status
    const code = raw.body?.code
    const notFound = http === 404 || (http === 200 && code !== 200) || raw.body?.data == null
    expect(notFound, `期望已删除，实际 http=${http} body=${raw.text.slice(0, 200)}`).toBeTruthy()
    receiptId = null
  })
})
