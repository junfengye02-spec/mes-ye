package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.RouteDTO;
import com.mes.process.domain.entity.Route;
import com.mes.process.domain.query.RouteQuery;
import com.mes.process.domain.vo.RouteVO;

/**
 * 工艺路线 Service 接口。
 */
public interface IRouteService extends IService<Route> {

    /** 分页查询 */
    PageResult<RouteVO> page(RouteQuery query);

    /** 获取详情 */
    RouteVO getDetail(Long id);

    /** 新增 */
    Long create(RouteDTO dto);

    /** 修改 */
    void update(Long id, RouteDTO dto);

    /** 删除 */
    void delete(Long id);

    /** 启用 */
    void activate(Long id);

    /** 停用 */
    void disable(Long id);

    /** 查询匹配产品的有效路线及步骤 */
    RouteVO findActiveRouteWithSteps(String productCode, String productCategory,
                                     String machineModel, String productType);
}
