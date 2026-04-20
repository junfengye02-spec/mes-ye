package com.mes.basic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.basic.domain.dto.MaterialDTO;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.vo.MaterialVO;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.MaterialPriceMapper;
import com.mes.basic.service.impl.MaterialServiceImpl;
import com.mes.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MaterialServiceImpl} 单元测试：增删改查与追溯校验
 * <p>详情接口在生产环境由 Spring Cache（{@code @Cacheable(value = "material")}）代理；本处校验业务数据映射与 Mapper 协作。</p>
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MaterialServiceTest {

    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private MaterialPriceMapper materialPriceMapper;

    @Spy
    @InjectMocks
    private MaterialServiceImpl materialService;

    @BeforeEach
    void bindBaseMapper() {
        ReflectionTestUtils.setField(materialService, "baseMapper", materialMapper);
    }

    // ==================== 1. 创建 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建物料 - 正常")
    void create_whenValidDto_shouldPersist() {
        MaterialDTO dto = baseDto("M-100", "铝合金板", "RAW");
        dto.setTraceMode("BATCH");
        dto.setBatchGenerator("BATCH-GEN-01");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(materialMapper.insert(any(Material.class))).thenAnswer(inv -> {
            Material m = inv.getArgument(0);
            m.setId(100L);
            return 1;
        });

        Long id = materialService.create(dto);

        assertNotNull(id);
        verify(materialMapper).insert(argThat(m ->
                "M-100".equals(m.getMaterialCode()) && "铝合金板".equals(m.getMaterialName())));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建物料 - 编码重复")
    void create_whenCodeDuplicate_shouldThrow() {
        MaterialDTO dto = baseDto("M-DUP", "重复编码", "RAW");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> materialService.create(dto));
        assertTrue(ex.getMessage().contains("物料编码已存在") || ex.getMessage().contains("M-DUP"));
        verify(materialMapper, never()).insert(any());
    }

    @Test
    @Order(3)
    @DisplayName("1.3 创建物料 - 单件追溯 SERIAL 必须配置序列号生成器")
    void create_whenSerialWithoutSerialGenerator_shouldThrow() {
        MaterialDTO dto = baseDto("M-SER", "序列件", "SEMI");
        dto.setTraceMode("SERIAL");
        dto.setSerialGenerator(null);

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> materialService.create(dto));
        assertTrue(ex.getMessage().contains("序列号生成器"));
    }

    @Test
    @Order(4)
    @DisplayName("1.4 创建物料 - 批次追溯 BATCH 必须配置批号生成器")
    void create_whenBatchWithoutBatchGenerator_shouldThrow() {
        MaterialDTO dto = baseDto("M-BAT", "批次件", "RAW");
        dto.setTraceMode("BATCH");
        dto.setBatchGenerator("  ");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> materialService.create(dto));
        assertTrue(ex.getMessage().contains("批号生成器"));
    }

    // ==================== 2. 更新 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 更新物料 - 正常")
    void update_whenMaterialExists_shouldUpdateRow() {
        Material existing = entity(1L, "M-200", "旧名", "RAW");
        MaterialDTO dto = baseDto("M-200", "新名", "RAW");

        when(materialMapper.selectById(1L)).thenReturn(existing);
        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);

        materialService.update(1L, dto);

        verify(materialMapper).updateById(argThat(m -> "新名".equals(m.getMaterialName())));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 更新物料 - 物料不存在")
    void update_whenMaterialMissing_shouldThrow() {
        when(materialMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> materialService.update(999L, baseDto("X", "无", "RAW")));
    }

    // ==================== 3. 删除 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 删除物料 - 正常")
    void delete_whenNoPriceReference_shouldRemove() {
        Material m = entity(2L, "M-DEL", "待删", "RAW");
        when(materialMapper.selectById(2L)).thenReturn(m);
        when(materialPriceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(true).when(materialService).removeById(2L);

        materialService.delete(2L);

        verify(materialService).removeById(2L);
    }

    @Test
    @Order(21)
    @DisplayName("3.2 删除物料 - 被物料价格引用不允许删除")
    void delete_whenReferencedByMaterialPrice_shouldThrow() {
        Material m = entity(3L, "M-PR", "有价", "RAW");
        when(materialMapper.selectById(3L)).thenReturn(m);
        when(materialPriceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> materialService.delete(3L));
        assertTrue(ex.getMessage().contains("价格"));
        verify(materialMapper, never()).deleteById(anyLong());
    }

    // ==================== 4. 详情（Cacheable 入口） ====================

    @Test
    @Order(30)
    @DisplayName("4.1 查询物料详情 - 命中数据（接口层 @Cacheable material）")
    void getDetail_whenExists_shouldReturnVo() {
        Material m = entity(8L, "M-008", "缓存演示物料", "RAW");
        when(materialMapper.selectById(8L)).thenReturn(m);

        MaterialVO vo = materialService.getDetail(8L);

        assertNotNull(vo);
        assertEquals("M-008", vo.getMaterialCode());
        assertEquals("缓存演示物料", vo.getMaterialName());
        verify(materialMapper).selectById(8L);
    }

    @Test
    @Order(31)
    @DisplayName("4.2 查询物料详情 - 不存在")
    void getDetail_whenMissing_shouldThrow() {
        when(materialMapper.selectById(404L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> materialService.getDetail(404L));
    }

    // ==================== 辅助 ====================

    private static MaterialDTO baseDto(String code, String name, String type) {
        MaterialDTO dto = new MaterialDTO();
        dto.setMaterialCode(code);
        dto.setMaterialName(name);
        dto.setMaterialType(type);
        dto.setBaseUnit("PCS");
        return dto;
    }

    private static Material entity(Long id, String code, String name, String type) {
        Material m = new Material();
        m.setId(id);
        m.setMaterialCode(code);
        m.setMaterialName(name);
        m.setMaterialType(type);
        m.setBaseUnit("PCS");
        return m;
    }
}
