package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.utils.NumberGenerator;
import com.mes.material.domain.dto.MaterialReturnDTO;
import com.mes.material.domain.entity.MaterialReturn;
import com.mes.material.domain.query.MaterialReturnQuery;
import com.mes.material.domain.vo.MaterialReturnVO;
import com.mes.material.enums.RequisitionStatus;
import com.mes.material.mapper.MaterialReturnMapper;
import com.mes.material.service.IMaterialReturnService;
import com.mes.material.service.IStorageInventoryService;
import com.mes.material.domain.entity.StorageInventory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产退料申请 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialReturnServiceImpl extends ServiceImpl<MaterialReturnMapper, MaterialReturn>
        implements IMaterialReturnService {

    private final IStorageInventoryService storageInventoryService;

    @Override
    public PageResult<MaterialReturnVO> page(MaterialReturnQuery query) {
        LambdaQueryWrapper<MaterialReturn> wrapper = new LambdaQueryWrapper<MaterialReturn>()
                .like(StringUtils.hasText(query.getReturnNo()),
                        MaterialReturn::getReturnNo, query.getReturnNo())
                .like(StringUtils.hasText(query.getWorkOrderNo()),
                        MaterialReturn::getWorkOrderNo, query.getWorkOrderNo())
                .eq(query.getWorkOrderId() != null,
                        MaterialReturn::getWorkOrderId, query.getWorkOrderId())
                .eq(StringUtils.hasText(query.getStatus()),
                        MaterialReturn::getStatus, query.getStatus())
                .orderByDesc(MaterialReturn::getCreatedTime);

        Page<MaterialReturn> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<MaterialReturnVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public MaterialReturnVO getDetail(Long id) {
        MaterialReturn entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialReturnDTO dto) {
        MaterialReturn entity = new MaterialReturn();
        BeanUtils.copyProperties(dto, entity);
        
        // 自动生成退料单号
        if (!StringUtils.hasText(entity.getReturnNo())) {
            entity.setReturnNo(NumberGenerator.generate("TL"));
        }
        
        entity.setStatus(RequisitionStatus.CREATED.getCode());
        save(entity);
        
        // 库存联动：退料增加库存
        if (StringUtils.hasText(entity.getProductCode()) && entity.getPlanQty() != null) {
            StorageInventory inventory = storageInventoryService.getOne(
                    new LambdaQueryWrapper<StorageInventory>()
                            .eq(StorageInventory::getMaterialCode, entity.getProductCode())
                            .last("LIMIT 1"));
            if (inventory != null) {
                storageInventoryService.addStock(inventory.getId(), entity.getPlanQty());
                log.info("退料增加库存: materialCode={}, qty={}", entity.getProductCode(), entity.getPlanQty());
            }
        }

        log.info("新增生产退料申请: {}", entity.getReturnNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialReturnDTO dto) {
        MaterialReturn existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        
        log.info("修改生产退料申请: {}", existing.getReturnNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialReturn entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        
        removeById(id);
        
        log.info("删除生产退料申请: {}", entity.getReturnNo());
    }

    // ==================== 私有方法 ====================

    private MaterialReturnVO toVO(MaterialReturn entity) {
        MaterialReturnVO vo = new MaterialReturnVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
