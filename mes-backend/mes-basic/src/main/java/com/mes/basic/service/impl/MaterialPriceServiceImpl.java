package com.mes.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.basic.domain.dto.MaterialPriceDTO;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.entity.MaterialPrice;
import com.mes.basic.domain.query.MaterialPriceQuery;
import com.mes.basic.domain.vo.MaterialPriceVO;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.MaterialPriceMapper;
import com.mes.basic.service.IMaterialPriceService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物料价格 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialPriceServiceImpl extends ServiceImpl<MaterialPriceMapper, MaterialPrice>
        implements IMaterialPriceService {

    private final MaterialMapper materialMapper;

    @Override
    public PageResult<MaterialPriceVO> page(MaterialPriceQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<MaterialPrice> wrapper = new LambdaQueryWrapper<MaterialPrice>()
                .orderByDesc(MaterialPrice::getCreatedTime);

        // 如果按物料编码/名称过滤，先查询匹配的物料ID
        if (StringUtils.hasText(query.getMaterialCode()) || StringUtils.hasText(query.getMaterialName())) {
            LambdaQueryWrapper<Material> materialWrapper = new LambdaQueryWrapper<Material>()
                    .like(StringUtils.hasText(query.getMaterialCode()),
                            Material::getMaterialCode, query.getMaterialCode())
                    .like(StringUtils.hasText(query.getMaterialName()),
                            Material::getMaterialName, query.getMaterialName());
            List<Long> materialIds = materialMapper.selectList(materialWrapper).stream()
                    .map(Material::getId)
                    .toList();
            if (materialIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(MaterialPrice::getMaterialId, materialIds);
        }

        // 分页查询
        Page<MaterialPrice> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        // 转换为 VO 并关联物料信息
        List<MaterialPriceVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        enrichMaterialInfo(voList);

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public MaterialPriceVO getDetail(Long id) {
        MaterialPrice price = getById(id);
        AssertUtil.notNull(price, ResultCode.DATA_NOT_EXIST);
        MaterialPriceVO vo = toVO(price);
        enrichMaterialInfo(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialPriceDTO dto) {
        // 校验物料存在
        Material material = materialMapper.selectById(dto.getMaterialId());
        AssertUtil.notNull(material, "物料不存在");

        // 校验 1:1 唯一性（每个物料只有一条价格记录）
        checkMaterialPriceUnique(dto.getMaterialId(), null);

        MaterialPrice price = new MaterialPrice();
        BeanUtils.copyProperties(dto, price);
        save(price);

        log.info("新增物料价格: 物料={}, 单价={}", material.getMaterialCode(), dto.getUnitPrice());
        return price.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialPriceDTO dto) {
        MaterialPrice existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        // 校验物料存在
        Material material = materialMapper.selectById(dto.getMaterialId());
        AssertUtil.notNull(material, "物料不存在");

        // 校验 1:1 唯一性（排除自身）
        checkMaterialPriceUnique(dto.getMaterialId(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改物料价格: 物料={}, 单价={}", material.getMaterialCode(), dto.getUnitPrice());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialPrice price = getById(id);
        AssertUtil.notNull(price, ResultCode.DATA_NOT_EXIST);

        removeById(id);
        log.info("删除物料价格: ID={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验物料价格唯一性（1:1 关系：每个物料只允许一条价格记录）
     */
    private void checkMaterialPriceUnique(Long materialId, Long excludeId) {
        LambdaQueryWrapper<MaterialPrice> wrapper = new LambdaQueryWrapper<MaterialPrice>()
                .eq(MaterialPrice::getMaterialId, materialId)
                .ne(excludeId != null, MaterialPrice::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "该物料已存在价格记录");
        }
    }

    /**
     * 批量填充物料编码/名称信息
     */
    private void enrichMaterialInfo(List<MaterialPriceVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        Set<Long> materialIds = voList.stream()
                .map(MaterialPriceVO::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (materialIds.isEmpty()) {
            return;
        }
        Map<Long, Material> materialMap = materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));
        voList.forEach(vo -> {
            Material m = materialMap.get(vo.getMaterialId());
            if (m != null) {
                vo.setMaterialCode(m.getMaterialCode());
                vo.setMaterialName(m.getMaterialName());
            }
        });
    }

    /**
     * Entity → VO 转换
     */
    private MaterialPriceVO toVO(MaterialPrice entity) {
        MaterialPriceVO vo = new MaterialPriceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
