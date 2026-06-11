package com.mes.process.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.process.domain.dto.MachiningProgramDTO;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.domain.entity.MachiningProgram;
import com.mes.process.domain.query.MachiningProgramQuery;
import com.mes.process.domain.vo.MachiningProgramVO;
import com.mes.process.mapper.MachiningProgramMapper;
import com.mes.process.service.IMachiningProgramService;
import com.mes.process.service.support.ProcessParameterSchemaDefinition;
import com.mes.process.service.support.ProcessParameterSearch;
import com.mes.process.service.support.ProcessParameterStoreService;
import com.mes.process.service.support.ProcessParameterUpsertCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 机械加工程序 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MachiningProgramServiceImpl extends ServiceImpl<MachiningProgramMapper, MachiningProgram>
        implements IMachiningProgramService {

    private final ProcessParameterStoreService processParameterStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResult<MachiningProgramVO> page(MachiningProgramQuery query) {
        ProcessParameterSearch search = ProcessParameterSearch.builder()
                .codeKeyword(query.getGCode())
                .nameKeyword(query.getProductName())
                .build();
        PageResult<ProcessParameterValue> page = processParameterStoreService.page(schemaDefinition(), query, search);
        List<MachiningProgramVO> voList = page.getList().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public MachiningProgramVO getDetail(Long id) {
        return toVO(processParameterStoreService.getRequired(id, schemaDefinition()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MachiningProgramDTO dto) {
        MachiningProgram entity = new MachiningProgram();
        BeanUtils.copyProperties(dto, entity);
        Long id = processParameterStoreService.create(schemaDefinition(), toCommand(entity));

        log.info("新增机械加工程序: {}", entity.getGCode());
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MachiningProgramDTO dto) {
        ProcessParameterValue existingValue = processParameterStoreService.getRequired(id, schemaDefinition());
        MachiningProgram existing = parse(existingValue.getParamValuesJson(), MachiningProgram.class);
        BeanUtils.copyProperties(dto, existing);
        processParameterStoreService.update(id, schemaDefinition(), toCommand(existing));

        log.info("修改机械加工程序: {}", existing.getGCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MachiningProgramVO entity = getDetail(id);
        processParameterStoreService.delete(id, schemaDefinition());
        log.info("删除机械加工程序: {}", entity.getGCode());
    }

    private ProcessParameterSchemaDefinition schemaDefinition() {
        return ProcessParameterSchemaDefinition.builder()
                .schemaCode("MACHINING_PROGRAM")
                .schemaName("机械加工程序")
                .processType("MACHINING")
                .fieldDefinitionsJson(writeJson(List.of(
                        field("gCode", "string", true),
                        field("programTable", "string", false),
                        field("productName", "string", false)
                )))
                .build();
    }

    private ProcessParameterUpsertCommand toCommand(MachiningProgram entity) {
        return ProcessParameterUpsertCommand.builder()
                .parameterCode(entity.getGCode())
                .parameterName(entity.getProductName())
                .processType("MACHINING")
                .searchText(String.join(" ",
                        Stream.of(entity.getProductName(), entity.getGCode())
                                .filter(org.springframework.util.StringUtils::hasText)
                                .toList()))
                .paramValuesJson(writeJson(toPayload(entity)))
                .build();
    }

    private MachiningProgramVO toVO(ProcessParameterValue value) {
        MachiningProgram entity = parse(value.getParamValuesJson(), MachiningProgram.class);
        MachiningProgramVO vo = new MachiningProgramVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setId(value.getId());
        vo.setCreatedBy(value.getCreatedBy());
        vo.setCreatedTime(value.getCreatedTime());
        vo.setUpdatedBy(value.getUpdatedBy());
        vo.setUpdatedTime(value.getUpdatedTime());
        return vo;
    }

    private Map<String, Object> toPayload(MachiningProgram entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gCode", entity.getGCode());
        payload.put("programTable", entity.getProgramTable());
        payload.put("productName", entity.getProductName());
        return payload;
    }

    private Map<String, Object> field(String name, String type, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type", type);
        field.put("required", required);
        return field;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL, "工艺参数JSON序列化失败");
        }
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL, "工艺参数JSON解析失败");
        }
    }
}
