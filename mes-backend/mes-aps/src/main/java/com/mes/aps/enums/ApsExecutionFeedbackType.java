package com.mes.aps.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * APS 执行反馈内部类型。
 * <p>这些反馈目前没有对应的 MES→APS HTTP 合同端点，因此不再占用 {@link SyncType} 的对外合同枚举，
 * 但仍保留事件分发和本地失败审计能力，方便后续与 APS 团队单独协商落地。</p>
 */
@Getter
@AllArgsConstructor
public enum ApsExecutionFeedbackType {
    DISPATCH("DISPATCH", "派工分配结果"),
    START_CHECK("START_CHECK", "开工检查结果"),
    CONSTRAINT("CONSTRAINT", "工单约束关系"),
    SHIFT_OUTPUT("SHIFT_OUTPUT", "交班实际产出"),
    MATERIAL_SHORTAGE("MATERIAL_SHORTAGE", "物料齐套/短缺"),
    REQUISITION("REQUISITION", "领料进度"),
    SUPPLY_PROGRESS("SUPPLY_PROGRESS", "供应计划完成度"),
    STATUS_CHANGE("STATUS_CHANGE", "工单状态变更"),
    PROCESS_CHANGE("PROCESS_CHANGE", "工艺变更通知");

    private final String code;
    private final String desc;

    public static Optional<ApsExecutionFeedbackType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }

    public static boolean contains(String code) {
        return fromCode(code).isPresent();
    }
}
