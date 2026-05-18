package com.tappy.pos.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import lombok.Setter;

/**
 * Discriminator for routing log events to different appenders based on tenant ID
 */
@Setter
public class TenantDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

    private static final String KEY = "tenantId";
    private static final String DEFAULT_VALUE = "default";

    private String key = KEY;
    private String defaultValue = DEFAULT_VALUE;

    @Override
    public String getDiscriminatingValue(ILoggingEvent event) {
        // Get tenant ID from the event's MDC property map
        String tenantId = event.getMDCPropertyMap().get(key);

        if (tenantId == null || tenantId.isEmpty()) {
            // Use default value for non-tenant logs
            return defaultValue;
        }

        // Return the actual tenant ID
        return tenantId;
    }

    @Override
    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void start() {
        started = true;
    }

}

