package com.mes.common.feign.client;

import com.mes.common.feign.dto.StockDeductRequest;
import com.mes.common.feign.dto.StockLockRequest;
import com.mes.common.feign.fallback.MaterialClientFallback;
import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mes-material-service", fallbackFactory = MaterialClientFallback.class)
public interface MaterialClient {

    @PostMapping("/internal/material/inventory/lock")
    R<Void> lockStock(@RequestBody StockLockRequest request);

    @PostMapping("/internal/material/inventory/deduct")
    R<Void> deductStock(@RequestBody StockDeductRequest request);

    @PostMapping("/internal/material/inventory/release")
    R<Void> releaseStock(@RequestBody StockLockRequest request);
}
