package com.mes.dispatch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 派工任务状态枚举
 * <p>状态流转：UNASSIGNED &rarr; ASSIGNED &rarr; IN_PROGRESS &rarr; COMPLETED</p>
 * <p>分支路径：ASSIGNED &rarr; REVOKED（回退为 UNASSIGNED）</p>
 * <p>任意非完工状态 &rarr; CANCELLED（需记录撤销原因）</p>
 */
@Getter
@AllArgsConstructor
public enum DispatchStatus {

    UNASSIGNED("UNASSIGNED", "未分派"),
    ASSIGNED("ASSIGNED", "已分派"),
    IN_PROGRESS("IN_PROGRESS", "开工中"),
    COMPLETED("COMPLETED", "已完工"),
    CANCELLED("CANCELLED", "已撤销"),
    REVOKED("REVOKED", "已取消指派");

    private final String code;
    private final String desc;
}
