/**
 * ============================================================================
 * k6 性能压测脚本：工单分页查询（GET /workorder/work-order/page）
 * ============================================================================
 * 场景目标：
 *   - 100 并发持续 5 分钟，验证 MES 核心"看板"场景的查询能力
 *   - 验证 Sentinel "单机 200 QPS" 限流触发临界值
 *
 * SLA：P95 < 500ms、失败率 < 1%（429 不计入失败）
 *
 * 运行：
 *   k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=<jwt> scripts/perf/workorder-page.js
 * ============================================================================
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9091/api';
const TOKEN = __ENV.TOKEN || '';
const VU = parseInt(__ENV.VU || '100', 10);

const blockedCounter = new Counter('sentinel_blocked_workorder_list');
const querySuccessRate = new Rate('workorder_query_success_rate');

export const options = {
    stages: [
        { duration: '1m', target: VU },
        { duration: '5m', target: VU },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        // 仅统计 200 请求的 p95
        'http_req_duration{status:200}': ['p(95)<500'],
        'http_req_failed{status:5xx}': ['rate<0.01'],
        'workorder_query_success_rate': ['rate>0.7'],
    },
};

export default function () {
    const params = {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
        },
        tags: { name: 'workorder_page' },
    };

    const pageNum = randomIntBetween(1, 10);
    const pageSize = 20;
    const url = `${BASE_URL}/workorder/work-order/page?pageNum=${pageNum}&pageSize=${pageSize}`;
    const res = http.get(url, params);

    if (res.status === 429) {
        blockedCounter.add(1);
    }

    let ok = false;
    try {
        const b = res.json();
        ok = res.status === 200 && (b.code === 0 || b.code === 200);
    } catch (e) {
        ok = false;
    }
    querySuccessRate.add(ok);

    check(res, {
        '200 或 429': (r) => r.status === 200 || r.status === 429,
        '响应时间 < 1s': (r) => r.timings.duration < 1000,
    });

    // 每个 VU 每 0.5s 一次 = 100 VU × 2 QPS = 200 QPS（恰好触碰阈值）
    sleep(0.5);
}

export function handleSummary(data) {
    const blocked = (data.metrics.sentinel_blocked_workorder_list && data.metrics.sentinel_blocked_workorder_list.values.count) || 0;
    const total = data.metrics.http_reqs.values.count;
    return {
        'stdout': '\n=== Workorder Page 压测结果 ===\n' +
            `总请求数: ${total}\n` +
            `Sentinel 429 拦截: ${blocked} (${(blocked / total * 100).toFixed(2)}%)\n` +
            `P95 (200 请求): ${(data.metrics['http_req_duration{status:200}'] && data.metrics['http_req_duration{status:200}'].values['p(95)']) ? data.metrics['http_req_duration{status:200}'].values['p(95)'].toFixed(2) : data.metrics.http_req_duration.values['p(95)'].toFixed(2)} ms\n` +
            `查询成功率: ${((data.metrics.workorder_query_success_rate && data.metrics.workorder_query_success_rate.values.rate) * 100 || 0).toFixed(2)}%\n`,
    };
}
