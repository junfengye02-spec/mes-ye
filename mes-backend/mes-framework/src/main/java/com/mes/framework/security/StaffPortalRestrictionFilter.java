package com.mes.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.common.result.R;
import com.mes.common.result.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 现场端账号（STAFF）禁止调用 /system/** 管理接口。
 */
@Component
@RequiredArgsConstructor
public class StaffPortalRestrictionFilter extends OncePerRequestFilter {

    public static final String ACCOUNT_STAFF = "STAFF";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getServletPath();
            if (path != null && path.startsWith("/system")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
                    if (ACCOUNT_STAFF.equals(lu.getAccountType())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.getWriter().write(objectMapper.writeValueAsString(R.fail(ResultCode.FORBIDDEN)));
                        return;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            com.mes.framework.tenant.TenantContextHolder.clear();
        }
    }
}
