/**
 * ============================================================================
 * k6 性能压测脚本：工单创建（POST /workorder/work-order）
 * ============================================================================
 * 场景目标：
 *   - 10 并发持续 3 分钟，模拟 MES 排产高峰时的工单批量下发
 *   - 验证数据库写入吞吐，不触发 Sentinel 限流（无阈值规则）
 *
 * SLA：P95 < 500ms、失败率 < 1%
 *
 * 运行：
 *   k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=<jwt> scripts/perf/workorder-create.js
 * ============================================================================
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9091/api';
const TOKEN = __ENV.TOKEN || '';     // 必填：压测前用户登录后的 access_token
const VU = parseInt(__ENV.VU || '10', 10);

const createdCounter = new Counter('workorder_created');

export const options = {
    stages: [
        { duration: '30s', target: VU },
        { duration: '3m', target: VU },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{status:200}': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
    },
};

export function setup() {
    if (!TOKEN) {
        console.warn('TOKEN 未设置，所有请求都会被 401；请先登录并通过 -e TOKEN=xxx 传入');
    }
    return { token: TOKEN };
}

export default function (data) {
    const payload = JSON.stringify({
        workOrderNo: `PERF-WO-${Date.now()}-${randomIntBetween(1000, 9999)}`,
        productId: randomIntBetween(1, 100),
        productCode: 'P-PERF-' + randomIntBetween(1, 100),
        productName: '压测产品-' + randomIntBetween(1, 100),
        plannedQty: randomIntBetween(1, 500),
        plannedStartTime: new Date().toISOString(),
        plannedEndTime: new Date(Date.now() + 86400000).toISOString(),
        priority: randomIntBetween(1, 5),
        remark: 'k6 压测生成',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${data.token}`,
        },
        tags: { name: 'workorder_create' },
    };

    const res = http.post(`${BASE_URL}/workorder/work-order`, payload, params);

    const ok = check(res, {
        '创建成功 (200)': (r) => r.status === 200,
        '响应体含工单 ID': (r) => {
            try { const b = r.json(); return b.code === 0 && b.data !== null; } catch { return false; }
        },
    });

    if (ok) createdCounter.add(1);

    sleep(0.3);
}

export function handleSummary(data) {
    return {
        'stdout': '\n=== Workorder Create 压测结果 ===\n' +
            `总请求数: ${data.metrics.http_reqs.values.count}\n` +
            `创建成功数: ${(data.metrics.workorder_created && data.metrics.workorder_created.values.count) || 0}\n` +
            `P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} ms\n` +
            `失败率: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`,
    };
}
