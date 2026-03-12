package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机械加工程序表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_machining_program")
public class MachiningProgram extends BaseEntity {

    /** G-code */
    private String gCode;

    /** 程序表 */
    private String programTable;

    /** 产品名称 */
    private String productName;
}
