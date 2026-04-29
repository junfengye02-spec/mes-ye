package com.mes.workorder.search;

import com.mes.common.core.PageResult;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.WorkOrderVO;

/**
 * 工单富查询服务（P2-28）
 *
 * <p>统一的查询入口：主路径走 ES，任何异常降级到 MyBatis。</p>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>工单号 / 订单号模糊搜索</li>
 *   <li>项目名称 / 产品名称中文搜索（IK 分词）</li>
 *   <li>状态 + 时间范围组合过滤</li>
 * </ul>
 */
public interface IWorkOrderSearchService {

    /**
     * 分页富查询。
     * <p>优先从 ES 查，ES 不可用或查询异常时自动降级到 MySQL。</p>
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<WorkOrderVO> queryRich(WorkOrderQuery query);
}
