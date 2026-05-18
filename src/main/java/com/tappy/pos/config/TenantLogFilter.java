package com.tappy.pos.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filter to accept only logs with tenant ID in MDC
 */
public class TenantLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String tenantId = event.getMDCPropertyMap().get("tenantId");

        // Accept if tenant ID exists and is not "default"
        if (tenantId != null && !tenantId.isEmpty() && !"default".equals(tenantId)) {
            return FilterReply.ACCEPT;
        }

        // Deny if no tenant ID or default
        return FilterReply.DENY;
    }
}

