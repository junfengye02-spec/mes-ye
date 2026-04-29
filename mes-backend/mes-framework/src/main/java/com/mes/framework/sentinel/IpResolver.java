package com.mes.framework.sentinel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 解析工具
 *
 * <p>多层反向代理环境下（Nginx / API Gateway / CLB），
 * 优先取 {@code X-Forwarded-For} 首个非 unknown 的 IP，兜底取 {@code remoteAddr}。</p>
 *
 * <p>此类专供 Sentinel 热点参数限流使用，与 {@code com.mes.framework.ratelimit.RateLimitFilter}
 * 内部的同名实现保持一致。</p>
 */
public final class IpResolver {

    private IpResolver() {
    }

    /** 无法取到 IP 时的占位符（Sentinel 热点参数不能为 null） */
    public static final String UNKNOWN_IP = "UNKNOWN";

    /**
     * 从当前 Spring 请求上下文解析客户端 IP
     *
     * @return 客户端 IP 字符串；若不在 HTTP 线程中则返回 {@link #UNKNOWN_IP}
     */
    public static String currentIp() {
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) {
                return UNKNOWN_IP;
            }
            return resolveIp(attr.getRequest());
        } catch (Exception e) {
            return UNKNOWN_IP;
        }
    }

    /**
     * 从给定请求对象解析客户端 IP
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 字符串
     */
    public static String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }
        String ip = firstNonEmpty(request.getHeader("X-Forwarded-For"), request.getHeader("X-Real-IP"));
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return StringUtils.hasText(ip) ? ip : UNKNOWN_IP;
    }

    /** 取第一个非空串 */
    private static String firstNonEmpty(String a, String b) {
        if (StringUtils.hasText(a) && !"unknown".equalsIgnoreCase(a)) {
            return a;
        }
        return b;
    }
}
