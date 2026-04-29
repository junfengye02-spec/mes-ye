-- ============================================================
-- V99_99__seed_test_data.sql
--   MES 系统综合测试数据（Flyway Versioned 脚本，版本号末尾）
--   覆盖全部模块，用于功能冒烟验证。
--
-- 【改名记录 2026-04-21 by mcp7】
--   原文件名 R__seed_test_data.sql（P0-06 由 mcp24 重命名为 Flyway Repeatable）。
--   mcp10 在 M8 Docker 冲烟（mes_m8_docker_smoke_full）时发现 Blocker A：
--   docker-entrypoint-initdb.d 按 ASCII 字母序执行 SQL，R 比 V 小，
--   导致 R__seed_test_data 先于 V1.11__auth_rbac 执行，INSERT sys_user
--   时表未建报 ERROR 1146，MySQL 容器 Exited(1) 链式打断整个栈启动。
--
--   权衡方案：改名为 V99_99__ 前缀
--   - Docker initdb 字母序：排到最后 ✅
--   - Flyway 版本控制：V99_99 > V2.05，正常跑一次后入 history 不再重复 ✅
--   - 代价：失去 repeatable 语义（每次启动重放数据）—— 生产合理
--   - 本地开发如需 reset，手工清库重跑即可
-- ============================================================
-- ⚠⚠⚠ 严禁入生产环境 ⚠⚠⚠
--   本脚本仅用于本地 / Docker 开发环境的一键初始化，内含 4 个与
--   admin 共用 BCrypt(admin123) 密文的弱口令测试账号：
--     zhangsan / lisi / wangwu / zhaoliu
--   直接拷贝到生产会导致：
--     1) 客户安装后裸奔 —— 公网弱口令爆破入口；
--     2) 违反等保三级账号独立原则；
--     3) P0-06 安全整改红线。
--
-- 落地约束（由 P0-06 整改流程执行）：
--   - Flyway 生产 profile 禁止扫描 R__ 开头脚本；
--   - CI/CD 打生产镜像时 `sql/R__*.sql` 必须从构建产物中剔除；
--   - 客户侧部署前必须强制调用 /system/user/change-my-password 改密，
--     由启动时的 WeakPasswordAuditor（prod profile）设置 must_change_password=1 硬卡。
-- ============================================================
-- 变更记录：
--   2026-04-21  mcp24  由 seed_test_data.sql 改名为 R__seed_test_data.sql，
--                     并加本警告头。原文件已从 sql/ 目录移除。
-- ============================================================

SET NAMES utf8mb4;
SET @now = NOW();

-- ==================== 1. 系统管理（补充用户） ====================

INSERT IGNORE INTO sys_user (username, password, real_name, phone, email, enabled, factory_code, created_by, created_time)
VALUES
('zhangsan', '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', '张三', '13800001001', 'zhangsan@mes.com', 1, 'F001', 'system', @now),
('lisi',    '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', '李四', '13800001002', 'lisi@mes.com',    1, 'F001', 'system', @now),
('wangwu',  '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', '王五', '13800001003', 'wangwu@mes.com',  1, 'F001', 'system', @now),
('zhaoliu', '$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy', '赵六', '13800001004', 'zhaoliu@mes.com', 1, 'F002', 'system', @now);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='zhangsan' AND r.role_code='PRODUCTION_MANAGER'
UNION ALL
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='lisi' AND r.role_code='QUALITY_MANAGER'
UNION ALL
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='wangwu' AND r.role_code='OPERATOR'
UNION ALL
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='zhaoliu' AND r.role_code='OPERATOR';

-- 生产主管角色分配菜单（工单/派工/计划/基础）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code='PRODUCTION_MANAGER' AND m.id IN (1,2,3,4,5,6,9,10,101,102,103,201,301,302,303,304,305,306,307,401,402,501,601,901,902,903,904,905,906,907,908,1001,1002,1003,1004,1005,1006,1007);

-- 质量管理员角色分配菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code='QUALITY_MANAGER' AND m.id IN (1,7,8,9,101,102,103,701,801,802,803,804,901,902,903,904,905,906,907,908);

-- 普通操作员角色分配菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code='OPERATOR' AND m.id IN (1,9,101,102,103,901,902,903,904,905,906,907,908);

-- ==================== 2. 基础数据 ====================

-- 物料档案
INSERT INTO mes_material (material_code, material_name, material_type, category_level1, category_level2, g_code, product_type, product_category, machine_model, part_name, factory, base_unit, trace_mode, need_inspection, drawing_no, created_by, created_time) VALUES
('MAT-001', '高温合金叶片毛坯', '原材料', '金属材料', '高温合金', 'G-HTA-001', '叶片', '涡轮部件', 'GT-F5', '涡轮叶片', '上海工厂', 'KG', 'BATCH', 1, 'DWG-HTA-001', 'admin', @now),
('MAT-002', '镍基合金棒材',     '原材料', '金属材料', '镍基合金', 'G-NIA-002', '棒材', '基础材料', 'GT-F5', '主轴',     '上海工厂', 'KG', 'BATCH', 1, 'DWG-NIA-002', 'admin', @now),
('MAT-003', '陶瓷涂层粉末',     '辅料',   '涂层材料', '陶瓷粉末', 'G-CTP-003', '粉末', '涂层材料', 'GT-F5', '热障涂层', '上海工厂', 'KG', 'BATCH', 0, 'DWG-CTP-003', 'admin', @now),
('MAT-004', '钛合金板材',       '原材料', '金属材料', '钛合金',   'G-TIA-004', '板材', '结构件',   'GT-H25', '压气机盘', '苏州工厂', 'KG', 'SERIAL', 1, 'DWG-TIA-004', 'admin', @now),
('MAT-005', '不锈钢管件',       '半成品', '金属材料', '不锈钢',   'G-SSP-005', '管件', '管路部件', 'GT-H25', '燃油管路', '苏州工厂', 'PC', 'SERIAL', 1, 'DWG-SSP-005', 'admin', @now),
('MAT-006', '密封垫圈',         '标准件', '密封件',   '橡胶件',   'G-SRG-006', '垫圈', '密封件',   'GT-F5',  '密封垫',   '上海工厂', 'PC', 'QUANTITY', 0, NULL, 'admin', @now),
('MAT-007', '涡轮叶片成品',     '成品',   '涡轮部件', '叶片成品', 'G-TBF-007', '叶片', '涡轮部件', 'GT-F5',  '涡轮叶片', '上海工厂', 'PC', 'SERIAL', 1, 'DWG-TBF-007', 'admin', @now),
('MAT-008', '压气机盘成品',     '成品',   '结构件',   '盘类成品', 'G-CDF-008', '盘件', '结构件',   'GT-H25', '压气机盘', '苏州工厂', 'PC', 'SERIAL', 1, 'DWG-CDF-008', 'admin', @now)
ON DUPLICATE KEY UPDATE
  material_name=VALUES(material_name), material_type=VALUES(material_type),
  category_level1=VALUES(category_level1), category_level2=VALUES(category_level2),
  g_code=VALUES(g_code), product_type=VALUES(product_type),
  product_category=VALUES(product_category), machine_model=VALUES(machine_model),
  part_name=VALUES(part_name), factory=VALUES(factory), base_unit=VALUES(base_unit),
  trace_mode=VALUES(trace_mode), need_inspection=VALUES(need_inspection),
  drawing_no=VALUES(drawing_no), created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 物料价格
INSERT INTO mes_material_price (material_id, unit_price, unit, created_by, created_time)
SELECT id, CASE material_code
    WHEN 'MAT-001' THEN 2580.0000
    WHEN 'MAT-002' THEN 1850.0000
    WHEN 'MAT-003' THEN 680.0000
    WHEN 'MAT-004' THEN 3200.0000
    WHEN 'MAT-005' THEN 450.0000
    WHEN 'MAT-006' THEN 12.5000
    WHEN 'MAT-007' THEN 15800.0000
    WHEN 'MAT-008' THEN 28500.0000
END, base_unit, 'admin', @now
FROM mes_material WHERE material_code IN ('MAT-001','MAT-002','MAT-003','MAT-004','MAT-005','MAT-006','MAT-007','MAT-008')
ON DUPLICATE KEY UPDATE
  unit_price=VALUES(unit_price), unit=VALUES(unit),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 工作中心
INSERT INTO mes_work_center (work_center_code, work_center_name, work_center_category, business_unit, work_calendar, resource_order, batch_qty, efficiency, resource_type, resource_capacity, created_by, created_time) VALUES
('WC-CAST',  '精密铸造中心', '生产',   '涡轮事业部', 'CAL-STD-8H', 1, 50.00, 0.92, '铸造设备', 100.00, 'admin', @now),
('WC-MACH',  '数控加工中心', '生产',   '涡轮事业部', 'CAL-STD-8H', 2, 20.00, 0.95, '数控机床', 80.00,  'admin', @now),
('WC-SPRAY', '热喷涂中心',   '生产',   '涡轮事业部', 'CAL-STD-8H', 3, 30.00, 0.88, '喷涂设备', 60.00,  'admin', @now),
('WC-HEAT',  '热处理中心',   '生产',   '涡轮事业部', 'CAL-24H',    4, 100.00, 0.90, '热处理炉', 200.00, 'admin', @now),
('WC-QC',    '质量检验中心', '检验',   '质量部',     'CAL-STD-8H', 5, 40.00, 0.93, '检测设备', 120.00, 'admin', @now),
('WC-ASSY',  '装配中心',     '生产',   '装配事业部', 'CAL-STD-8H', 6, 10.00, 0.91, '装配工位', 50.00,  'admin', @now),
('WC-PACK',  '包装发运中心', '物流',   '物流部',     'CAL-STD-8H', 7, 100.00, 0.96, '包装设备', 150.00, 'admin', @now)
ON DUPLICATE KEY UPDATE
  work_center_name=VALUES(work_center_name), work_center_category=VALUES(work_center_category),
  business_unit=VALUES(business_unit), work_calendar=VALUES(work_calendar),
  resource_order=VALUES(resource_order), batch_qty=VALUES(batch_qty),
  efficiency=VALUES(efficiency), resource_type=VALUES(resource_type),
  resource_capacity=VALUES(resource_capacity),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- ==================== 3. 班组管理 ====================

INSERT INTO mes_production_team (team_code, team_name, org_code, org_name, enabled, description, created_by, created_time) VALUES
('TM-CAST-A', '铸造A班', 'ORG-TURB', '涡轮事业部', 1, '精密铸造A班组，负责白班铸造作业', 'admin', @now),
('TM-CAST-B', '铸造B班', 'ORG-TURB', '涡轮事业部', 1, '精密铸造B班组，负责夜班铸造作业', 'admin', @now),
('TM-MACH-A', '加工A班', 'ORG-TURB', '涡轮事业部', 1, '数控加工A班组', 'admin', @now),
('TM-SPRAY',  '喷涂班',   'ORG-TURB', '涡轮事业部', 1, '热喷涂作业班组', 'admin', @now),
('TM-QC',     '质检班',   'ORG-QA',   '质量部',     1, '成品质量检验班组', 'admin', @now),
('TM-ASSY',   '装配班',   'ORG-ASSY', '装配事业部', 1, '总装装配班组', 'admin', @now)
ON DUPLICATE KEY UPDATE
  team_name=VALUES(team_name), org_code=VALUES(org_code), org_name=VALUES(org_name),
  enabled=VALUES(enabled), description=VALUES(description),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- ==================== 4. 工艺管理 ====================

-- 指示书
INSERT INTO mes_instruction (instruction_no, version, status, project_no, wbs, new_or_repair_type, main_type, product_category, product_type, part_name, work_order_no, qty, issue_date, assignee, created_by, created_time) VALUES
('INS-2026-001', 'V1.0', 'ACTIVE',   'PRJ-GT-F5-001',  'WBS-001', '新制', '主机', '涡轮部件', '叶片', '涡轮叶片',  'WO-2026-001', 50, '2026-03-01', '张三', 'admin', @now),
('INS-2026-002', 'V1.0', 'ACTIVE',   'PRJ-GT-H25-001', 'WBS-002', '新制', '主机', '结构件',   '盘件', '压气机盘',  'WO-2026-003', 20, '2026-03-05', '李四', 'admin', @now),
('INS-2026-003', 'V1.0', 'DRAFT',    'PRJ-GT-F5-002',  'WBS-003', '维修', '主机', '涡轮部件', '叶片', '涡轮叶片修复', NULL, 10, '2026-03-10', '王五', 'admin', @now)
ON DUPLICATE KEY UPDATE
  version=VALUES(version), status=VALUES(status), project_no=VALUES(project_no),
  wbs=VALUES(wbs), new_or_repair_type=VALUES(new_or_repair_type),
  main_type=VALUES(main_type), product_category=VALUES(product_category),
  product_type=VALUES(product_type), part_name=VALUES(part_name),
  work_order_no=VALUES(work_order_no), qty=VALUES(qty),
  issue_date=VALUES(issue_date), assignee=VALUES(assignee),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 指示书阶段（子表：无业务唯一索引，重跑前先清理对应主表记录的子表数据）
DELETE FROM mes_instruction_stage WHERE instruction_id IN (1,2);
INSERT INTO mes_instruction_stage (instruction_id, stage, role, content, required_date, actual_date, created_time) VALUES
(1, '收货前',       '售后服务',     '确认返修件数量及外观状态',       '2026-03-02', '2026-03-02', @now),
(1, '收货检查时',   '制造&品管',    '检测毛坯尺寸及表面缺陷',       '2026-03-03', '2026-03-03', @now),
(1, '收货检查报告后', '品管',       '出具检验报告，确认合格批次',     '2026-03-04', NULL, @now),
(2, '收货前',       '采购',         '确认原材料到货清单',           '2026-03-06', '2026-03-06', @now),
(2, '收货检查时',   '制造&品管',    '检测钛合金板材厚度与硬度',     '2026-03-07', NULL, @now)
ON DUPLICATE KEY UPDATE
  stage=VALUES(stage), role=VALUES(role), content=VALUES(content),
  required_date=VALUES(required_date), actual_date=VALUES(actual_date),
  created_time=VALUES(created_time);

-- 指示书序列号（子表：同上幂等策略）
DELETE FROM mes_instruction_serial WHERE instruction_id IN (1,2);
INSERT INTO mes_instruction_serial (instruction_id, product_type, qty, receive_kg_code, send_g_code) VALUES
(1, '叶片', 50, 'KG-INS001-001', 'G-INS001-001'),
(1, '叶片', 50, 'KG-INS001-002', 'G-INS001-002'),
(2, '盘件', 20, 'KG-INS002-001', 'G-INS002-001')
ON DUPLICATE KEY UPDATE
  product_type=VALUES(product_type), qty=VALUES(qty),
  receive_kg_code=VALUES(receive_kg_code), send_g_code=VALUES(send_g_code);

-- 工序模板
INSERT INTO mes_process_template (process_no, process_name, product_category, machine_model, product_type, process_type, handle_time, work_center_id, remark, created_by, created_time) VALUES
('PT-CAST-01',  '精密铸造',   '涡轮部件', 'GT-F5',  '叶片', '生产工序', 480.00, (SELECT id FROM mes_work_center WHERE work_center_code='WC-CAST' LIMIT 1),  '蜡模制作→壳型→浇注→脱壳', 'admin', @now),
('PT-MACH-01',  '数控粗加工', '涡轮部件', 'GT-F5',  '叶片', '生产工序', 120.00, (SELECT id FROM mes_work_center WHERE work_center_code='WC-MACH' LIMIT 1),  '五轴铣削粗加工', 'admin', @now),
('PT-MACH-02',  '数控精加工', '涡轮部件', 'GT-F5',  '叶片', '生产工序', 180.00, (SELECT id FROM mes_work_center WHERE work_center_code='WC-MACH' LIMIT 1),  '五轴铣削精加工', 'admin', @now),
('PT-HEAT-01',  '热处理',     '涡轮部件', 'GT-F5',  '叶片', '生产工序', 360.00, (SELECT id FROM mes_work_center WHERE work_center_code='WC-HEAT' LIMIT 1),  '固溶时效处理', 'admin', @now),
('PT-SPRAY-01', '热障涂层喷涂', '涡轮部件', 'GT-F5', '叶片', '生产工序', 240.00, (SELECT id FROM mes_work_center WHERE work_center_code='WC-SPRAY' LIMIT 1), '等离子喷涂陶瓷涂层', 'admin', @now),
('PT-QC-01',    '成品检验',   '涡轮部件', 'GT-F5',  '叶片', '检验工序', 60.00,  (SELECT id FROM mes_work_center WHERE work_center_code='WC-QC' LIMIT 1),    '尺寸/涂层/探伤全检', 'admin', @now),
('PT-ASSY-01',  '部件装配',   '涡轮部件', 'GT-F5',  '叶片', '生产工序', 90.00,  (SELECT id FROM mes_work_center WHERE work_center_code='WC-ASSY' LIMIT 1),  '叶片装配至涡轮盘', 'admin', @now)
ON DUPLICATE KEY UPDATE
  process_name=VALUES(process_name), product_category=VALUES(product_category),
  machine_model=VALUES(machine_model), product_type=VALUES(product_type),
  process_type=VALUES(process_type), handle_time=VALUES(handle_time),
  work_center_id=VALUES(work_center_id), remark=VALUES(remark),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 工序信息
INSERT INTO mes_process_info (process_no, process_name, process_code, product, g_code, product_category, machine_model, product_type, process_type, factory, business_org, work_center_id, team_id, handle_time, created_by, created_time) VALUES
('PI-CAST-001',  '铸造-涡轮叶片',   'PC-CAST-TB',  '涡轮叶片', 'G-HTA-001', '涡轮部件', 'GT-F5', '叶片', '生产工序', '上海工厂', '涡轮事业部', (SELECT id FROM mes_work_center WHERE work_center_code='WC-CAST' LIMIT 1),  (SELECT id FROM mes_production_team WHERE team_code='TM-CAST-A' LIMIT 1), 480.00, 'admin', @now),
('PI-MACH-001',  '粗加工-涡轮叶片', 'PC-MACH-TB1', '涡轮叶片', 'G-HTA-001', '涡轮部件', 'GT-F5', '叶片', '生产工序', '上海工厂', '涡轮事业部', (SELECT id FROM mes_work_center WHERE work_center_code='WC-MACH' LIMIT 1),  (SELECT id FROM mes_production_team WHERE team_code='TM-MACH-A' LIMIT 1), 120.00, 'admin', @now),
('PI-MACH-002',  '精加工-涡轮叶片', 'PC-MACH-TB2', '涡轮叶片', 'G-HTA-001', '涡轮部件', 'GT-F5', '叶片', '生产工序', '上海工厂', '涡轮事业部', (SELECT id FROM mes_work_center WHERE work_center_code='WC-MACH' LIMIT 1),  (SELECT id FROM mes_production_team WHERE team_code='TM-MACH-A' LIMIT 1), 180.00, 'admin', @now),
('PI-SPRAY-001', '喷涂-涡轮叶片',   'PC-SPRAY-TB', '涡轮叶片', 'G-HTA-001', '涡轮部件', 'GT-F5', '叶片', '生产工序', '上海工厂', '涡轮事业部', (SELECT id FROM mes_work_center WHERE work_center_code='WC-SPRAY' LIMIT 1), (SELECT id FROM mes_production_team WHERE team_code='TM-SPRAY' LIMIT 1),  240.00, 'admin', @now),
('PI-QC-001',    '终检-涡轮叶片',   'PC-QC-TB',    '涡轮叶片', 'G-HTA-001', '涡轮部件', 'GT-F5', '叶片', '检验工序', '上海工厂', '质量部',     (SELECT id FROM mes_work_center WHERE work_center_code='WC-QC' LIMIT 1),    (SELECT id FROM mes_production_team WHERE team_code='TM-QC' LIMIT 1),     60.00,  'admin', @now)
ON DUPLICATE KEY UPDATE
  process_name=VALUES(process_name), process_code=VALUES(process_code),
  product=VALUES(product), g_code=VALUES(g_code),
  product_category=VALUES(product_category), machine_model=VALUES(machine_model),
  product_type=VALUES(product_type), process_type=VALUES(process_type),
  factory=VALUES(factory), business_org=VALUES(business_org),
  work_center_id=VALUES(work_center_id), team_id=VALUES(team_id),
  handle_time=VALUES(handle_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 指导书
INSERT INTO mes_work_instruction (instruction_code, level, status, created_by, created_time) VALUES
('WI-CAST-001',  'A', 'ACTIVE',   'admin', @now),
('WI-MACH-001',  'A', 'ACTIVE',   'admin', @now),
('WI-SPRAY-001', 'B', 'ACTIVE',   'admin', @now),
('WI-QC-001',    'A', 'ACTIVE',   'admin', @now),
('WI-ASSY-001',  'B', 'DRAFT',    'admin', @now)
ON DUPLICATE KEY UPDATE
  level=VALUES(level), status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 指导书人员（子表：按 instruction_id 清理后再插入，保证幂等）
DELETE FROM mes_work_instruction_person
 WHERE instruction_id IN (SELECT id FROM mes_work_instruction WHERE instruction_code IN ('WI-CAST-001','WI-MACH-001','WI-SPRAY-001','WI-QC-001'));
INSERT INTO mes_work_instruction_person (instruction_id, person_code, person_name, person_category, gender, phone, email) VALUES
((SELECT id FROM mes_work_instruction WHERE instruction_code='WI-CAST-001'), 'P001', '陈铸造', '技术工程师', '男', '13900001001', 'chen@mes.com'),
((SELECT id FROM mes_work_instruction WHERE instruction_code='WI-MACH-001'), 'P002', '刘加工', '高级技师',   '男', '13900001002', 'liu@mes.com'),
((SELECT id FROM mes_work_instruction WHERE instruction_code='WI-SPRAY-001'), 'P003', '赵喷涂', '技术工程师', '女', '13900001003', 'zhao@mes.com'),
((SELECT id FROM mes_work_instruction WHERE instruction_code='WI-QC-001'),   'P004', '孙质检', '质检工程师', '女', '13900001004', 'sun@mes.com')
ON DUPLICATE KEY UPDATE
  person_name=VALUES(person_name), person_category=VALUES(person_category),
  gender=VALUES(gender), phone=VALUES(phone), email=VALUES(email);

-- 喷涂条件表
INSERT INTO mes_spray_condition (condition_no, powder_feed_rate, spray_distance, spray_gun_model, powder_feeder, powder_feeder_speed, oxygen_scfh, kerosene_gph, combustion_pressure, carrier_gas, equipment, powder_type, minister_approver, section_approver, leader_approver, created_by, created_time) VALUES
('SC-001', 45.00, 380.00, 'JP-8000', 'PF-1200', 3.50, 2000.00, 6.50, 150.00, '氮气N2', 'HVOF-01', 'YSZ 8wt%', '部长A', '工段长B', '系长C', 'admin', @now),
('SC-002', 38.00, 350.00, 'JP-5000', 'PF-1100', 3.00, 1800.00, 5.80, 140.00, '氮气N2', 'HVOF-02', 'CoNiCrAlY', '部长A', '工段长B', '系长C', 'admin', @now)
ON DUPLICATE KEY UPDATE
  powder_feed_rate=VALUES(powder_feed_rate), spray_distance=VALUES(spray_distance),
  spray_gun_model=VALUES(spray_gun_model), powder_feeder=VALUES(powder_feeder),
  powder_feeder_speed=VALUES(powder_feeder_speed), oxygen_scfh=VALUES(oxygen_scfh),
  kerosene_gph=VALUES(kerosene_gph), combustion_pressure=VALUES(combustion_pressure),
  carrier_gas=VALUES(carrier_gas), equipment=VALUES(equipment),
  powder_type=VALUES(powder_type), minister_approver=VALUES(minister_approver),
  section_approver=VALUES(section_approver), leader_approver=VALUES(leader_approver),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 机械加工程序表
INSERT INTO mes_machining_program (g_code, program_table, product_name, created_by, created_time) VALUES
('G-MP-001', 'PRG-TB-ROUGH-01', '涡轮叶片粗加工程序', 'admin', @now),
('G-MP-002', 'PRG-TB-FINISH-01', '涡轮叶片精加工程序', 'admin', @now),
('G-MP-003', 'PRG-CD-ROUGH-01', '压气机盘粗加工程序', 'admin', @now)
ON DUPLICATE KEY UPDATE
  program_table=VALUES(program_table), product_name=VALUES(product_name),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 制造BOM
INSERT INTO mes_manufacturing_bom (bom_code, bom_name, product_code, product_name, product_category, machine_model, product_type, new_or_repair_type, bom_version, status, effective_date, factory_org, created_by, created_time) VALUES
('BOM-TB-001', '涡轮叶片制造BOM', 'MAT-007', '涡轮叶片成品', '涡轮部件', 'GT-F5', '叶片', '新制', 'V1.0', 'PUBLISHED', '2026-01-01', '上海工厂', 'admin', @now),
('BOM-CD-001', '压气机盘制造BOM', 'MAT-008', '压气机盘成品', '结构件',   'GT-H25', '盘件', '新制', 'V1.0', 'PUBLISHED', '2026-01-01', '苏州工厂', 'admin', @now)
ON DUPLICATE KEY UPDATE
  bom_name=VALUES(bom_name), product_code=VALUES(product_code),
  product_name=VALUES(product_name), product_category=VALUES(product_category),
  machine_model=VALUES(machine_model), product_type=VALUES(product_type),
  new_or_repair_type=VALUES(new_or_repair_type), bom_version=VALUES(bom_version),
  status=VALUES(status), effective_date=VALUES(effective_date),
  factory_org=VALUES(factory_org),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- BOM明细（子表：按 bom_code 先清理再插入，保证幂等）
DELETE FROM mes_manufacturing_bom_item
 WHERE bom_id IN (SELECT id FROM mes_manufacturing_bom WHERE bom_code IN ('BOM-TB-001','BOM-CD-001'));
INSERT INTO mes_manufacturing_bom_item (bom_id, level, material_code, material_name, material_type, quantity, loss_rate, unit, supply_type, process_no, is_key_part, sequence_no, created_time)
SELECT b.id, 1, 'MAT-001', '高温合金叶片毛坯', '原材料', 2.500000, 5.00, 'KG', '采购', 'PT-CAST-01', 1, 1, @now FROM mes_manufacturing_bom b WHERE b.bom_code='BOM-TB-001'
UNION ALL
SELECT b.id, 1, 'MAT-003', '陶瓷涂层粉末', '辅料', 0.150000, 10.00, 'KG', '采购', 'PT-SPRAY-01', 0, 2, @now FROM mes_manufacturing_bom b WHERE b.bom_code='BOM-TB-001'
UNION ALL
SELECT b.id, 1, 'MAT-006', '密封垫圈', '标准件', 2.000000, 0.00, 'PC', '采购', 'PT-ASSY-01', 0, 3, @now FROM mes_manufacturing_bom b WHERE b.bom_code='BOM-TB-001'
UNION ALL
SELECT b.id, 1, 'MAT-004', '钛合金板材', '原材料', 8.000000, 3.00, 'KG', '采购', NULL, 1, 1, @now FROM mes_manufacturing_bom b WHERE b.bom_code='BOM-CD-001'
UNION ALL
SELECT b.id, 1, 'MAT-006', '密封垫圈', '标准件', 4.000000, 0.00, 'PC', '采购', NULL, 0, 2, @now FROM mes_manufacturing_bom b WHERE b.bom_code='BOM-CD-001'
ON DUPLICATE KEY UPDATE
  level=VALUES(level), material_name=VALUES(material_name),
  material_type=VALUES(material_type), quantity=VALUES(quantity),
  loss_rate=VALUES(loss_rate), unit=VALUES(unit),
  supply_type=VALUES(supply_type), process_no=VALUES(process_no),
  is_key_part=VALUES(is_key_part), sequence_no=VALUES(sequence_no),
  created_time=VALUES(created_time);

-- ==================== 5. 计划管理 ====================

INSERT INTO mes_order_plan (order_no, product_code, product_name, project_name, wbs_element, new_or_repair_type, work_type, machine_model, product_category, product_type, plan_qty, qty_unit, factory_org, plan_org, main_org, status, flow_status, plan_start_time, plan_end_time, data_source, created_by, created_time) VALUES
('OP-2026-001', 'MAT-007', '涡轮叶片成品', 'GT-F5燃机项目',   'WBS-F5-001',  '新制', '主机', 'GT-F5',  '涡轮部件', '叶片', 50.0000, 'PC', '上海工厂', '上海计划部', '涡轮事业部', 'RELEASED', 'RUNNING',    '2026-03-01 08:00:00', '2026-06-30 17:00:00', 'MANUAL', 'admin', @now),
('OP-2026-002', 'MAT-008', '压气机盘成品', 'GT-H25燃机项目',  'WBS-H25-001', '新制', '主机', 'GT-H25', '结构件',   '盘件', 20.0000, 'PC', '苏州工厂', '苏州计划部', '结构事业部', 'RELEASED', 'RUNNING',    '2026-03-10 08:00:00', '2026-07-31 17:00:00', 'MANUAL', 'admin', @now),
('OP-2026-003', 'MAT-007', '涡轮叶片成品', 'GT-F5维修项目',   'WBS-F5-R01',  '维修', '维修', 'GT-F5',  '涡轮部件', '叶片', 10.0000, 'PC', '上海工厂', '上海计划部', '涡轮事业部', 'CREATED',  NULL,         '2026-04-01 08:00:00', '2026-05-15 17:00:00', 'MANUAL', 'admin', @now),
('OP-2026-004', 'MAT-005', '不锈钢管件',   '管路系统项目',    'WBS-PIPE-01', '新制', '主机', 'GT-H25', '管路部件', '管件', 100.0000, 'PC', '苏州工厂', '苏州计划部', '管路事业部', 'RELEASED', 'RUNNING',    '2026-03-15 08:00:00', '2026-05-31 17:00:00', 'MANUAL', 'admin', @now)
ON DUPLICATE KEY UPDATE
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  project_name=VALUES(project_name), wbs_element=VALUES(wbs_element),
  new_or_repair_type=VALUES(new_or_repair_type), work_type=VALUES(work_type),
  machine_model=VALUES(machine_model), product_category=VALUES(product_category),
  product_type=VALUES(product_type), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), factory_org=VALUES(factory_org),
  plan_org=VALUES(plan_org), main_org=VALUES(main_org),
  status=VALUES(status), flow_status=VALUES(flow_status),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  data_source=VALUES(data_source),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 生产计划（无唯一索引，清理重跑保证幂等）
DELETE FROM mes_production_plan WHERE order_no IN ('OP-2026-001','OP-2026-002','OP-2026-004');
INSERT INTO mes_production_plan (order_plan_id, order_no, product_code, product_name, new_or_repair_type, work_type, machine_model, product_category, product_type, wbs_element, plan_org, plan_qty, qty_unit, status, plan_start_time, plan_end_time, created_by, created_time)
SELECT op.id, op.order_no, op.product_code, op.product_name, op.new_or_repair_type, op.work_type, op.machine_model, op.product_category, op.product_type, op.wbs_element, op.plan_org,
  CASE WHEN op.order_no='OP-2026-001' THEN 25.0000 ELSE op.plan_qty END,
  op.qty_unit, 'RELEASED',
  op.plan_start_time, DATE_ADD(op.plan_start_time, INTERVAL 45 DAY),
  'admin', @now
FROM mes_order_plan op WHERE op.order_no IN ('OP-2026-001','OP-2026-002','OP-2026-004') AND op.deleted=0
ON DUPLICATE KEY UPDATE
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  new_or_repair_type=VALUES(new_or_repair_type), work_type=VALUES(work_type),
  machine_model=VALUES(machine_model), product_category=VALUES(product_category),
  product_type=VALUES(product_type), wbs_element=VALUES(wbs_element),
  plan_org=VALUES(plan_org), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 第二批次 OP-2026-001
INSERT INTO mes_production_plan (order_plan_id, order_no, product_code, product_name, new_or_repair_type, work_type, machine_model, product_category, product_type, wbs_element, plan_org, plan_qty, qty_unit, status, plan_start_time, plan_end_time, created_by, created_time)
SELECT op.id, op.order_no, op.product_code, op.product_name, op.new_or_repair_type, op.work_type, op.machine_model, op.product_category, op.product_type, op.wbs_element, op.plan_org,
  25.0000, op.qty_unit, 'CREATED',
  DATE_ADD(op.plan_start_time, INTERVAL 46 DAY), op.plan_end_time,
  'admin', @now
FROM mes_order_plan op WHERE op.order_no='OP-2026-001' AND op.deleted=0
ON DUPLICATE KEY UPDATE
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  new_or_repair_type=VALUES(new_or_repair_type), work_type=VALUES(work_type),
  machine_model=VALUES(machine_model), product_category=VALUES(product_category),
  product_type=VALUES(product_type), wbs_element=VALUES(wbs_element),
  plan_org=VALUES(plan_org), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- ==================== 6. 生产工单 ====================

INSERT INTO mes_work_order (work_order_no, work_order_type, order_plan_no, product_code, product_name, main_product, machine_model, product_category, product_type, bom_code, project_name, wbs_element, new_or_repair_type, work_type, plan_qty, qty_unit, factory_org, plan_org, main_org, status, plan_start_time, plan_end_time, created_by, created_time) VALUES
('WO-2026-001', '标准生产', 'OP-2026-001', 'MAT-007', '涡轮叶片成品', '涡轮叶片成品', 'GT-F5',  '涡轮部件', '叶片', 'BOM-TB-001', 'GT-F5燃机项目',   'WBS-F5-001',  '新制', '主机', 25.0000, 'PC', '上海工厂', '上海计划部', '涡轮事业部', 'IN_PROGRESS', '2026-03-01 08:00:00', '2026-04-15 17:00:00', 'admin', @now),
('WO-2026-002', '标准生产', 'OP-2026-001', 'MAT-007', '涡轮叶片成品', '涡轮叶片成品', 'GT-F5',  '涡轮部件', '叶片', 'BOM-TB-001', 'GT-F5燃机项目',   'WBS-F5-001',  '新制', '主机', 25.0000, 'PC', '上海工厂', '上海计划部', '涡轮事业部', 'RELEASED',    '2026-04-16 08:00:00', '2026-06-30 17:00:00', 'admin', @now),
('WO-2026-003', '标准生产', 'OP-2026-002', 'MAT-008', '压气机盘成品', '压气机盘成品', 'GT-H25', '结构件',   '盘件', 'BOM-CD-001', 'GT-H25燃机项目',  'WBS-H25-001', '新制', '主机', 20.0000, 'PC', '苏州工厂', '苏州计划部', '结构事业部', 'RELEASED',    '2026-03-10 08:00:00', '2026-07-31 17:00:00', 'admin', @now),
('WO-2026-004', '标准生产', 'OP-2026-004', 'MAT-005', '不锈钢管件',   '不锈钢管件',   'GT-H25', '管路部件', '管件', NULL,         '管路系统项目',    'WBS-PIPE-01', '新制', '主机', 100.0000, 'PC', '苏州工厂', '苏州计划部', '管路事业部', 'CREATED',     '2026-03-15 08:00:00', '2026-05-31 17:00:00', 'admin', @now)
ON DUPLICATE KEY UPDATE
  work_order_type=VALUES(work_order_type), order_plan_no=VALUES(order_plan_no),
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  main_product=VALUES(main_product), machine_model=VALUES(machine_model),
  product_category=VALUES(product_category), product_type=VALUES(product_type),
  bom_code=VALUES(bom_code), project_name=VALUES(project_name),
  wbs_element=VALUES(wbs_element), new_or_repair_type=VALUES(new_or_repair_type),
  work_type=VALUES(work_type), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), factory_org=VALUES(factory_org),
  plan_org=VALUES(plan_org), main_org=VALUES(main_org),
  status=VALUES(status), plan_start_time=VALUES(plan_start_time),
  plan_end_time=VALUES(plan_end_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 工单工作清单（子表：按工单清理后再插入，保证幂等）
DELETE FROM mes_work_order_task
 WHERE work_order_id IN (SELECT id FROM mes_work_order WHERE work_order_no IN ('WO-2026-001','WO-2026-002','WO-2026-003'));
INSERT INTO mes_work_order_task (work_order_id, task_no, task_name, plan_qty, qty_unit, status, sequence_no, created_time)
SELECT wo.id, t.task_no, t.task_name, wo.plan_qty, wo.qty_unit, t.status, t.seq, @now
FROM mes_work_order wo
CROSS JOIN (
  SELECT 'WK-010' AS task_no, '精密铸造' AS task_name, 'COMPLETED' AS status, 1 AS seq UNION ALL
  SELECT 'WK-020', '数控粗加工', 'COMPLETED', 2 UNION ALL
  SELECT 'WK-030', '数控精加工', 'IN_PROGRESS', 3 UNION ALL
  SELECT 'WK-040', '热处理', 'CREATED', 4 UNION ALL
  SELECT 'WK-050', '热障涂层喷涂', 'CREATED', 5 UNION ALL
  SELECT 'WK-060', '成品检验', 'CREATED', 6
) t
WHERE wo.work_order_no = 'WO-2026-001'
ON DUPLICATE KEY UPDATE
  task_name=VALUES(task_name), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  sequence_no=VALUES(sequence_no), created_time=VALUES(created_time);

INSERT INTO mes_work_order_task (work_order_id, task_no, task_name, plan_qty, qty_unit, status, sequence_no, created_time)
SELECT wo.id, t.task_no, t.task_name, wo.plan_qty, wo.qty_unit, 'CREATED', t.seq, @now
FROM mes_work_order wo
CROSS JOIN (
  SELECT 'WK-010' AS task_no, '精密铸造' AS task_name, 1 AS seq UNION ALL
  SELECT 'WK-020', '数控粗加工', 2 UNION ALL
  SELECT 'WK-030', '数控精加工', 3 UNION ALL
  SELECT 'WK-040', '热处理', 4 UNION ALL
  SELECT 'WK-050', '热障涂层喷涂', 5 UNION ALL
  SELECT 'WK-060', '成品检验', 6
) t
WHERE wo.work_order_no = 'WO-2026-002'
ON DUPLICATE KEY UPDATE
  task_name=VALUES(task_name), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  sequence_no=VALUES(sequence_no), created_time=VALUES(created_time);

INSERT INTO mes_work_order_task (work_order_id, task_no, task_name, plan_qty, qty_unit, status, sequence_no, created_time)
SELECT wo.id, t.task_no, t.task_name, wo.plan_qty, wo.qty_unit, 'CREATED', t.seq, @now
FROM mes_work_order wo
CROSS JOIN (
  SELECT 'WK-010' AS task_no, '锻造' AS task_name, 1 AS seq UNION ALL
  SELECT 'WK-020', '粗车', 2 UNION ALL
  SELECT 'WK-030', '精车', 3 UNION ALL
  SELECT 'WK-040', '热处理', 4 UNION ALL
  SELECT 'WK-050', '成品检验', 5
) t
WHERE wo.work_order_no = 'WO-2026-003'
ON DUPLICATE KEY UPDATE
  task_name=VALUES(task_name), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  sequence_no=VALUES(sequence_no), created_time=VALUES(created_time);

-- 工单输入物料（子表）
DELETE FROM mes_work_order_input_material
 WHERE work_order_id IN (SELECT id FROM mes_work_order WHERE work_order_no IN ('WO-2026-001','WO-2026-002','WO-2026-003'));
INSERT INTO mes_work_order_input_material (work_order_id, material_code, material_name, required_qty, issued_qty, qty_unit)
SELECT wo.id, 'MAT-001', '高温合金叶片毛坯', 62.5000, 62.5000, 'KG' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'MAT-003', '陶瓷涂层粉末', 3.7500, 0, 'KG' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'MAT-001', '高温合金叶片毛坯', 62.5000, 0, 'KG' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-002'
UNION ALL
SELECT wo.id, 'MAT-004', '钛合金板材', 160.0000, 0, 'KG' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-003'
ON DUPLICATE KEY UPDATE
  material_name=VALUES(material_name), required_qty=VALUES(required_qty),
  issued_qty=VALUES(issued_qty), qty_unit=VALUES(qty_unit);

-- 工单输出物料（子表）
DELETE FROM mes_work_order_output_material
 WHERE work_order_id IN (SELECT id FROM mes_work_order WHERE work_order_no IN ('WO-2026-001','WO-2026-002','WO-2026-003'));
INSERT INTO mes_work_order_output_material (work_order_id, material_code, material_name, output_qty, qty_unit)
SELECT wo.id, 'MAT-007', '涡轮叶片成品', 0, 'PC' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'MAT-007', '涡轮叶片成品', 0, 'PC' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-002'
UNION ALL
SELECT wo.id, 'MAT-008', '压气机盘成品', 0, 'PC' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-003'
ON DUPLICATE KEY UPDATE
  material_name=VALUES(material_name), output_qty=VALUES(output_qty),
  qty_unit=VALUES(qty_unit);

-- 工单检验项目（子表）
DELETE FROM mes_work_order_quality_item
 WHERE work_order_id IN (SELECT id FROM mes_work_order WHERE work_order_no IN ('WO-2026-001'));
INSERT INTO mes_work_order_quality_item (work_order_id, quality_item_code, quality_item_name, requirement, status)
SELECT wo.id, 'QI-DIM-001', '尺寸检验',   '关键尺寸公差±0.02mm', 'PENDING' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'QI-COAT-001', '涂层厚度检验', '涂层厚度 0.15~0.25mm', 'PENDING' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'QI-NDT-001', '无损探伤',   'X射线探伤无裂纹', 'PENDING' FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
ON DUPLICATE KEY UPDATE
  quality_item_name=VALUES(quality_item_name), requirement=VALUES(requirement),
  status=VALUES(status);

-- ==================== 7. 生产派工 ====================

-- 派工任务（无业务唯一索引，按 order_no+process_no 清理重跑）
DELETE FROM mes_dispatch_task
 WHERE order_no IN ('OP-2026-001','OP-2026-002') AND process_no IN ('WK-010','WK-030','WK-040');
INSERT INTO mes_dispatch_task (work_order_id, order_no, process_no, work_name, serial_no, project_name, plan_qty, qty_unit, dispatch_status, plan_start_time, plan_end_time, created_time)
SELECT wo.id, 'OP-2026-001', 'WK-030', '数控精加工-涡轮叶片', 'SN-WO001-030', 'GT-F5燃机项目', 25.0000, 'PC', 'ASSIGNED', '2026-03-20 08:00:00', '2026-03-28 17:00:00', @now
FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'OP-2026-001', 'WK-040', '热处理-涡轮叶片', 'SN-WO001-040', 'GT-F5燃机项目', 25.0000, 'PC', 'UNASSIGNED', '2026-03-29 08:00:00', '2026-04-05 17:00:00', @now
FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
UNION ALL
SELECT wo.id, 'OP-2026-002', 'WK-010', '锻造-压气机盘', 'SN-WO003-010', 'GT-H25燃机项目', 20.0000, 'PC', 'UNASSIGNED', '2026-03-10 08:00:00', '2026-03-25 17:00:00', @now
FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-003'
ON DUPLICATE KEY UPDATE
  work_name=VALUES(work_name), serial_no=VALUES(serial_no),
  project_name=VALUES(project_name), plan_qty=VALUES(plan_qty),
  qty_unit=VALUES(qty_unit), dispatch_status=VALUES(dispatch_status),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  created_time=VALUES(created_time);

-- 派工分配（子表：按 dispatch_task_id 清理）
DELETE FROM mes_dispatch_assignment
 WHERE dispatch_task_id IN (SELECT id FROM mes_dispatch_task WHERE process_no='WK-030' AND dispatch_status='ASSIGNED');
INSERT INTO mes_dispatch_assignment (dispatch_task_id, assign_type, assignee_code, assignee_name, assigned_qty, qty_unit, status, assigned_by, assigned_time)
SELECT dt.id, 'TEAM', 'TM-MACH-A', '加工A班', 25.0000, 'PC', 'ACTIVE', 'admin', @now
FROM mes_dispatch_task dt WHERE dt.process_no='WK-030' AND dt.dispatch_status='ASSIGNED' LIMIT 1
ON DUPLICATE KEY UPDATE
  assign_type=VALUES(assign_type), assignee_code=VALUES(assignee_code),
  assignee_name=VALUES(assignee_name), assigned_qty=VALUES(assigned_qty),
  qty_unit=VALUES(qty_unit), status=VALUES(status),
  assigned_by=VALUES(assigned_by), assigned_time=VALUES(assigned_time);

-- ==================== 8. 异常管理 ====================

INSERT INTO mes_abnormal_contact (contact_no, subject, occur_stage, event_category, product_division, order_no, customer_project, initiate_dept, product_model, product_type, product_name, initiate_process, qty, storage_location, discovery_date, abnormal_desc, status, affect_schedule, publish_time, created_by, created_time) VALUES
('AC-2026-001', '涡轮叶片铸造缩孔缺陷',     '生产过程', '质量异常', '涡轮部件', 'OP-2026-001', 'GT-F5燃机项目/客户A', '涡轮事业部', 'GT-F5',  '叶片', '涡轮叶片成品', '精密铸造', 3.0000, '铸造车间暂存区A-01', '2026-03-10', '精密铸造过程中发现3件叶片存在缩孔缺陷，缩孔位于叶身中部，深度约0.5mm，超出标准允许范围。初步分析为浇注温度控制不当所致。', 'PROCESSING', 1, '2026-03-10 14:30:00', 'zhangsan', @now),
('AC-2026-002', '原材料批号追溯异常',         '来料检验', '物料异常', '基础材料', 'OP-2026-002', 'GT-H25燃机项目/客户B', '质量部', 'GT-H25', '盘件', '压气机盘成品', '来料检验', 1.0000, '原材料仓库B-03', '2026-03-12', '钛合金板材来料检验时发现一批次材料证书中的化学成分与实测值存在偏差，Mo含量偏高0.3%，需进一步确认是否可用。', 'SUBMITTED', 0, '2026-03-12 10:00:00', 'lisi', @now),
('AC-2026-003', '数控加工刀具异常磨损',       '生产过程', '设备异常', '涡轮部件', 'OP-2026-001', 'GT-F5燃机项目/客户A', '涡轮事业部', 'GT-F5',  '叶片', '涡轮叶片成品', '数控精加工', 0, NULL, '2026-03-15', '数控精加工过程中发现CBN刀具异常磨损，加工第8件后刀具后刀面磨损量达到0.3mm，远超正常寿命的50%。可能影响后续加工表面质量。', 'DRAFT', 0, NULL, 'zhangsan', @now)
ON DUPLICATE KEY UPDATE
  subject=VALUES(subject), occur_stage=VALUES(occur_stage),
  event_category=VALUES(event_category), product_division=VALUES(product_division),
  order_no=VALUES(order_no), customer_project=VALUES(customer_project),
  initiate_dept=VALUES(initiate_dept), product_model=VALUES(product_model),
  product_type=VALUES(product_type), product_name=VALUES(product_name),
  initiate_process=VALUES(initiate_process), qty=VALUES(qty),
  storage_location=VALUES(storage_location), discovery_date=VALUES(discovery_date),
  abnormal_desc=VALUES(abnormal_desc), status=VALUES(status),
  affect_schedule=VALUES(affect_schedule), publish_time=VALUES(publish_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- ==================== 9. 成品质量 ====================

-- 复检申请（无业务唯一索引，按业务键清理后再插入）
DELETE FROM mes_recheck_request
 WHERE production_order_no='WO-2026-001' AND material_code='MAT-007';
INSERT INTO mes_recheck_request (project_code, project_name, material_code, material_name, production_order_no, recheck_requirement, recheck_reason, recheck_proposer, recheck_propose_time, required_delivery_time, status, created_by, created_time) VALUES
('PRJ-GT-F5-001', 'GT-F5燃机项目', 'MAT-007', '涡轮叶片成品', 'WO-2026-001', '对缩孔修复后叶片进行尺寸及探伤复检', '铸造缩孔修复后需重新检验确认质量', '孙质检', '2026-03-15 09:00:00', '2026-03-20 17:00:00', 'CREATED', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  project_code=VALUES(project_code), project_name=VALUES(project_name),
  material_name=VALUES(material_name),
  recheck_requirement=VALUES(recheck_requirement), recheck_reason=VALUES(recheck_reason),
  recheck_proposer=VALUES(recheck_proposer), recheck_propose_time=VALUES(recheck_propose_time),
  required_delivery_time=VALUES(required_delivery_time), status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 生产工作开工检查（无业务唯一索引：清理重跑）
DELETE FROM mes_work_start_check WHERE work_order_no IN ('WO-2026-001','WO-2026-003');
INSERT INTO mes_work_start_check (work_no, work_order_no, check_item, check_result, check_status, checker, check_time, created_by, created_time)
VALUES
('WK-010', 'WO-2026-001', '设备预热状态',   '铸造炉预热温度达标1200℃', 'PASSED', '张三', '2026-03-01 08:30:00', 'zhangsan', @now),
('WK-010', 'WO-2026-001', '模具完好性检查', '蜡模完好，无变形',       'PASSED', '张三', '2026-03-01 08:35:00', 'zhangsan', @now),
('WK-020', 'WO-2026-001', '机床精度校验',   '五轴定位精度±0.005mm',   'PASSED', '张三', '2026-03-10 08:15:00', 'zhangsan', @now),
('WK-030', 'WO-2026-001', '刀具安装检查',   '刀具跳动量0.002mm',      'PASSED', '张三', '2026-03-20 08:10:00', 'zhangsan', @now),
('WK-010', 'WO-2026-003', '锻造设备检查',   '液压系统压力正常',       'PASSED', '李四', '2026-03-10 08:20:00', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  check_result=VALUES(check_result), check_status=VALUES(check_status),
  checker=VALUES(checker), check_time=VALUES(check_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 生产工单开工检查（无业务唯一索引：清理重跑）
DELETE FROM mes_order_start_check WHERE work_order_no IN ('WO-2026-001','WO-2026-003');
INSERT INTO mes_order_start_check (work_order_no, check_item, check_result, check_status, checker, check_time, created_by, created_time)
VALUES
('WO-2026-001', 'BOM物料齐套检查', '所需物料全部齐套', 'PASSED', '张三', '2026-03-01 07:50:00', 'zhangsan', @now),
('WO-2026-001', '工艺文件确认',   '工艺卡及图纸已签署', 'PASSED', '张三', '2026-03-01 07:55:00', 'zhangsan', @now),
('WO-2026-003', 'BOM物料齐套检查', '钛合金板材已到位',   'PASSED', '李四', '2026-03-10 08:00:00', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  check_result=VALUES(check_result), check_status=VALUES(check_status),
  checker=VALUES(checker), check_time=VALUES(check_time),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 交班记录（无业务唯一索引：按 product_serial_no 清理）
DELETE FROM mes_shift_handover WHERE product_serial_no IN ('SN-WO001-CAST','SN-WO001-MACH','SN-WO003-FORGE');
INSERT INTO mes_shift_handover (project_name, product_serial_no, process_content, handover_date, handover_weekday, handover_time, handover_team_name, handover_shift, takeover_shift, takeover_team_name, handover_person, takeover_person, team_leader, plan_qty, actual_qty, gap_analysis, handover_content, status, created_by, created_time) VALUES
('GT-F5燃机项目', 'SN-WO001-CAST', '精密铸造-涡轮叶片', '2026-03-05', 4, '17:30:00', '铸造A班', '白班', '夜班', '铸造B班', '陈铸造', '周夜班', '张班长', 8.0000, 7.0000, '第8件浇注温度波动导致废品1件，已调整工艺参数', '今日完成7件铸造，第8件缩孔报废。浇注系统已清理，模具状态良好，夜班可继续生产。', 'RECEIVED', 'zhangsan', @now),
('GT-F5燃机项目', 'SN-WO001-MACH', '数控粗加工-涡轮叶片', '2026-03-12', 4, '17:30:00', '加工A班', '白班', '夜班', '铸造B班', '刘加工', '周夜班', '张班长', 10.0000, 10.0000, NULL, '今日完成10件粗加工，尺寸全部合格。刀具剩余寿命约60%，可继续使用。设备运行正常。', 'RECEIVED', 'zhangsan', @now),
('GT-H25燃机项目', 'SN-WO003-FORGE', '锻造-压气机盘', '2026-03-15', 7, '17:30:00', '铸造A班', '白班', '夜班', '铸造B班', '陈铸造', '周夜班', '张班长', 5.0000, 5.0000, NULL, '今日完成5件锻造，锻件外观及尺寸合格。锻造模具磨损正常，液压系统正常。', 'PENDING', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  project_name=VALUES(project_name), process_content=VALUES(process_content),
  handover_date=VALUES(handover_date), handover_weekday=VALUES(handover_weekday),
  handover_time=VALUES(handover_time), handover_team_name=VALUES(handover_team_name),
  handover_shift=VALUES(handover_shift), takeover_shift=VALUES(takeover_shift),
  takeover_team_name=VALUES(takeover_team_name),
  handover_person=VALUES(handover_person), takeover_person=VALUES(takeover_person),
  team_leader=VALUES(team_leader), plan_qty=VALUES(plan_qty), actual_qty=VALUES(actual_qty),
  gap_analysis=VALUES(gap_analysis), handover_content=VALUES(handover_content),
  status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- ==================== 10. 物料管理 ====================

-- 存储地点库存
INSERT INTO mes_storage_inventory (factory, inventory_org, warehouse, storage_location, material_code, material_name, unrestricted_stock, quality_stock, frozen_stock, unit, created_by, created_time) VALUES
('上海工厂', '上海库存组织', '原材料仓库',     'SL-RM-A01', 'MAT-001', '高温合金叶片毛坯', 500.0000,  20.0000, 0, 'KG', 'admin', @now),
('上海工厂', '上海库存组织', '原材料仓库',     'SL-RM-A02', 'MAT-002', '镍基合金棒材',     300.0000,  0,       0, 'KG', 'admin', @now),
('上海工厂', '上海库存组织', '辅料仓库',       'SL-AX-B01', 'MAT-003', '陶瓷涂层粉末',     50.0000,   5.0000,  0, 'KG', 'admin', @now),
('苏州工厂', '苏州库存组织', '原材料仓库',     'SL-RM-C01', 'MAT-004', '钛合金板材',       200.0000,  10.0000, 0, 'KG', 'admin', @now),
('苏州工厂', '苏州库存组织', '半成品仓库',     'SL-WIP-D01', 'MAT-005', '不锈钢管件',       80.0000,   0,       0, 'PC', 'admin', @now),
('上海工厂', '上海库存组织', '标准件仓库',     'SL-STD-E01', 'MAT-006', '密封垫圈',         5000.0000, 0,       0, 'PC', 'admin', @now),
('上海工厂', '上海库存组织', '成品仓库',       'SL-FG-F01', 'MAT-007', '涡轮叶片成品',     0,         0,       0, 'PC', 'admin', @now),
('苏州工厂', '苏州库存组织', '成品仓库',       'SL-FG-G01', 'MAT-008', '压气机盘成品',     0,         0,       0, 'PC', 'admin', @now)
ON DUPLICATE KEY UPDATE
  factory=VALUES(factory), inventory_org=VALUES(inventory_org),
  warehouse=VALUES(warehouse), material_name=VALUES(material_name),
  unrestricted_stock=VALUES(unrestricted_stock), quality_stock=VALUES(quality_stock),
  frozen_stock=VALUES(frozen_stock), unit=VALUES(unit),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 生产领料
INSERT INTO mes_material_requisition (requisition_no, work_order_id, work_order_no, product_code, product_name, plan_qty, qty_unit, main_org, plan_start_time, plan_end_time, project_name, wbs_element, status, created_by, created_time)
SELECT 'MR-2026-001', wo.id, 'WO-2026-001', 'MAT-007', '涡轮叶片成品', 25.0000, 'PC', '涡轮事业部', '2026-03-01 08:00:00', '2026-04-15 17:00:00', 'GT-F5燃机项目', 'WBS-F5-001', 'CREATED', 'zhangsan', @now
FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-001'
ON DUPLICATE KEY UPDATE
  work_order_id=VALUES(work_order_id), work_order_no=VALUES(work_order_no),
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  plan_qty=VALUES(plan_qty), qty_unit=VALUES(qty_unit),
  main_org=VALUES(main_org),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  project_name=VALUES(project_name), wbs_element=VALUES(wbs_element),
  status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);
INSERT INTO mes_material_requisition (requisition_no, work_order_id, work_order_no, product_code, product_name, plan_qty, qty_unit, main_org, plan_start_time, plan_end_time, project_name, wbs_element, status, created_by, created_time)
SELECT 'MR-2026-002', wo.id, 'WO-2026-003', 'MAT-008', '压气机盘成品', 20.0000, 'PC', '结构事业部', '2026-03-10 08:00:00', '2026-07-31 17:00:00', 'GT-H25燃机项目', 'WBS-H25-001', 'CREATED', 'lisi', @now
FROM mes_work_order wo WHERE wo.work_order_no='WO-2026-003'
ON DUPLICATE KEY UPDATE
  work_order_id=VALUES(work_order_id), work_order_no=VALUES(work_order_no),
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  plan_qty=VALUES(plan_qty), qty_unit=VALUES(qty_unit),
  main_org=VALUES(main_org),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  project_name=VALUES(project_name), wbs_element=VALUES(wbs_element),
  status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 领料明细（子表：按 requisition_id 清理）
DELETE FROM mes_material_requisition_item
 WHERE requisition_id IN (SELECT id FROM mes_material_requisition WHERE requisition_no IN ('MR-2026-001','MR-2026-002'));
INSERT INTO mes_material_requisition_item (requisition_id, material_code, material_name, demand_qty, pending_qty, issue_qty, unit, issue_location, description, created_time)
SELECT mr.id, 'MAT-001', '高温合金叶片毛坯', 62.5000, 62.5000, 0, 'KG', 'SL-RM-A01', '首批25件涡轮叶片毛坯用料', @now
FROM mes_material_requisition mr WHERE mr.requisition_no='MR-2026-001'
UNION ALL
SELECT mr.id, 'MAT-003', '陶瓷涂层粉末', 3.7500, 3.7500, 0, 'KG', 'SL-AX-B01', '涂层喷涂用料', @now
FROM mes_material_requisition mr WHERE mr.requisition_no='MR-2026-001'
UNION ALL
SELECT mr.id, 'MAT-004', '钛合金板材', 160.0000, 160.0000, 0, 'KG', 'SL-RM-C01', '压气机盘用料', @now
FROM mes_material_requisition mr WHERE mr.requisition_no='MR-2026-002'
ON DUPLICATE KEY UPDATE
  material_name=VALUES(material_name), demand_qty=VALUES(demand_qty),
  pending_qty=VALUES(pending_qty), issue_qty=VALUES(issue_qty),
  unit=VALUES(unit), issue_location=VALUES(issue_location),
  description=VALUES(description), created_time=VALUES(created_time);

-- 领料单管理
INSERT INTO mes_requisition_order (delivery_request_no, line_no, work_order_no, material_code, material_name, requisition_qty, status, delivery_warehouse, delivery_location, created_by, created_time) VALUES
('DR-2026-001', '10', 'WO-2026-001', 'MAT-001', '高温合金叶片毛坯', 62.5000, 'PENDING', '原材料仓库', 'SL-RM-A01', 'zhangsan', @now),
('DR-2026-001', '20', 'WO-2026-001', 'MAT-003', '陶瓷涂层粉末',     3.7500,  'PENDING', '辅料仓库',   'SL-AX-B01', 'zhangsan', @now),
('DR-2026-002', '10', 'WO-2026-003', 'MAT-004', '钛合金板材',       160.0000, 'PENDING', '原材料仓库', 'SL-RM-C01', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  work_order_no=VALUES(work_order_no), material_code=VALUES(material_code),
  material_name=VALUES(material_name), requisition_qty=VALUES(requisition_qty),
  status=VALUES(status), delivery_warehouse=VALUES(delivery_warehouse),
  delivery_location=VALUES(delivery_location),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 完工入库申请
INSERT INTO mes_finished_goods_receipt_request (request_no, receipt_type, work_order_no, project_name, wbs_element, material_code, material_name, qty, unit, description, status, created_by, created_time) VALUES
('RR-2026-001', '新制品', 'WO-2026-001', 'GT-F5燃机项目', 'WBS-F5-001', 'MAT-007', '涡轮叶片成品', 25.0000, 'PC', '首批涡轮叶片完工入库申请', 'CREATED', 'zhangsan', @now)
ON DUPLICATE KEY UPDATE
  receipt_type=VALUES(receipt_type), work_order_no=VALUES(work_order_no),
  project_name=VALUES(project_name), wbs_element=VALUES(wbs_element),
  material_code=VALUES(material_code), material_name=VALUES(material_name),
  qty=VALUES(qty), unit=VALUES(unit),
  description=VALUES(description), status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 完工入库
INSERT INTO mes_finished_goods_receipt (receipt_no, receipt_type, warehouse, movement_type, plan_receipt_time, status, created_by, created_time) VALUES
('GR-2026-001', '新制品', '成品仓库', '101-成品入库', '2026-04-20 09:00:00', 'CREATED', 'admin', @now)
ON DUPLICATE KEY UPDATE
  receipt_type=VALUES(receipt_type), warehouse=VALUES(warehouse),
  movement_type=VALUES(movement_type), plan_receipt_time=VALUES(plan_receipt_time),
  status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 完工入库明细（子表）
DELETE FROM mes_finished_goods_receipt_item
 WHERE receipt_id IN (SELECT id FROM mes_finished_goods_receipt WHERE receipt_no='GR-2026-001');
INSERT INTO mes_finished_goods_receipt_item (receipt_id, item_code, work_order_no, material_code, material_name, receipt_qty, unit, storage_location, stock_status, wbs_element)
SELECT r.id, 'GRI-001', 'WO-2026-001', 'MAT-007', '涡轮叶片成品', 25.0000, 'PC', 'SL-FG-F01', '非限制', 'WBS-F5-001'
FROM mes_finished_goods_receipt r WHERE r.receipt_no='GR-2026-001'
ON DUPLICATE KEY UPDATE
  work_order_no=VALUES(work_order_no), material_code=VALUES(material_code),
  material_name=VALUES(material_name), receipt_qty=VALUES(receipt_qty),
  unit=VALUES(unit), storage_location=VALUES(storage_location),
  stock_status=VALUES(stock_status), wbs_element=VALUES(wbs_element);

-- 生产退料
INSERT INTO mes_material_return (return_no, work_order_no, order_no, product_code, product_name, project_name, wbs_element, new_or_repair_type, work_type, machine_model, product_category, product_type, plan_qty, factory_org, plan_org, main_org, status, created_by, created_time) VALUES
('RT-2026-001', 'WO-2026-001', 'OP-2026-001', 'MAT-007', '涡轮叶片成品', 'GT-F5燃机项目', 'WBS-F5-001', '新制', '主机', 'GT-F5', '涡轮部件', '叶片', 3.0000, '上海工厂', '上海计划部', '涡轮事业部', 'CREATED', 'zhangsan', @now)
ON DUPLICATE KEY UPDATE
  work_order_no=VALUES(work_order_no), order_no=VALUES(order_no),
  product_code=VALUES(product_code), product_name=VALUES(product_name),
  project_name=VALUES(project_name), wbs_element=VALUES(wbs_element),
  new_or_repair_type=VALUES(new_or_repair_type), work_type=VALUES(work_type),
  machine_model=VALUES(machine_model), product_category=VALUES(product_category),
  product_type=VALUES(product_type), plan_qty=VALUES(plan_qty),
  factory_org=VALUES(factory_org), plan_org=VALUES(plan_org),
  main_org=VALUES(main_org), status=VALUES(status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 发货签收（无业务唯一索引：按工单+行号清理）
DELETE FROM mes_delivery_sign WHERE work_order_no='WO-2026-001' AND line_no IN ('10','20');
INSERT INTO mes_delivery_sign (line_no, work_order_no, material_code, material_name, plan_delivery_qty, pending_sign_qty, unit, delivery_warehouse, delivery_location, order_creator, order_create_time, created_time) VALUES
('10', 'WO-2026-001', 'MAT-001', '高温合金叶片毛坯', 62.5000, 62.5000, 'KG', '原材料仓库', 'SL-RM-A01', '张三', '2026-03-01 10:00:00', @now),
('20', 'WO-2026-001', 'MAT-003', '陶瓷涂层粉末',     3.7500,  3.7500,  'KG', '辅料仓库',   'SL-AX-B01', '张三', '2026-03-01 10:00:00', @now)
ON DUPLICATE KEY UPDATE
  material_code=VALUES(material_code), material_name=VALUES(material_name),
  plan_delivery_qty=VALUES(plan_delivery_qty), pending_sign_qty=VALUES(pending_sign_qty),
  unit=VALUES(unit), delivery_warehouse=VALUES(delivery_warehouse),
  delivery_location=VALUES(delivery_location), order_creator=VALUES(order_creator),
  order_create_time=VALUES(order_create_time), created_time=VALUES(created_time);

-- ==================== 11. 工作查询 ====================

-- 生产工作
INSERT INTO mes_production_work (work_no, work_name, work_order_no, product_material, production_factory, production_org, plan_start_time, plan_end_time, actual_start_time, actual_end_time, actual_process_time, is_report_point, is_check_point, created_by, created_time) VALUES
('PW-001', '精密铸造-涡轮叶片',   'WO-2026-001', '高温合金叶片毛坯', '上海工厂', '涡轮事业部', '2026-03-01 08:00:00', '2026-03-08 17:00:00', '2026-03-01 08:15:00', '2026-03-08 16:30:00', 3600.00, 1, 0, 'zhangsan', @now),
('PW-002', '数控粗加工-涡轮叶片', 'WO-2026-001', '高温合金叶片毛坯', '上海工厂', '涡轮事业部', '2026-03-09 08:00:00', '2026-03-18 17:00:00', '2026-03-09 08:10:00', '2026-03-18 16:00:00', 2880.00, 1, 0, 'zhangsan', @now),
('PW-003', '数控精加工-涡轮叶片', 'WO-2026-001', '高温合金叶片毛坯', '上海工厂', '涡轮事业部', '2026-03-20 08:00:00', '2026-03-28 17:00:00', '2026-03-20 08:05:00', NULL, NULL, 0, 0, 'zhangsan', @now),
('PW-004', '锻造-压气机盘',       'WO-2026-003', '钛合金板材',       '苏州工厂', '结构事业部', '2026-03-10 08:00:00', '2026-03-25 17:00:00', '2026-03-10 08:20:00', NULL, NULL, 1, 0, 'lisi', @now)
ON DUPLICATE KEY UPDATE
  work_name=VALUES(work_name), work_order_no=VALUES(work_order_no),
  product_material=VALUES(product_material),
  production_factory=VALUES(production_factory), production_org=VALUES(production_org),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  actual_start_time=VALUES(actual_start_time), actual_end_time=VALUES(actual_end_time),
  actual_process_time=VALUES(actual_process_time),
  is_report_point=VALUES(is_report_point), is_check_point=VALUES(is_check_point),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 检验工作
INSERT INTO mes_inspection_work (work_no, work_name, plan_inspect_qty, inspected_qty, qualified_qty, unqualified_qty, judgment, is_check_point, work_status, inspect_type, inspect_category, qc_org, inspect_factory, work_order_no, order_status, created_by, created_time) VALUES
('IW-001', '涡轮叶片铸造后检验', 25.0000, 25.0000, 22.0000, 3.0000, '合格', 1, 'COMPLETED', '过程检验', '外观+尺寸', '质量部', '上海工厂', 'WO-2026-001', 'IN_PROGRESS', 'lisi', @now),
('IW-002', '涡轮叶片粗加工检验', 25.0000, 25.0000, 25.0000, 0,      '合格', 1, 'COMPLETED', '过程检验', '尺寸',      '质量部', '上海工厂', 'WO-2026-001', 'IN_PROGRESS', 'lisi', @now),
('IW-003', '涡轮叶片精加工检验', 25.0000, 0,       0,       0,      NULL,   1, 'CREATED',   '过程检验', '尺寸+表面', '质量部', '上海工厂', 'WO-2026-001', 'IN_PROGRESS', 'lisi', @now)
ON DUPLICATE KEY UPDATE
  work_name=VALUES(work_name), plan_inspect_qty=VALUES(plan_inspect_qty),
  inspected_qty=VALUES(inspected_qty), qualified_qty=VALUES(qualified_qty),
  unqualified_qty=VALUES(unqualified_qty), judgment=VALUES(judgment),
  is_check_point=VALUES(is_check_point), work_status=VALUES(work_status),
  inspect_type=VALUES(inspect_type), inspect_category=VALUES(inspect_category),
  qc_org=VALUES(qc_org), inspect_factory=VALUES(inspect_factory),
  work_order_no=VALUES(work_order_no), order_status=VALUES(order_status),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 工作状态查看
INSERT INTO mes_work_status_view (work_no, sequence_no, process_no, work_name, is_output, status, description, factory, business_org, plan_work_center_name, plan_team_name, plan_start_time, plan_end_time, actual_start_time, actual_end_time, created_time) VALUES
('WSV-001', 1, 'WK-010', '精密铸造',     0, 'COMPLETED',   '已完成', '上海工厂', '涡轮事业部', '精密铸造中心', '铸造A班', '2026-03-01 08:00:00', '2026-03-08 17:00:00', '2026-03-01 08:15:00', '2026-03-08 16:30:00', @now),
('WSV-002', 2, 'WK-020', '数控粗加工',   0, 'COMPLETED',   '已完成', '上海工厂', '涡轮事业部', '数控加工中心', '加工A班', '2026-03-09 08:00:00', '2026-03-18 17:00:00', '2026-03-09 08:10:00', '2026-03-18 16:00:00', @now),
('WSV-003', 3, 'WK-030', '数控精加工',   0, 'IN_PROGRESS', '进行中', '上海工厂', '涡轮事业部', '数控加工中心', '加工A班', '2026-03-20 08:00:00', '2026-03-28 17:00:00', '2026-03-20 08:05:00', NULL, @now),
('WSV-004', 4, 'WK-040', '热处理',       0, 'CREATED',     '待排产', '上海工厂', '涡轮事业部', '热处理中心',   NULL,      '2026-03-29 08:00:00', '2026-04-05 17:00:00', NULL, NULL, @now),
('WSV-005', 5, 'WK-050', '热障涂层喷涂', 0, 'CREATED',     '待排产', '上海工厂', '涡轮事业部', '热喷涂中心',   '喷涂班',  '2026-04-06 08:00:00', '2026-04-10 17:00:00', NULL, NULL, @now),
('WSV-006', 6, 'WK-060', '成品检验',     1, 'CREATED',     '待排产', '上海工厂', '质量部',     '质量检验中心', '质检班',  '2026-04-11 08:00:00', '2026-04-15 17:00:00', NULL, NULL, @now)
ON DUPLICATE KEY UPDATE
  sequence_no=VALUES(sequence_no), process_no=VALUES(process_no),
  work_name=VALUES(work_name), is_output=VALUES(is_output),
  status=VALUES(status), description=VALUES(description),
  factory=VALUES(factory), business_org=VALUES(business_org),
  plan_work_center_name=VALUES(plan_work_center_name),
  plan_team_name=VALUES(plan_team_name),
  plan_start_time=VALUES(plan_start_time), plan_end_time=VALUES(plan_end_time),
  actual_start_time=VALUES(actual_start_time), actual_end_time=VALUES(actual_end_time),
  created_time=VALUES(created_time);

-- ==================== 12. APS 集成 ====================

-- 数据映射（无业务唯一索引：按 mapping_type+mes_code 清理）
DELETE FROM mes_aps_data_mapping
 WHERE (mapping_type, mes_code) IN (
   ('MATERIAL','MAT-001'),('MATERIAL','MAT-004'),('MATERIAL','MAT-007'),
   ('WORK_CENTER','WC-CAST'),('WORK_CENTER','WC-MACH'),('WORK_CENTER','WC-SPRAY'),
   ('STATUS','CREATED'),('STATUS','RELEASED'),('STATUS','IN_PROGRESS'),('STATUS','COMPLETED'),
   ('FACTORY','F001'),('FACTORY','F002')
 );
INSERT INTO mes_aps_data_mapping (mapping_type, mes_code, mes_name, aps_code, aps_name, enabled, created_by, created_time) VALUES
('MATERIAL',    'MAT-001', '高温合金叶片毛坯', 'APS-M-001', 'HT Alloy Blade Blank',     1, 'admin', @now),
('MATERIAL',    'MAT-004', '钛合金板材',       'APS-M-004', 'Titanium Alloy Sheet',      1, 'admin', @now),
('MATERIAL',    'MAT-007', '涡轮叶片成品',     'APS-M-007', 'Turbine Blade Finished',    1, 'admin', @now),
('WORK_CENTER', 'WC-CAST', '精密铸造中心',     'APS-WC-01', 'Precision Casting Center',  1, 'admin', @now),
('WORK_CENTER', 'WC-MACH', '数控加工中心',     'APS-WC-02', 'CNC Machining Center',      1, 'admin', @now),
('WORK_CENTER', 'WC-SPRAY','热喷涂中心',       'APS-WC-03', 'Thermal Spray Center',      1, 'admin', @now),
('STATUS',      'CREATED',     '已创建',       'APS-S-NEW',     'New',          1, 'admin', @now),
('STATUS',      'RELEASED',    '已下达',       'APS-S-RELEASED', 'Released',    1, 'admin', @now),
('STATUS',      'IN_PROGRESS', '进行中',       'APS-S-WIP',     'In Progress', 1, 'admin', @now),
('STATUS',      'COMPLETED',   '已完成',       'APS-S-DONE',    'Completed',   1, 'admin', @now),
('FACTORY',     'F001',    '上海工厂',         'APS-F-SH',  'Shanghai Factory',  1, 'admin', @now),
('FACTORY',     'F002',    '苏州工厂',         'APS-F-SZ',  'Suzhou Factory',    1, 'admin', @now)
ON DUPLICATE KEY UPDATE
  mes_name=VALUES(mes_name), aps_code=VALUES(aps_code),
  aps_name=VALUES(aps_name), enabled=VALUES(enabled),
  created_by=VALUES(created_by), created_time=VALUES(created_time);

-- 同步日志
INSERT INTO mes_aps_sync_log (batch_id, sync_direction, sync_type, total_count, success_count, fail_count, status, start_time, end_time, duration_ms, created_time) VALUES
('BATCH-20260301-001', 'DOWNSTREAM', 'ORDER',     4, 4, 0, 'SUCCESS', '2026-03-01 06:00:00', '2026-03-01 06:00:03', 3200, @now),
('BATCH-20260301-002', 'DOWNSTREAM', 'WORKORDER', 4, 3, 1, 'PARTIAL', '2026-03-01 06:01:00', '2026-03-01 06:01:05', 5100, @now),
('BATCH-20260305-001', 'UPSTREAM',   'INVENTORY', 8, 8, 0, 'SUCCESS', '2026-03-05 18:00:00', '2026-03-05 18:00:02', 2400, @now)
ON DUPLICATE KEY UPDATE
  sync_direction=VALUES(sync_direction), sync_type=VALUES(sync_type),
  total_count=VALUES(total_count), success_count=VALUES(success_count),
  fail_count=VALUES(fail_count), status=VALUES(status),
  start_time=VALUES(start_time), end_time=VALUES(end_time),
  duration_ms=VALUES(duration_ms), created_time=VALUES(created_time);

SELECT 'Seed data loaded successfully!' AS result;
