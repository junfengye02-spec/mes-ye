package com.mes.process.service;

import com.mes.process.domain.dto.MachiningProgramDTO;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.domain.vo.MachiningProgramVO;
import com.mes.process.service.impl.MachiningProgramServiceImpl;
import com.mes.process.service.support.ProcessParameterSchemaDefinition;
import com.mes.process.service.support.ProcessParameterStoreService;
import com.mes.process.service.support.ProcessParameterUpsertCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MachiningProgramServiceImpl")
class MachiningProgramServiceTest {

    @Mock
    private ProcessParameterStoreService processParameterStoreService;

    private MachiningProgramServiceImpl machiningProgramService;

    @BeforeEach
    void setUp() {
        machiningProgramService = new MachiningProgramServiceImpl(processParameterStoreService);
    }

    @Test
    @DisplayName("create - 以通用工艺参数模型保存机械加工程序")
    void create_savesThroughGenericParameterStore() {
        MachiningProgramDTO dto = new MachiningProgramDTO();
        dto.setGCode("G-100");
        dto.setProgramTable("N10 G00 X0");
        dto.setProductName("叶轮");
        when(processParameterStoreService.create(any(), any())).thenReturn(88L);

        Long id = machiningProgramService.create(dto);

        assertEquals(88L, id);

        ArgumentCaptor<ProcessParameterSchemaDefinition> schemaCaptor =
                ArgumentCaptor.forClass(ProcessParameterSchemaDefinition.class);
        ArgumentCaptor<ProcessParameterUpsertCommand> commandCaptor =
                ArgumentCaptor.forClass(ProcessParameterUpsertCommand.class);
        verify(processParameterStoreService).create(schemaCaptor.capture(), commandCaptor.capture());
        assertEquals("MACHINING_PROGRAM", schemaCaptor.getValue().getSchemaCode());
        assertEquals("G-100", commandCaptor.getValue().getParameterCode());
        assertEquals("叶轮", commandCaptor.getValue().getParameterName());
        assertTrue(commandCaptor.getValue().getParamValuesJson().contains("\"programTable\":\"N10 G00 X0\""));
    }

    @Test
    @DisplayName("getDetail - 从通用工艺参数记录映射机械加工程序 VO")
    void getDetail_mapsGenericParameterValueToVo() {
        ProcessParameterValue value = new ProcessParameterValue();
        value.setId(99L);
        value.setCreatedTime(LocalDateTime.of(2026, 5, 27, 11, 0));
        value.setParamValuesJson("""
                {"gCode":"G-200","programTable":"N20 G01 X1","productName":"导向器"}
                """);
        when(processParameterStoreService.getRequired(any(), any())).thenReturn(value);

        MachiningProgramVO detail = machiningProgramService.getDetail(99L);

        assertEquals(99L, detail.getId());
        assertEquals("G-200", detail.getGCode());
        assertEquals("N20 G01 X1", detail.getProgramTable());
        assertEquals("导向器", detail.getProductName());
    }
}
