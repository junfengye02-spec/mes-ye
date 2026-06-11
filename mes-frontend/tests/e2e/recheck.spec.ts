import { test, expect, loginAsAdmin } from './fixtures'

function todayDate() {
  return new Date().toISOString().slice(0, 10)
}

test.describe('复检申请 / Recheck (UI workflow)', () => {
  test.beforeEach(async ({ page, backendAlive }) => {
    test.skip(!backendAlive, 'MES 后端不可达，无法登录进入业务页面')
    await loginAsAdmin(page)
  })

  test('状态筛选包含完整复检状态机选项', async ({ page }) => {
    await page.goto('/quality/recheck')
    await expect(page).not.toHaveURL(/\/login/)

    const statusSelect = page
      .locator('.recheck-request-list .search-card .el-form-item')
      .filter({ hasText: '状态' })
      .locator('.el-select')
      .first()
    await statusSelect.click()

    for (const label of ['已创建', '已提交', '审核中', '已批准', '已驳回', '已完成']) {
      await expect(page.locator('.el-select-dropdown__item').filter({ hasText: label }).first()).toBeVisible()
    }
  })

  test('列表根据复检状态展示对应工作流动作', async ({ page }) => {
    const rows = [
      { id: 101, projectCode: 'E2E-CREATED', materialCode: 'MAT-101', materialName: 'Created Row', productionOrderNo: 'WO-101', recheckReason: 'reason', recheckProposer: 'tester', status: 'CREATED' },
      { id: 102, projectCode: 'E2E-SUBMITTED', materialCode: 'MAT-102', materialName: 'Submitted Row', productionOrderNo: 'WO-102', recheckReason: 'reason', recheckProposer: 'tester', status: 'SUBMITTED' },
      { id: 103, projectCode: 'E2E-REVIEW', materialCode: 'MAT-103', materialName: 'Review Row', productionOrderNo: 'WO-103', recheckReason: 'reason', recheckProposer: 'tester', status: 'IN_REVIEW' },
      { id: 104, projectCode: 'E2E-APPROVED', materialCode: 'MAT-104', materialName: 'Approved Row', productionOrderNo: 'WO-104', recheckReason: 'reason', recheckProposer: 'tester', status: 'APPROVED' },
    ]

    let submitHit = false
    let reviewHit = false
    let approveHit = false
    let completeHit = false

    await page.route('**/api/quality/recheck/page**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'ok',
          data: {
            list: rows,
            total: rows.length,
          },
        }),
      })
    })

    await page.route('**/api/quality/recheck/101/submit', async route => {
      submitHit = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data: null }),
      })
    })

    await page.route('**/api/quality/recheck/102/review', async route => {
      reviewHit = true
      const payload = route.request().postDataJSON()
      if (payload?.reviewer !== 'E2E Reviewer' || payload?.reviewDate !== todayDate() || payload?.isReasonable !== 1) {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({ code: 400, message: 'unexpected payload', data: null }),
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data: null }),
      })
    })

    await page.route('**/api/quality/recheck/103/approve', async route => {
      const payload = route.request().postDataJSON()
      if (payload?.approved === true) {
        approveHit = true
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data: null }),
      })
    })

    await page.route('**/api/quality/recheck/104/complete', async route => {
      completeHit = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data: null }),
      })
    })

    await page.goto('/quality/recheck')
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: '提交' }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '审核' }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '批准' }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '驳回' }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '完结' }).first()).toBeVisible()

    await page.getByRole('button', { name: '提交' }).first().click()
    await page.locator('.el-message-box__btns .el-button--primary').click()
    expect(submitHit).toBeTruthy()

    await page.getByRole('button', { name: '审核' }).first().click()
    await page.getByPlaceholder('请输入审核人').fill('E2E Reviewer')
    await page.locator('.el-dialog input[placeholder=\"请选择审核日期\"]').fill(todayDate())
    await page.locator('.el-dialog input[placeholder=\"请选择审核日期\"]').press('Tab')
    await page.locator('.el-dialog .el-select').last().click()
    await page.locator('.el-select-dropdown__item').filter({ hasText: '合理' }).first().click()
    await page.locator('.el-dialog__footer .el-button--primary').click()
    expect(reviewHit).toBeTruthy()

    await page.getByRole('button', { name: '批准' }).first().click()
    await page.locator('.el-message-box__btns .el-button--primary').click()
    expect(approveHit).toBeTruthy()

    await page.getByRole('button', { name: '完结' }).first().click()
    await page.locator('.el-message-box__btns .el-button--primary').click()
    expect(completeHit).toBeTruthy()
  })
})
