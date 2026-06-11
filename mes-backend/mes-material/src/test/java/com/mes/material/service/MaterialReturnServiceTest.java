package com.mes.material.service;

import com.mes.material.domain.dto.MaterialReturnDTO;
import com.mes.material.domain.entity.MaterialReturn;
import com.mes.material.domain.vo.MaterialReturnVO;
import com.mes.material.mapper.MaterialReturnMapper;
import com.mes.material.service.impl.MaterialReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MaterialReturnServiceImpl")
class MaterialReturnServiceTest {

    @Mock
    private MaterialReturnMapper materialReturnMapper;

    @Mock
    private IStorageInventoryService storageInventoryService;

    private MaterialReturnServiceImpl materialReturnService;

    @BeforeEach
    void setUp() {
        materialReturnService = new MaterialReturnServiceImpl(storageInventoryService);
        ReflectionTestUtils.setField(materialReturnService, "baseMapper", materialReturnMapper);
    }

    @Test
    @DisplayName("create - 使用通用 flowCode 字段保存退料流程编码")
    void create_persistsGenericFlowCode() {
        MaterialReturnDTO dto = new MaterialReturnDTO();
        dto.setWorkOrderId(10L);
        dto.setFlowCode("FLOW-RETURN");
        dto.setBusinessType("REPAIR");

        when(materialReturnMapper.insert(any(MaterialReturn.class))).thenAnswer(invocation -> {
            MaterialReturn entity = invocation.getArgument(0);
            entity.setId(501L);
            return 1;
        });

        Long id = materialReturnService.create(dto);

        assertEquals(501L, id);

        ArgumentCaptor<MaterialReturn> entityCaptor = ArgumentCaptor.forClass(MaterialReturn.class);
        verify(materialReturnMapper).insert(entityCaptor.capture());
        assertEquals("FLOW-RETURN", entityCaptor.getValue().getFlowCode());
        assertEquals("REPAIR", entityCaptor.getValue().getBusinessType());
    }

    @Test
    @DisplayName("getDetail - 返回通用 flowCode 字段")
    void getDetail_returnsGenericFlowCode() {
        MaterialReturn entity = new MaterialReturn();
        entity.setId(601L);
        entity.setReturnNo("TL-001");
        entity.setFlowCode("FLOW-DETAIL");

        when(materialReturnMapper.selectById(601L)).thenReturn(entity);

        MaterialReturnVO detail = materialReturnService.getDetail(601L);

        assertEquals("FLOW-DETAIL", detail.getFlowCode());
    }
}
