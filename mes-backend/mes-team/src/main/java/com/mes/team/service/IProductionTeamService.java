package com.mes.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.team.domain.dto.ProductionTeamDTO;
import com.mes.team.domain.entity.ProductionTeam;
import com.mes.team.domain.query.ProductionTeamQuery;
import com.mes.team.domain.vo.ProductionTeamVO;

/**
 * 生产班组 Service 接口
 */
public interface IProductionTeamService extends IService<ProductionTeam> {

    /**
     * 分页查询班组
     */
    PageResult<ProductionTeamVO> page(ProductionTeamQuery query);

    /**
     * 获取班组详情
     */
    ProductionTeamVO getDetail(Long id);

    /**
     * 新增班组
     */
    Long create(ProductionTeamDTO dto);

    /**
     * 修改班组
     */
    void update(Long id, ProductionTeamDTO dto);

    /**
     * 删除班组
     */
    void delete(Long id);

    /**
     * 启用/停用切换
     */
    void toggleEnabled(Long id);
}
