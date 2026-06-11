package com.mes.process.service;

import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.SprayConditionDTO;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.domain.query.SprayConditionQuery;
import com.mes.process.domain.vo.SprayConditionVO;
import com.mes.process.service.impl.SprayConditionServiceImpl;
import com.mes.process.service.support.ProcessParameterSchemaDefinition;
import com.mes.process.service.support.ProcessParameterSearch;
import com.mes.process.service.support.ProcessParameterStoreService;
import com.mes.process.service.support.ProcessParameterUpsertCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SprayConditionServiceImpl")
class SprayConditionServiceTest {

    @Mock
    private ProcessParameterStoreService processParameterStoreService;

    private SprayConditionServiceImpl sprayConditionService;

    @BeforeEach
    void setUp() {
        sprayConditionService = new SprayConditionServiceImpl(processParameterStoreService);
    }

    @Test
    @DisplayName("create - 以通用工艺参数模型保存喷涂条件")
    void create_savesThroughGenericParameterStore() {
        SprayConditionDTO dto = new SprayConditionDTO();
        dto.setConditionNo("SC-001");
        dto.setSprayGunModel("JP5000");
        dto.setEquipment("HVOF-01");
        dto.setPowderFeedRate(new BigDecimal("35.5"));
        when(processParameterStoreService.create(any(), any())).thenReturn(100L);

        Long id = sprayConditionService.create(dto);

        assertEquals(100L, id);

        ArgumentCaptor<ProcessParameterSchemaDefinition> schemaCaptor =
                ArgumentCaptor.forClass(ProcessParameterSchemaDefinition.class);
        ArgumentCaptor<ProcessParameterUpsertCommand> commandCaptor =
                ArgumentCaptor.forClass(ProcessParameterUpsertCommand.class);
        verify(processParameterStoreService).create(schemaCaptor.capture(), commandCaptor.capture());
        assertEquals("SPRAY_CONDITION", schemaCaptor.getValue().getSchemaCode());
        assertEquals("SC-001", commandCaptor.getValue().getParameterCode());
        assertTrue(commandCaptor.getValue().getSearchText().contains("JP5000"));
        assertTrue(commandCaptor.getValue().getParamValuesJson().contains("\"conditionNo\":\"SC-001\""));
    }

    @Test
    @DisplayName("page - 从通用工艺参数记录映射喷涂条件 VO")
    void page_mapsGenericParameterValuesToVo() {
        ProcessParameterValue value = new ProcessParameterValue();
        value.setId(200L);
        value.setParameterCode("SC-002");
        value.setCreatedBy("tester");
        value.setCreatedTime(LocalDateTime.of(2026, 5, 27, 10, 0));
        value.setParamValuesJson("""
                {"conditionNo":"SC-002","sprayGunModel":"JP6000","equipment":"HVOF-02","powderFeedRate":42.5}
                """);
        when(processParameterStoreService.page(any(), any(), any()))
                .thenReturn(PageResult.of(List.of(value), 1L));

        SprayConditionQuery query = new SprayConditionQuery();
        query.setConditionNo("SC");
        query.setSprayGunModel("JP");
        query.setEquipment("HVOF");

        PageResult<SprayConditionVO> result = sprayConditionService.page(query);

        assertEquals(1L, result.getTotal());
        assertEquals("SC-002", result.getList().get(0).getConditionNo());
        assertEquals("JP6000", result.getList().get(0).getSprayGunModel());
        assertEquals("HVOF-02", result.getList().get(0).getEquipment());

        ArgumentCaptor<ProcessParameterSearch> searchCaptor = ArgumentCaptor.forClass(ProcessParameterSearch.class);
        verify(processParameterStoreService).page(any(), any(), searchCaptor.capture());
        assertEquals("SC", searchCaptor.getValue().getCodeKeyword());
        assertTrue(searchCaptor.getValue().getSearchKeywords().contains("JP"));
        assertTrue(searchCaptor.getValue().getSearchKeywords().contains("HVOF"));
    }
}
