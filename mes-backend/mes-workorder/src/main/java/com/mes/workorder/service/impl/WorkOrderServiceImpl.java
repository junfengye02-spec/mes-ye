package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.event.WorkOrderCompletedEvent;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.workorder.domain.dto.*;
import com.mes.workorder.domain.entity.*;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.*;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.mapper.*;
import com.mes.workorder.event.WorkOrderReleasedEvent;
import com.mes.workorder.service.IWorkOrderService;
import com.mes.common.event.ApsSyncEvent;
import com.mes.workorder.service.IWorkOrderStatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工单 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder>
        implements IWorkOrderService {

    private final WorkOrderTaskMapper taskMapper;
    private final WorkOrderInputMaterialMapper inputMaterialMapper;
    private final WorkOrderOutputMaterialMapper outputMaterialMapper;
    private final WorkOrderQualityItemMapper qualityItemMapper;
    private final WorkOrderConstraintMapper constraintMapper;
    private final WorkOrderSupplyPlanMapper supplyPlanMapper;
    private final WorkOrderAttachmentMapper attachmentMapper;
    private final IWorkOrderStatusLogService statusLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<WorkOrderVO> page(WorkOrderQuery query) {
        String businessType = resolveBusinessType(query.getBusinessType(), query.getWorkType());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .like(StringUtils.hasText(query.getWorkOrderNo()),
                        WorkOrder::getWorkOrderNo, query.getWorkOrderNo())
                .like(StringUtils.hasText(query.getOrderNo()),
                        WorkOrder::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getProductCode()),
                        WorkOrder::getProductCode, query.getProductCode())
                .like(StringUtils.hasText(query.getProductName()),
                        WorkOrder::getProductName, query.getProductName())
                .eq(StringUtils.hasText(query.getStatus()),
                        WorkOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(businessType),
                        WorkOrder::getBusinessType, businessType)
                .eq(StringUtils.hasText(query.getMachineModel()),
                        WorkOrder::getMachineModel, query.getMachineModel())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        WorkOrder::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getOrderPlanNo()),
                        WorkOrder::getOrderPlanNo, query.getOrderPlanNo())
                .orderByDesc(WorkOrder::getCreatedTime);

        Page<WorkOrder> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<WorkOrderVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public WorkOrderVO getDetail(Long id) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        WorkOrderVO vo = toVO(entity);

        // 查询所有子表
        vo.setTasks(queryTasks(id));
        vo.setInputMaterials(queryInputMaterials(id));
        vo.setOutputMaterials(queryOutputMaterials(id));
        vo.setQualityItems(queryQualityItems(id));
        vo.setConstraints(queryConstraints(id));
        vo.setSupplyPlans(querySupplyPlans(id));
        vo.setAttachments(queryAttachments(id));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkOrderDTO dto) {
        // 校验工单号唯一性
        long count = count(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getWorkOrderNo, dto.getWorkOrderNo()));
        AssertUtil.isFalse(count > 0, "工单号 " + dto.getWorkOrderNo() + " 已存在");

        // 校验计划数量
        AssertUtil.isTrue(dto.getPlanQty() != null && dto.getPlanQty().compareTo(BigDecimal.ZERO) > 0,
                "计划数量必须大于0");

        WorkOrder entity = new WorkOrder();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(WorkOrderStatus.CREATED.getCode());
        save(entity);

        Long workOrderId = entity.getId();

        // 保存子表
        saveTasks(workOrderId, dto.getTasks());
        saveInputMaterials(workOrderId, dto.getInputMaterials());
        saveOutputMaterials(workOrderId, dto.getOutputMaterials());
        saveQualityItems(workOrderId, dto.getQualityItems());
        saveConstraints(workOrderId, dto.getConstraints());
        saveSupplyPlans(workOrderId, dto.getSupplyPlans());

        statusLogService.log(workOrderId, null, WorkOrderStatus.CREATED.getCode(),
                "创建", "创建工单 " + entity.getWorkOrderNo());

        log.info("新增工单: {}", entity.getWorkOrderNo());
        return workOrderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WorkOrderDTO dto) {
        WorkOrder existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.CREATED.getCode().equals(existing.getStatus()),
                "仅创建状态的工单可以编辑");

        String status = existing.getStatus();
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        updateById(existing);

        // 先删后插更新子表
        deleteSubTables(id);
        saveTasks(id, dto.getTasks());
        saveInputMaterials(id, dto.getInputMaterials());
        saveOutputMaterials(id, dto.getOutputMaterials());
        saveQualityItems(id, dto.getQualityItems());
        saveConstraints(id, dto.getConstraints());
        saveSupplyPlans(id, dto.getSupplyPlans());

        log.info("修改工单: {}", existing.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的工单可以删除");

        deleteSubTables(id);
        removeById(id);

        log.info("删除工单: {}", entity.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long id) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的工单可以下发");

        // 校验工作清单非空
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<WorkOrderTask>()
                .eq(WorkOrderTask::getWorkOrderId, id));
        AssertUtil.isTrue(taskCount > 0, "工单必须包含至少一个工作清单才能下发");

        String fromStatus = entity.getStatus();
        entity.setStatus(WorkOrderStatus.RELEASED.getCode());
        updateById(entity);

        statusLogService.log(id, fromStatus, WorkOrderStatus.RELEASED.getCode(),
                "下发", "工单 " + entity.getWorkOrderNo() + " 下发");

        // 发布工单下发事件，派工模块监听后自动生成派工任务
        eventPublisher.publishEvent(new WorkOrderReleasedEvent(this, id));

        // 发布APS同步事件：工单状态变更
        publishApsSyncEvent(entity, WorkOrderStatus.RELEASED.getCode());

        log.info("工单下发: {}", entity.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.RELEASED.getCode().equals(entity.getStatus()),
                "仅已下发状态的工单可以开工");

        String fromStatus = entity.getStatus();
        entity.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        entity.setActualStartTime(LocalDateTime.now());
        updateById(entity);

        statusLogService.log(id, fromStatus, WorkOrderStatus.IN_PROGRESS.getCode(),
                "开工", "工单 " + entity.getWorkOrderNo() + " 开工");

        // 发布APS同步事件：工单开工
        publishApsSyncEvent(entity, WorkOrderStatus.IN_PROGRESS.getCode());

        log.info("工单开工: {}", entity.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.IN_PROGRESS.getCode().equals(entity.getStatus()),
                "仅执行中状态的工单可以完工");

        String fromStatus = entity.getStatus();
        entity.setStatus(WorkOrderStatus.COMPLETED.getCode());
        entity.setActualEndTime(LocalDateTime.now());
        updateById(entity);

        statusLogService.log(id, fromStatus, WorkOrderStatus.COMPLETED.getCode(),
                "完工", "工单 " + entity.getWorkOrderNo() + " 完工");

        // 发布APS同步事件：工单完工
        publishApsSyncEvent(entity, WorkOrderStatus.COMPLETED.getCode());
        eventPublisher.publishEvent(new WorkOrderCompletedEvent(
                this,
                entity.getId(),
                entity.getWorkOrderNo(),
                entity.getProductionPlanNo(),
                entity.getOrderPlanNo(),
                entity.getPlanQty(),
                entity.getActualEndTime()
        ));

        log.info("工单完工: {}", entity.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceComplete(Long id, String reason) {
        WorkOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(WorkOrderStatus.IN_PROGRESS.getCode().equals(entity.getStatus()),
                "仅执行中状态的工单可以强制完工");
        AssertUtil.isTrue(StringUtils.hasText(reason), "强制完工必须填写原因");

        String fromStatus = entity.getStatus();
        entity.setStatus(WorkOrderStatus.FORCE_COMPLETED.getCode());
        entity.setActualEndTime(LocalDateTime.now());
        updateById(entity);

        statusLogService.log(id, fromStatus, WorkOrderStatus.FORCE_COMPLETED.getCode(),
                "强制完工", "工单 " + entity.getWorkOrderNo() + " 强制完工，原因: " + reason);

        // 发布APS同步事件：工单强制完工
        publishApsSyncEvent(entity, WorkOrderStatus.FORCE_COMPLETED.getCode());

        log.info("工单强制完工: {}, 原因: {}", entity.getWorkOrderNo(), reason);
    }

    // ==================== 子表查询 ====================

    private List<WorkOrderTaskVO> queryTasks(Long workOrderId) {
        return taskMapper.selectList(new LambdaQueryWrapper<WorkOrderTask>()
                .eq(WorkOrderTask::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderTask::getSequenceNo))
                .stream().map(e -> { WorkOrderTaskVO vo = new WorkOrderTaskVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderInputMaterialVO> queryInputMaterials(Long workOrderId) {
        return inputMaterialMapper.selectList(new LambdaQueryWrapper<WorkOrderInputMaterial>()
                .eq(WorkOrderInputMaterial::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderInputMaterialVO vo = new WorkOrderInputMaterialVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderOutputMaterialVO> queryOutputMaterials(Long workOrderId) {
        return outputMaterialMapper.selectList(new LambdaQueryWrapper<WorkOrderOutputMaterial>()
                .eq(WorkOrderOutputMaterial::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderOutputMaterialVO vo = new WorkOrderOutputMaterialVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderQualityItemVO> queryQualityItems(Long workOrderId) {
        return qualityItemMapper.selectList(new LambdaQueryWrapper<WorkOrderQualityItem>()
                .eq(WorkOrderQualityItem::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderQualityItemVO vo = new WorkOrderQualityItemVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderConstraintVO> queryConstraints(Long workOrderId) {
        return constraintMapper.selectList(new LambdaQueryWrapper<WorkOrderConstraint>()
                .eq(WorkOrderConstraint::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderConstraintVO vo = new WorkOrderConstraintVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderSupplyPlanVO> querySupplyPlans(Long workOrderId) {
        return supplyPlanMapper.selectList(new LambdaQueryWrapper<WorkOrderSupplyPlan>()
                .eq(WorkOrderSupplyPlan::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderSupplyPlanVO vo = new WorkOrderSupplyPlanVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    private List<WorkOrderAttachmentVO> queryAttachments(Long workOrderId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<WorkOrderAttachment>()
                .eq(WorkOrderAttachment::getWorkOrderId, workOrderId))
                .stream().map(e -> { WorkOrderAttachmentVO vo = new WorkOrderAttachmentVO(); BeanUtils.copyProperties(e, vo); return vo; }).toList();
    }

    // ==================== 子表保存 ====================

    private void saveTasks(Long workOrderId, List<WorkOrderTaskDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderTaskDTO dto : dtos) {
            WorkOrderTask entity = new WorkOrderTask();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            entity.setStatus("CREATED");
            entity.setCreatedTime(LocalDateTime.now());
            entity.setUpdatedTime(LocalDateTime.now());
            taskMapper.insert(entity);
        }
    }

    private void saveInputMaterials(Long workOrderId, List<WorkOrderInputMaterialDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderInputMaterialDTO dto : dtos) {
            WorkOrderInputMaterial entity = new WorkOrderInputMaterial();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            entity.setIssuedQty(BigDecimal.ZERO);
            inputMaterialMapper.insert(entity);
        }
    }

    private void saveOutputMaterials(Long workOrderId, List<WorkOrderOutputMaterialDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderOutputMaterialDTO dto : dtos) {
            WorkOrderOutputMaterial entity = new WorkOrderOutputMaterial();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            outputMaterialMapper.insert(entity);
        }
    }

    private void saveQualityItems(Long workOrderId, List<WorkOrderQualityItemDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderQualityItemDTO dto : dtos) {
            WorkOrderQualityItem entity = new WorkOrderQualityItem();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            entity.setStatus("PENDING");
            qualityItemMapper.insert(entity);
        }
    }

    private void saveConstraints(Long workOrderId, List<WorkOrderConstraintDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderConstraintDTO dto : dtos) {
            WorkOrderConstraint entity = new WorkOrderConstraint();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            constraintMapper.insert(entity);
        }
    }

    private void saveSupplyPlans(Long workOrderId, List<WorkOrderSupplyPlanDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return;
        for (WorkOrderSupplyPlanDTO dto : dtos) {
            WorkOrderSupplyPlan entity = new WorkOrderSupplyPlan();
            BeanUtils.copyProperties(dto, entity);
            entity.setWorkOrderId(workOrderId);
            entity.setCompletedQty(BigDecimal.ZERO);
            supplyPlanMapper.insert(entity);
        }
    }

    // ==================== 子表删除 ====================

    private void deleteSubTables(Long workOrderId) {
        taskMapper.delete(new LambdaQueryWrapper<WorkOrderTask>().eq(WorkOrderTask::getWorkOrderId, workOrderId));
        inputMaterialMapper.delete(new LambdaQueryWrapper<WorkOrderInputMaterial>().eq(WorkOrderInputMaterial::getWorkOrderId, workOrderId));
        outputMaterialMapper.delete(new LambdaQueryWrapper<WorkOrderOutputMaterial>().eq(WorkOrderOutputMaterial::getWorkOrderId, workOrderId));
        qualityItemMapper.delete(new LambdaQueryWrapper<WorkOrderQualityItem>().eq(WorkOrderQualityItem::getWorkOrderId, workOrderId));
        constraintMapper.delete(new LambdaQueryWrapper<WorkOrderConstraint>().eq(WorkOrderConstraint::getWorkOrderId, workOrderId));
        supplyPlanMapper.delete(new LambdaQueryWrapper<WorkOrderSupplyPlan>().eq(WorkOrderSupplyPlan::getWorkOrderId, workOrderId));
        attachmentMapper.delete(new LambdaQueryWrapper<WorkOrderAttachment>().eq(WorkOrderAttachment::getWorkOrderId, workOrderId));
    }

    private WorkOrderVO toVO(WorkOrder entity) {
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setWorkType(entity.getBusinessType());
        return vo;
    }

    private String resolveBusinessType(String businessType, String legacyWorkType) {
        return StringUtils.hasText(businessType) ? businessType : legacyWorkType;
    }

    // ==================== APS 同步事件发布 ====================

    private void publishApsSyncEvent(WorkOrder entity, String newStatus) {
        try {
            String payload = String.format(
                    "{\"workOrderId\":%d,\"workOrderNo\":\"%s\",\"status\":\"%s\"}",
                    entity.getId(), entity.getWorkOrderNo(), newStatus);
            eventPublisher.publishEvent(new ApsSyncEvent(
                    this, "WORKORDER", "STATUS_CHANGE",
                    entity.getId(), entity.getWorkOrderNo(), 3, payload));
        } catch (Exception e) {
            log.warn("发布APS同步事件失败（不影响业务）: {}", e.getMessage());
        }
    }
}
