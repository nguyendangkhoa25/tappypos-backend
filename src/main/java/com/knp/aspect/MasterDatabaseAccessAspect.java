package com.knp.aspect;

import com.knp.config.JwtTokenProvider;
import com.knp.exception.ForbiddenException;
import com.knp.multitenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Aspect to enforce master database access restrictions
 * Intercepts methods/classes annotated with @MasterDatabaseOnly
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDatabaseAccessAspect {

    private final TenantContext tenantContext;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MASTER_TENANT_ROLE = "MASTER_TENANT";

    /**
     * Check if current request is accessing master database with proper role
     * Requirements:
     * 1. No tenant context (user is logged into master database)
     * 2. User has MASTER_TENANT role
     * 3. JWT token has isMasterUser flag set to true
     */
    @Around("@within(com.knp.annotation.MasterDatabaseOnly) || @annotation(com.knp.annotation.MasterDatabaseOnly)")
    public Object checkMasterDatabaseAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String currentTenantId = tenantContext.getCurrentTenantId();

        // Check 1: User must be logged into master database (no tenant context)
        if (currentTenantId != null) {
            log.warn("Access denied to master-only endpoint. User is logged into tenant: {}", currentTenantId);
            throw new ForbiddenException("error.access.master_only");
        }

        // Get JWT token from request
        String token = extractJwtFromRequest();
        if (token == null) {
            log.warn("Access denied to master-only endpoint. No JWT token found.");
            throw new ForbiddenException("error.access.master_only");
        }

        // Check 2: User must have MASTER_TENANT role
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        if (roles == null || !roles.contains(MASTER_TENANT_ROLE)) {
            log.warn("Access denied to master-only endpoint. User does not have MASTER_TENANT role. Roles: {}", roles);
            throw new ForbiddenException("error.access.master_only");
        }

        // Check 3: JWT token must have isMasterUser flag explicitly set to true.
        Boolean isMasterUser = jwtTokenProvider.isMasterUserFromToken(token);
        if (!Boolean.TRUE.equals(isMasterUser)) {
            log.warn("Access denied to master-only endpoint. isMasterUser flag is not true: {}", isMasterUser);
            throw new ForbiddenException("error.access.master_only");
        }

        log.debug("Access granted to master-only endpoint. User is logged into master database with MASTER_TENANT role.");
        return joinPoint.proceed();
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractJwtFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}

