package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.InstructionDTO;
import com.mes.process.domain.dto.InstructionSerialDTO;
import com.mes.process.domain.dto.InstructionStageDTO;
import com.mes.process.domain.entity.Instruction;
import com.mes.process.domain.entity.InstructionFlowLog;
import com.mes.process.domain.entity.InstructionSerial;
import com.mes.process.domain.entity.InstructionStage;
import com.mes.process.domain.query.InstructionQuery;
import com.mes.process.domain.vo.*;
import com.mes.process.enums.InstructionStatus;
import com.mes.process.mapper.InstructionFlowLogMapper;
import com.mes.process.mapper.InstructionMapper;
import com.mes.process.mapper.InstructionSerialMapper;
import com.mes.process.mapper.InstructionStageMapper;
import com.mes.process.service.IInstructionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指示书 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstructionServiceImpl extends ServiceImpl<InstructionMapper, Instruction>
        implements IInstructionService {

    private static final String EXT_KEY_GT_TYPE = "gtType";
    private static final String EXT_KEY_REPAIR_GUIDE_DRAWING = "repairGuideDrawing";

    private final InstructionStageMapper stageMapper;
    private final InstructionSerialMapper serialMapper;
    private final InstructionFlowLogMapper flowLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public PageResult<InstructionVO> page(InstructionQuery query) {
        LambdaQueryWrapper<Instruction> wrapper = new LambdaQueryWrapper<Instruction>()
                .like(StringUtils.hasText(query.getInstructionNo()),
                        Instruction::getInstructionNo, query.getInstructionNo())
                .eq(StringUtils.hasText(query.getVersion()),
                        Instruction::getVersion, query.getVersion())
                .eq(StringUtils.hasText(query.getStatus()),
                        Instruction::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        Instruction::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getProductType()),
                        Instruction::getProductType, query.getProductType())
                .orderByDesc(Instruction::getCreatedTime);

        Page<Instruction> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<InstructionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public InstructionVO getDetail(Long id) {
        Instruction entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        InstructionVO vo = toVO(entity);

        // 查询阶段子表
        List<InstructionStage> stages = stageMapper.selectList(
                new LambdaQueryWrapper<InstructionStage>()
                        .eq(InstructionStage::getInstructionId, id));
        vo.setStages(stages.stream().map(this::toStageVO).toList());

        // 查询序列号子表
        List<InstructionSerial> serials = serialMapper.selectList(
                new LambdaQueryWrapper<InstructionSerial>()
                        .eq(InstructionSerial::getInstructionId, id));
        vo.setSerials(serials.stream().map(this::toSerialVO).toList());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InstructionDTO dto) {
        // 新建指示书，版本为 V1，状态为 DRAFT
        Instruction entity = new Instruction();
        applyDto(dto, entity);
        entity.setVersion("V1");
        entity.setStatus(InstructionStatus.DRAFT.getCode());
        save(entity);

        // 保存阶段子表
        saveStages(entity.getId(), dto.getStages());
        // 保存序列号子表
        saveSerials(entity.getId(), dto.getSerials());
        // 记录流程日志
        saveFlowLog(entity.getId(), "创建", "创建指示书 " + entity.getInstructionNo() + " V1");

        log.info("新增指示书: {} {}", entity.getInstructionNo(), entity.getVersion());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, InstructionDTO dto) {
        Instruction existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        // 保留版本和状态，不允许通过 update 修改
        String version = existing.getVersion();
        String status = existing.getStatus();

        applyDto(dto, existing);
        existing.setId(id);
        existing.setVersion(version);
        existing.setStatus(status);
        updateById(existing);

        // 先删后插更新子表
        stageMapper.delete(new LambdaQueryWrapper<InstructionStage>()
                .eq(InstructionStage::getInstructionId, id));
        saveStages(id, dto.getStages());

        serialMapper.delete(new LambdaQueryWrapper<InstructionSerial>()
                .eq(InstructionSerial::getInstructionId, id));
        saveSerials(id, dto.getSerials());

        saveFlowLog(id, "修改", "修改指示书 " + existing.getInstructionNo() + " " + version);
        log.info("修改指示书: {} {}", existing.getInstructionNo(), version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Instruction entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 级联删除子表
        stageMapper.delete(new LambdaQueryWrapper<InstructionStage>()
                .eq(InstructionStage::getInstructionId, id));
        serialMapper.delete(new LambdaQueryWrapper<InstructionSerial>()
                .eq(InstructionSerial::getInstructionId, id));
        removeById(id);

        saveFlowLog(id, "删除", "删除指示书 " + entity.getInstructionNo() + " " + entity.getVersion());
        log.info("删除指示书: {} {}", entity.getInstructionNo(), entity.getVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upgrade(Long id) {
        Instruction source = getById(id);
        AssertUtil.notNull(source, ResultCode.DATA_NOT_EXIST);

        // 计算新版本号
        String newVersion = incrementVersion(source.getVersion());

        // 检查新版本是否已存在
        long existCount = count(new LambdaQueryWrapper<Instruction>()
                .eq(Instruction::getInstructionNo, source.getInstructionNo())
                .eq(Instruction::getVersion, newVersion));
        AssertUtil.isFalse(existCount > 0, "版本 " + newVersion + " 已存在");

        // 深拷贝主表
        Instruction newInstruction = new Instruction();
        BeanUtils.copyProperties(source, newInstruction);
        newInstruction.setId(null);
        newInstruction.setVersion(newVersion);
        newInstruction.setStatus(InstructionStatus.DRAFT.getCode());
        newInstruction.setUpgradeFromId(source.getId());
        newInstruction.setCreatedBy(null);
        newInstruction.setCreatedTime(null);
        newInstruction.setUpdatedBy(null);
        newInstruction.setUpdatedTime(null);
        newInstruction.setDeleted(null);
        save(newInstruction);

        // 深拷贝阶段子表
        List<InstructionStage> stages = stageMapper.selectList(
                new LambdaQueryWrapper<InstructionStage>()
                        .eq(InstructionStage::getInstructionId, id));
        for (InstructionStage stage : stages) {
            InstructionStage newStage = new InstructionStage();
            BeanUtils.copyProperties(stage, newStage);
            newStage.setId(null);
            newStage.setInstructionId(newInstruction.getId());
            newStage.setCreatedTime(null);
            newStage.setUpdatedTime(null);
            stageMapper.insert(newStage);
        }

        // 深拷贝序列号子表
        List<InstructionSerial> serials = serialMapper.selectList(
                new LambdaQueryWrapper<InstructionSerial>()
                        .eq(InstructionSerial::getInstructionId, id));
        for (InstructionSerial serial : serials) {
            InstructionSerial newSerial = new InstructionSerial();
            BeanUtils.copyProperties(serial, newSerial);
            newSerial.setId(null);
            newSerial.setInstructionId(newInstruction.getId());
            serialMapper.insert(newSerial);
        }

        // 旧版本标记为 SUPERSEDED
        source.setStatus(InstructionStatus.SUPERSEDED.getCode());
        updateById(source);

        // 记录流程日志
        saveFlowLog(newInstruction.getId(), "升级",
                "从 " + source.getVersion() + " 升级到 " + newVersion);
        saveFlowLog(id, "被替代",
                "被版本 " + newVersion + " 替代");

        log.info("指示书版本升级: {} {} -> {}", source.getInstructionNo(), source.getVersion(), newVersion);
        return newInstruction.getId();
    }

    @Override
    public PageResult<InstructionFlowLogVO> getFlowLogs(Long id, PageQuery query) {
        Page<InstructionFlowLog> page = flowLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<InstructionFlowLog>()
                        .eq(InstructionFlowLog::getInstructionId, id)
                        .orderByDesc(InstructionFlowLog::getOperatedTime)
        );

        List<InstructionFlowLogVO> voList = page.getRecords().stream()
                .map(this::toFlowLogVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    // ==================== 私有方法 ====================

    private String incrementVersion(String currentVersion) {
        // V1 -> V2, V2 -> V3, etc.
        if (currentVersion != null && currentVersion.startsWith("V")) {
            try {
                int num = Integer.parseInt(currentVersion.substring(1));
                return "V" + (num + 1);
            } catch (NumberFormatException e) {
                // fallback
            }
        }
        return currentVersion + ".1";
    }

    private void saveStages(Long instructionId, List<InstructionStageDTO> stageDTOs) {
        if (CollectionUtils.isEmpty(stageDTOs)) {
            return;
        }
        for (InstructionStageDTO dto : stageDTOs) {
            InstructionStage stage = new InstructionStage();
            BeanUtils.copyProperties(dto, stage);
            stage.setInstructionId(instructionId);
            stage.setCreatedTime(LocalDateTime.now());
            stage.setUpdatedTime(LocalDateTime.now());
            stageMapper.insert(stage);
        }
    }

    private void saveSerials(Long instructionId, List<InstructionSerialDTO> serialDTOs) {
        if (CollectionUtils.isEmpty(serialDTOs)) {
            return;
        }
        for (InstructionSerialDTO dto : serialDTOs) {
            InstructionSerial serial = new InstructionSerial();
            BeanUtils.copyProperties(dto, serial);
            serial.setInstructionId(instructionId);
            serialMapper.insert(serial);
        }
    }

    private void saveFlowLog(Long instructionId, String action, String detail) {
        InstructionFlowLog flowLog = new InstructionFlowLog();
        flowLog.setInstructionId(instructionId);
        flowLog.setAction(action);
        flowLog.setOperator("system"); // TODO: 从 SecurityContext 获取当前用户
        flowLog.setOperatedTime(LocalDateTime.now());
        flowLog.setDetail(detail);
        flowLogMapper.insert(flowLog);
    }

    private InstructionVO toVO(Instruction entity) {
        InstructionVO vo = new InstructionVO();
        BeanUtils.copyProperties(entity, vo);
        Map<String, Object> extensionData = parseExtensionData(entity.getExtensionDataJson());
        vo.setExtensionData(extensionData);
        vo.setGtType(readText(extensionData, EXT_KEY_GT_TYPE));
        vo.setRepairGuideDrawing(readText(extensionData, EXT_KEY_REPAIR_GUIDE_DRAWING));
        return vo;
    }

    private void applyDto(InstructionDTO dto, Instruction entity) {
        BeanUtils.copyProperties(dto, entity, "extensionData", "gtType", "repairGuideDrawing");
        entity.setExtensionDataJson(writeExtensionData(dto));
    }

    private String writeExtensionData(InstructionDTO dto) {
        Map<String, Object> extensionData = new LinkedHashMap<>();
        if (dto.getExtensionData() != null) {
            extensionData.putAll(dto.getExtensionData());
        }
        if (dto.getGtType() != null) {
            extensionData.put(EXT_KEY_GT_TYPE, dto.getGtType());
        }
        if (dto.getRepairGuideDrawing() != null) {
            extensionData.put(EXT_KEY_REPAIR_GUIDE_DRAWING, dto.getRepairGuideDrawing());
        }
        if (extensionData.isEmpty()) {
            return null;
        }
        return writeJson(extensionData);
    }

    private Map<String, Object> parseExtensionData(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL, "指示书扩展属性JSON解析失败");
        }
    }

    private String readText(Map<String, Object> extensionData, String key) {
        Object value = extensionData.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL, "指示书扩展属性JSON序列化失败");
        }
    }

    private InstructionStageVO toStageVO(InstructionStage entity) {
        InstructionStageVO vo = new InstructionStageVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private InstructionSerialVO toSerialVO(InstructionSerial entity) {
        InstructionSerialVO vo = new InstructionSerialVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private InstructionFlowLogVO toFlowLogVO(InstructionFlowLog entity) {
        InstructionFlowLogVO vo = new InstructionFlowLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
