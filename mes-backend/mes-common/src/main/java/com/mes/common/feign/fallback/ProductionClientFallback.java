package com.mes.common.feign.fallback;

import com.mes.common.feign.client.ProductionClient;
import com.mes.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ProductionClientFallback implements FallbackFactory<ProductionClient> {

    @Override
    public ProductionClient create(Throwable cause) {
        log.error("ProductionClient fallback triggered: {}", cause.getMessage());
        return new ProductionClient() {
            @Override
            public R<Map<String, Object>> getWorkOrder(Long workOrderId) {
                return R.fail(500, "生产服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, Object>> getWorkOrderByNo(String workOrderNo) {
                return R.fail(500, "生产服务不可用: " + cause.getMessage());
            }
        };
    }
}
