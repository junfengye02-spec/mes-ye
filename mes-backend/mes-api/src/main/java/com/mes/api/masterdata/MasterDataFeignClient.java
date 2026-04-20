package com.mes.api.masterdata;

import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mes-master-data", contextId = "masterDataFeignClient", path = "/api")
public interface MasterDataFeignClient {

    @GetMapping("/basic/material/{id}")
    R<Map<String, Object>> getMaterial(@PathVariable("id") Long id);

    @GetMapping("/basic/workcenter/{id}")
    R<Map<String, Object>> getWorkCenter(@PathVariable("id") Long id);
}
