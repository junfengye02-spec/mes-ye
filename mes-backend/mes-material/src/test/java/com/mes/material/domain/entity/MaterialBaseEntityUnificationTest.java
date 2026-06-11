package com.mes.material.domain.entity;

import com.mes.common.core.BaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("mes-material BaseEntity 统一")
class MaterialBaseEntityUnificationTest {

    @Test
    @DisplayName("11.1 目标实体应继承 BaseEntity")
    void entities_shouldExtendBaseEntity() {
        assertTrue(BaseEntity.class.isAssignableFrom(DeliverySign.class));
        assertTrue(BaseEntity.class.isAssignableFrom(MaterialRequisitionItem.class));
        assertTrue(BaseEntity.class.isAssignableFrom(FinishedGoodsReceiptItem.class));
    }
}
