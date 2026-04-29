package com.mes.dispatch.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务表实体
 *
 * <p>P0 修复 R3（mcp30）：改为 {@code extends BaseEntity}，把
 * {@code id / createdBy / createdTime / updatedBy / updatedTime / tenantId / deleted}
 * 全部交给 BaseEntity + MetaObjectHandler 自动填充。</p>
 *
 * <p>原版本没有声明 {@code tenantId}，而 {@code mes_dispatch_task.tenant_id BIGINT NOT NULL DEFAULT 1}
 * 自 V1.16 起就已建好：应用层 INSERT 时 MP 不会把当前 TenantContext 写进去，
 * DB 默认值 1 兜底 → 租户 2 / 3 创建的派工永远落到租户 1，形成跨租户污染。
 * 本次修复后：</p>
 * <ul>
 *   <li>INSERT：MetaObjectHandler#insertFill 从 {@code TenantContextHolder} 取 tenantId 写入；</li>
 *   <li>SELECT：{@code TenantLineInnerInterceptor} 自动拼 {@code WHERE tenant_id = ?}；</li>
 *   <li>UPDATE：同 SELECT，拦截器自动拼条件，防止改到别人的任务。</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_dispatch_task")
public class DispatchTask extends BaseEntity {

    /** 工单ID */
    private Long workOrderId;

    /** 工作清单ID */
    private Long workOrderTaskId;

    /** 订单编号 */
    private String orderNo;

    /** 工序号 */
    private String processNo;

    /** 工作名称 */
    private String workName;

    /** 计划工作中心 */
    private Long planWorkCenterId;

    /** 序列号 */
    private String serialNo;

    /** 项目 */
    private String projectName;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 分派状态 */
    private String dispatchStatus;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 实际开工时间 */
    private LocalDateTime actualStartTime;

    /** 实际完工时间 */
    private LocalDateTime actualEndTime;

    /** 实际完成数量 */
    private BigDecimal actualQty;

    /** 质量结果：PASS=合格，FAIL=不合格，NA=不适用 */
    private String qualityResult;

    /** 撤销原因（状态=CANCELLED 时必填） */
    private String cancelReason;
}
