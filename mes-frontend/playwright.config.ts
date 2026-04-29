import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright 配置（MES 前端 E2E 业务回归）
 *
 * 运行方式：
 *   pnpm exec playwright test             本地默认 (chromium)
 *   pnpm exec playwright test --project=webkit
 *   pnpm exec playwright test --project=chromium --project=webkit
 *   npm run test:e2e:ci                   CI 一次性运行（junit + html + trace）
 *
 * 环境变量：
 *   E2E_BASE            前端站点 URL（默认 http://localhost:3000）
 *   E2E_USER/E2E_PASS   管理员账号（默认 admin / admin123）
 *   E2E_TENANT          租户编码（可选）
 *   E2E_BACKEND_BASE    后端 API 基址（默认 http://localhost:9091），用于 seed/API 回读
 *
 * 注：业务回归 spec 会按「seed 能否 ready」自动降级为 skip；
 *     即使 CI 没有后端，也能跑 UI smoke 子集。
 */

const baseURL = process.env.E2E_BASE || 'http://localhost:3000'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  // CI / 本地统一输出 junit + html + list；本地额外 on-failure 打开报告
  reporter: process.env.CI
    ? [
        ['list'],
        ['html', { open: 'never', outputFolder: 'playwright-report' }],
        ['junit', { outputFile: 'playwright-report/junit.xml' }],
      ]
    : [
        ['list'],
        ['html', { open: 'on-failure', outputFolder: 'playwright-report' }],
        ['junit', { outputFile: 'playwright-report/junit.xml' }],
      ],

  use: {
    baseURL,
    headless: true,
    viewport: { width: 1440, height: 900 },
    // CI 保留失败 trace / screenshot / video；本地亦保留，方便排查
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    ignoreHTTPSErrors: true,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],

  webServer: process.env.CI
    ? {
        command: 'npm run dev -- --host 0.0.0.0',
        url: baseURL,
        timeout: 120_000,
        reuseExistingServer: false,
        stdout: 'pipe',
        stderr: 'pipe',
      }
    : undefined,
})
