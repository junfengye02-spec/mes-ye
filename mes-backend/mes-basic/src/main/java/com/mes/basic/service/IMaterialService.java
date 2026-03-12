package com.mes.basic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.basic.domain.dto.MaterialDTO;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.query.MaterialQuery;
import com.mes.basic.domain.vo.MaterialVO;
import com.mes.common.core.PageResult;

/**
 * 物料档案 Service 接口
 */
public interface IMaterialService extends IService<Material> {

    /**
     * 分页查询物料
     */
    PageResult<MaterialVO> page(MaterialQuery query);

    /**
     * 获取物料详情
     */
    MaterialVO getDetail(Long id);

    /**
     * 新增物料
     */
    Long create(MaterialDTO dto);

    /**
     * 修改物料
     */
    void update(Long id, MaterialDTO dto);

    /**
     * 删除物料
     */
    void delete(Long id);
}
