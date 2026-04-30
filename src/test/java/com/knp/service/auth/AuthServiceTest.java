package com.knp.service.auth;

import com.knp.config.JwtTokenProvider;
import com.knp.exception.AccountLockedException;
import com.knp.exception.BadRequestException;
import com.knp.exception.DeviceConflictException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.UnauthorizedException;
import com.knp.model.dto.auth.AuthResponse;
import com.knp.model.dto.auth.LoginRequest;
import com.knp.model.dto.auth.UserDTO;
import com.knp.model.entity.auth.RefreshToken;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.auth.User;
import com.knp.model.entity.tenant.Tenant;
import com.knp.multitenant.TenantContext;
import com.knp.repository.auth.RefreshTokenRepository;
import com.knp.repository.auth.UserRepository;
import com.knp.service.MessageService;
import com.knp.service.audit.ActivityLogService;
import com.knp.service.tenant.TenantFeatureService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantContext tenantContext;
    @Mock private RoleFeatureService roleFeatureService;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private MessageService messageService;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        Role shopOwnerRole = Role.builder().name("SHOP_OWNER").build();
        shopOwnerRole.setId(1L);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .fullName("Test User")
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction(null)
                .roles(new HashSet<>(Set.of(shopOwnerRole)))
                .build();
        testUser.setId(1L);
        testUser.setCreatedAt(LocalDateTime.now());

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        testTenant = new Tenant();
        testTenant.setTenantId("shop1");
    }

    // ─── authenticateUser ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authenticateUser")
    class AuthenticateUser {

        @Test
        @DisplayName("Should authenticate successfully in tenant context")
        void success_tenantContext() {
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of("DASHBOARD"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"), anyList(), anyList(), eq(false), anyString(), any(), eq("shop1"));
        }

        @Test
        @DisplayName("Should authenticate successfully as master user (no tenant)")
        void success_masterUser() {
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of("DASHBOARD"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("master-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response.getAccessToken()).isEqualTo("master-token");
            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"), anyList(), anyList(), eq(true), anyString(), any(), eq("master"));
        }

        @Test
        @DisplayName("Should set rememberMe refresh token when requested")
        void success_withRememberMe() {
            loginRequest.setRememberMe(true);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("raw-refresh-token");
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(passwordEncoder.encode("raw-refresh-token")).thenReturn("hashed-refresh");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should not generate refresh token when rememberMe is false")
        void success_withoutRememberMe() {
            loginRequest.setRememberMe(false);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response.getRefreshToken()).isNull();
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw AccountLockedException when account is locked")
        void fail_accountLocked() {
            testUser.setAccountNonLocked(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.account.locked")).thenReturn("Account locked");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when account is inactive")
        void fail_inactiveAccount() {
            testUser.setActive(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.user.inactive")).thenReturn("Inactive");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException on wrong password")
        void fail_wrongPassword() {
            testUser.setFailedLoginAttempts(0);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(messageService.getMessage(eq("error.invalid.credentials.remaining"), anyInt())).thenReturn("4 attempts left");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should lock account after 5 failed attempts")
        void fail_lockAfterMaxAttempts() {
            testUser.setFailedLoginAttempts(4);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(messageService.getMessage("error.account.locked")).thenReturn("Locked");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(AccountLockedException.class);
            assertThat(testUser.getAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("Should reset failed attempts on successful login")
        void success_resetFailedAttempts() {
            testUser.setFailedLoginAttempts(3);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
            verify(userRepository, atLeastOnce()).save(testUser);
        }

        @Test
        @DisplayName("Should throw DeviceConflictException when session already exists (no force)")
        void fail_deviceConflict() {
            SessionInfo existingSession = new SessionInfo("sid", "192.168.1.1", "OtherAgent", LocalDateTime.now());
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.of(existingSession));

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "NewAgent"))
                    .isInstanceOf(DeviceConflictException.class);
        }

        @Test
        @DisplayName("Should return requiredAction response when user has pending action")
        void success_requiredAction() {
            testUser.setRequireAction("CHANGE_PASSWORD");
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("temp-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            assertThat(response.getRequiredAction()).isEqualTo("CHANGE_PASSWORD");
        }

        @Test
        @DisplayName("Should include all roles in token generation")
        void success_multipleRoles() {
            Role managerRole = Role.builder().name("MANAGER").build();
            testUser.getRoles().add(managerRole);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of("DASHBOARD", "ORDER"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"),
                    argThat(roles -> roles.size() == 2),
                    argThat(features -> features.size() == 2),
                    eq(false), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should evict existing session on force login")
        void success_forceLogin() {
            SessionInfo existing = new SessionInfo("old-sid", "1.2.3.4", "OldAgent", LocalDateTime.now());
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.of(existing));
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.forceLogin(loginRequest, "127.0.0.1", "NewAgent");

            assertThat(response).isNotNull();
            verify(sessionRegistry).register(anyString(), eq("testuser"), any(SessionInfo.class));
        }

        @Test
        @DisplayName("Should log activity for tenant user login")
        void success_logsActivityForTenantUser() {
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(activityLogService).logAsync(eq("shop1"), eq("testuser"), anyString(), any(), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should not log activity for master user login")
        void success_noActivityLogForMasterUser() {
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(activityLogService, never()).logAsync(any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    // ─── refreshAccessToken ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("Should refresh token successfully")
        void success() {
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L)
                    .build();

            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of("DASHBOARD"));
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString()))
                    .thenReturn("new-access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.refreshAccessToken("testuser", "raw-rt");

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            verify(jwtTokenProvider).generateTokenWithRolesAndFeatures(
                    eq("testuser"), anyList(), anyList(), eq(false), any(), eq("shop1"));
        }

        @Test
        @DisplayName("Should use master tenantId when no tenant context")
        void success_masterTenantId() {
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L)
                    .build();

            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString()))
                    .thenReturn("new-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.refreshAccessToken("testuser", "raw-rt");

            verify(jwtTokenProvider).generateTokenWithRolesAndFeatures(
                    eq("testuser"), anyList(), anyList(), eq(true), any(), eq("master"));
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when refresh token is blank")
        void fail_blankToken() {
            when(messageService.getMessage("error.refresh.token.invalid")).thenReturn("Invalid");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", ""))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage(eq("error.user.not.found"), anyString())).thenReturn("Not found");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "some-token"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when no active tokens found")
        void fail_noActiveTokens() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of());
            when(messageService.getMessage("error.refresh.token.invalid")).thenReturn("Invalid");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "rt"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token hash does not match")
        void fail_tokenMismatch() {
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("different-hash").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L)
                    .build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("wrong-token", "different-hash")).thenReturn(false);
            when(messageService.getMessage("error.refresh.token.invalid")).thenReturn("Invalid");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "wrong-token"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when user has pending required action")
        void fail_requiredAction() {
            testUser.setRequireAction("CHANGE_PASSWORD");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.refresh.token.required")).thenReturn("Action required");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "rt"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ─── logoutUser ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logoutUser")
    class LogoutUser {

        @Test
        @DisplayName("Should deactivate all refresh tokens and remove session")
        void success() {
            RefreshToken token = RefreshToken.builder().user(testUser).active(true).build();
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of(token));
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

            authService.logoutUser("testuser");

            assertThat(token.getActive()).isFalse();
            verify(refreshTokenRepository).save(token);
            verify(sessionRegistry).remove("shop1", "testuser");
        }

        @Test
        @DisplayName("Should use MASTER_KEY when no tenant context")
        void success_masterKey() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of());
            when(tenantContext.getCurrentTenant()).thenReturn(null);

            authService.logoutUser("testuser");

            verify(sessionRegistry).remove(SessionRegistry.MASTER_KEY, "testuser");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            when(messageService.getMessage(eq("error.user.not.found"), anyString())).thenReturn("Not found");

            assertThatThrownBy(() -> authService.logoutUser("unknown"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─── getUserProfile ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("Should return user profile DTO")
        void success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDTO dto = authService.getUserProfile("testuser");

            assertThat(dto.getUsername()).isEqualTo("testuser");
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getFullName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
            when(messageService.getMessage(eq("error.user.not.found"), anyString())).thenReturn("Not found");

            assertThatThrownBy(() -> authService.getUserProfile("nobody"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─── getRefreshToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRefreshToken")
    class GetRefreshToken {

        @Test
        @DisplayName("Should return token value from cookie")
        void success_cookieFound() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie cookie = new Cookie("refresh-token", "my-refresh-token");
            when(request.getCookies()).thenReturn(new Cookie[]{cookie});

            String result = authService.getRefreshToken(request);

            assertThat(result).isEqualTo("my-refresh-token");
        }

        @Test
        @DisplayName("Should return empty string when no cookies")
        void success_noCookies() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(null);

            assertThat(authService.getRefreshToken(request)).isEmpty();
        }

        @Test
        @DisplayName("Should return empty string when cookie not present")
        void success_cookieNotFound() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie other = new Cookie("session", "abc");
            when(request.getCookies()).thenReturn(new Cookie[]{other});

            assertThat(authService.getRefreshToken(request)).isEmpty();
        }
    }

    // ─── clearRefreshTokenCookie ─────────────────────────────────────────────────

    @Nested
    @DisplayName("clearRefreshTokenCookie")
    class ClearRefreshTokenCookie {

        @Test
        @DisplayName("Should add expired Set-Cookie header to response")
        void success() {
            HttpServletResponse response = mock(HttpServletResponse.class);

            authService.clearRefreshTokenCookie(response);

            verify(response).addHeader(eq("Set-Cookie"), argThat(v -> v.contains("refresh-token") && v.contains("Max-Age=0")));
        }
    }
}
