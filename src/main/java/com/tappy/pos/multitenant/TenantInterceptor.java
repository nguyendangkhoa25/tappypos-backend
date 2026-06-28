package com.tappy.pos.multitenant;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
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
    private final FeatureContext featureContext;

    // Paths that don't require tenant header (always use master DB)
    private static final String[] PUBLIC_PATHS = {
            "/api/tenants",              // Get available tenants + self-provision
            "/api/swagger-ui",           // Swagger UI
            "/api/v3/api-docs",          // API docs
            "/api/actuator",             // Health check (context path is /api)
            "/api/contact",              // Public lead capture from landing page
            "/api/payments/webhook",     // Server-to-server payment callbacks (signature-verified)
            "/api/integrations/oauth",   // OAuth2 callback from providers (no tenant header)
            "/api/shop-types",           // Onboarding: shop type list (no tenant yet)
            "/api/product-templates",    // Onboarding: product template list
            "/api/expense-suggestions"   // Onboarding: expense suggestions
    };

    // Paths that support both tenant and non-tenant access
    private static final String[] FLEXIBLE_PATHS = {
            "/api/auth",
            "/api/users",
            "/api/multi-tenants",        // Tenant management (master DB only)
            "/api/profiles",             // Profile management (works for both master and tenant users)
            "/api/feedback",             // Feedback (stored in master DB, accessible from any tenant)
            "/api/product-catalog",      // Product catalog (master DB, accessible from master and tenant contexts)
            "/api/master-dashboard",     // Master dashboard stats (accessible to master admin and agents)
            "/api/invitations",          // Join-by-code: preview/join are tenant-agnostic (the code
                                         // identifies the shop); a not-yet-joined user has no tenant.
            "/api/public"                // QR customer ordering: customer page sends X-Tenant-ID from the
                                         // URL, so context is set + RLS-scoped before the transaction.
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

        // Soft-deleted shops: return 410 Gone so mobile can detect and route to onboarding
        if (tenant.isDeleted()) {
            log.warn("Deleted tenant {} attempted access to {}", tenantId, requestPath);
            response.setStatus(HttpStatus.GONE.value());
            response.setContentType("application/json");
            String message = messageService.getMessage("error.tenant.deleted");
            response.getWriter().write(
                    "{\"success\": false, \"error\": \"SHOP_DELETED\", " +
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

        // Read-only ("gray-out") mode for expired tenants: reads (GET/HEAD/OPTIONS) still succeed so
        // the shop can log in and view all its data, but mutating requests are rejected with
        // TENANT_READONLY until the subscription is renewed. Auth/subscription/payment paths stay
        // writable so the user can authenticate, see the expiry banner, and pay to renew. (Master
        // tenant is handled earlier and never carries an expiration date.)
        if (isTenantExpired(tenant) && isMutatingMethod(request.getMethod()) && !isReadOnlyExemptPath(requestPath)) {
            log.warn("Tenant {} is expired — read-only mode, blocking {} {}",
                    tenantId, request.getMethod(), requestPath);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");

            String message = messageService.getMessage("error.tenant.readonly");
            String expirationDate = tenant.getExpirationDate() != null ? tenant.getExpirationDate().toString() : null;
            String errorResponse = expirationDate != null
                    ? "{\"success\": false, \"error\": \"TENANT_READONLY\", " +
                        "\"message\": \"" + message + "\", \"field\": \"" + expirationDate + "\"}"
                    : "{\"success\": false, \"error\": \"TENANT_READONLY\", \"message\": \"" + message + "\"}";

            response.getWriter().write(errorResponse);
            tenantContext.clear();
            return false;
        }

        // Stale-token detection: the tenant's features changed after this token was issued
        // (master admin upgraded/downgraded the package). Signal the client to refresh — both
        // web and mobile refresh silently on a 401 that is not DEVICE_SWITCHED, picking up the
        // new features within seconds and without logging the user out.
        // Skipped for /api/auth so the refresh call itself is never blocked, and for older
        // tokens that carry no `fv` claim (they refresh naturally within the token lifetime).
        if (!requestPath.startsWith("/api/auth")) {
            Integer tokenFv = featureContext.getTokenFeaturesVersion();
            if (tokenFv != null
                    && tenant.getFeaturesVersion() != null
                    && !tokenFv.equals(tenant.getFeaturesVersion())) {
                log.info("Stale token for tenant {} (token fv={}, current fv={}) — signalling refresh",
                        tenantId, tokenFv, tenant.getFeaturesVersion());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                String message = messageService.getMessage("error.token.stale");
                response.getWriter().write(
                        "{\"success\": false, \"error\": \"TOKEN_STALE\", \"message\": \"" + message + "\"}");
                tenantContext.clear();
                return false;
            }
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

    // When a tenant is expired (read-only mode), writes to these path prefixes are still allowed so
    // the user can authenticate and pay to renew. Kept intentionally narrow: reads (GET/HEAD/OPTIONS)
    // already pass the gate regardless of path, so viewing the current plan (GET /subscriptions/current)
    // needs no exemption — only the auth and renewal-payment write flows do.
    private static final String[] READONLY_EXEMPT_PREFIXES = {
            "/api/auth",           // login / refresh / logout / force-login
            "/api/payments"        // renew / checkout
    };

    /**
     * A tenant is expired only once its expiration date is strictly in the past (it keeps the full
     * expiration day). Delegates to {@link Tenant#isExpired()} so this read-only gate and the
     * status reported by {@code SubscriptionServiceImpl} never disagree by a day.
     */
    private boolean isTenantExpired(Tenant tenant) {
        return tenant.isExpired();
    }

    /** Mutating HTTP verbs are blocked in read-only mode; GET/HEAD/OPTIONS pass through. */
    private boolean isMutatingMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    /** True for paths that must stay writable even when the tenant is in read-only mode. */
    private boolean isReadOnlyExemptPath(String requestPath) {
        for (String prefix : READONLY_EXEMPT_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}

