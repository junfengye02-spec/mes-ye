"""前后端联调测试 - 通过前端Vite代理访问后端API"""
import requests
import json

FRONTEND = "http://localhost:3000"
PASS = 0
FAIL = 0


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


def api(method, path, token=None, json_data=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = requests.request(method, f"{FRONTEND}{path}", headers=headers, json=json_data, timeout=15)
    try:
        return r.json(), r.status_code
    except:
        return {"raw": r.text[:200]}, r.status_code


print("===== 前后端联调测试（通过Vite代理） =====\n")

r = requests.get(f"{FRONTEND}/", timeout=10)
log(r.status_code == 200 and ("html" in r.text.lower() or "vue" in r.text.lower() or "app" in r.text.lower()),
    "F1 前端页面可访问", f"status={r.status_code}, size={len(r.text)}")

data, status = api("POST", "/api/auth/login", json_data={
    "username": "admin", "password": "admin123", "loginClient": "ADMIN"
})
token = None
if data.get("code") == 200:
    token = data["data"]["accessToken"]
    log(True, "F2 通过前端代理登录admin", "token获取成功")
else:
    log(False, "F2 通过前端代理登录admin", str(data)[:200])

if token:
    data, status = api("GET", "/api/auth/user-info", token=token)
    log(data.get("code") == 200, "F3 获取用户信息",
        f"username={data.get('data',{}).get('username')}" if data.get("code") == 200 else str(data))

    data, status = api("GET", "/api/system/menu/user-tree", token=token)
    log(data.get("code") == 200, "F4 获取用户菜单树(路由)")

    data, status = api("GET", "/api/basic/material/page?page=1&pageSize=10", token=token)
    if data.get("code") == 200:
        total = data["data"]["total"]
        log(True, "F5 物料列表(代理)", f"共{total}条")
    else:
        log(False, "F5 物料列表(代理)")

    data, status = api("GET", "/api/workorder/work-order/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F6 工单列表(代理)")

    data, status = api("GET", "/api/plan/order-plan/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F7 订单计划列表(代理)")

    data, status = api("GET", "/api/dispatch/task/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F8 派工任务(代理)")

    data, status = api("GET", "/api/quality/shift-handover/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F9 交班记录(代理)")

    data, status = api("GET", "/api/abnormal/contact/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F10 异常联系单(代理)")

    data, status = api("GET", "/api/material/inventory/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F11 库存查询(代理)")

    data, status = api("GET", "/api/process/process-info/page?page=1&pageSize=10", token=token)
    log(data.get("code") == 200, "F12 工序列表(代理)")

    data, status = api("GET", "/api/system/role/list", token=token)
    log(data.get("code") == 200, "F13 角色列表(代理)")

    staff_data, _ = api("POST", "/api/auth/login", json_data={
        "username": "zhangsan", "password": "admin123", "loginClient": "USER"
    })
    if staff_data.get("code") == 200:
        staff_token = staff_data["data"]["accessToken"]
        log(True, "F14 STAFF登录(代理)")

        d, s = api("GET", "/api/system/user/page?page=1&pageSize=10", token=staff_token)
        log(s == 403 or d.get("code") != 200, "F15 STAFF被拒访问system(代理)", f"status={s}")

        d, s = api("GET", "/api/basic/material/page?page=1&pageSize=10", token=staff_token)
        log(d.get("code") == 200, "F16 STAFF能查物料(代理)")
    else:
        log(False, "F14 STAFF登录失败")

print(f"\n{'='*50}")
print(f"联调测试完成: 通过={PASS}, 失败={FAIL}, 总计={PASS+FAIL}")
print(f"通过率: {PASS/(PASS+FAIL)*100:.1f}%")
print(f"{'='*50}")
