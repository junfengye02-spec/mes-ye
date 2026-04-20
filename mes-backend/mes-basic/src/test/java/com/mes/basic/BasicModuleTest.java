package com.mes.basic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.basic.domain.dto.MaterialDTO;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.entity.MaterialPrice;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.MaterialPriceMapper;
import com.mes.basic.service.impl.MaterialServiceImpl;
import com.mes.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 基础数据模块单元测试
 * 覆盖物料管理、物料价格、工作中心的业务规则与校验逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BasicModuleTest {

    @Mock private MaterialMapper materialMapper;
    @Mock private MaterialPriceMapper materialPriceMapper;

    @Spy
    @InjectMocks
    private MaterialServiceImpl materialService;

    @BeforeEach
    void bindBaseMapper() {
        // MyBatis-Plus ServiceImpl 的 baseMapper 来自父类字段，Mockito 不会自动注入，需要反射绑定
        ReflectionTestUtils.setField(materialService, "baseMapper", materialMapper);
    }

    // ==================== 1. 物料创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建物料 - 正常流程")
    void testCreateMaterial_Success() {
        MaterialDTO dto = buildMaterialDTO("MAT-001", "钛合金棒料", "RAW");
        dto.setTraceMode("BATCH");
        dto.setBatchGenerator("BG-001");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(materialMapper.insert(any(Material.class))).thenReturn(1);

        Long id = materialService.create(dto);

        verify(materialMapper).insert(argThat(m ->
                "MAT-001".equals(m.getMaterialCode()) &&
                "钛合金棒料".equals(m.getMaterialName())));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建物料 - 编码重复应拒绝")
    void testCreateMaterial_DuplicateCode() {
        MaterialDTO dto = buildMaterialDTO("MAT-001", "测试物料", "RAW");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> materialService.create(dto),
                "重复的物料编码应被拒绝");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 创建物料 - SERIAL 追溯方式未配置序列号生成器应拒绝")
    void testCreateMaterial_SerialWithoutGenerator() {
        MaterialDTO dto = buildMaterialDTO("MAT-002", "序列号物料", "SEMI");
        dto.setTraceMode("SERIAL");
        dto.setSerialGenerator(null);

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(BusinessException.class, () -> materialService.create(dto),
                "SERIAL 追溯方式必须配置序列号生成器");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 创建物料 - BATCH 追溯方式未配置批号生成器应拒绝")
    void testCreateMaterial_BatchWithoutGenerator() {
        MaterialDTO dto = buildMaterialDTO("MAT-003", "批次物料", "RAW");
        dto.setTraceMode("BATCH");
        dto.setBatchGenerator(null);

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(BusinessException.class, () -> materialService.create(dto),
                "BATCH 追溯方式必须配置批号生成器");
    }

    @Test
    @Order(5)
    @DisplayName("1.5 创建物料 - SERIAL 追溯方式配置序列号生成器应成功")
    void testCreateMaterial_SerialWithGenerator() {
        MaterialDTO dto = buildMaterialDTO("MAT-004", "序列号物料2", "SEMI");
        dto.setTraceMode("SERIAL");
        dto.setSerialGenerator("SG-001");

        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(materialMapper.insert(any(Material.class))).thenReturn(1);

        assertDoesNotThrow(() -> materialService.create(dto));
        verify(materialMapper).insert(any(Material.class));
    }

    // ==================== 2. 物料更新测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 更新物料 - 正常流程")
    void testUpdateMaterial_Success() {
        Material existing = buildMaterial(1L, "MAT-001", "旧名称", "RAW");
        MaterialDTO dto = buildMaterialDTO("MAT-001", "新名称", "RAW");

        when(materialMapper.selectById(1L)).thenReturn(existing);
        when(materialMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);

        materialService.update(1L, dto);

        verify(materialMapper).updateById(argThat(m -> "新名称".equals(m.getMaterialName())));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 更新物料 - 不存在应抛异常")
    void testUpdateMaterial_NotExist() {
        when(materialMapper.selectById(999L)).thenReturn(null);
        MaterialDTO dto = buildMaterialDTO("MAT-999", "不存在", "RAW");

        assertThrows(Exception.class, () -> materialService.update(999L, dto));
    }

    // ==================== 3. 物料删除测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 删除物料 - 无价格引用应成功")
    void testDeleteMaterial_Success() {
        Material material = buildMaterial(1L, "MAT-001", "测试物料", "RAW");

        when(materialMapper.selectById(1L)).thenReturn(material);
        when(materialPriceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // ServiceImpl.removeById 内部依赖 TableInfoHelper 注册表，单测环境下 tableInfo 为 null 会 NPE；
        // 通过 Spy 直接打桩 removeById，验证业务最终调用到了“按 id 删除”的动作即可
        doReturn(true).when(materialService).removeById(1L);

        materialService.delete(1L);

        verify(materialService).removeById(1L);
    }

    @Test
    @Order(21)
    @DisplayName("3.2 删除物料 - 存在价格记录应拒绝")
    void testDeleteMaterial_HasPriceReference() {
        Material material = buildMaterial(1L, "MAT-001", "测试物料", "RAW");

        when(materialMapper.selectById(1L)).thenReturn(material);
        when(materialPriceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(Exception.class, () -> materialService.delete(1L),
                "存在价格引用时不应允许删除物料");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 删除不存在的物料 - 应抛异常")
    void testDeleteMaterial_NotExist() {
        when(materialMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> materialService.delete(999L));
    }

    // ==================== 4. 物料详情查询（缓存）测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 查询物料详情 - 正常返回")
    void testGetDetail_Success() {
        Material material = buildMaterial(1L, "MAT-001", "钛合金棒料", "RAW");
        when(materialMapper.selectById(1L)).thenReturn(material);

        var vo = materialService.getDetail(1L);

        assertNotNull(vo);
        assertEquals("MAT-001", vo.getMaterialCode());
        assertEquals("钛合金棒料", vo.getMaterialName());
    }

    @Test
    @Order(31)
    @DisplayName("4.2 查询不存在的物料 - 应抛异常")
    void testGetDetail_NotExist() {
        when(materialMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> materialService.getDetail(999L));
    }

    // ==================== 辅助方法 ====================

    private MaterialDTO buildMaterialDTO(String code, String name, String type) {
        MaterialDTO dto = new MaterialDTO();
        dto.setMaterialCode(code);
        dto.setMaterialName(name);
        dto.setMaterialType(type);
        dto.setBaseUnit("PCS");
        return dto;
    }

    private Material buildMaterial(Long id, String code, String name, String type) {
        Material m = new Material();
        m.setId(id);
        m.setMaterialCode(code);
        m.setMaterialName(name);
        m.setMaterialType(type);
        m.setBaseUnit("PCS");
        return m;
    }
}
