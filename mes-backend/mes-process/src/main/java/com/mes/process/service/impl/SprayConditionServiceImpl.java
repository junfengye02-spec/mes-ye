package com.mes.process.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.process.domain.dto.SprayConditionDTO;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.domain.entity.SprayCondition;
import com.mes.process.domain.query.SprayConditionQuery;
import com.mes.process.domain.vo.SprayConditionVO;
import com.mes.process.mapper.SprayConditionMapper;
import com.mes.process.service.ISprayConditionService;
import com.mes.process.service.support.ProcessParameterSchemaDefinition;
import com.mes.process.service.support.ProcessParameterSearch;
import com.mes.process.service.support.ProcessParameterStoreService;
import com.mes.process.service.support.ProcessParameterUpsertCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 喷涂条件 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SprayConditionServiceImpl extends ServiceImpl<SprayConditionMapper, SprayCondition>
        implements ISprayConditionService {

    private final ProcessParameterStoreService processParameterStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public PageResult<SprayConditionVO> page(SprayConditionQuery query) {
        ProcessParameterSearch search = ProcessParameterSearch.builder()
                .codeKeyword(query.getConditionNo())
                .searchKeywords(Stream.of(query.getSprayGunModel(), query.getEquipment())
                        .filter(org.springframework.util.StringUtils::hasText)
                        .toList())
                .build();

        PageResult<ProcessParameterValue> page = processParameterStoreService.page(schemaDefinition(), query, search);
        List<SprayConditionVO> voList = page.getList().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public SprayConditionVO getDetail(Long id) {
        return toVO(processParameterStoreService.getRequired(id, schemaDefinition()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SprayConditionDTO dto) {
        SprayCondition entity = new SprayCondition();
        BeanUtils.copyProperties(dto, entity);

        Long id = processParameterStoreService.create(schemaDefinition(), toCommand(entity));
        log.info("新增喷涂条件: {}", entity.getConditionNo());
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SprayConditionDTO dto) {
        ProcessParameterValue existingValue = processParameterStoreService.getRequired(id, schemaDefinition());
        SprayCondition existing = parse(existingValue.getParamValuesJson(), SprayCondition.class);
        BeanUtils.copyProperties(dto, existing);
        processParameterStoreService.update(id, schemaDefinition(), toCommand(existing));

        log.info("修改喷涂条件: {}", existing.getConditionNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SprayConditionVO entity = getDetail(id);
        processParameterStoreService.delete(id, schemaDefinition());
        log.info("删除喷涂条件: {}", entity.getConditionNo());
    }

    private ProcessParameterSchemaDefinition schemaDefinition() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("conditionNo", "string", true));
        fields.add(field("sprayGunModel", "string", false));
        fields.add(field("equipment", "string", false));
        fields.add(field("powderFeedRate", "decimal", false));
        fields.add(field("sprayDistance", "decimal", false));
        return ProcessParameterSchemaDefinition.builder()
                .schemaCode("SPRAY_CONDITION")
                .schemaName("喷涂条件")
                .processType("SPRAY")
                .fieldDefinitionsJson(writeJson(fields))
                .build();
    }

    private ProcessParameterUpsertCommand toCommand(SprayCondition entity) {
        return ProcessParameterUpsertCommand.builder()
                .parameterCode(entity.getConditionNo())
                .parameterName(entity.getConditionNo())
                .processType("SPRAY")
                .searchText(String.join(" ",
                        Stream.of(entity.getSprayGunModel(), entity.getEquipment(), entity.getPowderType())
                                .filter(org.springframework.util.StringUtils::hasText)
                                .toList()))
                .paramValuesJson(writeJson(toPayload(entity)))
                .build();
    }

    private SprayConditionVO toVO(ProcessParameterValue value) {
        SprayCondition entity = parse(value.getParamValuesJson(), SprayCondition.class);
        SprayConditionVO vo = new SprayConditionVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setId(value.getId());
        vo.setCreatedBy(value.getCreatedBy());
        vo.setCreatedTime(value.getCreatedTime());
        vo.setUpdatedBy(value.getUpdatedBy());
        vo.setUpdatedTime(value.getUpdatedTime());
        return vo;
    }

    private Map<String, Object> toPayload(SprayCondition entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conditionNo", entity.getConditionNo());
        payload.put("ministerApprover", entity.getMinisterApprover());
        payload.put("ministerApproveTime", entity.getMinisterApproveTime());
        payload.put("sectionApprover", entity.getSectionApprover());
        payload.put("sectionApproveTime", entity.getSectionApproveTime());
        payload.put("leaderApprover", entity.getLeaderApprover());
        payload.put("leaderApproveTime", entity.getLeaderApproveTime());
        payload.put("powderFeedRate", entity.getPowderFeedRate());
        payload.put("sprayDistance", entity.getSprayDistance());
        payload.put("sprayGunModel", entity.getSprayGunModel());
        payload.put("faiReport", entity.getFaiReport());
        payload.put("faiGuide", entity.getFaiGuide());
        payload.put("powderFeeder", entity.getPowderFeeder());
        payload.put("powderFeederSpeed", entity.getPowderFeederSpeed());
        payload.put("oxygenScfh", entity.getOxygenScfh());
        payload.put("keroseneGph", entity.getKeroseneGph());
        payload.put("combustionPressure", entity.getCombustionPressure());
        payload.put("carrierGas", entity.getCarrierGas());
        payload.put("equipment", entity.getEquipment());
        payload.put("powderType", entity.getPowderType());
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
