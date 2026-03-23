package com.mes.common.feign.fallback;

import com.mes.common.feign.client.AuthClient;
import com.mes.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AuthClientFallback implements FallbackFactory<AuthClient> {

    @Override
    public AuthClient create(Throwable cause) {
        log.error("AuthClient fallback triggered: {}", cause.getMessage());
        return new AuthClient() {
            @Override
            public R<Map<String, Object>> getUserInfo(Long userId) {
                return R.fail(500, "认证服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Set<String>> getUserPermissions(Long userId) {
                return R.fail(500, "认证服务不可用: " + cause.getMessage());
            }
        };
    }
}
