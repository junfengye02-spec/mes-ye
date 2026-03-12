package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 交班记录表实体
 */
@Data
@TableName("mes_shift_handover")
public class ShiftHandover implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目名称 */
    private String projectName;

    /** 产品序列号 */
    private String productSerialNo;

    /** 工序内容 */
    private String processContent;

    /** 交班日期 */
    private LocalDate handoverDate;

    /** 发起星期 */
    private Integer handoverWeekday;

    /** 发起时间 */
    private LocalTime handoverTime;

    /** 发起班组ID */
    private Long handoverTeamId;

    /** 发起班组 */
    private String handoverTeamName;

    /** 发起班次 */
    private String handoverShift;

    /** 接收班次 */
    private String takeoverShift;

    /** 接班班组ID */
    private Long takeoverTeamId;

    /** 接班班组 */
    private String takeoverTeamName;

    /** 交接人员 */
    private String handoverPerson;

    /** 接班人员 */
    private String takeoverPerson;

    /** 班组长 */
    private String teamLeader;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 实际完成 */
    private BigDecimal actualQty;

    /** 未达标分析 */
    private String gapAnalysis;

    /** 交班内容 */
    private String handoverContent;

    /** 状态（PENDING/RECEIVED） */
    private String status;

    /** 其它需要交付事宜 */
    private String otherMatters;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改人 */
    private String updatedBy;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
