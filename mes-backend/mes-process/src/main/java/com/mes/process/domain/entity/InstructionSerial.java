package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 指示书序列号表实体
 */
@Data
@TableName("mes_instruction_serial")
public class InstructionSerial implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指示书ID */
    private Long instructionId;

    /** 产品类型 */
    private String productType;

    /** 数量 */
    private Integer qty;

    /** 接收K/G编码 */
    private String receiveKgCode;

    /** 发送时G编码 */
    private String sendGCode;

    /** 定检时间 */
    private LocalDateTime scheduledCheckTime;

    /** 接收时间 */
    private LocalDateTime receiveTime;

    /** 备注 */
    private String remark;
}
