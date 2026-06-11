package com.mes.aps;

import com.mes.aps.enums.SyncType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("APS SyncType 语义收口")
class SyncTypeSemanticsTest {

    @Test
    @DisplayName("12.6 SyncType 仅保留真实集成合同类型")
    void syncType_keepsOnlyRealContractTypes() {
        assertTrue(SyncType.fromCode("WORKORDER").isPresent());
        assertTrue(SyncType.fromCode("MRP").isPresent());
        assertTrue(SyncType.fromCode("TEAM").isPresent());
        assertTrue(SyncType.fromCode("DISPATCH").isEmpty());
        assertTrue(SyncType.fromCode("STATUS_CHANGE").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SyncType.valueOf("DISPATCH"));
        assertThrows(IllegalArgumentException.class, () -> SyncType.valueOf("PROCESS_CHANGE"));
        assertEquals(20, SyncType.values().length);
    }

    @Test
    @DisplayName("12.6 重排与上行入队语义仍由 SyncType 统一声明")
    void syncType_declaresRescheduleAndUpstreamQueueSemantics() {
        assertTrue(SyncType.ABNORMAL.isRescheduleTrigger());
        assertFalse(SyncType.QUALITY.isRescheduleTrigger());
        assertTrue(SyncType.isUpstreamQueueSupported("WORKORDER"));
        assertFalse(SyncType.isUpstreamQueueSupported("DISPATCH"));
    }

    @Test
    @DisplayName("12.6 当前已落地的上行合同端点由 SyncType 统一声明")
    void syncType_declaresCurrentUpstreamContractEndpoints() {
        assertEquals("/api/mes/status/sync", SyncType.WORKORDER.requireUpstreamContractEndpoint());
        assertEquals("/api/mes/master-data/work-centers", SyncType.WORK_CENTER.requireUpstreamContractEndpoint());
        assertEquals("/api/mes/reschedule", SyncType.ABNORMAL.requireUpstreamContractEndpoint());
        assertTrue(SyncType.ABNORMAL.isAsyncUpstreamContractCall());
        assertThrows(IllegalStateException.class, SyncType.ORDER::requireUpstreamContractEndpoint);
    }
}
