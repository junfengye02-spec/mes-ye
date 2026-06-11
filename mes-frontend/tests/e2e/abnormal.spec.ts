import { test, expect, loginAsAdmin } from './fixtures'
import { ApiClient } from './seed/api-client'
import { E2ESeed, SeedUnavailableError, SeedData } from './seed/seed-data'

/**
 * 异常联络 + 权限越界回归：
 *   UI smoke（保留）：列表页可达 / 提交对话框可弹出 / 状态筛选可见
 *   数据级 & RBAC：
 *     A1. admin 创建异常联络 → submit → process → close（每步 GET 回读状态变化）
 *     A2. 普通用户（STAFF）登录后，调用 admin 限定接口（/system/user）应 403/越权业务码
 */
test.describe('异常联络 / Abnormal (UI smoke)', () => {
  test.beforeEach(async ({ page, backendAlive }) => {
    test.skip(!backendAlive, 'MES 后端不可达，无法登录进入业务页面')
    await loginAsAdmin(page)
  })

  test('异常联络列表可达', async ({ page }) => {
    await page.goto('/abnormal/contact')
    await expect(page).not.toHaveURL(/\/login/)
    const panel = page.locator('.el-table, .el-empty, [role="table"]').first()
    await expect(panel).toBeVisible({ timeout: 15000 })
  })

  test('提交异常对话框可弹出并取消', async ({ page }) => {
    await page.goto('/abnormal/contact')
    const addBtn = page
      .locator('button:has-text("提交"), button:has-text("新增"), button:has-text("新 增"), button:has-text("上报")')
      .first()
    if (await addBtn.isVisible().catch(() => false)) {
      await addBtn.click()
      const dialog = page.locator('.el-dialog, .el-drawer').first()
      await expect(dialog).toBeVisible({ timeout: 10000 })
      await page.keyboard.press('Escape')
    }
  })

  test('新增异常对话框展示关联工单和派工字段', async ({ page }) => {
    await page.goto('/abnormal/contact')
    const addBtn = page
      .locator('button:has-text("新增"), button:has-text("新 增"), button:has-text("上报")')
      .first()
    await addBtn.click()
    const dialog = page.locator('.el-dialog').first()
    await expect(dialog).toBeVisible({ timeout: 10000 })
    await expect(dialog.getByText('关联工单ID')).toBeVisible()
    await expect(dialog.getByText('关联派工任务ID')).toBeVisible()
  })

  test('新增异常对话框会把关联工单和派工字段提交到请求体', async ({ page }) => {
    let capturedPayload: any = null

    await page.route('**/api/abnormal/contact/page**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { list: [], total: 0 } }),
      })
    })

    await page.route('**/api/abnormal/contact', async route => {
      if (route.request().method() === 'POST') {
        capturedPayload = route.request().postDataJSON()
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: 1001 }),
        })
        return
      }
      await route.continue()
    })

    await page.goto('/abnormal/contact')
    const addBtn = page
      .locator('button:has-text("新增"), button:has-text("新 增"), button:has-text("上报")')
      .first()
    await addBtn.click()
    const dialog = page.locator('.el-dialog').first()

    await dialog.getByLabel('主题').last().fill('UI abnormal payload test')
    await dialog.getByLabel('关联工单ID').fill('101')
    await dialog.getByLabel('关联派工任务ID').fill('202')
    await page.locator('.el-dialog__footer .el-button--primary').click()

    await expect.poll(() => capturedPayload).not.toBeNull()
    expect(capturedPayload.workOrderId).toBe(101)
    expect(capturedPayload.dispatchTaskId).toBe(202)
  })

  test('状态筛选或处理按钮可见', async ({ page }) => {
    await page.goto('/abnormal/contact')
    await expect(page.locator('.abnormal-contact-list .search-card')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('异常联系单列表')).toBeVisible()
    const hasStatusFilter = await page
      .locator('.abnormal-contact-list .search-card .el-form-item')
      .filter({ hasText: '状态' })
      .first()
      .isVisible()
      .catch(() => false)
    const hasRowAction = (await page.locator('button:has-text("处理"), button:has-text("关闭")').filter({ visible: true }).count()) > 0
    expect(hasStatusFilter || hasRowAction).toBeTruthy()
  })
})

test.describe('异常联络 / Abnormal (数据级状态机)', () => {
  let seed: E2ESeed | null = null
  let data: SeedData | null = null
  let skipReason: string | null = null
  let contactId: number | null = null

  test.beforeAll(async () => {
    try {
      seed = await E2ESeed.create()
      data = await seed.setup({ materialCount: 1, workCenterCount: 1, workOrderCount: 1 })
    } catch (e: any) {
      skipReason = e instanceof SeedUnavailableError ? e.message : e?.message || String(e)
    }
  })

  test.afterAll(async () => {
    if (seed && contactId) {
      await seed.client.delete(`/abnormal/contact/${contactId}`).catch(() => undefined)
    }
    await seed?.teardown()
  })

  test('A1.1: create 异常联络', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const wo = data!.workOrders[0]
    const dto = {
      subject: `e2e-abn-${data!.prefix}`,
      occurStage: 'PRODUCTION',
      eventCategory: 'QUALITY',
      orderNo: wo.orderNo,
      customerProject: 'E2E',
      initiateDept: 'QA',
      productName: wo.materialName,
      qty: 1,
      discoveryDate: new Date().toISOString().slice(0, 10),
      abnormalDesc: 'playwright auto abnormal',
      affectSchedule: 0,
    }
    contactId = await api.post<number>('/abnormal/contact', dto)
    expect(Number(contactId)).toBeGreaterThan(0)
    const detail = await api.get<any>(`/abnormal/contact/${contactId}`)
    expect(detail?.subject).toBe(dto.subject)
  })

  test('A1.2: submit → 状态 SUBMITTED', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason || !contactId, `seed skip: ${skipReason || ''}`)
    await api.post(`/abnormal/contact/${contactId}/submit`)
    const detail = await api.get<any>(`/abnormal/contact/${contactId}`)
    const st = String(detail.status ?? detail.state ?? '')
    expect(st).toMatch(/SUBMITTED|submitted|已提交|处理中/i)
  })

  test('A1.3: process → 状态 PROCESSING', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason || !contactId, `seed skip: ${skipReason || ''}`)
    await api.post(`/abnormal/contact/${contactId}/process`).catch(() => undefined)
    const detail = await api.get<any>(`/abnormal/contact/${contactId}`)
    const st = String(detail.status ?? detail.state ?? '')
    expect(st).toMatch(/PROCESS|处理|IN_PROGRESS|已处理/i)
  })

  test('A1.4: close → 状态 CLOSED', async ({ api }) => {
    test.skip(!seed || !data || !!skipReason || !contactId, `seed skip: ${skipReason || ''}`)
    await api.post(`/abnormal/contact/${contactId}/close`).catch(() => undefined)
    const detail = await api.get<any>(`/abnormal/contact/${contactId}`)
    const st = String(detail.status ?? detail.state ?? '')
    expect(st).toMatch(/CLOSED|closed|关闭|已关闭/i)
  })
})

test.describe('异常联络 / Abnormal (RBAC 权限越界 403)', () => {
  let seed: E2ESeed | null = null
  let data: SeedData | null = null
  let skipReason: string | null = null

  test.beforeAll(async () => {
    try {
      seed = await E2ESeed.create()
      data = await seed.setup({ materialCount: 0, workCenterCount: 0, workOrderCount: 0 })
    } catch (e: any) {
      skipReason = e instanceof SeedUnavailableError ? e.message : e?.message || String(e)
    }
  })

  test.afterAll(async () => {
    await seed?.teardown()
  })

  test('A2: STAFF 调 /system/user → 403 / 越权业务码', async () => {
    test.skip(!seed || !data || !!skipReason, `seed skip: ${skipReason || ''}`)
    const op = data!.operator
    // 用 ApiClient 以 STAFF 身份登录（USER 客户端）
    const staffClient = await ApiClient.create()
    try {
      // 先尝试 USER 入口（STAFF 正常路径）
      const loginRes = await staffClient.raw('POST', '/auth/login', {
        username: op.username,
        password: op.password,
        loginClient: 'USER',
      })
      const token = loginRes.body?.data?.accessToken
      if (!token) {
        test.info().annotations.push({
          type: 'note',
          description: 'STAFF 无法登录，跳过越权断言',
        })
        return
      }
      staffClient.setToken(token)

      const probes = [
        { url: '/system/user/page', params: { pageNum: 1, pageSize: 1 } },
        { url: '/platform/tenants', params: { pageNum: 1, pageSize: 1 } },
      ]
      let anyDenied = false
      const details: string[] = []
      for (const p of probes) {
        const r = await staffClient.raw('GET', p.url, undefined, p.params)
        const http = r.status
        const code = r.body?.code
        const denied = http === 401 || http === 403 || (http === 200 && code !== 200)
        details.push(`${p.url}: http=${http} code=${code}`)
        if (denied) anyDenied = true
      }
      expect(anyDenied, `期望至少一个管理员接口对 STAFF 拒绝，实际：${details.join(' | ')}`).toBeTruthy()
    } finally {
      await staffClient.dispose()
    }
  })
})
