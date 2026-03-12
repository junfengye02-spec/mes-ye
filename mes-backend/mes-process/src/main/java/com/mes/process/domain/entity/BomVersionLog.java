package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BOM版本日志表实体
 */
@Data
@TableName("mes_bom_version_log")
public class BomVersionLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BOM主表ID */
    private Long bomId;

    /** 原版本 */
    private String fromVersion;

    /** 新版本 */
    private String toVersion;

    /** 动作 */
    private String action;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operatedTime;

    /** 变更摘要 */
    private String changeSummary;
}
