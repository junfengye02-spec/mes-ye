package com.mes.plan.service;

import com.mes.common.exception.BusinessException;
import com.mes.common.id.DistributedIdGenerator;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.entity.ProductionPlan;
import com.mes.plan.enums.ProductionPlanStatus;
import com.mes.plan.mapper.ProductionPlanMapper;
import com.mes.plan.service.impl.ProductionPlanServiceImpl;
import com.mes.process.domain.vo.RouteStepVO;
import com.mes.process.domain.vo.RouteVO;
import com.mes.process.service.IRouteService;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.service.IWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductionPlanServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionPlanServiceTest {

    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private IOrderPlanService orderPlanService;
    @Mock
    private IPlanStatusLogService planStatusLogService;
    @Mock
    private IWorkOrderService workOrderService;
    @Mock
    private IRouteService routeService;
    @Mock
    private DistributedIdGenerator distributedIdGenerator;

    @InjectMocks
    private ProductionPlanServiceImpl productionPlanService;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(productionPlanService, "baseMapper", productionPlanMapper);
    }

    @Test
    @DisplayName("下达生产计划 - 根据工艺路线生成工单工作清单")
    void release_generatesWorkOrderTasksFromRoute() {
        ProductionPlan plan = productionPlan(100L, "P1", "CAT-A", "M1");
        plan.setBusinessType("INSPECTION");
        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(orderPlanService.getById(10L)).thenReturn(orderPlan("ORD-1"));
        when(routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"))
                .thenReturn(routeWithSteps());
        when(productionPlanMapper.updateById(any(ProductionPlan.class))).thenReturn(1);
        when(workOrderService.create(any(WorkOrderDTO.class))).thenReturn(200L);
        when(distributedIdGenerator.nextIdStr()).thenReturn("9876543210");

        productionPlanService.release(100L);

        ArgumentCaptor<WorkOrderDTO> captor = ArgumentCaptor.forClass(WorkOrderDTO.class);
        verify(workOrderService).create(captor.capture());

        WorkOrderDTO dto = captor.getValue();
        assertEquals("INSPECTION", dto.getBusinessType());
        assertEquals("WO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-9876543210",
                dto.getWorkOrderNo());
        assertEquals(2, dto.getTasks().size());
        assertEquals("OP-010", dto.getTasks().get(0).getTaskNo());
        assertEquals("首道工序", dto.getTasks().get(0).getTaskName());
        assertEquals(new BigDecimal("5"), dto.getTasks().get(0).getPlanQty());
        assertEquals("PCS", dto.getTasks().get(0).getQtyUnit());
        assertEquals(10, dto.getTasks().get(0).getSequenceNo());
        assertEquals(300L, dto.getTasks().get(0).getPlanWorkCenterId());
    }

    @Test
    @DisplayName("下达生产计划 - 无匹配路线时不更新计划状态也不创建工单")
    void release_rejectsWhenNoActiveRoute() {
        ProductionPlan plan = productionPlan(100L, "P1", "CAT-A", "M1");
        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(routeService.findActiveRouteWithSteps("P1", "CAT-A", "M1", "TYPE-A"))
                .thenThrow(new BusinessException("未找到匹配的有效工艺路线"));

        assertThrows(BusinessException.class, () -> productionPlanService.release(100L));

        verify(productionPlanMapper, never()).updateById(any());
        verify(workOrderService, never()).create(any());
    }

    private static ProductionPlan productionPlan(Long id, String productCode,
                                                 String productCategory, String machineModel) {
        ProductionPlan plan = new ProductionPlan();
        plan.setId(id);
        plan.setOrderPlanId(10L);
        plan.setOrderNo("ORD-1");
        plan.setProductCode(productCode);
        plan.setProductName("产品1");
        plan.setProductCategory(productCategory);
        plan.setMachineModel(machineModel);
        plan.setProductType("TYPE-A");
        plan.setStatus(ProductionPlanStatus.CREATED.getCode());
        plan.setPlanQty(new BigDecimal("5"));
        plan.setQtyUnit("PCS");
        return plan;
    }

    private static OrderPlan orderPlan(String orderNo) {
        OrderPlan orderPlan = new OrderPlan();
        orderPlan.setId(10L);
        orderPlan.setOrderNo(orderNo);
        return orderPlan;
    }

    private static RouteVO routeWithSteps() {
        RouteVO route = new RouteVO();
        route.setId(1L);
        route.setRouteCode("R-P1");
        route.setSteps(List.of(
                routeStep(10, "OP-010", "首道工序", 300L),
                routeStep(20, "OP-020", "二道工序", 301L)));
        return route;
    }

    private static RouteStepVO routeStep(Integer sequenceNo, String processNo,
                                         String processName, Long workCenterId) {
        RouteStepVO step = new RouteStepVO();
        step.setSequenceNo(sequenceNo);
        step.setProcessNo(processNo);
        step.setProcessName(processName);
        step.setWorkCenterId(workCenterId);
        return step;
    }
}
