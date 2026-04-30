package com.knp.multitenant;

import com.knp.model.entity.tenant.Tenant;
import com.knp.repository.tenant.TenantRepository;
import com.knp.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * TenantInterceptor - Validates X-Tenant-ID header on all protected requests
 * Uses i18n messages for error responses
 * <p>
 * Public Paths (no header required):
 * - /api/tenants - Get available tenants
 * - /api/swagger-ui - Swagger documentation
 * - /api/v3/api-docs - API documentation
 * - /actuator - Health check
 * <p>
 * Protected Paths (header required):
 * - All other endpoints require X-Tenant-ID header
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String MASTER_TENANT = "master";
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;

    // Paths that don't require tenant header (always use master DB)
    private static final String[] PUBLIC_PATHS = {
            "/api/tenants",              // Get available tenants
            "/api/swagger-ui",           // Swagger UI
            "/api/v3/api-docs",          // API docs
            "/actuator"                  // Health check
    };

    // Paths that support both tenant and non-tenant access
    private static final String[] FLEXIBLE_PATHS = {
            "/api/auth",
            "/api/users",
            "/api/employees",
            "/api/multi-tenants",        // Tenant management (master DB only)
            "/api/profiles",             // Profile management (works for both master and tenant users)
            "/api/feedback"              // Feedback (stored in master DB, accessible from any tenant)
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.info("Intercepting request: {} {}", request.getMethod(), request.getRequestURI());
        String requestPath = request.getRequestURI();

        // Skip tenant validation for public paths (always use master DB)
        if (isPublicPath(requestPath)) {
            log.debug("Public path accessed: {}", requestPath);
            return true;
        }

        // Handle flexible paths (auth endpoints) - support both tenant and non-tenant
        if (isFlexiblePath(requestPath)) {
            return handleFlexiblePath(request, response, requestPath);
        }

        // For all other protected paths, tenant header is mandatory
        return handleProtectedPath(request, response, requestPath);
    }

    /**
     * Handle flexible paths that support both tenant and non-tenant access
     * - With X-Tenant-ID header: validate tenant and set context (use tenant DB)
     * - Without X-Tenant-ID header: use master DB
     */
    private boolean handleFlexiblePath(HttpServletRequest request, HttpServletResponse response, String requestPath)
            throws Exception {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            // No tenant header - use master database
            log.info("Non-tenant request to {}: using master database", requestPath);
            // Don't set tenant context - will use master DB
            return true;
        }

        // Trim tenantId for consistency
        tenantId = tenantId.trim();

        // Special case: "master" tenant uses master database without validation
        if (MASTER_TENANT.equalsIgnoreCase(tenantId)) {
            log.info("Master tenant request to {}: using master database directly", requestPath);
            return true;
        }

        // Tenant header present - validate tenant and set context
        log.info("Tenant request to {} with tenant ID: {}", requestPath, tenantId);
        return validateTenantAndSetContext(request, response, requestPath, tenantId);
    }

    /**
     * Handle protected paths that require tenant header
     */
    private boolean handleProtectedPath(HttpServletRequest request, HttpServletResponse response, String requestPath)
            throws Exception {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Request to protected path {} without X-Tenant-ID header", requestPath);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");

            // Get i18n message for missing header
            String message = messageService.getMessage("error.tenant.header.required");
            response.getWriter().write(
                    "{\"success\": false, \"error\": \"BAD_REQUEST\", " +
                            "\"message\": \"" + message + "\"}"
            );
            return false;
        }

        // Trim tenantId for consistency
        tenantId = tenantId.trim();

        // Special case: "master" tenant uses master database without validation
        if (MASTER_TENANT.equalsIgnoreCase(tenantId)) {
            log.info("Master tenant request to {}: using master database directly", requestPath);
            return true;
        }

        return validateTenantAndSetContext(request, response, requestPath, tenantId);
    }

    /**
     * Validate tenant and set context
     */
    private boolean validateTenantAndSetContext(HttpServletRequest request, HttpServletResponse response,
                                                String requestPath, String tenantId) throws Exception {
        // Fetch tenant from database
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElse(null);

        if (tenant == null) {
            log.warn("Invalid tenant ID: {} in request to {}", tenantId, requestPath);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");
            
            // Get i18n message for invalid tenant
            String message = messageService.getMessage("error.tenant.invalid");
            response.getWriter().write(
                    "{\"success\": false, \"error\": \"TENANT_NOT_FOUND\", " +
                            "\"message\": \"" + message + "\"}"
            );
            return false;
        }

        if (!tenant.isActive()) {
            log.warn("Inactive tenant {} attempted access to {}", tenantId, requestPath);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            
            // Get i18n message for inactive tenant
            String message = messageService.getMessage("error.tenant.inactive");
            response.getWriter().write(
                    "{\"success\": false, \"error\": \"FORBIDDEN\", " +
                            "\"message\": \"" + message + "\"}"
            );
            return false;
        }

        // Set current tenant in context for use in request processing
        tenantContext.setCurrentTenant(tenant);

        // Validate tenant is not expired
        try {
            tenantContext.validateTenantNotExpired();
        } catch (Exception e) {
            log.warn("Tenant {} is expired, rejecting request", tenantId);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");

            // Get i18n message for expired tenant
            String message = messageService.getMessage("error.tenant.expired");
            
            // Extract expiration date from exception if available
            String expirationDate = null;
            if (e instanceof com.knp.exception.TenantExpiredException) {
                expirationDate = ((com.knp.exception.TenantExpiredException) e).getExpirationDate();
            }

            // Build error response with expiration date
            String errorResponse;
            if (expirationDate != null) {
                errorResponse = "{\"success\": false, \"error\": \"TENANT_EXPIRED\", " +
                        "\"message\": \"" + message + "\", " +
                        "\"field\": \"" + expirationDate + "\"}";
            } else {
                errorResponse = "{\"success\": false, \"error\": \"TENANT_EXPIRED\", " +
                        "\"message\": \"" + message + "\"}";
            }

            response.getWriter().write(errorResponse);
            tenantContext.clear();
            return false;
        }

        log.debug("Tenant {} authorized for request to {}", tenantId, requestPath);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clear tenant context after request is complete
        // This prevents data leakage between requests
        tenantContext.clear();
    }

    /**
     * Check if the request path is a public path that doesn't require X-Tenant-ID header
     */
    private boolean isPublicPath(String requestPath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the request path is a flexible path that supports both tenant and non-tenant access
     */
    private boolean isFlexiblePath(String requestPath) {
        for (String flexiblePath : FLEXIBLE_PATHS) {
            if (requestPath.startsWith(flexiblePath)) {
                return true;
            }
        }
        return false;
    }
}

