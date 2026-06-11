package com.mes.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.process.domain.dto.InstructionDTO;
import com.mes.process.domain.entity.Instruction;
import com.mes.process.domain.entity.InstructionSerial;
import com.mes.process.domain.entity.InstructionStage;
import com.mes.process.domain.vo.InstructionVO;
import com.mes.process.mapper.InstructionFlowLogMapper;
import com.mes.process.mapper.InstructionMapper;
import com.mes.process.mapper.InstructionSerialMapper;
import com.mes.process.mapper.InstructionStageMapper;
import com.mes.process.service.impl.InstructionServiceImpl;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InstructionServiceImpl")
class InstructionServiceTest {

    @Mock
    private InstructionMapper instructionMapper;

    @Mock
    private InstructionStageMapper stageMapper;

    @Mock
    private InstructionSerialMapper serialMapper;

    @Mock
    private InstructionFlowLogMapper flowLogMapper;

    private InstructionServiceImpl instructionService;

    @BeforeEach
    void setUp() {
        instructionService = spy(new InstructionServiceImpl(stageMapper, serialMapper, flowLogMapper));
        ReflectionTestUtils.setField(instructionService, "baseMapper", instructionMapper);
    }

    @Test
    @DisplayName("create - 将维修专属字段写入扩展属性 JSON")
    void create_movesRepairSpecificFieldsIntoExtensionData() {
        InstructionDTO dto = new InstructionDTO();
        dto.setInstructionNo("INS-100");
        dto.setProjectNo("PJ-1");
        dto.setNewOrRepairType("维修");
        dto.setWorkInstructionId(300L);
        dto.setGtType("GT-A");
        dto.setRepairGuideDrawing("RG-9");
        Map<String, Object> extensionData = new LinkedHashMap<>();
        extensionData.put("inspectionMode", "visual");
        dto.setExtensionData(extensionData);

        when(instructionMapper.insert(any(Instruction.class))).thenAnswer(invocation -> {
            Instruction entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });

        Long id = instructionService.create(dto);

        assertEquals(88L, id);

        ArgumentCaptor<Instruction> instructionCaptor = ArgumentCaptor.forClass(Instruction.class);
        verify(instructionMapper).insert(instructionCaptor.capture());
        Instruction saved = instructionCaptor.getValue();
        assertEquals("INS-100", saved.getInstructionNo());
        assertEquals(300L, saved.getWorkInstructionId());
        assertNotNull(saved.getExtensionDataJson());
        assertTrue(saved.getExtensionDataJson().contains("\"gtType\":\"GT-A\""));
        assertTrue(saved.getExtensionDataJson().contains("\"repairGuideDrawing\":\"RG-9\""));
        assertTrue(saved.getExtensionDataJson().contains("\"inspectionMode\":\"visual\""));
    }

    @Test
    @DisplayName("getDetail - 从扩展属性 JSON 还原兼容字段")
    void getDetail_readsRepairSpecificFieldsFromExtensionData() {
        Instruction entity = new Instruction();
        entity.setId(9L);
        entity.setInstructionNo("INS-200");
        entity.setVersion("V2");
        entity.setWorkInstructionId(301L);
        entity.setExtensionDataJson("{\"gtType\":\"GT-B\",\"repairGuideDrawing\":\"RG-10\",\"inspectionMode\":\"visual\"}");

        when(instructionMapper.selectById(9L)).thenReturn(entity);
        when(stageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.<InstructionStage>of());
        when(serialMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.<InstructionSerial>of());

        InstructionVO detail = instructionService.getDetail(9L);

        assertEquals("INS-200", detail.getInstructionNo());
        assertEquals(301L, detail.getWorkInstructionId());
        assertEquals("GT-B", detail.getGtType());
        assertEquals("RG-10", detail.getRepairGuideDrawing());
        assertNotNull(detail.getExtensionData());
        assertEquals("visual", detail.getExtensionData().get("inspectionMode"));
    }
}
