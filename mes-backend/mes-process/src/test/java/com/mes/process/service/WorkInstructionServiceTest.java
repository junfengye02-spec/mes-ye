package com.mes.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.WorkInstructionDTO;
import com.mes.process.domain.dto.WorkInstructionPersonDTO;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.domain.entity.WorkInstruction;
import com.mes.process.domain.entity.WorkInstructionPerson;
import com.mes.process.domain.query.WorkInstructionQuery;
import com.mes.process.domain.vo.WorkInstructionVO;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.process.mapper.WorkInstructionMapper;
import com.mes.process.mapper.WorkInstructionPersonMapper;
import com.mes.process.service.impl.WorkInstructionServiceImpl;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkInstructionServiceImpl")
class WorkInstructionServiceTest {

    @Mock
    private WorkInstructionMapper workInstructionMapper;

    @Mock
    private WorkInstructionPersonMapper personMapper;

    @Mock
    private ProcessInfoMapper processInfoMapper;

    private WorkInstructionServiceImpl workInstructionService;

    @BeforeEach
    void setUp() {
        workInstructionService = spy(new WorkInstructionServiceImpl(personMapper, processInfoMapper));
        ReflectionTestUtils.setField(workInstructionService, "baseMapper", workInstructionMapper);
    }

    @Test
    @DisplayName("create - 保存扩展后的指导书字段并写入关联责任人")
    void create_savesExpandedFieldsAndPersons() {
        WorkInstructionDTO dto = new WorkInstructionDTO();
        dto.setInstructionCode("WI-100");
        dto.setInstructionName("喷涂标准作业指导书");
        dto.setProcessId(101L);
        dto.setVersion("A.1");
        dto.setContent("1. 清理表面 2. 预热");
        dto.setRemark("需双人复核");
        WorkInstructionPersonDTO personDTO = new WorkInstructionPersonDTO();
        personDTO.setPersonCode("P001");
        personDTO.setPersonName("张工");
        dto.setPersons(List.of(personDTO));

        ProcessInfo processInfo = new ProcessInfo();
        processInfo.setId(101L);
        processInfo.setProcessName("喷涂");

        when(workInstructionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(processInfoMapper.selectById(101L)).thenReturn(processInfo);
        when(workInstructionMapper.insert(any(WorkInstruction.class))).thenAnswer(invocation -> {
            WorkInstruction entity = invocation.getArgument(0);
            entity.setId(500L);
            return 1;
        });

        Long id = workInstructionService.create(dto);

        assertEquals(500L, id);

        ArgumentCaptor<WorkInstruction> instructionCaptor = ArgumentCaptor.forClass(WorkInstruction.class);
        verify(workInstructionMapper).insert(instructionCaptor.capture());
        assertEquals("WI-100", instructionCaptor.getValue().getInstructionCode());
        assertEquals("喷涂标准作业指导书", instructionCaptor.getValue().getInstructionName());
        assertEquals(101L, instructionCaptor.getValue().getProcessId());
        assertEquals("A.1", instructionCaptor.getValue().getVersion());
        assertEquals("1. 清理表面 2. 预热", instructionCaptor.getValue().getContent());
        assertEquals("需双人复核", instructionCaptor.getValue().getRemark());

        ArgumentCaptor<WorkInstructionPerson> personCaptor = ArgumentCaptor.forClass(WorkInstructionPerson.class);
        verify(personMapper).insert(personCaptor.capture());
        assertEquals(500L, personCaptor.getValue().getInstructionId());
        assertEquals("张工", personCaptor.getValue().getPersonName());
    }

    @Test
    @DisplayName("getDetail - 返回扩展字段并补充工序名称")
    void getDetail_returnsExpandedFieldsAndProcessName() {
        WorkInstruction entity = new WorkInstruction();
        entity.setId(11L);
        entity.setInstructionCode("WI-200");
        entity.setInstructionName("机加检验指导书");
        entity.setProcessId(202L);
        entity.setVersion("B.2");
        entity.setContent("首件确认后批量加工");
        entity.setRemark("抽检频率 30 分钟");

        ProcessInfo processInfo = new ProcessInfo();
        processInfo.setId(202L);
        processInfo.setProcessName("数控加工");

        WorkInstructionPerson person = new WorkInstructionPerson();
        person.setId(9L);
        person.setInstructionId(11L);
        person.setPersonName("李工");

        when(workInstructionMapper.selectById(11L)).thenReturn(entity);
        when(processInfoMapper.selectById(202L)).thenReturn(processInfo);
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(person));

        WorkInstructionVO detail = workInstructionService.getDetail(11L);

        assertEquals("WI-200", detail.getInstructionCode());
        assertEquals("机加检验指导书", detail.getInstructionName());
        assertEquals(202L, detail.getProcessId());
        assertEquals("数控加工", detail.getProcessName());
        assertEquals("B.2", detail.getVersion());
        assertEquals("首件确认后批量加工", detail.getContent());
        assertEquals("抽检频率 30 分钟", detail.getRemark());
        assertEquals(1, detail.getPersons().size());
        assertEquals("李工", detail.getPersons().get(0).getPersonName());
    }

    @Test
    @DisplayName("page - 列表结果补充工序名称")
    void page_enrichesProcessName() {
        WorkInstruction entity = new WorkInstruction();
        entity.setId(21L);
        entity.setInstructionCode("WI-300");
        entity.setInstructionName("喷砂指导书");
        entity.setProcessId(303L);
        entity.setVersion("C.0");

        Page<WorkInstruction> page = new Page<>(1, 20);
        page.setRecords(List.of(entity));
        page.setTotal(1L);

        ProcessInfo processInfo = new ProcessInfo();
        processInfo.setId(303L);
        processInfo.setProcessName("喷砂");

        doReturn(page).when(workInstructionService).page(any(Page.class), any(LambdaQueryWrapper.class));
        when(processInfoMapper.selectBatchIds(List.of(303L))).thenReturn(List.of(processInfo));

        WorkInstructionQuery query = new WorkInstructionQuery();
        query.setInstructionName("喷砂");
        query.setPageNum(1);
        query.setPageSize(20);

        PageResult<WorkInstructionVO> result = workInstructionService.page(query);

        assertEquals(1L, result.getTotal());
        assertEquals("喷砂指导书", result.getList().get(0).getInstructionName());
        assertEquals("喷砂", result.getList().get(0).getProcessName());
        assertEquals("C.0", result.getList().get(0).getVersion());
    }
}
