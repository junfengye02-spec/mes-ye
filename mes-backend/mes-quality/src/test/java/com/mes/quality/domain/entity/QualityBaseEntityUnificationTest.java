package com.mes.quality.domain.entity;

import com.mes.common.core.BaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("mes-quality BaseEntity 统一")
class QualityBaseEntityUnificationTest {

    @Test
    @DisplayName("11.1 目标实体应继承 BaseEntity")
    void entities_shouldExtendBaseEntity() {
        assertTrue(BaseEntity.class.isAssignableFrom(ShiftHandover.class));
        assertTrue(BaseEntity.class.isAssignableFrom(RecheckOrderPlan.class));
        assertTrue(BaseEntity.class.isAssignableFrom(RecheckSerial.class));
    }
}
