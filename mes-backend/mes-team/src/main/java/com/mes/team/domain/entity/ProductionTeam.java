package com.mes.team.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产班组实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_team")
public class ProductionTeam extends BaseEntity {

    /** 班组编码 */
    private String teamCode;

    /** 班组名称 */
    private String teamName;

    /** 生产组织ID */
    private Long orgId;

    /** 生产组织编码 */
    private String orgCode;

    /** 生产组织名称 */
    private String orgName;

    /** 是否启用（1=启用, 0=停用） */
    private Integer enabled;

    /** 说明 */
    private String description;
}
