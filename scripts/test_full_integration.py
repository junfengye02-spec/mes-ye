"""MES 全功能集成测试脚本"""
import requests
import json
import sys
import time

BASE = "http://localhost:9091/api"
PASS = 0
FAIL = 0
RESULTS = []


def log(ok, name, detail=""):
    global PASS, FAIL
    tag = "[PASS]" if ok else "[FAIL]"
    if ok:
        PASS += 1
    else:
        FAIL += 1
    msg = f"{tag} {name}"
    if detail:
        msg += f" - {detail}"
    print(msg)
    RESULTS.append((ok, name, detail))


def api(method, path, token=None, json_data=None, expect_status=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    url = f"{BASE}{path}"
    try:
        r = requests.request(method, url, headers=headers, json=json_data, timeout=15)
        if expect_status and r.status_code != expect_status:
            return None, r.status_code, r.text
        return r.json() if r.text else {}, r.status_code, r.text
    except Exception as e:
        return None, 0, str(e)


def login(username, password="admin123", client="ADMIN"):
    data, status, raw = api("POST", "/auth/login", json_data={
        "username": username, "password": password, "loginClient": client
    })
    if data and data.get("code") == 200:
        return data["data"]["accessToken"]
    return None


# ===================== 1. 认证测试 =====================
print("\n===== 1. 认证模块测试 =====")

admin_token = login("admin")
log(admin_token is not None, "1.1 admin登录(ADMIN端)", "获取token成功" if admin_token else "登录失败")

staff_token = login("zhangsan", client="USER")
log(staff_token is not None, "1.2 zhangsan登录(USER端)", "获取token成功" if staff_token else "登录失败")

staff_admin_fail = login("zhangsan", client="ADMIN")
log(staff_admin_fail is None, "1.3 STAFF账号不能登录ADMIN端", "正确拒绝" if staff_admin_fail is None else "错误允许")

bad_login = login("admin", password="wrongpass")
log(bad_login is None, "1.4 错误密码登录被拒绝")

data, status, _ = api("GET", "/auth/user-info", token=admin_token)
if data and data.get("code") == 200:
    info = data["data"]
    log(True, "1.5 获取admin用户信息", f"tenantId={info.get('tenantId')}, accountType={info.get('accountType')}")
else:
    log(False, "1.5 获取admin用户信息")

if not admin_token:
    print("\n[FATAL] admin登录失败，无法继续测试")
    sys.exit(1)

# ===================== 2. STAFF权限隔离测试 =====================
print("\n===== 2. STAFF权限隔离测试 =====")

if staff_token:
    data, status, _ = api("GET", "/auth/user-info", token=staff_token)
    log(data and data.get("code") == 200, "2.1 STAFF能访问/auth/user-info")

    data, status, _ = api("GET", "/system/user/page?page=1&pageSize=10", token=staff_token)
    log(status == 403 or (data and data.get("code") != 200), "2.2 STAFF不能访问/system/user", f"status={status}")

    data, status, _ = api("GET", "/system/role/list", token=staff_token)
    log(status == 403 or (data and data.get("code") != 200), "2.3 STAFF不能访问/system/role", f"status={status}")

    data, status, _ = api("GET", "/system/menu/tree", token=staff_token)
    log(status == 403 or (data and data.get("code") != 200), "2.4 STAFF不能访问/system/menu", f"status={status}")
else:
    log(False, "2.1~2.4 STAFF权限测试跳过(登录失败)")

# ===================== 3. 系统管理(ADMIN) =====================
print("\n===== 3. 系统管理模块测试 =====")

data, status, _ = api("GET", "/system/user/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "3.1 用户列表查询", f"总数={data['data']['total']}" if data and data.get("code") == 200 else "")

data, status, _ = api("GET", "/system/role/list", token=admin_token)
log(data and data.get("code") == 200, "3.2 角色列表查询")

data, status, _ = api("GET", "/system/menu/tree", token=admin_token)
log(data and data.get("code") == 200, "3.3 菜单树查询")

data, status, _ = api("GET", "/system/menu/user-tree", token=admin_token)
log(data and data.get("code") == 200, "3.4 用户菜单树查询")

# ===================== 4. 基础数据模块 =====================
print("\n===== 4. 基础数据模块测试 =====")

data, status, _ = api("GET", "/basic/material/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "4.1 物料列表查询")

material_id = None
data, status, _ = api("POST", "/basic/material", token=admin_token, json_data={
    "materialCode": f"TEST-MAT-{int(time.time())}",
    "materialName": "测试物料-集成测试",
    "materialType": "RAW",
    "baseUnit": "PCS",
    "categoryLevel1": "测试分类"
})
if data and data.get("code") == 200:
    material_id = data.get("data")
    log(True, "4.2 新增物料", f"id={material_id}")
else:
    log(False, "4.2 新增物料", str(data))

data, status, _ = api("GET", "/basic/work-center/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "4.3 工作中心列表查询")

wc_id = None
data, status, _ = api("POST", "/basic/work-center", token=admin_token, json_data={
    "workCenterCode": f"TEST-WC-{int(time.time())}",
    "workCenterName": "测试工作中心",
    "workCenterCategory": "MACHINING",
    "efficiency": 0.95,
    "resourceCapacity": 480
})
if data and data.get("code") == 200:
    wc_id = data.get("data")
    log(True, "4.4 新增工作中心", f"id={wc_id}")
else:
    log(False, "4.4 新增工作中心", str(data))

data, status, _ = api("GET", "/basic/material-price/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "4.5 物料价格列表查询")

# ===================== 5. 班组管理 =====================
print("\n===== 5. 班组管理测试 =====")

data, status, _ = api("GET", "/team/production-team/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "5.1 班组列表查询")

team_id = None
data, status, _ = api("POST", "/team/production-team", token=admin_token, json_data={
    "teamCode": f"TEST-TEAM-{int(time.time())}",
    "teamName": "测试班组-甲班",
    "orgCode": "ORG-TEST",
    "orgName": "测试车间"
})
if data and data.get("code") == 200:
    team_id = data.get("data")
    log(True, "5.2 新增班组", f"id={team_id}")
else:
    log(False, "5.2 新增班组", str(data))

# ===================== 6. 工艺管理 =====================
print("\n===== 6. 工艺管理测试 =====")

data, status, _ = api("GET", "/process/process-info/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.1 工序列表查询")

data, status, _ = api("GET", "/process/process-template/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.2 工艺模板列表查询")

data, status, _ = api("GET", "/process/manufacturing-bom/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.3 制造BOM列表查询")

data, status, _ = api("GET", "/process/instruction/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.4 指令列表查询")

data, status, _ = api("GET", "/process/work-instruction/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.5 作业指导书列表查询")

data, status, _ = api("GET", "/process/machining-program/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.6 加工程序列表查询")

data, status, _ = api("GET", "/process/spray-condition/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "6.7 喷涂条件列表查询")

# ===================== 7. 计划管理 =====================
print("\n===== 7. 计划管理测试 =====")

data, status, _ = api("GET", "/plan/order-plan/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "7.1 订单计划列表查询")

data, status, _ = api("GET", "/plan/production-plan/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "7.2 生产计划列表查询")

# ===================== 8. 工单管理 =====================
print("\n===== 8. 工单管理测试 =====")

data, status, _ = api("GET", "/workorder/work-order/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "8.1 工单列表查询")

# ===================== 9. 派工管理 =====================
print("\n===== 9. 派工管理测试 =====")

data, status, _ = api("GET", "/dispatch/task/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "9.1 派工任务列表查询")

data, status, _ = api("GET", "/dispatch/assignment/list/1", token=admin_token)
log(data and data.get("code") == 200 or status == 200, "9.2 派工分配查询(按任务ID)")

# ===================== 10. 质量管理 =====================
print("\n===== 10. 质量管理测试 =====")

data, status, _ = api("GET", "/quality/shift-handover/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "10.1 交班记录列表查询")

data, status, _ = api("GET", "/quality/order-start-check/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "10.2 开工检查列表查询")

data, status, _ = api("GET", "/quality/work-start-check/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "10.3 工序开工检查列表查询")

data, status, _ = api("GET", "/quality/recheck/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "10.4 复检列表查询")

# ===================== 11. 异常管理 =====================
print("\n===== 11. 异常管理测试 =====")

data, status, _ = api("GET", "/abnormal/contact/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "11.1 异常联系单列表查询")

# ===================== 12. 物料管理 =====================
print("\n===== 12. 物料管理测试 =====")

data, status, _ = api("GET", "/material/requisition/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.1 领料申请列表查询")

data, status, _ = api("GET", "/material/inventory/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.2 库存列表查询")

data, status, _ = api("GET", "/material/return/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.3 退料列表查询")

data, status, _ = api("GET", "/material/delivery-sign/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.4 交付签收列表查询")

data, status, _ = api("GET", "/material/receipt/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.5 成品入库列表查询")

data, status, _ = api("GET", "/material/requisition-order/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "12.6 领料工单列表查询")

# ===================== 13. 综合查询 =====================
print("\n===== 13. 综合查询测试 =====")

data, status, _ = api("GET", "/query/production-work/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "13.1 生产作业查询")

data, status, _ = api("GET", "/query/inspection-work/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "13.2 检验作业查询")

data, status, _ = api("GET", "/query/work-status-view/page?page=1&pageSize=10", token=admin_token)
log(data and data.get("code") == 200, "13.3 工序状态查询")

# ===================== 14. 租户隔离测试 =====================
print("\n===== 14. 租户隔离验证 =====")

import subprocess
t2_username = f"t2admin_{int(time.time())}"
insert_sql = (
    f"INSERT INTO sys_user (username, password, real_name, phone, email, enabled, "
    f"factory_code, tenant_id, account_type) VALUES "
    f"('{t2_username}', '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', "
    f"'租户2管理员', '13900000002', 't2@test.com', 1, 'F002', 2, 'ADMIN');"
)
r = subprocess.run(
    ['docker', 'exec', 'mes-mysql-primary', 'mysql', '-u', 'root', '-p12345678', 'mes', '-e', insert_sql],
    capture_output=True, text=True
)
t2_user_created = r.returncode == 0
log(t2_user_created, "14.1 创建租户2管理员账号(SQL)", t2_username if t2_user_created else r.stderr[:200])

if t2_user_created:
    t2_token = login(t2_username)
    if t2_token:
        log(True, "14.2 租户2管理员登录成功")

        t2_mat_code = f"T2-MAT-{int(time.time())}"
        data, status, _ = api("POST", "/basic/material", token=t2_token, json_data={
            "materialCode": t2_mat_code,
            "materialName": "租户2的物料",
            "materialType": "RAW",
            "baseUnit": "PCS"
        })
        t2_mat_created = data and data.get("code") == 200
        log(t2_mat_created, "14.3 租户2创建物料")

        data, status, _ = api("GET", "/basic/material/page?page=1&pageSize=100", token=t2_token)
        if data and data.get("code") == 200:
            records = data["data"].get("records", [])
            t1_visible = any("TEST-MAT" in str(r.get("materialCode", "")) for r in records)
            log(not t1_visible, "14.4 租户2看不到租户1的物料",
                f"租户2物料数={len(records)}, 看到租户1数据={t1_visible}")
        else:
            log(False, "14.4 租户隔离验证(物料)")

        data, status, _ = api("GET", "/basic/material/page?page=1&pageSize=100", token=admin_token)
        if data and data.get("code") == 200:
            records = data["data"].get("records", [])
            t2_visible = any(t2_mat_code in str(r.get("materialCode", "")) for r in records)
            log(not t2_visible, "14.5 租户1看不到租户2的物料",
                f"租户1物料数={len(records)}, 看到租户2数据={t2_visible}")
        else:
            log(False, "14.5 租户隔离验证(反向)")
    else:
        log(False, "14.2 租户2管理员登录失败")
        log(False, "14.3~14.5 租户隔离测试跳过")
else:
    log(False, "14.2~14.5 租户隔离测试跳过(创建用户失败)")

# ===================== 15. STAFF端业务访问测试 =====================
print("\n===== 15. STAFF端业务访问测试 =====")

if staff_token:
    data, status, _ = api("GET", "/basic/material/page?page=1&pageSize=10", token=staff_token)
    log(data and data.get("code") == 200, "15.1 STAFF能查询物料列表")

    data, status, _ = api("GET", "/workorder/work-order/page?page=1&pageSize=10", token=staff_token)
    log(data and data.get("code") == 200, "15.2 STAFF能查询工单列表")

    data, status, _ = api("GET", "/dispatch/task/page?page=1&pageSize=10", token=staff_token)
    log(data and data.get("code") == 200, "15.3 STAFF能查询派工任务")

    data, status, _ = api("GET", "/quality/shift-handover/page?page=1&pageSize=10", token=staff_token)
    log(data and data.get("code") == 200, "15.4 STAFF能查询交班记录")
else:
    log(False, "15.1~15.4 STAFF端测试跳过(登录失败)")

# ===================== 16. 数据修改/删除测试 =====================
print("\n===== 16. 数据修改/删除测试 =====")

if material_id:
    data, status, _ = api("GET", f"/basic/material/{material_id}", token=admin_token)
    log(data and data.get("code") == 200, "16.1 按ID查询物料详情")

    mat_detail = data["data"] if data and data.get("code") == 200 else {}
    data, status, _ = api("PUT", f"/basic/material/{material_id}", token=admin_token, json_data={
        "materialCode": mat_detail.get("materialCode", f"TEST-MAT-{int(time.time())}"),
        "materialName": "测试物料-已修改",
        "materialType": "RAW",
        "baseUnit": "KG"
    })
    log(data and data.get("code") == 200, "16.2 修改物料信息")

    data, status, _ = api("DELETE", f"/basic/material/{material_id}", token=admin_token)
    log(data and data.get("code") == 200, "16.3 删除物料(逻辑删除)")
else:
    log(False, "16.1~16.3 物料CRUD跳过(创建失败)")

if wc_id:
    data, status, _ = api("DELETE", f"/basic/work-center/{wc_id}", token=admin_token)
    log(data and data.get("code") == 200, "16.4 删除工作中心(逻辑删除)")
else:
    log(False, "16.4 工作中心删除跳过(创建失败)")

if team_id:
    data, status, _ = api("DELETE", f"/team/production-team/{team_id}", token=admin_token)
    log(data and data.get("code") == 200, "16.5 删除班组(逻辑删除)")
else:
    log(False, "16.5 班组删除跳过(创建失败)")

# ===================== 汇总 =====================
print(f"\n{'='*50}")
print(f"测试完成: 通过={PASS}, 失败={FAIL}, 总计={PASS+FAIL}")
print(f"通过率: {PASS/(PASS+FAIL)*100:.1f}%")
print(f"{'='*50}")

failed = [(name, detail) for ok, name, detail in RESULTS if not ok]
if failed:
    print("\n失败项:")
    for name, detail in failed:
        print(f"  [FAIL] {name} - {detail}")

sys.exit(0 if FAIL == 0 else 1)
