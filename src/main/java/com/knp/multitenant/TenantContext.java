package com.knp.multitenant;

import com.knp.exception.TenantExpiredException;
import com.knp.model.entity.Tenant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TenantContext holds the current tenant information for the request
 * This is thread-safe as it uses ThreadLocal
 */
@Component
@Slf4j
public class TenantContext {

    private static final ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();
    private static final String MDC_TENANT_KEY = "tenantId";

    /**
     * Set the current tenant for this thread/request
     */
    public void setCurrentTenant(Tenant tenant) {
        String tenantId = tenant.getTenantId();
        currentTenant.set(tenant);
        MDC.put(MDC_TENANT_KEY, tenantId);
    }

    /**
     * Get the current tenant
     */
    public Tenant getCurrentTenant() {
        return currentTenant.get();
    }

    /**
     * Get the current tenant ID
     */
    public String getCurrentTenantId() {
        Tenant tenant = currentTenant.get();
        return tenant != null ? tenant.getTenantId() : null;
    }

    /**
     * Clear the tenant context after request is complete
     */
    public void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
        MDC.remove(MDC_TENANT_KEY);
    }

    /**
     * Validate that the tenant is not expired
     * Throws TenantExpiredException if tenant has expired
     * Note: If expiration date is today or earlier, tenant is considered expired
     */
    public void validateTenantNotExpired() {
        Tenant tenant = currentTenant.get();
        if (tenant == null) {
            log.warn("validateTenantNotExpired called but no tenant is set in context");
            return;
        }

        // If tenant has no expiration date, it never expires
        if (tenant.getExpirationDate() == null) {
            log.debug("Tenant {} has no expiration date, access granted", tenant.getTenantId());
            return;
        }

        // Check if expiration date has passed (including today)
        LocalDate now = LocalDate.now();
        if (!now.isBefore(tenant.getExpirationDate())) {
            // Tenant has expired (expiration date is today or in the past)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = tenant.getExpirationDate().format(formatter);
            log.warn("Tenant {} has expired on {}. Current time: {}",
                    tenant.getTenantId(), formattedDate, now.format(formatter));
            throw new TenantExpiredException(tenant.getTenantId(), formattedDate);
        }

        // Warn if tenant is about to expire (within 7 days)
        if (now.plusDays(7).isAfter(tenant.getExpirationDate())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            log.info("Tenant {} will expire soon on {}", tenant.getTenantId(),
                    tenant.getExpirationDate().format(formatter));
        }
    }
}

