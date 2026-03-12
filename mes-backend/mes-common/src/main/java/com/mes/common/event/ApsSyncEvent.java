package com.mes.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * APS 同步事件
 * <p>业务模块发布此事件，mes-aps 模块监听并写入同步队列</p>
 */
@Getter
public class ApsSyncEvent extends ApplicationEvent {

    /** 同步类型（WORKORDER/INVENTORY/QUALITY/ABNORMAL） */
    private final String syncType;
    /** 数据类型 */
    private final String dataType;
    /** 关联数据ID */
    private final Long dataId;
    /** 关联数据编号 */
    private final String dataNo;
    /** 优先级（1最高，10最低） */
    private final int priority;
    /** 同步数据载荷（JSON） */
    private final String payload;

    public ApsSyncEvent(Object source, String syncType, String dataType,
                        Long dataId, String dataNo, int priority, String payload) {
        super(source);
        this.syncType = syncType;
        this.dataType = dataType;
        this.dataId = dataId;
        this.dataNo = dataNo;
        this.priority = priority;
        this.payload = payload;
    }
}
