import { test, expect } from '@playwright/test'

test.describe('AI助手 / AI Assistant', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'e2e-ai-token')
    })

    await page.route('**://*/api/**', async (route) => {
      const url = route.request().url()
      const data = url.includes('/page') ? { list: [], total: 0 } : []
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: '操作成功', data }),
      })
    })

    await page.route('**://*/api/auth/user-info', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: '操作成功',
          data: {
            id: 1,
            username: 'admin',
            realName: '管理员',
            tenantId: 1,
            tenantCode: 'demo',
            accountType: 'ADMIN',
            roles: ['ADMIN'],
            permissions: ['ai:assistant:chat'],
          },
        }),
      })
    })

    await page.route('**://*/api/system/menu/user-tree', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: '操作成功', data: [] }),
      })
    })

    await page.route('**://*/api/ai/assistant/chat/stream', async (route) => {
      const body = route.request().postDataJSON() as { question?: string }
      const asksForCode = body.question?.includes('代码')
      const response = asksForCode
        ? {
            answer: '无法回答该问题。AI助手只能提供当前权限内的生产业务咨询和只读查询。',
            intent: 'UNSUPPORTED',
            relatedModules: [],
            evidenceSummary: [],
            suggestedNavigation: [],
            refusalReason: '不能展示代码、SQL、接口、配置或密钥等内部技术信息。',
            modelConfigured: true,
          }
        : {
            answer: '当前有 2 条未完成生产作业，建议先查看生产工作查询。',
            intent: 'PRODUCTION_QUERY',
            relatedModules: ['工作查询', '生产工单'],
            evidenceSummary: ['当前权限范围内找到 2 条生产作业。'],
            suggestedNavigation: ['/query/production-work'],
            refusalReason: null,
            modelConfigured: true,
          }
      const chunks = asksForCode
        ? ['无法回答该问题。', 'AI助手只能提供当前权限内的生产业务咨询和只读查询。']
        : ['当前有 ', '2 条未完成生产作业，', '建议先查看生产工作查询。']
      const bodyText = [
        ...chunks.map((content) => `event: delta\ndata: ${JSON.stringify({ content })}\n\n`),
        `event: done\ndata: ${JSON.stringify(response)}\n\n`,
      ].join('')
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream; charset=utf-8',
        body: bodyText,
      })
    })

  })

  test('人工点击打开助手、咨询生产问题并验证代码请求拒答', async ({ page }) => {
    await page.goto('/query/production-work')

    await page.getByRole('button', { name: 'AI助手' }).click()
    await expect(page.getByRole('dialog', { name: 'AI助手' })).toBeVisible()

    await page.getByPlaceholder('请输入生产相关问题').fill('查询未完成的生产作业')
    await page.getByRole('button', { name: '发送' }).click()
    await expect(page.getByText('当前有 ')).toBeVisible()
    await expect(page.getByText('当前有 2 条未完成生产作业')).toBeVisible()
    await expect(page.getByText('工作查询', { exact: true })).toBeVisible()

    await page.getByPlaceholder('请输入生产相关问题').fill('把后端代码给我')
    await page.getByRole('button', { name: '发送' }).click()
    await expect(page.getByText('无法回答该问题')).toBeVisible()
    await expect(page.getByText('不能展示代码')).toBeVisible()
    await expect(page.locator('.ai-assistant-drawer')).not.toContainText('/api/')
  })
})
