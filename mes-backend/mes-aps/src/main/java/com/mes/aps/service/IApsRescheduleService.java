package com.mes.aps.service;

/**
 * APS 异常重排触发服务
 */
public interface IApsRescheduleService {

    /**
     * 触发 APS 重排（含 5 分钟防抖）
     * @param eventCategory 事件分类（如 EQUIPMENT_FAILURE, MATERIAL_SHORTAGE 等）
     * @param reason 触发原因描述
     * @param dataId 关联数据ID
     * @param dataNo 关联数据编号
     */
    void triggerReschedule(String eventCategory, String reason, Long dataId, String dataNo);

    /**
     * 判断是否启用重排触发
     */
    boolean isRescheduleEnabled();
}
