package com.knp.aspect;

import com.knp.config.JwtTokenProvider;
import com.knp.exception.ForbiddenException;
import com.knp.multitenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MasterDatabaseAccessAspect Unit Tests")
class MasterDatabaseAccessAspectTest {

    @Mock
    private TenantContext tenantContext;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private MasterDatabaseAccessAspect aspect;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String MASTER_ROLE = "MASTER_TENANT";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @BeforeEach
    void setUp() {
        // Reset request context before each test
        RequestContextHolder.resetRequestAttributes();
    }

    // ============= SUCCESS SCENARIOS =============

    @Test
    @DisplayName("Should allow access when all checks pass - tenant context null, valid token, MASTER_TENANT role, isMasterUser true")
    void testCheckMasterDatabaseAccess_AllChecksPass() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("Success");

        // When
        Object result = aspect.checkMasterDatabaseAccess(joinPoint);

        // Then
        assertThat(result).isEqualTo("Success");
        verify(joinPoint, times(1)).proceed();
        verify(tenantContext, times(1)).getCurrentTenantId();
        verify(jwtTokenProvider, times(1)).getRolesFromToken(VALID_TOKEN);
        verify(jwtTokenProvider, times(1)).isMasterUserFromToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should allow access when user has multiple roles including MASTER_TENANT")
    void testCheckMasterDatabaseAccess_MultipleRolesIncludeMasterTenant() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN))
                .thenReturn(List.of("USER", "ADMIN", MASTER_ROLE, "SUPER_ADMIN"));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("AccessGranted");

        // When
        Object result = aspect.checkMasterDatabaseAccess(joinPoint);

        // Then
        assertThat(result).isEqualTo("AccessGranted");
        verify(joinPoint, times(1)).proceed();
    }

    // ============= TENANT CONTEXT CHECK FAILURE =============

    @Test
    @DisplayName("Should deny access when user has tenant context set")
    void testCheckMasterDatabaseAccess_TenantContextExists() throws Throwable {
        // Given
        String tenantId = "tenant-123";
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
        verify(jwtTokenProvider, never()).getRolesFromToken(any());
    }

    // ============= JWT TOKEN EXTRACTION FAILURE =============

    @Test
    @DisplayName("Should deny access when no request attributes available")
    void testCheckMasterDatabaseAccess_NoRequestAttributes() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        RequestContextHolder.resetRequestAttributes();

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Should deny access when Authorization header is missing")
    void testCheckMasterDatabaseAccess_NoAuthorizationHeader() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
        verify(jwtTokenProvider, never()).getRolesFromToken(any());
    }

    @Test
    @DisplayName("Should deny access when Authorization header doesn't have Bearer prefix")
    void testCheckMasterDatabaseAccess_InvalidAuthorizationHeaderFormat() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn("InvalidToken");

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
        verify(jwtTokenProvider, never()).getRolesFromToken(any());
    }

    @Test
    @DisplayName("Should deny access when Authorization header is empty string")
    void testCheckMasterDatabaseAccess_EmptyAuthorizationHeader() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn("");

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    // ============= ROLE CHECK FAILURE =============

    @Test
    @DisplayName("Should deny access when user doesn't have MASTER_TENANT role")
    void testCheckMasterDatabaseAccess_MissingMasterTenantRole() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of("USER", "ADMIN"));

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
        verify(jwtTokenProvider, never()).isMasterUserFromToken(any());
    }

    @Test
    @DisplayName("Should deny access when roles list is null")
    void testCheckMasterDatabaseAccess_NullRolesList() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Should deny access when roles list is empty")
    void testCheckMasterDatabaseAccess_EmptyRolesList() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    // ============= isMasterUser FLAG CHECK FAILURE =============

    @Test
    @DisplayName("Should deny access when isMasterUser flag is false")
    void testCheckMasterDatabaseAccess_IsMasterUserFalse() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Should deny access when isMasterUser flag is null")
    void testCheckMasterDatabaseAccess_IsMasterUserNull() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    // ============= EDGE CASES =============

    @Test
    @DisplayName("Should deny access when token is empty string")
    void testCheckMasterDatabaseAccess_EmptyToken() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Should allow access with Bearer prefix variations")
    void testCheckMasterDatabaseAccess_BearerPrefixExactMatch() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        // Exact Bearer prefix with space
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("Success");

        // When
        Object result = aspect.checkMasterDatabaseAccess(joinPoint);

        // Then
        assertThat(result).isEqualTo("Success");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Should extract token correctly from Authorization header")
    void testCheckMasterDatabaseAccess_TokenExtractionCorrect() throws Throwable {
        // Given
        String complexToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + complexToken);
        when(jwtTokenProvider.getRolesFromToken(complexToken)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(complexToken)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("TokenProcessed");

        // When
        Object result = aspect.checkMasterDatabaseAccess(joinPoint);

        // Then
        assertThat(result).isEqualTo("TokenProcessed");
        verify(jwtTokenProvider, times(1)).getRolesFromToken(complexToken);
        verify(jwtTokenProvider, times(1)).isMasterUserFromToken(complexToken);
    }

    @Test
    @DisplayName("Should verify exception message is correct for all failure scenarios")
    void testCheckMasterDatabaseAccess_ExceptionMessageConsistency() throws Throwable {
        // Test 1: Tenant context exists
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant-1");
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        // Test 2: No request attributes
        reset(tenantContext);
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        RequestContextHolder.resetRequestAttributes();
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        // Test 3: Missing role
        reset(tenantContext);
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of("OTHER_ROLE"));
        
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");
    }

    @Test
    @DisplayName("Should handle joinPoint exception propagation")
    void testCheckMasterDatabaseAccess_JoinPointThrowsException() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        
        RuntimeException joinPointException = new RuntimeException("Method execution failed");
        when(joinPoint.proceed()).thenThrow(joinPointException);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Method execution failed");
    }

    @Test
    @DisplayName("Should handle case sensitive Bearer prefix")
    void testCheckMasterDatabaseAccess_CaseSensitiveBearerPrefix() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        // lowercase "bearer" should NOT match (it's case-sensitive)
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn("bearer " + VALID_TOKEN);

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(jwtTokenProvider, never()).getRolesFromToken(any());
    }

    @Test
    @DisplayName("Should verify all checks are performed in correct order")
    void testCheckMasterDatabaseAccess_CheckOrder() throws Throwable {
        // Given - Set up mocks
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        
        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of(MASTER_ROLE));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("Success");

        // When
        aspect.checkMasterDatabaseAccess(joinPoint);

        // Then - Verify order of invocations
        inOrder(tenantContext, jwtTokenProvider, joinPoint);
        verify(tenantContext).getCurrentTenantId();
        verify(jwtTokenProvider).getRolesFromToken(VALID_TOKEN);
        verify(jwtTokenProvider).isMasterUserFromToken(VALID_TOKEN);
        verify(joinPoint).proceed();
    }

    // ============= AGENT ROLE =============

    @Test
    @DisplayName("Should allow access when user has AGENT role and isMasterUser true")
    void testCheckMasterDatabaseAccess_AgentAllowed() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);

        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of("AGENT"));
        when(jwtTokenProvider.isMasterUserFromToken(VALID_TOKEN)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("Success");

        // When
        Object result = aspect.checkMasterDatabaseAccess(joinPoint);

        // Then
        assertThat(result).isEqualTo("Success");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Should deny access when user has neither MASTER_TENANT nor AGENT role")
    void testCheckMasterDatabaseAccess_UnrecognisedRoleDenied() throws Throwable {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);

        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        when(httpRequest.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtTokenProvider.getRolesFromToken(VALID_TOKEN)).thenReturn(List.of("SHOP_OWNER"));

        // When & Then
        assertThatThrownBy(() -> aspect.checkMasterDatabaseAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("error.access.master_only");

        verify(joinPoint, never()).proceed();
        verify(jwtTokenProvider, never()).isMasterUserFromToken(any());
    }
}



