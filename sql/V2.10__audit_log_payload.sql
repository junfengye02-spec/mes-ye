-- ============================================================
-- V2.10  审计日志大 payload 分片存储（P3-12）
--   - sys_audit_log_payload ：超过 @AuditLog.payloadMaxSize 的响应/请求体
--     会被切片成 LONGTEXT 片段，按 seq 顺序存入本表，
--     主表 sys_audit_log.payload_json 只保留首 N 字节 + payloadRef=audit_log_id 指针，
--     以便大导出 / 批量上传 / 报表场景可完整溯源。
--
-- 使用方式：
--   1) @AuditLog(payloadMaxSize = 64 * 1024) 可按接口差异化提升阈值；
--   2) 未显式配置时走全局 mes.audit.max-payload-bytes（默认 10KB）；
--   3) 超过阈值时切面调用 AuditLogService.savePayloadChunks() 写入本表。
--
-- 备注：
--   - payload_type 目前只有 'RESPONSE' / 'REQUEST' / 'EXCEPTION'；
--   - chunk_seq 从 0 开始递增；
--   - content_sha256 用于后续抽样去重与完整性校验；
--   - storage_backend='DB' 表示本表直存，'MINIO' 表示 object_key 指向 MinIO 对象（留作扩展）。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_audit_log_payload (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  audit_log_id BIGINT NOT NULL COMMENT '关联 sys_audit_log.id',
  tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '冗余租户隔离字段，便于 per-tenant 归档',
  payload_type VARCHAR(16) NOT NULL DEFAULT 'RESPONSE' COMMENT 'RESPONSE / REQUEST / EXCEPTION',
  chunk_seq INT NOT NULL DEFAULT 0 COMMENT '分片序号，从 0 开始递增',
  chunk_total INT NOT NULL DEFAULT 1 COMMENT '总分片数',
  content_length INT NOT NULL DEFAULT 0 COMMENT '本片字节数',
  total_length BIGINT NOT NULL DEFAULT 0 COMMENT '原始 payload 总字节数',
  content_sha256 CHAR(64) DEFAULT NULL COMMENT '原始 payload 的 SHA-256，用于完整性校验 / 抽样去重',
  storage_backend VARCHAR(16) NOT NULL DEFAULT 'DB' COMMENT 'DB / MINIO',
  object_key VARCHAR(512) DEFAULT NULL COMMENT 'storage_backend=MINIO 时的对象键',
  content_chunk LONGTEXT COMMENT '分片原文（storage_backend=DB 时有效）',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_aludp_audit (audit_log_id, chunk_seq),
  INDEX idx_aludp_tenant_time (tenant_id, created_time),
  INDEX idx_aludp_backend (storage_backend, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志大 payload 分片表（P3-12）';
