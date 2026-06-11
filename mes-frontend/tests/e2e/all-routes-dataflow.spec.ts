import { expect, loginAsAdmin, test } from './fixtures'

type RouteCase = {
  path: string
  name: string
  endpoints: string[]
  optionalEndpoints?: string[]
}

const ROUTES: RouteCase[] = [
  { path: '/basic/material', name: '物料档案', endpoints: ['/api/basic/material/page'] },
  {
    path: '/basic/material-price',
    name: '物料价格',
    endpoints: ['/api/basic/material-price/page'],
    optionalEndpoints: ['/api/basic/material/page'],
  },
  { path: '/basic/work-center', name: '工作中心', endpoints: ['/api/basic/work-center/page'] },
  { path: '/team/production-team', name: '班组管理', endpoints: ['/api/team/production-team/page'] },
  { path: '/process/instruction', name: '指示书', endpoints: ['/api/process/instruction/page'] },
  { path: '/process/template', name: '工艺模板', endpoints: ['/api/process/process-template/tree'] },
  { path: '/process/info', name: '工序信息', endpoints: ['/api/process/process-info/page'] },
  { path: '/process/work-instruction', name: '作业指导书', endpoints: ['/api/process/work-instruction/page'] },
  { path: '/process/spray-condition', name: '喷涂条件', endpoints: ['/api/process/spray-condition/page'] },
  { path: '/process/machining-program', name: '加工程序', endpoints: ['/api/process/machining-program/page'] },
  { path: '/process/bom', name: '制造 BOM', endpoints: ['/api/process/manufacturing-bom/page'] },
  { path: '/plan/order', name: '订单计划', endpoints: ['/api/plan/order-plan/page'] },
  { path: '/plan/production', name: '生产计划', endpoints: ['/api/plan/production-plan/page'] },
  { path: '/workorder/list', name: '生产工单', endpoints: ['/api/workorder/work-order/page'] },
  { path: '/dispatch/task', name: '派工管理', endpoints: ['/api/dispatch/task/page'] },
  { path: '/abnormal/contact', name: '异常联络单', endpoints: ['/api/abnormal/contact/page'] },
  { path: '/quality/recheck', name: '复检申请', endpoints: ['/api/quality/recheck/page'] },
  { path: '/quality/work-start-check', name: '开工检查', endpoints: ['/api/quality/work-start-check/page'] },
  { path: '/quality/order-start-check', name: '订单开工检查', endpoints: ['/api/quality/order-start-check/page'] },
  { path: '/quality/shift-handover', name: '交接班', endpoints: ['/api/quality/shift-handover/page'] },
  { path: '/query/work-status', name: '作业状态查询', endpoints: ['/api/query/work-status-view/page'] },
  { path: '/query/production-work', name: '生产作业查询', endpoints: ['/api/query/production-work/page'] },
  { path: '/query/inspection-work', name: '检验作业查询', endpoints: ['/api/query/inspection-work/page'] },
  { path: '/query/work-start-check', name: '开工检查查询', endpoints: ['/api/quality/work-start-check/page'] },
  { path: '/query/order-start-check', name: '订单开工检查查询', endpoints: ['/api/quality/order-start-check/page'] },
  { path: '/query/shift-handover', name: '交接班查询', endpoints: ['/api/quality/shift-handover/page'] },
  { path: '/query/work-order', name: '工单查询', endpoints: ['/api/workorder/work-order/page'] },
  { path: '/query/dispatch-work', name: '派工作业查询', endpoints: ['/api/dispatch/task/page'] },
  { path: '/material-mgmt/inventory', name: '库存查询', endpoints: ['/api/material/inventory/page'] },
  { path: '/material-mgmt/requisition', name: '领料单', endpoints: ['/api/material/requisition/page'] },
  { path: '/material-mgmt/requisition-order', name: '领料需求', endpoints: ['/api/material/requisition-order/page'] },
  { path: '/material-mgmt/receipt-request', name: '入库申请', endpoints: ['/api/material/receipt/request/page'] },
  { path: '/material-mgmt/receipt', name: '完工入库', endpoints: ['/api/material/receipt/page'] },
  { path: '/material-mgmt/return', name: '退料单', endpoints: ['/api/material/return/page'] },
  { path: '/material-mgmt/delivery-sign', name: '配送签收', endpoints: ['/api/material/delivery-sign/page'] },
  { path: '/aps/sync-config', name: 'APS 同步配置', endpoints: ['/api/aps/sync/status', '/api/aps/config/page'] },
  { path: '/aps/sync-log', name: 'APS 同步日志', endpoints: ['/api/aps/log/page'] },
  { path: '/aps/data-mapping', name: 'APS 数据映射', endpoints: ['/api/aps/mapping/page'] },
  { path: '/system/user', name: '用户管理', endpoints: ['/api/system/user/page', '/api/system/role/list'] },
  { path: '/system/role', name: '角色管理', endpoints: ['/api/system/role/page'] },
  { path: '/system/menu', name: '菜单管理', endpoints: ['/api/system/menu/tree'] },
]

async function assertSuccessfulEnvelope(response: import('@playwright/test').Response, endpoint: string) {
  expect(response.status(), `${endpoint} HTTP status`).toBeLessThan(400)

  const contentType = response.headers()['content-type'] || ''
  if (!contentType.includes('application/json')) return

  const body = await response.json().catch(() => null)
  if (body && typeof body === 'object' && 'code' in body) {
    expect(body.code, `${endpoint} envelope code: ${body.message || ''}`).toBe(200)
  }
}

test.describe('全前端路由 / Page dataflow', () => {
  test('所有管理员前端路由都能连通后端并渲染列表数据流', async ({ page, backendAlive }) => {
    test.skip(!backendAlive, 'MES 后端不可达，无法验证真实数据流')
    await loginAsAdmin(page)

    const failures: string[] = []

    for (const route of ROUTES) {
      await test.step(`${route.name} ${route.path}`, async () => {
        const consoleErrors: string[] = []
        const consoleHandler = (msg: import('@playwright/test').ConsoleMessage) => {
          if (msg.type() === 'error') {
            consoleErrors.push(msg.text())
          }
        }
        page.on('console', consoleHandler)

        const responseWaiters = route.endpoints.map((endpoint) =>
          page
            .waitForResponse((response) => response.url().includes(endpoint), { timeout: 20_000 })
            .then((response) => ({ endpoint, response }))
            .catch((error) => {
              failures.push(`${route.path} 未观察到接口 ${endpoint}: ${error.message}`)
              return null
            }),
        )
        const optionalResponseWaiters = (route.optionalEndpoints || []).map((endpoint) =>
          page
            .waitForResponse((response) => response.url().includes(endpoint), { timeout: 5_000 })
            .then((response) => ({ endpoint, response }))
            .catch(() => null),
        )

        await page.goto(route.path)
        await expect(page).toHaveURL(new RegExp(route.path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
        await expect(page.locator('main')).toBeVisible()
        await page.waitForLoadState('networkidle').catch(() => undefined)

        const responses = (await Promise.all([...responseWaiters, ...optionalResponseWaiters])).filter(Boolean) as Array<{
          endpoint: string
          response: import('@playwright/test').Response
        }>

        for (const { endpoint, response } of responses) {
          try {
            await assertSuccessfulEnvelope(response, endpoint)
          } catch (error: any) {
            failures.push(`${route.path} 接口 ${endpoint} 返回异常: ${error?.message || error}`)
          }
        }

        if (consoleErrors.length) {
          failures.push(`${route.path} 控制台错误: ${consoleErrors.join(' | ')}`)
        }
        page.off('console', consoleHandler)
      })
    }

    expect(failures, failures.join('\n')).toEqual([])
  })
})
