-- ============================================================
-- V1.13: APS 回调配置 + 基础地址更新
-- ============================================================

-- 回调配置
INSERT INTO mes_aps_sync_config (config_key, config_value, config_desc, enabled, created_time) VALUES
('aps.callback.url', 'http://localhost:9000', 'APS排程结果回调基础地址', 1, NOW()),
('aps.callback.enabled', 'true', 'APS回调接收开关', 1, NOW());

-- 更新 APS 基础地址指向网关
UPDATE mes_aps_sync_config SET config_value = 'http://localhost:9000'
WHERE config_key = 'aps.base.url';
