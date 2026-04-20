package com.mes.api.material;

import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "mes-material-svc", contextId = "materialFeignClient", path = "/api")
public interface MaterialFeignClient {

    @GetMapping("/material/inventory/{materialId}")
    R<Map<String, Object>> getInventory(@PathVariable("materialId") Long materialId);

    @GetMapping("/material/inventory/list")
    R<List<Map<String, Object>>> listInventory(@RequestParam(required = false) Map<String, Object> params);
}
