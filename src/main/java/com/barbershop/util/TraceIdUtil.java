package com.barbershop.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Utility class for trace ID management
 */
public class TraceIdUtil {
    TraceIdUtil() {
    }


    private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();

    /**
     * Generate a new trace ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get or generate trace ID
     */
    public static String getOrGenerateTraceId() {
        String traceId = getTraceId();
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * Set trace ID in thread local
     */
    public static void setTraceId(String traceId) {
        traceIdHolder.set(traceId);
    }

    /**
     * Get trace ID from thread local
     */
    public static String getTraceId() {
        return traceIdHolder.get();
    }

    /**
     * Clear trace ID from thread local
     */
    public static void clearTraceId() {
        traceIdHolder.remove();
    }

    /**
     * Get client IP address
     */
    public static String getClientIpAddress() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
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
        } catch (Exception e) {
            // Return localhost if we can't get from request context
        }
        return "UNKNOWN";
    }
}

