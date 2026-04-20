"""调试失败的接口"""
import requests
import json

BASE = "http://localhost:9091/api"

def api(method, path, token=None, json_data=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = requests.request(method, f"{BASE}{path}", headers=headers, json=json_data, timeout=15)
    return r.status_code, r.json() if r.text else {}

def login():
    _, data = api("POST", "/auth/login", json_data={"username":"admin","password":"admin123","loginClient":"ADMIN"})
    return data["data"]["accessToken"]

token = login()
print("=== Login OK ===\n")

print("--- 3.2 角色列表 /system/role/list ---")
s, d = api("GET", "/system/role/list", token)
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")

print("--- 3.3 菜单树 /system/menu/tree ---")
s, d = api("GET", "/system/menu/tree", token)
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")

print("--- 9.2 派工分配列表 /dispatch/assignment/page ---")
s, d = api("GET", "/dispatch/assignment/page?page=1&pageSize=10", token)
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")

print("--- 14.1 创建租户2用户 /system/user ---")
import time
s, d = api("POST", "/system/user", token, json_data={
    "username": f"tenant2_admin_{int(time.time())}",
    "realName": "租户2管理员",
    "phone": "13900000002",
    "email": "t2@test.com",
    "factoryCode": "F002",
    "tenantId": 2,
    "accountType": "ADMIN",
    "roleIds": []
})
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")

print("--- 16.2 修改物料 PUT /basic/material/9 ---")
s, d = api("PUT", "/basic/material/9", token, json_data={
    "materialName": "测试物料-已修改",
    "materialType": "RAW",
    "baseUnit": "KG"
})
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")

print("--- 5.2 新增班组 /team/production-team ---")
s, d = api("POST", "/team/production-team", token, json_data={
    "teamCode": f"TEST-TEAM-{int(time.time())}",
    "teamName": "测试班组-甲班",
    "orgCode": "ORG-TEST",
    "orgName": "测试车间",
    "enabled": 1
})
print(f"status={s}, response={json.dumps(d, ensure_ascii=False, indent=2)[:500]}\n")
