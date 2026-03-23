package com.mes.common.feign.client;

import com.mes.common.feign.fallback.AuthClientFallback;
import com.mes.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "mes-auth-service", fallbackFactory = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/internal/auth/user/{userId}")
    R<Map<String, Object>> getUserInfo(@PathVariable("userId") Long userId);

    @GetMapping("/internal/auth/user/{userId}/permissions")
    R<Set<String>> getUserPermissions(@PathVariable("userId") Long userId);
}
