package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指示书阶段内容表实体
 */
@Data
@TableName("mes_instruction_stage")
public class InstructionStage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指示书ID */
    private Long instructionId;

    /** 阶段 */
    private String stage;

    /** 角色 */
    private String role;

    /** 内容 */
    private String content;

    /** 要求纳期 */
    private LocalDate requiredDate;

    /** 实际纳期 */
    private LocalDate actualDate;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
