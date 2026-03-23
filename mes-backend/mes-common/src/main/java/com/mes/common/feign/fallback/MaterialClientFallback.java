package com.mes.common.feign.fallback;

import com.mes.common.feign.client.MaterialClient;
import com.mes.common.feign.dto.StockDeductRequest;
import com.mes.common.feign.dto.StockLockRequest;
import com.mes.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MaterialClientFallback implements FallbackFactory<MaterialClient> {

    @Override
    public MaterialClient create(Throwable cause) {
        log.error("MaterialClient fallback triggered: {}", cause.getMessage());
        return new MaterialClient() {
            @Override
            public R<Void> lockStock(StockLockRequest request) {
                return R.fail(500, "物料服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Void> deductStock(StockDeductRequest request) {
                return R.fail(500, "物料服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Void> releaseStock(StockLockRequest request) {
                return R.fail(500, "物料服务不可用: " + cause.getMessage());
            }
        };
    }
}
