package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.RouteDTO;
import com.mes.process.domain.dto.RouteStepDTO;
import com.mes.process.domain.entity.Route;
import com.mes.process.domain.entity.RouteStep;
import com.mes.process.domain.query.RouteQuery;
import com.mes.process.domain.vo.RouteStepVO;
import com.mes.process.domain.vo.RouteVO;
import com.mes.process.enums.RouteStatus;
import com.mes.process.mapper.RouteMapper;
import com.mes.process.mapper.RouteStepMapper;
import com.mes.process.service.IRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工艺路线 Service 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements IRouteService {

    private final RouteStepMapper routeStepMapper;

    @Override
    public PageResult<RouteVO> page(RouteQuery query) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<Route>()
                .like(StringUtils.hasText(query.getRouteCode()), Route::getRouteCode, query.getRouteCode())
                .like(StringUtils.hasText(query.getRouteName()), Route::getRouteName, query.getRouteName())
                .like(StringUtils.hasText(query.getProductCode()), Route::getProductCode, query.getProductCode())
                .eq(StringUtils.hasText(query.getProductCategory()), Route::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getMachineModel()), Route::getMachineModel, query.getMachineModel())
                .eq(StringUtils.hasText(query.getStatus()), Route::getStatus, query.getStatus())
                .orderByDesc(Route::getCreatedTime);

        Page<Route> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<RouteVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal());
    }

    @Override
    public RouteVO getDetail(Long id) {
        Route route = getById(id);
        AssertUtil.notNull(route, ResultCode.DATA_NOT_EXIST);

        RouteVO vo = toVO(route);
        vo.setSteps(querySteps(id).stream().map(this::toStepVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteDTO dto) {
        Route route = new Route();
        BeanUtils.copyProperties(dto, route);
        route.setStatus(RouteStatus.DRAFT.getCode());
        save(route);

        saveSteps(route.getId(), dto.getSteps());

        log.info("新增工艺路线: {}", route.getRouteCode());
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteDTO dto) {
        Route route = getById(id);
        AssertUtil.notNull(route, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(RouteStatus.DRAFT.getCode().equals(route.getStatus()),
                "仅草稿状态的工艺路线可以编辑");

        String status = route.getStatus();
        BeanUtils.copyProperties(dto, route);
        route.setId(id);
        route.setStatus(status);
        updateById(route);

        deleteSteps(id);
        saveSteps(id, dto.getSteps());

        log.info("修改工艺路线: {}", route.getRouteCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Route route = getById(id);
        AssertUtil.notNull(route, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(RouteStatus.DRAFT.getCode().equals(route.getStatus()),
                "仅草稿状态的工艺路线可以删除");

        deleteSteps(id);
        removeById(id);

        log.info("删除工艺路线: {}", route.getRouteCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        Route route = getById(id);
        AssertUtil.notNull(route, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(!querySteps(id).isEmpty(), "工艺路线必须至少包含一个工序步骤才能启用");

        route.setStatus(RouteStatus.ACTIVE.getCode());
        updateById(route);

        log.info("启用工艺路线: {}", route.getRouteCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        Route route = getById(id);
        AssertUtil.notNull(route, ResultCode.DATA_NOT_EXIST);

        route.setStatus(RouteStatus.DISABLED.getCode());
        updateById(route);

        log.info("停用工艺路线: {}", route.getRouteCode());
    }

    @Override
    public RouteVO findActiveRouteWithSteps(String productCode, String productCategory,
                                            String machineModel, String productType) {
        Route route = findExactProductRoute(productCode)
                .or(() -> findCategoryAndMachineRoute(productCategory, machineModel))
                .or(() -> findCategoryRoute(productCategory))
                .orElseThrow(() -> new BusinessException("未找到匹配的有效工艺路线"));

        List<RouteStep> steps = querySteps(route.getId());
        AssertUtil.isFalse(steps.isEmpty(), "工艺路线未配置工序步骤");

        RouteVO vo = toVO(route);
        vo.setSteps(steps.stream().map(this::toStepVO).toList());
        return vo;
    }

    private Optional<Route> findExactProductRoute(String productCode) {
        if (!StringUtils.hasText(productCode)) {
            return Optional.empty();
        }
        return first(baseActiveWrapper()
                .eq(Route::getProductCode, productCode));
    }

    private Optional<Route> findCategoryAndMachineRoute(String productCategory, String machineModel) {
        if (!StringUtils.hasText(productCategory) || !StringUtils.hasText(machineModel)) {
            return Optional.empty();
        }
        return first(baseActiveWrapper()
                .and(w -> w.isNull(Route::getProductCode).or().eq(Route::getProductCode, ""))
                .eq(Route::getProductCategory, productCategory)
                .eq(Route::getMachineModel, machineModel));
    }

    private Optional<Route> findCategoryRoute(String productCategory) {
        if (!StringUtils.hasText(productCategory)) {
            return Optional.empty();
        }
        return first(baseActiveWrapper()
                .and(w -> w.isNull(Route::getProductCode).or().eq(Route::getProductCode, ""))
                .and(w -> w.isNull(Route::getMachineModel).or().eq(Route::getMachineModel, ""))
                .eq(Route::getProductCategory, productCategory));
    }

    private Optional<Route> first(LambdaQueryWrapper<Route> wrapper) {
        List<Route> routes = baseMapper.selectList(wrapper.last("LIMIT 1"));
        return routes.isEmpty() ? Optional.empty() : Optional.of(routes.get(0));
    }

    private LambdaQueryWrapper<Route> baseActiveWrapper() {
        LocalDate today = LocalDate.now();
        return new LambdaQueryWrapper<Route>()
                .eq(Route::getStatus, RouteStatus.ACTIVE.getCode())
                .and(w -> w.isNull(Route::getEffectiveDate).or().le(Route::getEffectiveDate, today))
                .and(w -> w.isNull(Route::getExpiryDate).or().ge(Route::getExpiryDate, today))
                .orderByDesc(Route::getUpdatedTime)
                .orderByDesc(Route::getCreatedTime)
                .orderByAsc(Route::getId);
    }

    private List<RouteStep> querySteps(Long routeId) {
        return routeStepMapper.selectList(new LambdaQueryWrapper<RouteStep>()
                .eq(RouteStep::getRouteId, routeId)
                .orderByAsc(RouteStep::getSequenceNo)
                .orderByAsc(RouteStep::getId))
                .stream()
                .sorted(Comparator
                        .comparing(RouteStep::getSequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RouteStep::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private void saveSteps(Long routeId, List<RouteStepDTO> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return;
        }

        AtomicInteger fallbackSequence = new AtomicInteger(1);
        for (RouteStepDTO dto : steps) {
            RouteStep step = new RouteStep();
            BeanUtils.copyProperties(dto, step);
            step.setRouteId(routeId);
            if (step.getSequenceNo() == null) {
                step.setSequenceNo(fallbackSequence.get());
            }
            if (step.getParallelFlag() == null) {
                step.setParallelFlag(0);
            }
            if (step.getOptionalFlag() == null) {
                step.setOptionalFlag(0);
            }
            routeStepMapper.insert(step);
            fallbackSequence.incrementAndGet();
        }
    }

    private void deleteSteps(Long routeId) {
        routeStepMapper.delete(new LambdaQueryWrapper<RouteStep>()
                .eq(RouteStep::getRouteId, routeId));
    }

    private RouteVO toVO(Route route) {
        RouteVO vo = new RouteVO();
        BeanUtils.copyProperties(route, vo);
        return vo;
    }

    private RouteStepVO toStepVO(RouteStep step) {
        RouteStepVO vo = new RouteStepVO();
        BeanUtils.copyProperties(step, vo);
        return vo;
    }
}
