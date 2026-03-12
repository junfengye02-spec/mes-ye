package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.ManufacturingBomDTO;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.query.ManufacturingBomQuery;
import com.mes.process.domain.vo.ManufacturingBomItemVO;
import com.mes.process.domain.vo.ManufacturingBomVO;

import java.util.List;

/**
 * 制造BOM Service 接口
 */
public interface IManufacturingBomService extends IService<ManufacturingBom> {

    /** 分页查询 */
    PageResult<ManufacturingBomVO> page(ManufacturingBomQuery query);

    /** 获取详情（含树形BOM明细） */
    ManufacturingBomVO getDetail(Long id);

    /** 新增（含明细+替代料） */
    Long create(ManufacturingBomDTO dto);

    /** 修改 */
    void update(Long id, ManufacturingBomDTO dto);

    /** 删除 */
    void delete(Long id);

    /** BOM版本升级 */
    Long upgrade(Long id);

    /** 发布 DRAFT→PUBLISHED */
    void publish(Long id);

    /** 停用 PUBLISHED→DISABLED */
    void disable(Long id);

    /** 获取BOM明细树 */
    List<ManufacturingBomItemVO> getItemTree(Long bomId);
}
