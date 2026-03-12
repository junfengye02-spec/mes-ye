package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 指示书流程日志表实体
 */
@Data
@TableName("mes_instruction_flow_log")
public class InstructionFlowLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指示书ID */
    private Long instructionId;

    /** 动作 */
    private String action;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operatedTime;

    /** 说明 */
    private String detail;
}
