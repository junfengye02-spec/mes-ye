package com.mes.common.feign.fallback;

import com.mes.common.feign.client.MasterDataClient;
import com.mes.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MasterDataClientFallback implements FallbackFactory<MasterDataClient> {

    @Override
    public MasterDataClient create(Throwable cause) {
        log.error("MasterDataClient fallback triggered: {}", cause.getMessage());
        return new MasterDataClient() {
            @Override
            public R<Map<String, Object>> getMaterial(Long materialId) {
                return R.fail(500, "主数据服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, Object>> getMaterialByCode(String materialCode) {
                return R.fail(500, "主数据服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, Object>> getWorkCenter(Long workCenterId) {
                return R.fail(500, "主数据服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, Object>> getProcessInfo(Long processId) {
                return R.fail(500, "主数据服务不可用: " + cause.getMessage());
            }
        };
    }
}
