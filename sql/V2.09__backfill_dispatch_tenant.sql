-- ============================================================
-- V2.09  派工任务表 tenant_id 兜底回填（配合 R3 DispatchTask entity 加 tenantId）
--
-- 背景：
--   V1.16 已对 mes_dispatch_task 加了 `tenant_id BIGINT NOT NULL DEFAULT 1`；
--   V2.07 对核心业务表做过一次兜底回填，包含 mes_dispatch_task。
--   本脚本在"DispatchTask 实体补齐 tenantId 字段 + MetaObjectHandler 自动填充"上线之前，
--   再做一次保险回填，并覆盖 V2.07 未涵盖的两个派工关联表：
--     - mes_dispatch_assignment（派工分配）
--     - mes_dispatch_status_log（派工状态日志）
--   目的：避免"先加了 tenant_id 列但历史数据通过 ddl-auto=update 模糊接入 / 或跑半成品
--   脚本后回滚导致残留 NULL 值"的极端情形，确保应用层 fail-closed TenantLine 拦截器
--   能够读到每一行合法 tenant_id。
--
-- 幂等性：
--   WHERE tenant_id IS NULL 在 NOT NULL 列上恒为 false，重复执行 0 行影响；
--   第一次执行时若有 NULL 值则回填为 1（默认租户）。
--
-- 连跑验证：
--   $ mysql ... < V2.09__backfill_dispatch_tenant.sql
--   $ mysql ... < V2.09__backfill_dispatch_tenant.sql
--   两次均应返回 SUCCESS，第二次的 Rows matched: 0 / Changed: 0。
--
-- 依赖：V1.16、V1.17、V1.19 已执行。
-- ============================================================

-- 1) 派工任务（mes_dispatch_task）--------------------------------------
--    与 V2.07 重复写一次完全幂等——重复 UPDATE 0 行命中，无副作用
UPDATE mes_dispatch_task
   SET tenant_id = 1
 WHERE tenant_id IS NULL;

-- 2) 派工分配（mes_dispatch_assignment）--------------------------------
UPDATE mes_dispatch_assignment
   SET tenant_id = 1
 WHERE tenant_id IS NULL;

-- 3) 派工状态日志（mes_dispatch_status_log）----------------------------
UPDATE mes_dispatch_status_log
   SET tenant_id = 1
 WHERE tenant_id IS NULL;
