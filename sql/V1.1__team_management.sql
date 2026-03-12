-- ============================================================
-- 模块：班组管理
-- 表数量：1
-- ============================================================

CREATE TABLE IF NOT EXISTS mes_production_team (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  team_code VARCHAR(50) NOT NULL COMMENT '班组编码',
  team_name VARCHAR(200) NOT NULL COMMENT '班组名称',
  org_id BIGINT COMMENT '生产组织ID',
  org_code VARCHAR(50) COMMENT '生产组织编码',
  org_name VARCHAR(200) COMMENT '生产组织名称',
  enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  description VARCHAR(500) COMMENT '说明',
  created_by VARCHAR(50),
  created_time DATETIME,
  updated_by VARCHAR(50),
  updated_time DATETIME,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_team_code (team_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产班组表';
