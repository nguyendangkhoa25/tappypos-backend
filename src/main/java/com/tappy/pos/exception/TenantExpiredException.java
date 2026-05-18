package com.tappy.pos.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when a tenant has expired and cannot access the system
 */
@Getter
@Slf4j
public class TenantExpiredException extends RuntimeException {
    private final String tenantId;
    private final String expirationDate;

    public TenantExpiredException(String tenantId, String expirationDate) {
        super(String.format("Tenant '%s' has expired on %s. Please contact support to renew.", tenantId, expirationDate));
        this.tenantId = tenantId;
        this.expirationDate = expirationDate;
        log.error("Tenant access denied due to expiration: {} (expired on {})", tenantId, expirationDate);
    }
}

