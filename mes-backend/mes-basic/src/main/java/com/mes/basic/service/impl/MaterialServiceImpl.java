package com.mes.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.basic.domain.dto.MaterialDTO;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.entity.MaterialPrice;
import com.mes.basic.domain.query.MaterialQuery;
import com.mes.basic.domain.vo.MaterialVO;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.MaterialPriceMapper;
import com.mes.basic.service.IMaterialService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 物料档案 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material>
        implements IMaterialService {

    private final MaterialPriceMapper materialPriceMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<MaterialVO> page(MaterialQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .like(StringUtils.hasText(query.getMaterialCode()),
                        Material::getMaterialCode, query.getMaterialCode())
                .like(StringUtils.hasText(query.getMaterialName()),
                        Material::getMaterialName, query.getMaterialName())
                .eq(StringUtils.hasText(query.getMaterialType()),
                        Material::getMaterialType, query.getMaterialType())
                .like(StringUtils.hasText(query.getDrawingNo()),
                        Material::getDrawingNo, query.getDrawingNo())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        Material::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getMachineModel()),
                        Material::getMachineModel, query.getMachineModel())
                .orderByDesc(Material::getCreatedTime);

        // 分页查询
        Page<Material> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        // 转换为 VO
        List<MaterialVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "material", key = "#id", unless = "#result == null")
    public MaterialVO getDetail(Long id) {
        Material material = getById(id);
        AssertUtil.notNull(material, ResultCode.DATA_NOT_EXIST);
        return toVO(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialDTO dto) {
        // 校验编码唯一性
        checkCodeUnique(dto.getMaterialCode(), null);

        // 校验追溯方式与生成器的联动
        validateTraceMode(dto);

        Material material = new Material();
        BeanUtils.copyProperties(dto, material);
        save(material);

        log.info("新增物料: {} - {}", material.getMaterialCode(), material.getMaterialName());
        return material.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @org.springframework.cache.annotation.CacheEvict(value = "material", key = "#id")
    public void update(Long id, MaterialDTO dto) {
        Material existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        checkCodeUnique(dto.getMaterialCode(), id);
        validateTraceMode(dto);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改物料: {} - {}", existing.getMaterialCode(), existing.getMaterialName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @org.springframework.cache.annotation.CacheEvict(value = "material", key = "#id")
    public void delete(Long id) {
        Material material = getById(id);
        AssertUtil.notNull(material, ResultCode.DATA_NOT_EXIST);

        // 检查是否被物料价格引用
        LambdaQueryWrapper<MaterialPrice> priceWrapper = new LambdaQueryWrapper<MaterialPrice>()
                .eq(MaterialPrice::getMaterialId, id);
        AssertUtil.isFalse(materialPriceMapper.selectCount(priceWrapper) > 0,
                "该物料存在价格记录，无法删除");

        assertNotReferenced("mes_manufacturing_bom", "product_id", id, "制造BOM主产品");
        assertNotReferenced("mes_manufacturing_bom_item", "material_id", id, "制造BOM明细");
        assertNotReferenced("mes_bom_substitute", "main_material_id", id, "BOM主物料替代关系");
        assertNotReferenced("mes_bom_substitute", "substitute_material_id", id, "BOM替代物料关系");
        assertNotReferenced("mes_work_order_input_material", "material_id", id, "工单投入物料");
        assertNotReferenced("mes_work_order_output_material", "material_id", id, "工单产出物料");
        assertNotReferenced("mes_storage_inventory", "material_id", id, "库存台账");
        assertNotReferenced("mes_material_requisition_item", "material_id", id, "领料申请");
        assertNotReferenced("mes_requisition_order", "material_id", id, "领料工单");
        assertNotReferenced("mes_finished_goods_receipt_request", "material_id", id, "成品入库申请");
        assertNotReferenced("mes_finished_goods_receipt_item", "material_id", id, "成品入库明细");
        assertNotReferenced("mes_delivery_sign", "material_id", id, "交付签收");
        assertNotReferenced("mes_outsource_order", "material_id", id, "APS外协订单");
        assertNotReferenced("mes_transfer_order", "material_id", id, "APS调拨订单");

        removeById(id);
        log.info("删除物料: {} - {}", material.getMaterialCode(), material.getMaterialName());
    }

    // ==================== 私有方法 ====================

    /**
     * 校验物料编码唯一性
     */
    private void checkCodeUnique(String materialCode, Long excludeId) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, materialCode)
                .ne(excludeId != null, Material::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "物料编码已存在: " + materialCode);
        }
    }

    /**
     * 校验追溯方式与生成器的联动关系
     */
    private void validateTraceMode(MaterialDTO dto) {
        if ("SERIAL".equals(dto.getTraceMode()) && !StringUtils.hasText(dto.getSerialGenerator())) {
            throw new BusinessException("单件追溯方式必须配置序列号生成器");
        }
        if ("BATCH".equals(dto.getTraceMode()) && !StringUtils.hasText(dto.getBatchGenerator())) {
            throw new BusinessException("批次追溯方式必须配置批号生成器");
        }
    }

    /**
     * Entity → VO 转换
     */
    private MaterialVO toVO(Material entity) {
        MaterialVO vo = new MaterialVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void assertNotReferenced(String table, String column, Long id, String label) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                id);
        AssertUtil.isFalse(count != null && count > 0, "该物料已被" + label + "引用，无法删除");
    }
}
