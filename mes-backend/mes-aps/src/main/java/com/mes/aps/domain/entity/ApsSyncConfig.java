package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * APS同步配置表实体
 */
@Data
@TableName("mes_aps_sync_config")
public class ApsSyncConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置键 */
    private String configKey;
    /** 配置值 */
    private String configValue;
    /** 配置说明 */
    private String configDesc;
    /** 是否启用 */
    private Integer enabled;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
}
