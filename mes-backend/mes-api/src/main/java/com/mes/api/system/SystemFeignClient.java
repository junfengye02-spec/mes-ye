package com.mes.api.system;

import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mes-system", contextId = "systemFeignClient", path = "/api")
public interface SystemFeignClient {

    @GetMapping("/system/user/{id}")
    R<Map<String, Object>> getUser(@PathVariable("id") Long id);
}
