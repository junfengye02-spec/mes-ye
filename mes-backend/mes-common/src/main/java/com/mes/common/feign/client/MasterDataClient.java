package com.mes.common.feign.client;

import com.mes.common.feign.fallback.MasterDataClientFallback;
import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "mes-masterdata-service", fallbackFactory = MasterDataClientFallback.class)
public interface MasterDataClient {

    @GetMapping("/internal/masterdata/material/{materialId}")
    R<Map<String, Object>> getMaterial(@PathVariable("materialId") Long materialId);

    @GetMapping("/internal/masterdata/material/code/{materialCode}")
    R<Map<String, Object>> getMaterialByCode(@PathVariable("materialCode") String materialCode);

    @GetMapping("/internal/masterdata/workcenter/{workCenterId}")
    R<Map<String, Object>> getWorkCenter(@PathVariable("workCenterId") Long workCenterId);

    @GetMapping("/internal/masterdata/process/{processId}")
    R<Map<String, Object>> getProcessInfo(@PathVariable("processId") Long processId);
}
