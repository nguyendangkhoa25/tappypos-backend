package com.tappy.pos.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public class TraceIdUtil {
    private TraceIdUtil() {}

    private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();

    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    public static String getOrGenerateTraceId() {
        String traceId = traceIdHolder.get();
        if (traceId == null) {
            traceId = generateTraceId();
            traceIdHolder.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        traceIdHolder.set(traceId);
    }

    public static String getTraceId() {
        return traceIdHolder.get();
    }

    public static void clearTraceId() {
        traceIdHolder.remove();
    }

    public static String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }
}
