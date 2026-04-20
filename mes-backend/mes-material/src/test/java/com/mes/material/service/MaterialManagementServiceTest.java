package com.mes.material.service;

import com.mes.common.event.ApsSyncEvent;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.material.domain.dto.StorageInventoryDTO;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.mapper.StorageInventoryMapper;
import com.mes.material.service.impl.StorageInventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 物料管理模块 Service 单元测试（JUnit 5 + Mockito）
 */
@DisplayName("物料管理 Service 测试")
class MaterialManagementServiceTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("StorageInventoryServiceImpl")
    class StorageInventoryServiceImplTest {

        @Mock
        private StorageInventoryMapper storageInventoryMapper;

        @Mock
        private ApplicationEventPublisher applicationEventPublisher;

        private StorageInventoryServiceImpl storageInventoryService;

        @BeforeEach
        void setUp() {
            storageInventoryService = spy(new StorageInventoryServiceImpl(applicationEventPublisher));
            ReflectionTestUtils.setField(storageInventoryService, "baseMapper", storageInventoryMapper);
        }

        @Test
        @DisplayName("创建库存 - 正常，null 数量字段初始化为 0")
        void create_shouldInitializeNullStockFieldsToZero() {
            when(storageInventoryMapper.insert(any(StorageInventory.class))).thenAnswer(invocation -> {
                StorageInventory entity = invocation.getArgument(0);
                entity.setId(9001L);
                return 1;
            });

            StorageInventoryDTO dto = new StorageInventoryDTO();
            dto.setMaterialCode("M-001");
            dto.setMaterialName("测试物料");
            dto.setWarehouse("WH01");
            dto.setStorageLocation("LOC-A");
            dto.setFactory("F01");
            dto.setUnrestrictedStock(null);
            dto.setQualityStock(null);
            dto.setFrozenStock(null);

            Long id = storageInventoryService.create(dto);

            assertEquals(9001L, id);
            ArgumentCaptor<StorageInventory> captor = ArgumentCaptor.forClass(StorageInventory.class);
            verify(storageInventoryMapper).insert(captor.capture());
            StorageInventory saved = captor.getValue();
            assertEquals(BigDecimal.ZERO, saved.getUnrestrictedStock());
            assertEquals(BigDecimal.ZERO, saved.getQualityStock());
            assertEquals(BigDecimal.ZERO, saved.getFrozenStock());
            assertEquals("M-001", saved.getMaterialCode());
        }

        @Test
        @DisplayName("更新库存 - 正常")
        void update_shouldSucceedWhenInventoryExists() {
            Long id = 10L;
            StorageInventory existing = new StorageInventory();
            existing.setId(id);
            existing.setMaterialCode("M-OLD");
            existing.setMaterialName("旧名称");
            doReturn(existing).when(storageInventoryService).getById(id);
            when(storageInventoryMapper.updateById(any(StorageInventory.class))).thenReturn(1);

            StorageInventoryDTO dto = new StorageInventoryDTO();
            dto.setMaterialCode("M-NEW");
            dto.setMaterialName("新名称");
            dto.setWarehouse("WH02");
            dto.setUnrestrictedStock(new BigDecimal("12.5"));

            storageInventoryService.update(id, dto);

            ArgumentCaptor<StorageInventory> captor = ArgumentCaptor.forClass(StorageInventory.class);
            verify(storageInventoryService).updateById(captor.capture());
            StorageInventory updated = captor.getValue();
            assertEquals(id, updated.getId());
            assertEquals("M-NEW", updated.getMaterialCode());
            assertEquals("新名称", updated.getMaterialName());
            assertEquals("WH02", updated.getWarehouse());
            assertEquals(new BigDecimal("12.5"), updated.getUnrestrictedStock());
        }

        @Test
        @DisplayName("更新库存 - 库存不存在")
        void update_shouldThrowWhenInventoryNotExists() {
            Long id = 99L;
            doReturn(null).when(storageInventoryService).getById(id);
            StorageInventoryDTO dto = new StorageInventoryDTO();
            dto.setMaterialName("任意");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> storageInventoryService.update(id, dto));

            assertEquals(ResultCode.DATA_NOT_EXIST.getCode(), ex.getCode());
            verify(storageInventoryService, never()).updateById(any());
        }

        @Test
        @DisplayName("扣减库存 - 正常并发布 APS 库存同步事件")
        void deductStock_shouldPublishApsEventWhenSufficient() {
            Long inventoryId = 20L;
            BigDecimal qty = new BigDecimal("3");
            when(storageInventoryMapper.deductStock(inventoryId, qty)).thenReturn(1);

            StorageInventory after = new StorageInventory();
            after.setId(inventoryId);
            after.setMaterialCode("MAT-APS");
            doReturn(after).when(storageInventoryService).getById(inventoryId);

            storageInventoryService.deductStock(inventoryId, qty);

            verify(storageInventoryMapper).deductStock(inventoryId, qty);
            ArgumentCaptor<ApsSyncEvent> eventCaptor = ArgumentCaptor.forClass(ApsSyncEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            ApsSyncEvent event = eventCaptor.getValue();
            assertEquals("INVENTORY", event.getSyncType());
            assertEquals("STOCK_CHANGE", event.getDataType());
            assertEquals(inventoryId, event.getDataId());
            assertEquals("MAT-APS", event.getDataNo());
            assertEquals(5, event.getPriority());
            assertTrue(event.getPayload().contains("\"action\":\"DEDUCT\""));
            assertTrue(event.getPayload().contains("\"qty\":3"));
        }

        @Test
        @DisplayName("扣减库存 - 库存不足")
        void deductStock_shouldThrowWhenInsufficient() {
            Long inventoryId = 30L;
            BigDecimal qty = new BigDecimal("100");
            when(storageInventoryMapper.deductStock(inventoryId, qty)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> storageInventoryService.deductStock(inventoryId, qty));

            assertEquals("库存不足", ex.getMessage());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("增加库存 - 正常并发布 APS 库存同步事件")
        void addStock_shouldPublishApsEvent() {
            Long inventoryId = 40L;
            BigDecimal qty = new BigDecimal("7.5");
            when(storageInventoryMapper.addStock(eq(inventoryId), eq(qty))).thenReturn(1);

            StorageInventory inv = new StorageInventory();
            inv.setId(inventoryId);
            inv.setMaterialCode("MAT-ADD");
            doReturn(inv).when(storageInventoryService).getById(inventoryId);

            storageInventoryService.addStock(inventoryId, qty);

            verify(storageInventoryMapper).addStock(inventoryId, qty);
            ArgumentCaptor<ApsSyncEvent> eventCaptor = ArgumentCaptor.forClass(ApsSyncEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            ApsSyncEvent event = eventCaptor.getValue();
            assertEquals("INVENTORY", event.getSyncType());
            assertEquals("STOCK_CHANGE", event.getDataType());
            assertEquals(inventoryId, event.getDataId());
            assertEquals("MAT-ADD", event.getDataNo());
            assertNotNull(event.getPayload());
            assertTrue(event.getPayload().contains("\"action\":\"ADD\""));
            assertTrue(event.getPayload().contains("\"qty\":7.5"));
        }
    }
}
