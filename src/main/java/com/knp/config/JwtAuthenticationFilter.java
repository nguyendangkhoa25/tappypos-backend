package com.knp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.model.dto.ApiResponse;
import com.knp.service.SessionRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter - Extracts JWT token from request header and validates it.
 * Also enforces single-device login by checking the session ID against SessionRegistry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthContext authContext;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String sessionId = jwtTokenProvider.getSessionIdFromToken(jwt);
                Boolean isMasterUser = jwtTokenProvider.isMasterUserFromToken(jwt);

                String tenantKey = resolveTenantKey(request, isMasterUser);

                if (!sessionRegistry.isValid(tenantKey, username, sessionId)) {
                    log.warn("Session invalidated (device switch): tenant={} user={}", tenantKey, username);
                    writeDeviceSwitchedResponse(response);
                    return;
                }

                log.debug("JWT token valid for user: {}", username);
                authContext.setCurrentUsername(username);
                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String resolveTenantKey(HttpServletRequest request, Boolean isMasterUser) {
        if (Boolean.TRUE.equals(isMasterUser)) {
            return SessionRegistry.MASTER_KEY;
        }
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank() || "master".equalsIgnoreCase(tenantId.trim())) {
            return SessionRegistry.MASTER_KEY;
        }
        return tenantId.trim();
    }

    private void writeDeviceSwitchedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .error("DEVICE_SWITCHED")
                .message("Your session has been ended because you logged in from another device.")
                .build();
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
