package com.mes.process.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.entity.ProcessParameterSchema;
import com.mes.process.domain.entity.ProcessParameterValue;
import com.mes.process.mapper.ProcessParameterSchemaMapper;
import com.mes.process.mapper.ProcessParameterValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 通用工艺参数持久化服务
 */
@Service
@RequiredArgsConstructor
public class ProcessParameterStoreService {

    private final ProcessParameterSchemaMapper schemaMapper;
    private final ProcessParameterValueMapper valueMapper;

    public PageResult<ProcessParameterValue> page(
            ProcessParameterSchemaDefinition definition,
            PageQuery query,
            ProcessParameterSearch search) {
        ProcessParameterSchema schema = ensureSchema(definition);

        LambdaQueryWrapper<ProcessParameterValue> wrapper = new LambdaQueryWrapper<ProcessParameterValue>()
                .eq(ProcessParameterValue::getSchemaId, schema.getId())
                .like(StringUtils.hasText(search.getCodeKeyword()),
                        ProcessParameterValue::getParameterCode, search.getCodeKeyword())
                .like(StringUtils.hasText(search.getNameKeyword()),
                        ProcessParameterValue::getParameterName, search.getNameKeyword())
                .eq(StringUtils.hasText(search.getStatus()),
                        ProcessParameterValue::getStatus, search.getStatus())
                .orderByDesc(ProcessParameterValue::getCreatedTime);

        if (search.getSearchKeywords() != null) {
            for (String keyword : search.getSearchKeywords()) {
                wrapper.like(StringUtils.hasText(keyword), ProcessParameterValue::getSearchText, keyword);
            }
        }

        Page<ProcessParameterValue> page = valueMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    public ProcessParameterValue getRequired(Long id, ProcessParameterSchemaDefinition definition) {
        ProcessParameterSchema schema = ensureSchema(definition);
        ProcessParameterValue value = valueMapper.selectById(id);
        AssertUtil.notNull(value, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(schema.getId().equals(value.getSchemaId()), "工艺参数模板不匹配");
        return value;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProcessParameterSchemaDefinition definition, ProcessParameterUpsertCommand command) {
        ProcessParameterSchema schema = ensureSchema(definition);
        checkCodeUnique(schema.getId(), command.getParameterCode(), null);

        ProcessParameterValue value = new ProcessParameterValue();
        applyCommand(value, schema, definition, command);
        valueMapper.insert(value);
        return value.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProcessParameterSchemaDefinition definition, ProcessParameterUpsertCommand command) {
        ProcessParameterSchema schema = ensureSchema(definition);
        ProcessParameterValue existing = getRequired(id, definition);
        checkCodeUnique(schema.getId(), command.getParameterCode(), id);

        applyCommand(existing, schema, definition, command);
        existing.setId(id);
        valueMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, ProcessParameterSchemaDefinition definition) {
        getRequired(id, definition);
        valueMapper.deleteById(id);
    }

    private void applyCommand(
            ProcessParameterValue value,
            ProcessParameterSchema schema,
            ProcessParameterSchemaDefinition definition,
            ProcessParameterUpsertCommand command) {
        value.setSchemaId(schema.getId());
        value.setParameterCode(command.getParameterCode());
        value.setParameterName(command.getParameterName());
        value.setProcessInfoId(command.getProcessInfoId());
        value.setProcessType(StringUtils.hasText(command.getProcessType())
                ? command.getProcessType() : definition.getProcessType());
        value.setStatus(command.getStatus());
        value.setSearchText(command.getSearchText());
        value.setParamValuesJson(command.getParamValuesJson());
    }

    private void checkCodeUnique(Long schemaId, String parameterCode, Long excludeId) {
        LambdaQueryWrapper<ProcessParameterValue> wrapper = new LambdaQueryWrapper<ProcessParameterValue>()
                .eq(ProcessParameterValue::getSchemaId, schemaId)
                .eq(ProcessParameterValue::getParameterCode, parameterCode)
                .ne(excludeId != null, ProcessParameterValue::getId, excludeId);
        if (valueMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST.getCode(), "参数编码已存在: " + parameterCode);
        }
    }

    private ProcessParameterSchema ensureSchema(ProcessParameterSchemaDefinition definition) {
        LambdaQueryWrapper<ProcessParameterSchema> wrapper = new LambdaQueryWrapper<ProcessParameterSchema>()
                .eq(ProcessParameterSchema::getSchemaCode, definition.getSchemaCode());
        ProcessParameterSchema schema = schemaMapper.selectOne(wrapper);
        if (schema == null) {
            schema = new ProcessParameterSchema();
            schema.setSchemaCode(definition.getSchemaCode());
            schema.setSchemaName(definition.getSchemaName());
            schema.setProcessType(definition.getProcessType());
            schema.setFieldDefinitionsJson(definition.getFieldDefinitionsJson());
            schemaMapper.insert(schema);
            return schema;
        }

        boolean changed = false;
        if (!java.util.Objects.equals(schema.getSchemaName(), definition.getSchemaName())) {
            schema.setSchemaName(definition.getSchemaName());
            changed = true;
        }
        if (!java.util.Objects.equals(schema.getProcessType(), definition.getProcessType())) {
            schema.setProcessType(definition.getProcessType());
            changed = true;
        }
        if (!java.util.Objects.equals(schema.getFieldDefinitionsJson(), definition.getFieldDefinitionsJson())) {
            schema.setFieldDefinitionsJson(definition.getFieldDefinitionsJson());
            changed = true;
        }
        if (changed) {
            schemaMapper.updateById(schema);
        }
        return schema;
    }
}
