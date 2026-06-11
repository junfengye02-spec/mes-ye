package com.mes.process.service.support;

import com.mes.common.exception.BusinessException;
import com.mes.process.domain.entity.ProcessParameterSchema;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.mapper.ProcessParameterSchemaMapper;
import com.mes.process.mapper.ProcessParameterValueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessParameterStoreService")
class ProcessParameterStoreServiceTest {

    @Mock
    private ProcessParameterSchemaMapper schemaMapper;

    @Mock
    private ProcessParameterValueMapper valueMapper;

    private ProcessParameterStoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new ProcessParameterStoreService(schemaMapper, valueMapper);
    }

    @Test
    @DisplayName("create - schema 缺失时自动注册并写入参数值")
    void create_autoRegistersSchemaWhenMissing() {
        when(schemaMapper.selectOne(any())).thenReturn(null);
        when(schemaMapper.insert(any(ProcessParameterSchema.class))).thenAnswer(invocation -> {
            ProcessParameterSchema schema = invocation.getArgument(0);
            schema.setId(11L);
            return 1;
        });
        when(valueMapper.selectCount(any())).thenReturn(0L);
        when(valueMapper.insert(any(ProcessParameterValue.class))).thenAnswer(invocation -> {
            ProcessParameterValue value = invocation.getArgument(0);
            value.setId(22L);
            return 1;
        });

        Long id = storeService.create(
                ProcessParameterSchemaDefinition.builder()
                        .schemaCode("SPRAY_CONDITION")
                        .schemaName("喷涂条件")
                        .processType("SPRAY")
                        .fieldDefinitionsJson("[{\"name\":\"conditionNo\"}]")
                        .build(),
                ProcessParameterUpsertCommand.builder()
                        .parameterCode("SC-001")
                        .parameterName("SC-001")
                        .status("ACTIVE")
                        .searchText("枪型A 设备A")
                        .paramValuesJson("{\"conditionNo\":\"SC-001\"}")
                        .build()
        );

        assertEquals(22L, id);

        ArgumentCaptor<ProcessParameterSchema> schemaCaptor = ArgumentCaptor.forClass(ProcessParameterSchema.class);
        verify(schemaMapper).insert(schemaCaptor.capture());
        assertEquals("SPRAY_CONDITION", schemaCaptor.getValue().getSchemaCode());

        ArgumentCaptor<ProcessParameterValue> valueCaptor = ArgumentCaptor.forClass(ProcessParameterValue.class);
        verify(valueMapper).insert(valueCaptor.capture());
        assertEquals(11L, valueCaptor.getValue().getSchemaId());
        assertEquals("SC-001", valueCaptor.getValue().getParameterCode());
        assertEquals("{\"conditionNo\":\"SC-001\"}", valueCaptor.getValue().getParamValuesJson());
    }

    @Test
    @DisplayName("create - 同 schema 下参数编码重复时拒绝")
    void create_rejectsDuplicateParameterCode() {
        ProcessParameterSchema existingSchema = new ProcessParameterSchema();
        existingSchema.setId(11L);
        existingSchema.setSchemaCode("MACHINING_PROGRAM");
        when(schemaMapper.selectOne(any())).thenReturn(existingSchema);
        when(valueMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> storeService.create(
                ProcessParameterSchemaDefinition.builder()
                        .schemaCode("MACHINING_PROGRAM")
                        .schemaName("机械加工程序")
                        .processType("MACHINING")
                        .fieldDefinitionsJson("[{\"name\":\"gCode\"}]")
                        .build(),
                ProcessParameterUpsertCommand.builder()
                        .parameterCode("G-100")
                        .parameterName("叶轮")
                        .paramValuesJson("{\"gCode\":\"G-100\"}")
                        .build()
        ));

        assertEquals("参数编码已存在: G-100", ex.getMessage());
    }
}
