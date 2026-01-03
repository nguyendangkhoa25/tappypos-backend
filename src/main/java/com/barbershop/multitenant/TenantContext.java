package com.barbershop.multitenant;

import com.barbershop.model.entity.Tenant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

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
}

