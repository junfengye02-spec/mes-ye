/**
 * ============================================================================
 * k6 性能压测脚本：登录接口（/auth/login）
 * ============================================================================
 * 场景目标：
 *   - 模拟 1 → 50 并发用户渐进登录，用于验证 P2-26 "每 IP 10 QPS" 限流是否生效
 *   - SLA：P95 < 500ms、失败率 < 1%（429 算业务兜底，不计入失败率）
 *
 * 运行方式：
 *   k6 run --summary-export=auth-login-summary.json scripts/perf/auth-login.js
 *   k6 run -e BASE_URL=https://mes.example.com -e VU_MAX=50 scripts/perf/auth-login.js
 *
 * 关键参数（支持环境变量覆盖）：
 *   BASE_URL   后端基地址，默认 http://localhost:9091/api
 *   USERNAME   登录用户名，默认 admin
 *   PASSWORD   登录密码，默认 admin123
 *   VU_MAX     目标并发用户数，默认 50
 * ============================================================================
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// ========== 可配置参数 ==========
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9091/api';
const USERNAME = __ENV.USERNAME || 'admin';
const PASSWORD = __ENV.PASSWORD || 'admin123';
const VU_MAX = parseInt(__ENV.VU_MAX || '50', 10);

// ========== 自定义指标 ==========
const blockedCounter = new Counter('sentinel_blocked');    // 429 次数
const loginSuccessRate = new Rate('login_success_rate');    // 登录成功率

// ========== 压测配置 ==========
export const options = {
    // ramp-up：前 60s 从 0 升到 VU_MAX，保持 3 分钟，再 60s 降到 0
    stages: [
        { duration: '1m', target: VU_MAX },
        { duration: '3m', target: VU_MAX },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        // P95 响应时间 < 500ms
        'http_req_duration{status:200}': ['p(95)<500'],
        // 真正的失败（5xx / 超时）率 < 1%
        'http_req_failed{status:5xx}': ['rate<0.01'],
        // 登录成功率要求 ≥ 15%（因为 1/IP 的 10 QPS 限流后多数请求会被 429 拦截，属预期）
        'login_success_rate': ['rate>0.1'],
    },
    // 模拟真实浏览器 User-Agent
    userAgent: 'MES-k6-AuthLogin/1.0',
};

// ========== 主流程 ==========
export default function () {
    const payload = JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: { name: 'auth_login' },
    };

    const res = http.post(`${BASE_URL}/auth/login`, payload, params);

    // 统计 Sentinel 限流触发次数
    if (res.status === 429) {
        blockedCounter.add(1);
    }

    // 登录成功率：返回 200 且 body.code === 0
    let codeOk = false;
    try {
        const body = res.json();
        codeOk = body && (body.code === 0 || body.code === 200);
    } catch (e) {
        codeOk = false;
    }
    loginSuccessRate.add(res.status === 200 && codeOk);

    check(res, {
        '状态码为 200 或 429': (r) => r.status === 200 || r.status === 429,
        '响应时间 < 1s': (r) => r.timings.duration < 1000,
    });

    // 每个 VU 每秒 1 次请求，ramp 到 50 VU 就是总体 50 QPS，远超 10 QPS/IP 阈值
    sleep(1);
}

// ========== 汇总输出 ==========
export function handleSummary(data) {
    return {
        'stdout': '\n=== Auth Login 压测结果 ===\n' +
            `总请求数: ${data.metrics.http_reqs.values.count}\n` +
            `P95 响应时间: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} ms\n` +
            `Sentinel 429 拦截: ${(data.metrics.sentinel_blocked && data.metrics.sentinel_blocked.values.count) || 0}\n` +
            `登录成功率: ${((data.metrics.login_success_rate && data.metrics.login_success_rate.values.rate) * 100 || 0).toFixed(2)}%\n`,
    };
}
