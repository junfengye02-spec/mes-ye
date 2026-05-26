package com.mes.process.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.exception.BusinessException;
import com.mes.process.domain.entity.Route;
import com.mes.process.domain.entity.RouteStep;
import com.mes.process.domain.vo.RouteStepVO;
import com.mes.process.domain.vo.RouteVO;
import com.mes.process.enums.RouteStatus;
import com.mes.process.mapper.RouteMapper;
import com.mes.process.mapper.RouteStepMapper;
import com.mes.process.service.impl.RouteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link RouteServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteServiceTest {

    @Mock
    private RouteMapper routeMapper;
    @Mock
    private RouteStepMapper routeStepMapper;

    @InjectMocks
    private RouteServiceImpl routeService;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(routeService, "baseMapper", routeMapper);
    }

    @Test
    @DisplayName("匹配活动路线 - 优先产品编码精确匹配")
    void findActiveRouteWithSteps_prefersExactProductCode() {
        Route exact = route(1L, "R-P1", "P1", "CAT-A", "M1");
        when(routeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(exact));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                step(11L, 1L, 20, "OP-020"),
                step(10L, 1L, 10, "OP-010")));

        RouteVO result = routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A");

        assertEquals(1L, result.getId());
        assertEquals("R-P1", result.getRouteCode());
        assertEquals(List.of("OP-010", "OP-020"),
                result.getSteps().stream().map(RouteStepVO::getProcessNo).toList());
    }

    @Test
    @DisplayName("匹配活动路线 - 无产品编码时按产品类别和机型回退")
    void findActiveRouteWithSteps_fallsBackToCategoryAndMachineModel() {
        when(routeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(route(2L, "R-CAT-M", null, "CAT-A", "M1")));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(step(20L, 2L, 10, "OP-010")));

        RouteVO result = routeService.findActiveRouteWithSteps("P-MISSING", "CAT-A", "M1", "TYPE-A");

        assertEquals(2L, result.getId());
        assertEquals("R-CAT-M", result.getRouteCode());
    }

    @Test
    @DisplayName("匹配活动路线 - 活动路线无步骤时拒绝")
    void findActiveRouteWithSteps_rejectsRouteWithoutSteps() {
        when(routeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(route(3L, "R-EMPTY", "P1", "CAT-A", "M1")));
        when(routeStepMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"));

        assertTrue(ex.getMessage().contains("未配置工序步骤"));
    }

    private static Route route(Long id, String code, String productCode,
                               String productCategory, String machineModel) {
        Route route = new Route();
        route.setId(id);
        route.setRouteCode(code);
        route.setRouteName(code);
        route.setProductCode(productCode);
        route.setProductCategory(productCategory);
        route.setMachineModel(machineModel);
        route.setStatus(RouteStatus.ACTIVE.getCode());
        return route;
    }

    private static RouteStep step(Long id, Long routeId, Integer sequenceNo, String processNo) {
        RouteStep step = new RouteStep();
        step.setId(id);
        step.setRouteId(routeId);
        step.setSequenceNo(sequenceNo);
        step.setProcessId(id + 1000);
        step.setProcessNo(processNo);
        step.setProcessName("工序" + sequenceNo);
        step.setWorkCenterId(300L + sequenceNo);
        return step;
    }
}
