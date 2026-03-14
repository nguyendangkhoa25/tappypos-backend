package com.knp.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Custom Logback converter to extract tenant ID from MDC (Mapped Diagnostic Context)
 * This allows creating separate log files per tenant
 */
public class TenantConverter extends ClassicConverter {

    private static final String TENANT_KEY = "tenantId";
    private static final String DEFAULT_TENANT = "default";

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdcMap = event.getMDCPropertyMap();

        if (mdcMap == null) {
            return DEFAULT_TENANT;
        }

        String tenantId = mdcMap.get(TENANT_KEY);

        return (tenantId != null && !tenantId.isEmpty())
                ? tenantId
                : DEFAULT_TENANT;
    }
}

