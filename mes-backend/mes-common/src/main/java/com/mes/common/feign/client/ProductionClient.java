package com.mes.common.feign.client;

import com.mes.common.feign.fallback.ProductionClientFallback;
import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mes-production-service", fallbackFactory = ProductionClientFallback.class)
public interface ProductionClient {

    @GetMapping("/internal/production/workorder/{workOrderId}")
    R<Map<String, Object>> getWorkOrder(@PathVariable("workOrderId") Long workOrderId);

    @GetMapping("/internal/production/workorder/no/{workOrderNo}")
    R<Map<String, Object>> getWorkOrderByNo(@PathVariable("workOrderNo") String workOrderNo);
}
