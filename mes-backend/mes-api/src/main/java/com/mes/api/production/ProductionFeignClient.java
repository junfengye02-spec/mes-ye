package com.mes.api.production;

import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "mes-production", contextId = "productionFeignClient", path = "/api")
public interface ProductionFeignClient {

    @GetMapping("/workorder/{id}")
    R<Map<String, Object>> getWorkOrder(@PathVariable("id") Long id);

    @GetMapping("/workorder/list")
    R<List<Map<String, Object>>> listWorkOrders(@RequestParam(required = false) Map<String, Object> params);

    @GetMapping("/plan/{id}")
    R<Map<String, Object>> getPlan(@PathVariable("id") Long id);
}
