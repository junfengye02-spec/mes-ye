package com.mes.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.process.domain.dto.ManufacturingBomDTO;
import com.mes.process.domain.dto.ManufacturingBomItemDTO;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.entity.ManufacturingBomItem;
import com.mes.process.domain.vo.ManufacturingBomItemVO;
import com.mes.process.mapper.BomSubstituteMapper;
import com.mes.process.mapper.BomVersionLogMapper;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.service.impl.ManufacturingBomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManufacturingBom route-step compatibility")
class ManufacturingBomRouteStepCompatibilityTest {

    @Mock
    private ManufacturingBomMapper manufacturingBomMapper;

    @Mock
    private ManufacturingBomItemMapper itemMapper;

    @Mock
    private BomSubstituteMapper substituteMapper;

    @Mock
    private BomVersionLogMapper versionLogMapper;

    @InjectMocks
    private ManufacturingBomServiceImpl manufacturingBomService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(manufacturingBomService, "baseMapper", manufacturingBomMapper);
    }

    @Test
    @DisplayName("create - BOM 明细以 routeStepId 为主，缺省时兼容 processId")
    void create_prefersRouteStepIdButFallsBackToProcessId() {
        when(manufacturingBomMapper.insert(any(ManufacturingBom.class))).thenAnswer(invocation -> {
            ManufacturingBom entity = invocation.getArgument(0);
            entity.setId(200L);
            return 1;
        });

        ManufacturingBomItemDTO legacyOnly = new ManufacturingBomItemDTO();
        legacyOnly.setMaterialId(1L);
        legacyOnly.setQuantity(java.math.BigDecimal.ONE);
        legacyOnly.setProcessId(88L);

        ManufacturingBomItemDTO primary = new ManufacturingBomItemDTO();
        primary.setMaterialId(2L);
        primary.setQuantity(java.math.BigDecimal.ONE);
        primary.setRouteStepId(99L);
        primary.setProcessId(12L);

        ManufacturingBomDTO dto = new ManufacturingBomDTO();
        dto.setBomCode("BOM-ROUTE-001");
        dto.setBomName("Route compatibility");
        dto.setItems(List.of(legacyOnly, primary));

        manufacturingBomService.create(dto);

        ArgumentCaptor<ManufacturingBomItem> itemCaptor = ArgumentCaptor.forClass(ManufacturingBomItem.class);
        verify(itemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        List<ManufacturingBomItem> savedItems = itemCaptor.getAllValues();

        assertEquals(88L, savedItems.get(0).getRouteStepId());
        assertEquals(88L, savedItems.get(0).getProcessId());
        assertEquals(99L, savedItems.get(1).getRouteStepId());
        assertEquals(99L, savedItems.get(1).getProcessId());
    }

    @Test
    @DisplayName("getItemTree - BOM 明细 VO 以 routeStepId 为主，同时回显 processId 兼容字段")
    void getItemTree_echoesLegacyProcessIdFromRouteStepId() {
        ManufacturingBomItem entity = new ManufacturingBomItem();
        entity.setId(1L);
        entity.setBomId(10L);
        entity.setMaterialId(20L);
        entity.setRouteStepId(66L);
        entity.setProcessId(null);

        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(entity));
        when(substituteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<ManufacturingBomItemVO> tree = manufacturingBomService.getItemTree(10L);

        assertEquals(1, tree.size());
        assertEquals(66L, tree.get(0).getRouteStepId());
        assertEquals(66L, tree.get(0).getProcessId());
    }
}
