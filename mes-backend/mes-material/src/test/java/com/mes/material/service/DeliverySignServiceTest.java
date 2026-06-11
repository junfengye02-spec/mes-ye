package com.mes.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.material.domain.entity.DeliverySign;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.mapper.DeliverySignMapper;
import com.mes.material.service.impl.DeliverySignServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliverySignServiceImpl")
class DeliverySignServiceTest {

    @Mock
    private DeliverySignMapper mapper;

    @Mock
    private IStorageInventoryService storageInventoryService;

    @Test
    @DisplayName("确认签收 - 优先命中单据指定的发货仓库和库位")
    void confirm_prefersSpecifiedWarehouseAndLocation() {
        DeliverySign entity = new DeliverySign();
        entity.setId(1L);
        entity.setMaterialId(100L);
        entity.setPendingSignQty(new BigDecimal("4"));
        entity.setDeliveryWarehouse("WH-B");
        entity.setDeliveryLocation("LOC-2");

        when(mapper.selectById(1L)).thenReturn(entity);
        when(storageInventoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                inventory(10L, "WH-A", "LOC-1", new BigDecimal("20")),
                inventory(20L, "WH-B", "LOC-2", new BigDecimal("5"))));

        DeliverySignServiceImpl service = new DeliverySignServiceImpl(mapper, storageInventoryService);
        service.confirm(1L);

        verify(storageInventoryService).deductStock(20L, new BigDecimal("4"));
    }

    private StorageInventory inventory(Long id, String warehouse, String location, BigDecimal unrestrictedStock) {
        StorageInventory inventory = new StorageInventory();
        inventory.setId(id);
        inventory.setMaterialId(100L);
        inventory.setWarehouse(warehouse);
        inventory.setStorageLocation(location);
        inventory.setUnrestrictedStock(unrestrictedStock);
        return inventory;
    }
}
