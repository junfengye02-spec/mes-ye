package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指导书实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_instruction")
public class WorkInstruction extends BaseEntity {

    /** 指导书编号 */
    private String instructionCode;

    /** 等级 */
    private String level;

    /** 状态 */
    private String status;
}
