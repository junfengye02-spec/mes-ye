import type { Page } from '@playwright/test'
import { expect, test } from './fixtures'

type ApiCall = {
  method: string
  path: string
  url: string
  payload: unknown
}

const ok = (data: unknown) => ({
  code: 200,
  message: 'OK',
  data,
})

const pageResult = (list: unknown[]) => ({ list, total: list.length })

const material = {
  id: 501,
  materialCode: 'MAT-ACTION',
  materialName: '动作物料',
  baseUnit: 'PCS',
}

const workOrderCreated = {
  id: 100,
  workOrderNo: 'WO-ACTION-CREATED',
  workOrderType: 'NORMAL',
  productionPlanNo: 'PP-ACTION',
  orderPlanNo: 'OP-ACTION',
  orderNo: 'ORDER-ACTION',
  productCode: 'PROD-ACTION',
  productName: '动作产品',
  planQty: 5,
  qtyUnit: 'PCS',
  status: 'CREATED',
  mainOrg: 'ORG-A',
  planStartTime: '2026-05-31 08:00:00',
  planEndTime: '2026-05-31 12:00:00',
  inputMaterials: [
    {
      materialId: material.id,
      materialCode: material.materialCode,
      materialName: material.materialName,
      requiredQty: 3,
      issuedQty: 1,
      qtyUnit: 'PCS',
    },
  ],
}

const workOrderReleased = { ...workOrderCreated, id: 101, workOrderNo: 'WO-ACTION-RELEASED', status: 'RELEASED' }
const workOrderProgress = { ...workOrderCreated, id: 102, workOrderNo: 'WO-ACTION-PROGRESS', status: 'IN_PROGRESS' }

const PAGE_ROWS: Record<string, unknown[]> = {
  '/aps/config/page': [
    { id: 10, configKey: 'sync.enabled', configValue: 'true', configDesc: '是否启用', enabled: 1 },
  ],
  '/aps/mapping/page': [
    { id: 11, mappingType: 'WORK_CENTER', mesCode: 'MES-WC', mesName: 'MES工作中心', apsCode: 'APS-WC', apsName: 'APS资源', enabled: 1 },
  ],
  '/basic/material/page': [material],
  '/process/instruction/page': [
    { id: 20, instructionNo: 'INS-ACTION', projectNo: 'PRJ-ACTION', productCategory: 'CAT', productType: 'TYPE', workOrderNo: 'WO-ACTION', status: 'DRAFT' },
  ],
  '/process/manufacturing-bom/page': [
    { id: 30, bomCode: 'BOM-ACTION', bomName: '动作BOM', version: 'A', productCode: 'PROD-ACTION', productName: '动作产品', status: 'DRAFT' },
  ],
  '/team/production-team/page': [
    { id: 40, teamCode: 'TEAM-ACTION', teamName: '动作班组', orgId: 1, orgCode: 'ORG', orgName: '组织', enabled: 1 },
  ],
  '/material/delivery-sign/page': [
    {
      id: 50,
      lineNo: '10',
      workOrderId: 100,
      workOrderNo: 'WO-ACTION',
      materialId: material.id,
      materialCode: material.materialCode,
      materialName: material.materialName,
      planDeliveryQty: 3,
      pendingSignQty: 2,
      unit: 'PCS',
      deliveryWarehouse: 'WH-A',
    },
  ],
  '/abnormal/contact/page': [
    { id: 60, contactNo: 'ABN-DRAFT', subject: '草稿异常', status: 'DRAFT', occurStage: '生产', eventCategory: '质量', productName: '产品A', qty: 1 },
    { id: 61, contactNo: 'ABN-SUBMITTED', subject: '已提交异常', status: 'SUBMITTED', occurStage: '生产', eventCategory: '质量', productName: '产品B', qty: 1 },
    { id: 62, contactNo: 'ABN-PROCESSING', subject: '处理中异常', status: 'PROCESSING', occurStage: '生产', eventCategory: '质量', productName: '产品C', qty: 1 },
  ],
  '/plan/order-plan/page': [
    { id: 70, orderNo: 'OP-ACTION-CREATED', productCode: 'PROD-A', productName: '产品A', planQty: 2, qtyUnit: 'PCS', status: 'CREATED', dataSource: 'MES' },
    { id: 71, orderNo: 'OP-ACTION-RELEASED', productCode: 'PROD-B', productName: '产品B', planQty: 2, qtyUnit: 'PCS', status: 'RELEASED', dataSource: 'MES' },
  ],
  '/plan/production-plan/page': [
    { id: 72, orderPlanId: 70, orderNo: 'OP-ACTION-CREATED', productCode: 'PROD-A', productName: '产品A', planQty: 2, qtyUnit: 'PCS', status: 'CREATED' },
  ],
  '/workorder/work-order/page': [workOrderCreated, workOrderReleased, workOrderProgress],
  '/dispatch/task/page': [
    { id: 80, workOrderId: 100, workOrderTaskId: 1000, orderNo: 'ORDER-ACTION', processNo: 'P10', workName: '未派工', planQty: 3, qtyUnit: 'PCS', dispatchStatus: 'UNASSIGNED' },
    { id: 81, workOrderId: 101, workOrderTaskId: 1001, orderNo: 'ORDER-ACTION', processNo: 'P20', workName: '已派工', planQty: 3, qtyUnit: 'PCS', dispatchStatus: 'ASSIGNED' },
    { id: 82, workOrderId: 102, workOrderTaskId: 1002, orderNo: 'ORDER-ACTION', processNo: 'P30', workName: '进行中', planQty: 3, qtyUnit: 'PCS', dispatchStatus: 'IN_PROGRESS' },
  ],
  '/quality/recheck/page': [
    { id: 90, projectCode: 'PRJ-ACTION', materialCode: 'MAT-ACTION', materialName: '动作物料', productionOrderNo: 'WO-ACTION', recheckReason: '原因', recheckProposer: '张三', status: 'CREATED' },
    { id: 91, projectCode: 'PRJ-ACTION', materialCode: 'MAT-ACTION', materialName: '动作物料', productionOrderNo: 'WO-ACTION', recheckReason: '原因', recheckProposer: '张三', status: 'SUBMITTED' },
    { id: 92, projectCode: 'PRJ-ACTION', materialCode: 'MAT-ACTION', materialName: '动作物料', productionOrderNo: 'WO-ACTION', recheckReason: '原因', recheckProposer: '张三', status: 'IN_REVIEW', reviewer: '李四', reviewDate: '2026-05-31', isReasonable: 1 },
    { id: 93, projectCode: 'PRJ-ACTION', materialCode: 'MAT-ACTION', materialName: '动作物料', productionOrderNo: 'WO-ACTION', recheckReason: '原因', recheckProposer: '张三', status: 'APPROVED' },
  ],
  '/quality/shift-handover/page': [
    { id: 94, projectName: '项目A', productSerialNo: 'SN-ACTION', handoverDate: '2026-05-31', handoverTeamName: '一班', handoverPerson: '交接人', takeoverPerson: '接班人', status: 'PENDING', handoverContent: '交接内容' },
  ],
  '/quality/work-start-check/page': [
    { id: 95, workOrderId: 100, workOrderNo: 'WO-ACTION', workNo: 'WORK-ACTION', checkItem: '首件检查', checkResult: '正常', checkStatus: 'PASSED', checker: '检查员' },
  ],
  '/quality/order-start-check/page': [
    { id: 96, workOrderNo: 'ORDER-ACTION', workNo: 'ORDER-WORK-ACTION', checkItem: '订单检查', checkResult: '正常', checkStatus: 'PASSED', checker: '检查员' },
  ],
  '/query/production-work/page': [
    { id: 120, workOrderNo: 'PW-ACTION', productCode: 'PROD-ACTION', productName: '动作产品', status: 'IN_PROGRESS' },
  ],
  '/query/inspection-work/page': [
    { id: 121, workNo: 'INSP-ACTION', workName: '动作检验作业', workOrderNo: 'WO-ACTION', workStatus: 'CREATED', inspectCategory: 'FINAL' },
  ],
  '/system/user/page': [
    { id: 130, username: 'action_user', realName: '动作用户', enabled: true, accountType: 'ADMIN' },
  ],
  '/system/role/page': [
    { id: 131, roleName: '动作角色', roleCode: 'ACTION_ROLE', enabled: true },
  ],
  '/system/menu/tree': [
    { id: 132, parentId: 0, menuName: '动作菜单', menuType: 'MENU', sortOrder: 1, visible: true },
  ],
  '/platform/tenants': [
    { id: 140, tenantCode: 'tenant-action', tenantName: '动作租户', status: 1, schemaMode: 'SHARED', quotaUsers: 10 },
    { id: 141, tenantCode: 'tenant-paused', tenantName: '暂停租户', status: 3, schemaMode: 'SHARED', quotaUsers: 10 },
  ],
}

function dataFor(path: string, method: string) {
  if (path === '/auth/user-info') {
    return {
      id: 1,
      username: 'admin',
      realName: '管理员',
      tenantId: 0,
      accountType: 'ADMIN',
      roles: ['ADMIN', 'PLATFORM_ADMIN'],
      permissions: ['*:*:*'],
    }
  }

  if (path === '/system/menu/user-tree') return []
  if (path === '/aps/sync/status') {
    return {
      apsAvailable: true,
      circuitBreakerState: 'CLOSED',
      pendingUpstreamCount: 0,
      pendingCompensationCount: 0,
    }
  }
  if (path === '/system/role/list') return PAGE_ROWS['/system/role/page']
  if (path === '/system/role/131/menus') return [132]
  if (path === '/dispatch/assignment/list/80') {
    return [
      { id: 801, taskId: 80, assignType: 'PERSON', assigneeName: '张三', assigneeCode: 'P001', assignedQty: 1, status: 'ACTIVE', assignedBy: 'admin' },
    ]
  }
  if (path === '/process/manufacturing-bom/30/items/tree') {
    return [
      { id: 301, materialCode: 'MAT-ACTION', materialName: '动作物料', quantity: 1, unit: 'PCS' },
    ]
  }
  if (path === '/abnormal/contact/60') {
    return {
      ...(PAGE_ROWS['/abnormal/contact/page'][0] as object),
      attachments: [
        { id: 601, fileNo: 'F001', fileName: '异常附件.pdf', responsiblePerson: '张三', team: '一班', signed: false },
      ],
    }
  }
  if (path === '/abnormal/contact/60/attachments') {
    return [
      { id: 601, fileNo: 'F001', fileName: '异常附件.pdf', responsiblePerson: '张三', team: '一班', signed: false },
    ]
  }
  if (path === '/workorder/work-order/100') return workOrderCreated
  if (path === '/platform/tenants/140') return PAGE_ROWS['/platform/tenants'][0]

  if (method === 'GET') {
    if (PAGE_ROWS[path]) {
      if (path.endsWith('/tree')) return PAGE_ROWS[path]
      return pageResult(PAGE_ROWS[path])
    }
    if (path.endsWith('/page')) return pageResult([])
    if (path.endsWith('/tree') || path.includes('/list/')) return []
    return {}
  }

  return 1
}

async function mockApi(page: Page) {
  const calls: ApiCall[] = []
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'action-dataflow-token')
  })

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (!url.pathname.startsWith('/api/')) {
      await route.fallback()
      return
    }

    const method = request.method()
    const path = url.pathname.replace(/^\/api/, '')
    let payload: unknown
    try {
      payload = request.postDataJSON()
    } catch {
      payload = undefined
    }

    if (path !== '/auth/user-info' && path !== '/system/menu/user-tree') {
      calls.push({ method, path, url: url.toString(), payload })
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok(dataFor(path, method))),
    })
  })

  return calls
}

async function openPage(page: Page, calls: ApiCall[], path: string) {
  await page.goto(path)
  await expect(page.locator('body')).toBeVisible()
  await page.waitForLoadState('networkidle').catch(() => undefined)
  calls.length = 0
}

function findCall(calls: ApiCall[], method: string, path: string) {
  return calls.find((call) => call.method === method && call.path === path)
}

async function expectCall(calls: ApiCall[], method: string, path: string) {
  await expect.poll(() => Boolean(findCall(calls, method, path)), {
    message: `${method} ${path}`,
  }).toBeTruthy()
  return findCall(calls, method, path)!
}

async function clickAndExpect(page: Page, calls: ApiCall[], method: string, path: string, click: () => Promise<void>) {
  calls.length = 0
  await click()
  return expectCall(calls, method, path)
}

async function confirmMessage(page: Page) {
  const box = page.locator('.el-message-box').last()
  if (await box.isVisible().catch(() => false)) {
    await box.locator('.el-button--primary').last().click()
    await expect(box).toBeHidden()
    return
  }

  const popconfirm = page.locator('.el-popper, [role="tooltip"]').filter({ hasText: /确定|确认|删除/ }).last()
  await expect(popconfirm).toBeVisible()
  await popconfirm.getByRole('button', { name: /^(Yes|确定|确认)$/ }).last().click()
  await expect(popconfirm).toBeHidden()
}

async function promptMessage(page: Page, value = 'action reason') {
  const box = page.locator('.el-message-box').last()
  await expect(box).toBeVisible()
  await box.locator('input').fill(value)
  await box.locator('.el-button--primary').last().click()
  await expect(box).toBeHidden()
}

function dialog(page: Page) {
  return page.locator('.el-dialog:visible').last()
}

async function closeDialog(page: Page) {
  const target = dialog(page)
  await expect(target).toBeVisible()
  await target.locator('.el-dialog__headerbtn').click()
  await expect(target).toBeHidden()
}

async function closeDrawer(page: Page) {
  const drawer = page.locator('.el-drawer:visible').last()
  await expect(drawer).toBeVisible()
  await drawer.locator('.el-drawer__close-btn').click()
  await expect(drawer).toBeHidden()
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function fillDialogFields(page: Page, fields: Record<string, string>) {
  const target = dialog(page)
  await expect(target).toBeVisible()
  for (const [label, value] of Object.entries(fields)) {
    const input = target.getByLabel(new RegExp(`^\\*?\\s*${escapeRegExp(label)}$`)).first()
    await expect(input, `form input ${label}`).toBeVisible()
    await input.fill(value)
  }
}

async function submitDialog(page: Page, buttonName: RegExp | string = /^确定$/) {
  await dialog(page).getByRole('button', { name: buttonName }).click()
}

async function fillDatePicker(page: Page, value: string, selector = '.el-date-editor input') {
  const input = dialog(page).locator(selector).last()
  await input.click()
  await input.fill(value)
  await input.press('Enter')
  await expect(input).toHaveValue(value)
}

test.describe('前端动作级数据流 / Action dataflow', () => {
  test('APS 配置、数据映射和同步按钮都发出正确请求', async ({ page }) => {
    const calls = await mockApi(page)
    await openPage(page, calls, '/aps/sync-config')

    await clickAndExpect(page, calls, 'POST', '/aps/sync/downstream', () =>
      page.getByRole('button', { name: '手动下行同步' }).click(),
    )
    await clickAndExpect(page, calls, 'POST', '/aps/sync/upstream', () =>
      page.getByRole('button', { name: '手动上行同步' }).click(),
    )

    await clickAndExpect(page, calls, 'PUT', '/aps/config/10', async () => {
      await page.getByRole('button', { name: '编辑' }).first().click()
      await fillDialogFields(page, { 配置值: 'false' })
      await submitDialog(page)
    })

    await clickAndExpect(page, calls, 'DELETE', '/aps/config/10', async () => {
      await page.getByRole('button', { name: '删除' }).first().click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/aps/data-mapping')
    await clickAndExpect(page, calls, 'PUT', '/aps/mapping/11', async () => {
      await page.getByRole('button', { name: '编辑' }).first().click()
      await fillDialogFields(page, { APS编码: 'APS-WC-2' })
      await submitDialog(page)
    })
    await clickAndExpect(page, calls, 'DELETE', '/aps/mapping/11', async () => {
      await page.getByRole('button', { name: '删除' }).first().click()
      await confirmMessage(page)
    })
  })

  test('计划、BOM、指示书、班组和配送签收动作都发出正确请求', async ({ page }) => {
    const calls = await mockApi(page)

    await openPage(page, calls, '/plan/order')
    await clickAndExpect(page, calls, 'POST', '/plan/order-plan/70/release', async () => {
      await page.getByRole('button', { name: '下达' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/plan/order-plan/71/complete', async () => {
      await page.getByRole('button', { name: '完成' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/plan/order-plan/71/terminate', async () => {
      await page.getByRole('button', { name: '终止' }).click()
      await promptMessage(page, '测试终止')
    })

    await openPage(page, calls, '/plan/production')
    await clickAndExpect(page, calls, 'POST', '/plan/production-plan/72/release', async () => {
      await page.getByRole('button', { name: '下达' }).click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/process/bom')
    await clickAndExpect(page, calls, 'GET', '/process/manufacturing-bom/30/items/tree', () =>
      page.getByText('BOM-ACTION').click(),
    )
    await closeDialog(page)
    await clickAndExpect(page, calls, 'POST', '/process/manufacturing-bom/30/upgrade', () =>
      page.getByRole('button', { name: '升级版本' }).click(),
    )
    await clickAndExpect(page, calls, 'POST', '/process/manufacturing-bom/30/publish', () =>
      page.getByRole('button', { name: '发布' }).click(),
    )
    await clickAndExpect(page, calls, 'POST', '/process/manufacturing-bom/30/disable', () =>
      page.getByRole('button', { name: '停用' }).click(),
    )

    await openPage(page, calls, '/process/instruction')
    await clickAndExpect(page, calls, 'POST', '/process/instruction/20/upgrade', () =>
      page.getByRole('button', { name: '升级版本' }).click(),
    )

    await openPage(page, calls, '/team/production-team')
    await clickAndExpect(page, calls, 'PUT', '/team/production-team/40/toggle-enabled', () =>
      page.getByRole('button', { name: '停用' }).click(),
    )

    await openPage(page, calls, '/material-mgmt/delivery-sign')
    await clickAndExpect(page, calls, 'POST', '/material/delivery-sign/50/confirm', async () => {
      await page.getByRole('button', { name: '确认签收' }).click()
      await confirmMessage(page)
    })
  })

  test('工单和派工生命周期动作都发出正确请求与请求体', async ({ page }) => {
    const calls = await mockApi(page)

    await openPage(page, calls, '/workorder/list')
    await clickAndExpect(page, calls, 'POST', '/workorder/work-order/100/release', async () => {
      await page.getByRole('button', { name: '下发' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/workorder/work-order/101/start', async () => {
      await page.getByRole('button', { name: '开工' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/workorder/work-order/102/complete', async () => {
      await page.getByRole('button', { name: /^完工$/ }).click()
      await confirmMessage(page)
    })
    const forceCall = await clickAndExpect(page, calls, 'POST', '/workorder/work-order/102/force-complete', async () => {
      await page.getByRole('button', { name: '强制完工' }).click()
      await fillDialogFields(page, { 原因: '强制完工原因' })
      await submitDialog(page)
    })
    expect(forceCall.payload).toMatchObject({ reason: '强制完工原因' })

    await openPage(page, calls, '/dispatch/task')
    const assignCall = await clickAndExpect(page, calls, 'POST', '/dispatch/task/assign', async () => {
      await page.getByRole('button', { name: '派人员' }).click()
      await submitDialog(page)
    })
    expect(assignCall.payload).toMatchObject({ taskId: 80, assignType: 'PERSON', assigneeIds: [1] })

    await clickAndExpect(page, calls, 'GET', '/dispatch/assignment/list/80', () =>
      page.getByRole('button', { name: '查看派工' }).first().click(),
    )
    await clickAndExpect(page, calls, 'POST', '/dispatch/task/unassign/801', async () => {
      await dialog(page).getByRole('button', { name: /^撤销$/ }).click()
      await promptMessage(page, '撤销派工原因')
      await closeDialog(page)
    })
    await clickAndExpect(page, calls, 'POST', '/dispatch/task/start/81', async () => {
      await page.getByRole('button', { name: '开工' }).click()
      await confirmMessage(page)
    })
    const completeCall = await clickAndExpect(page, calls, 'POST', '/dispatch/task/complete/82', async () => {
      await page.getByRole('button', { name: /^完工$/ }).click()
      await fillDatePicker(page, '2026-05-31 10:00:00')
      await submitDialog(page, '确定完工')
    })
    expect(completeCall.payload).toMatchObject({ actualEndTime: '2026-05-31 10:00:00', qualityResult: 'PASS' })
    await clickAndExpect(page, calls, 'POST', '/dispatch/task/cancel/80', async () => {
      await page.getByRole('button', { name: '撤销任务' }).first().click()
      await promptMessage(page, '撤销任务原因')
    })
  })

  test('异常、复检、交接班和查询详情动作都发出正确请求', async ({ page }) => {
    const calls = await mockApi(page)

    await openPage(page, calls, '/abnormal/contact')
    await clickAndExpect(page, calls, 'POST', '/abnormal/contact/60/submit', () =>
      page.getByRole('button', { name: '提交' }).click(),
    )
    await clickAndExpect(page, calls, 'POST', '/abnormal/contact/61/process', () =>
      page.getByRole('button', { name: '处理' }).click(),
    )
    await clickAndExpect(page, calls, 'POST', '/abnormal/contact/62/close', () =>
      page.getByRole('button', { name: '关闭' }).click(),
    )
    await clickAndExpect(page, calls, 'GET', '/abnormal/contact/60', () =>
      page.getByRole('button', { name: '查看详情' }).first().click(),
    )
    await clickAndExpect(page, calls, 'POST', '/abnormal/contact/attachments/601/sign', () =>
      page.getByRole('button', { name: '签署' }).click(),
    )

    await openPage(page, calls, '/quality/recheck')
    await clickAndExpect(page, calls, 'POST', '/quality/recheck/90/submit', async () => {
      await page.getByRole('button', { name: '提交' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/quality/recheck/91/review', async () => {
      await page.getByRole('button', { name: '审核' }).click()
      await fillDialogFields(page, { 审核人: '审核员' })
      await fillDatePicker(page, '2026-05-31')
      await submitDialog(page)
    })
    await clickAndExpect(page, calls, 'POST', '/quality/recheck/92/approve', async () => {
      await page.getByRole('button', { name: '批准' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/quality/recheck/93/complete', async () => {
      await page.getByRole('button', { name: '完结' }).click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/quality/shift-handover')
    await clickAndExpect(page, calls, 'POST', '/quality/shift-handover/94/receive', () =>
      page.getByRole('button', { name: '接收' }).click(),
    )

    const detailRoutes = [
      ['/query/production-work', '/query/production-work/120', 'PW-ACTION'],
      ['/query/inspection-work', '/query/inspection-work/121', 'INSP-ACTION'],
      ['/query/work-start-check', '/quality/work-start-check/95', 'WORK-ACTION'],
      ['/query/order-start-check', '/quality/order-start-check/96', 'ORDER-ACTION'],
      ['/query/shift-handover', '/quality/shift-handover/94', 'SN-ACTION'],
      ['/query/work-order', '/workorder/work-order/100', 'WO-ACTION-CREATED'],
      ['/query/dispatch-work', '/dispatch/task/80', '未派工'],
    ] as const

    for (const [route, apiPath, rowText] of detailRoutes) {
      await openPage(page, calls, route)
      await clickAndExpect(page, calls, 'GET', apiPath, () => page.getByText(rowText).first().click())
    }
  })

  test('系统与平台租户动作都发出正确请求', async ({ page }) => {
    const calls = await mockApi(page)

    await openPage(page, calls, '/system/user')
    await clickAndExpect(page, calls, 'PUT', '/system/user/130/reset-password', () =>
      page.getByRole('button', { name: '重置密码' }).click(),
    )
    await clickAndExpect(page, calls, 'DELETE', '/system/user/130', async () => {
      await page.getByRole('button', { name: '删除' }).click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/system/role')
    await clickAndExpect(page, calls, 'PUT', '/system/role/131/menus', async () => {
      await page.getByRole('button', { name: '分配菜单' }).click()
      await submitDialog(page, '保存')
    })
    await clickAndExpect(page, calls, 'DELETE', '/system/role/131', async () => {
      await page.getByRole('button', { name: '删除' }).click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/system/menu')
    await clickAndExpect(page, calls, 'DELETE', '/system/menu/132', async () => {
      await page.getByRole('button', { name: '删除' }).click()
      await confirmMessage(page)
    })

    await openPage(page, calls, '/platform/tenants')
    await clickAndExpect(page, calls, 'GET', '/platform/tenants/140', () =>
      page.getByRole('button', { name: '详情' }).first().click(),
    )
    await closeDrawer(page)
    await clickAndExpect(page, calls, 'POST', '/platform/tenants/register', async () => {
      await page.getByRole('button', { name: '新建租户' }).click()
      await fillDialogFields(page, { 租户编码: 'tenant-new', 租户名称: '新租户' })
      await submitDialog(page)
    })
    await clickAndExpect(page, calls, 'POST', '/platform/tenants/140/suspend', async () => {
      await page.getByRole('button', { name: '停用' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/platform/tenants/141/resume', async () => {
      await page.getByRole('button', { name: '恢复' }).click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/platform/tenants/140/archive', async () => {
      await page.getByRole('button', { name: '归档' }).first().click()
      await confirmMessage(page)
    })
    await clickAndExpect(page, calls, 'POST', '/platform/tenants/140/reprovision', async () => {
      await page.getByRole('button', { name: '重新配置' }).first().click()
      await confirmMessage(page)
    })
  })
})
