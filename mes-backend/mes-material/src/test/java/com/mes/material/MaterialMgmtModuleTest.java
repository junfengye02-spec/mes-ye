package com.mes.material;

import com.mes.material.domain.dto.StorageInventoryDTO;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.mapper.StorageInventoryMapper;
import com.mes.material.service.impl.StorageInventoryServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 物料管理模块单元测试
 * 覆盖库存管理（创建/扣减/增加）、APS同步事件发布
 */
@ExtendWith(MockitoExtension.class)
// MyBatis-Plus ServiceImpl baseMapper 需反射注入；保持 LENIENT 避免 stub 噪音。
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MaterialMgmtModuleTest {

    @Mock private StorageInventoryMapper inventoryMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private StorageInventoryServiceImpl inventoryService;

    @BeforeEach
    void injectBaseMapper() {
        // 见其他 Service 单元测试注释：显式反射注入 baseMapper，避免 IService 系列方法 NPE
        ReflectionTestUtils.setField(inventoryService, "baseMapper", inventoryMapper);
    }

    // ==================== 1. 库存创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建库存 - 正常流程")
    void testCreateInventory_Success() {
        StorageInventoryDTO dto = new StorageInventoryDTO();
        dto.setMaterialCode("MAT-001");
        dto.setMaterialName("钛合金棒料");
        dto.setWarehouse("WH-01");
        dto.setUnrestrictedStock(new BigDecimal("500"));

        when(inventoryMapper.insert(any(StorageInventory.class))).thenReturn(1);

        Long id = inventoryService.create(dto);

        verify(inventoryMapper).insert(argThat(inv ->
                "MAT-001".equals(inv.getMaterialCode()) &&
                new BigDecimal("500").compareTo(inv.getUnrestrictedStock()) == 0));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建库存 - null 库存量应初始化为 0")
    void testCreateInventory_NullStockInitToZero() {
        StorageInventoryDTO dto = new StorageInventoryDTO();
        dto.setMaterialCode("MAT-002");
        dto.setMaterialName("测试物料");

        when(inventoryMapper.insert(any(StorageInventory.class))).thenReturn(1);

        inventoryService.create(dto);

        verify(inventoryMapper).insert(argThat(inv -> {
            assertEquals(BigDecimal.ZERO, inv.getUnrestrictedStock(), "非限制库存应初始化为0");
            assertEquals(BigDecimal.ZERO, inv.getQualityStock(), "质检库存应初始化为0");
            assertEquals(BigDecimal.ZERO, inv.getFrozenStock(), "冻结库存应初始化为0");
            return true;
        }));
    }

    // ==================== 2. 库存扣减测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 扣减库存 - 正常流程（应发布APS事件）")
    void testDeductStock_Success() {
        when(inventoryMapper.deductStock(1L, new BigDecimal("50"))).thenReturn(1);

        StorageInventory inv = new StorageInventory();
        inv.setId(1L);
        inv.setMaterialCode("MAT-001");
        when(inventoryMapper.selectById(1L)).thenReturn(inv);

        inventoryService.deductStock(1L, new BigDecimal("50"));

        verify(inventoryMapper).deductStock(1L, new BigDecimal("50"));
        // ApsSyncEvent extends ApplicationEvent，Mockito 会绑定 publishEvent(ApplicationEvent) 重载
        verify(eventPublisher).publishEvent(any(org.springframework.context.ApplicationEvent.class));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 库存不足时扣减 - 应拒绝")
    void testDeductStock_Insufficient() {
        when(inventoryMapper.deductStock(1L, new BigDecimal("9999"))).thenReturn(0);

        assertThrows(Exception.class,
                () -> inventoryService.deductStock(1L, new BigDecimal("9999")),
                "库存不足时应抛出异常");
    }

    // ==================== 3. 库存增加测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 增加库存 - 正常流程（应发布APS事件）")
    void testAddStock_Success() {
        StorageInventory inv = new StorageInventory();
        inv.setId(1L);
        inv.setMaterialCode("MAT-001");

        when(inventoryMapper.selectById(1L)).thenReturn(inv);

        inventoryService.addStock(1L, new BigDecimal("100"));

        verify(inventoryMapper).addStock(1L, new BigDecimal("100"));
        // ApsSyncEvent extends ApplicationEvent
        verify(eventPublisher).publishEvent(any(org.springframework.context.ApplicationEvent.class));
    }

    // ==================== 4. 库存查询测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 查询库存详情 - 正常返回")
    void testGetDetail_Success() {
        StorageInventory inv = new StorageInventory();
        inv.setId(1L);
        inv.setMaterialCode("MAT-001");
        inv.setMaterialName("钛合金棒料");
        inv.setUnrestrictedStock(new BigDecimal("500"));

        when(inventoryMapper.selectById(1L)).thenReturn(inv);

        var vo = inventoryService.getDetail(1L);

        assertNotNull(vo);
        assertEquals("MAT-001", vo.getMaterialCode());
        assertEquals(new BigDecimal("500"), vo.getUnrestrictedStock());
    }

    @Test
    @Order(31)
    @DisplayName("4.2 查询不存在的库存 - 应抛异常")
    void testGetDetail_NotExist() {
        when(inventoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> inventoryService.getDetail(999L));
    }

    // ==================== 5. 库存更新测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 更新库存信息 - 正常流程")
    void testUpdateInventory_Success() {
        StorageInventory existing = new StorageInventory();
        existing.setId(1L);
        existing.setMaterialCode("MAT-001");

        StorageInventoryDTO dto = new StorageInventoryDTO();
        dto.setMaterialCode("MAT-001");
        dto.setWarehouse("WH-02");

        when(inventoryMapper.selectById(1L)).thenReturn(existing);
        when(inventoryMapper.updateById(any(StorageInventory.class))).thenReturn(1);

        inventoryService.update(1L, dto);

        verify(inventoryMapper).updateById(any());
    }

    @Test
    @Order(41)
    @DisplayName("5.2 更新不存在的库存 - 应抛异常")
    void testUpdateInventory_NotExist() {
        when(inventoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class,
                () -> inventoryService.update(999L, new StorageInventoryDTO()));
    }
}
