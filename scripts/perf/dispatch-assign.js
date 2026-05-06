/**
 * ============================================================================
 * k6 性能压测脚本：派工指派（POST /dispatch/task/assign）
 * ============================================================================
 * 场景目标：
 *   - 5 并发持续 3 分钟，模拟班组长同时派工的真实节奏
 *   - 验证事务写入 + 分派记录表的并发安全
 *
 * SLA：P95 < 500ms、失败率 < 1%
 *
 * 运行：
 *   k6 run -e BASE_URL=http://localhost:9091/api -e TOKEN=<jwt> \
 *          -e TASK_IDS=1001,1002,1003 scripts/perf/dispatch-assign.js
 * ============================================================================
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9091/api';
const TOKEN = __ENV.TOKEN || '';
const TASK_IDS = (__ENV.TASK_IDS || '1001,1002,1003').split(',').map(Number);
const STAFF_IDS = (__ENV.STAFF_IDS || '101,102,103,104,105').split(',').map(Number);
const VU = parseInt(__ENV.VU || '5', 10);

const assignedCounter = new Counter('dispatch_assigned');
const assignSuccessRate = new Rate('dispatch_assign_success_rate');

export const options = {
    stages: [
        { duration: '30s', target: VU },
        { duration: '3m', target: VU },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{status:200}': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
        'dispatch_assign_success_rate': ['rate>0.99'],
    },
};

export default function () {
    const taskId = TASK_IDS[exec.scenario.iterationInTest % TASK_IDS.length];
    const staffId = STAFF_IDS[randomIntBetween(0, STAFF_IDS.length - 1)];

    const payload = JSON.stringify({
        taskId: taskId,
        assignType: 'PERSON',
        assigneeIds: [staffId],
        assigneeCodes: ['PERF-STAFF-' + staffId],
        assigneeNames: ['压测人员-' + staffId],
        assignedQty: 1,
        qtyUnit: 'PC',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${TOKEN}`,
        },
        tags: { name: 'dispatch_assign' },
    };

    const res = http.post(`${BASE_URL}/dispatch/task/assign`, payload, params);

    let ok = false;
    try {
        const b = res.json();
        ok = res.status === 200 && (b.code === 0 || b.code === 200);
    } catch (e) {
        ok = false;
    }
    assignSuccessRate.add(ok);
    if (ok) {
        assignedCounter.add(1);
    }

    check(res, {
        '指派成功 (200)': () => ok,
        '响应时间 < 1s': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': '\n=== Dispatch Assign 压测结果 ===\n' +
            '总请求数: ' + data.metrics.http_reqs.values.count + '\n' +
            '指派成功数: ' + ((data.metrics.dispatch_assigned && data.metrics.dispatch_assigned.values.count) || 0) + '\n' +
            'P95: ' + data.metrics.http_req_duration.values['p(95)'].toFixed(2) + ' ms\n' +
            '失败率: ' + (data.metrics.http_req_failed.values.rate * 100).toFixed(2) + '%\n' +
            '业务成功率: ' + (((data.metrics.dispatch_assign_success_rate && data.metrics.dispatch_assign_success_rate.values.rate) || 0) * 100).toFixed(2) + '%\n',
    };
}
