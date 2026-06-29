package com.tappy.pos.service.auth;

import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.exception.AccountLockedException;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.DeviceConflictException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.exception.UnauthorizedException;
import com.tappy.pos.model.dto.auth.AuthResponse;
import com.tappy.pos.model.dto.auth.LoginRequest;
import com.tappy.pos.model.dto.auth.UserDTO;
import com.tappy.pos.model.entity.auth.RefreshToken;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RefreshTokenRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.tenant.TenantFeatureService;
import com.tappy.pos.service.tenant.TenantService;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;
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
    @Mock private TenantService tenantService;
    @Mock private MessageService messageService;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private ActivityLogService activityLogService;
    @Mock private PhoneVerificationService phoneVerificationService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private User masterUser;
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

        Role masterRole = Role.builder().name("MASTER_TENANT").build();
        masterRole.setId(2L);
        masterUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .fullName("Test User")
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction(null)
                .roles(new HashSet<>(Set.of(masterRole)))
                .build();
        masterUser.setId(1L);
        masterUser.setCreatedAt(LocalDateTime.now());

        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── authenticateUser ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authenticateUser")
    class AuthenticateUser {

        @Test
        @DisplayName("Should authenticate successfully in tenant context")
        void success_tenantContext() {
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of("DASHBOARD"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"), anyList(), anyList(), eq(false), anyString(), any(), eq("shop1"), any());
        }

        @Test
        @DisplayName("Should authenticate successfully as master user (no tenant)")
        void success_masterUser() {
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(masterUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of("DASHBOARD"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("master-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response.getAccessToken()).isEqualTo("master-token");
            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"), anyList(), anyList(), eq(true), anyString(), any(), eq("master"), any());
        }

        @Test
        @DisplayName("Should set rememberMe refresh token when requested")
        void success_withRememberMe() {
            loginRequest.setRememberMe(true);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "TestAgent");

            assertThat(response.getRefreshToken()).isNull();
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not found")
        void fail_userNotFound() {
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw AccountLockedException when account is locked")
        void fail_accountLocked() {
            testUser.setAccountNonLocked(false);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.account.locked")).thenReturn("Account locked");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when account is inactive")
        void fail_inactiveAccount() {
            testUser.setActive(false);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.user.inactive")).thenReturn("Inactive");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException on wrong password")
        void fail_wrongPassword() {
            testUser.setFailedLoginAttempts(0);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of("DASHBOARD", "ORDER"));
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(jwtTokenProvider).generateTokenWithSession(
                    eq("testuser"),
                    argThat(roles -> roles.size() == 2),
                    argThat(features -> features.size() == 2),
                    eq(false), anyString(), any(), anyString(), any());
        }

        @Test
        @DisplayName("Should evict existing session on force login")
        void success_forceLogin() {
            SessionInfo existing = new SessionInfo("old-sid", "1.2.3.4", "OldAgent", LocalDateTime.now());
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.of(existing));
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(activityLogService).logAsync(eq("shop1"), eq("testuser"), anyString(), any(), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should log activity with 'master' tenant key for master user login")
        void success_noActivityLogForMasterUser() {
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(masterUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            verify(activityLogService).logAsync(eq("master"), eq("testuser"), any(), any(), any(), any(), any(), any());
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of("DASHBOARD"));
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("new-access-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.refreshAccessToken("testuser", "raw-rt");

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            verify(jwtTokenProvider).generateTokenWithRolesAndFeatures(
                    eq("testuser"), anyList(), anyList(), eq(false), any(), eq("shop1"), any());
        }

        @Test
        @DisplayName("Should use master tenantId when no tenant context")
        void success_masterTenantId() {
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L)
                    .build();

            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(masterUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(masterUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("new-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.refreshAccessToken("testuser", "raw-rt");

            verify(jwtTokenProvider).generateTokenWithRolesAndFeatures(
                    eq("testuser"), anyList(), anyList(), eq(true), any(), eq("master"), any());
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
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage(eq("error.user.not.found"), anyString())).thenReturn("Not found");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "some-token"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when no active tokens found")
        void fail_noActiveTokens() {
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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

            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of());
            when(tenantContext.getCurrentTenant()).thenReturn(null);

            authService.logoutUser("testuser");

            verify(sessionRegistry).remove(SessionRegistry.MASTER_KEY, "testuser");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsernameTenantScoped("unknown")).thenReturn(Optional.empty());
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
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));

            UserDTO dto = authService.getUserProfile("testuser");

            assertThat(dto.getUsername()).isEqualTo("testuser");
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getFullName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsernameTenantScoped("nobody")).thenReturn(Optional.empty());
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

        @Test
        @DisplayName("Should add cookie domain when configured")
        void success_withCookieDomain() {
            ReflectionTestUtils.setField(authService, "cookieDomain", ".pos.tappy.vn");
            HttpServletResponse response = mock(HttpServletResponse.class);

            authService.clearRefreshTokenCookie(response);

            verify(response).addHeader(eq("Set-Cookie"), argThat(v -> v.contains("Domain=.pos.tappy.vn")));
        }
    }

    // ─── getRefreshTokenResponseCookie ───────────────────────────────────────────

    @Nested
    @DisplayName("getRefreshTokenResponseCookie")
    class GetRefreshTokenResponseCookie {

        @Test
        @DisplayName("Should build secure HttpOnly cookie without domain by default")
        void success_noDomain() {
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

            ResponseCookie cookie = authService.getRefreshTokenResponseCookie("rt-value");

            assertThat(cookie.getName()).isEqualTo("refresh-token");
            assertThat(cookie.getValue()).isEqualTo("rt-value");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getDomain()).isNull();
        }

        @Test
        @DisplayName("Should set cookie domain when configured")
        void success_withDomain() {
            ReflectionTestUtils.setField(authService, "cookieDomain", ".pos.tappy.vn");
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

            ResponseCookie cookie = authService.getRefreshTokenResponseCookie("rt-value");

            assertThat(cookie.getDomain()).isEqualTo(".pos.tappy.vn");
        }
    }

    // ─── global lookup branches (no X-Tenant-ID header) ──────────────────────────

    @Nested
    @DisplayName("doAuthenticate global lookup")
    class GlobalLookup {

        @Test
        @DisplayName("Should restore tenant context for shop user found globally")
        void success_shopUserRestoresTenant() {
            testUser.setTenantId("shop1");
            // No tenant header initially (both the log + lookup checks see null); becomes
            // non-null only after setCurrentTenant restores it, so model that statefully.
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            doAnswer(inv -> { when(tenantContext.getCurrentTenant()).thenReturn(testTenant); return null; })
                    .when(tenantContext).setCurrentTenant(testTenant);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(tenantService.getTenantEntity("shop1")).thenReturn(testTenant);
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            assertThat(response).isNotNull();
            verify(tenantService).getTenantEntity("shop1");
            verify(tenantContext).setCurrentTenant(testTenant);
        }

        @Test
        @DisplayName("Should swallow exception when tenant restore fails")
        void success_tenantRestoreFailsGracefully() {
            testUser.setTenantId("shop1");
            when(tenantContext.getCurrentTenant()).thenReturn(null).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(tenantService.getTenantEntity("shop1")).thenThrow(new RuntimeException("boom"));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(sessionRegistry.getSession(anyString(), anyString())).thenReturn(Optional.empty());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.authenticateUser(loginRequest, "127.0.0.1", "agent");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should reject login for user with no tenant and no master role")
        void fail_noTenantNonMaster() {
            testUser.setTenantId(null); // shop owner role, no tenant
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.user.no.tenant")).thenReturn("No tenant");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not found globally")
        void fail_userNotFoundGlobal() {
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.authenticateUser(loginRequest, "127.0.0.1", "agent"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ─── refresh global lookup branches ──────────────────────────────────────────

    @Nested
    @DisplayName("refreshAccessToken global lookup")
    class RefreshGlobalLookup {

        @Test
        @DisplayName("Should restore tenant context during refresh for shop user")
        void success_restoresTenant() {
            testUser.setTenantId("shop1");
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L).build();
            when(tenantContext.getCurrentTenant()).thenReturn(null).thenReturn(testTenant);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(tenantService.getTenantEntity("shop1")).thenReturn(testTenant);
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("new-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            authService.refreshAccessToken("testuser", "raw-rt");

            verify(tenantContext).setCurrentTenant(testTenant);
        }

        @Test
        @DisplayName("Should swallow exception when tenant restore fails during refresh")
        void success_restoreFailsGracefully() {
            testUser.setTenantId("shop1");
            RefreshToken storedToken = RefreshToken.builder()
                    .user(testUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L).build();
            when(tenantContext.getCurrentTenant()).thenReturn(null).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(tenantService.getTenantEntity("shop1")).thenThrow(new RuntimeException("boom"));
            when(refreshTokenRepository.findAllByUserAndActive(eq(testUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("new-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.refreshAccessToken("testuser", "raw-rt");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should reject refresh for user with no tenant and no master role")
        void fail_noTenantNonMaster() {
            testUser.setTenantId(null);
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.user.no.tenant")).thenReturn("No tenant");

            assertThatThrownBy(() -> authService.refreshAccessToken("testuser", "raw-rt"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Should refresh for master user found globally with no tenant")
        void success_masterNoTenant() {
            masterUser.setTenantId(null);
            RefreshToken storedToken = RefreshToken.builder()
                    .user(masterUser).token("hashed-rt").active(true)
                    .expiryDate(System.currentTimeMillis() + 999999L).build();
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(userRepository.findByUsernameGlobal("testuser")).thenReturn(Optional.of(masterUser));
            when(refreshTokenRepository.findAllByUserAndActive(eq(masterUser), anyLong())).thenReturn(List.of(storedToken));
            when(passwordEncoder.matches("raw-rt", "hashed-rt")).thenReturn(true);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("new-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authService.refreshAccessToken("testuser", "raw-rt");

            assertThat(response.getAccessToken()).isEqualTo("new-token");
        }
    }

    // ─── loginWithPin ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginWithPin")
    class LoginWithPin {

        @Test
        @DisplayName("Should login with valid PIN in tenant context")
        void success_tenantContext() {
            testUser.setPinHash("hashed-pin");
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("1234", "hashed-pin")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of("POS"));
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("pin-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("raw-rt");
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(passwordEncoder.encode("raw-rt")).thenReturn("hashed-rt");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

            AuthResponse response = authService.loginWithPin("testuser", "1234", "127.0.0.1", "agent");

            assertThat(response.getAccessToken()).isEqualTo("pin-token");
            assertThat(response.getRefreshToken()).isEqualTo("raw-rt");
            verify(sessionRegistry).register(eq("shop1"), eq("testuser"), any(SessionInfo.class));
        }

        @Test
        @DisplayName("Should login with PIN as master user")
        void success_masterUser() {
            masterUser.setPinHash("hashed-pin");
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(masterUser));
            when(passwordEncoder.matches("1234", "hashed-pin")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(masterUser);
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithSession(anyString(), anyList(), anyList(), anyBoolean(), anyString(), any(), anyString(), any()))
                    .thenReturn("pin-token");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("raw-rt");
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(passwordEncoder.encode("raw-rt")).thenReturn("hashed-rt");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

            AuthResponse response = authService.loginWithPin("testuser", "1234", "127.0.0.1", "agent");

            assertThat(response).isNotNull();
            verify(sessionRegistry).register(eq(SessionRegistry.MASTER_KEY), eq("testuser"), any(SessionInfo.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when user not found")
        void fail_userNotFound() {
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.loginWithPin("testuser", "1234", "ip", "ua"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw AccountLockedException when account locked")
        void fail_locked() {
            testUser.setAccountNonLocked(false);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.account.locked")).thenReturn("Locked");

            assertThatThrownBy(() -> authService.loginWithPin("testuser", "1234", "ip", "ua"))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when account inactive")
        void fail_inactive() {
            testUser.setActive(false);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.user.inactive")).thenReturn("Inactive");

            assertThatThrownBy(() -> authService.loginWithPin("testuser", "1234", "ip", "ua"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when PIN not set")
        void fail_pinNotSet() {
            testUser.setPinHash(null);
            testUser.setFailedLoginAttempts(0);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(messageService.getMessage("error.auth.pin.invalid")).thenReturn("Invalid PIN");

            assertThatThrownBy(() -> authService.loginWithPin("testuser", "1234", "ip", "ua"))
                    .isInstanceOf(BadRequestException.class);
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw BadRequestException and lock account after max wrong PINs")
        void fail_wrongPinLocksAccount() {
            testUser.setPinHash("hashed-pin");
            testUser.setFailedLoginAttempts(4);
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("0000", "hashed-pin")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(messageService.getMessage("error.auth.pin.invalid")).thenReturn("Invalid PIN");

            assertThatThrownBy(() -> authService.loginWithPin("testuser", "0000", "ip", "ua"))
                    .isInstanceOf(BadRequestException.class);
            assertThat(testUser.getAccountNonLocked()).isFalse();
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        }
    }

    // ─── setupPin / deletePin ────────────────────────────────────────────────────

    @Nested
    @DisplayName("setupPin & deletePin")
    class PinManagement {

        @Test
        @DisplayName("setupPin: should hash and store PIN")
        void setupPin_success() {
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("1234")).thenReturn("hashed-pin");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.setupPin("testuser", "1234");

            assertThat(testUser.getPinHash()).isEqualTo("hashed-pin");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("setupPin: should throw when user not found")
        void setupPin_userNotFound() {
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.setupPin("testuser", "1234"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("deletePin: should clear PIN hash")
        void deletePin_success() {
            testUser.setPinHash("hashed-pin");
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.deletePin("testuser");

            assertThat(testUser.getPinHash()).isNull();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("deletePin: should throw when user not found")
        void deletePin_userNotFound() {
            when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
            when(messageService.getMessage("error.unauthorized")).thenReturn("Unauthorized");

            assertThatThrownBy(() -> authService.deletePin("testuser"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ─── registerUser ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("Should register new user and return tokens")
        void success() {
            when(userRepository.findByUsernameGlobal("0900000000")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tenantContext.getCurrentTenant()).thenReturn(null);
            when(tenantFeatureService.getAccessibleFeaturesByUserAndTenant(any(), anyList())).thenReturn(List.of());
            when(jwtTokenProvider.generateTokenWithRolesAndFeatures(anyString(), anyList(), anyList(), anyBoolean(), any(), anyString(), any()))
                    .thenReturn("reg-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("raw-rt");
            when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(passwordEncoder.encode("raw-rt")).thenReturn("hashed-rt");
            when(jwtTokenProvider.getTokenExpirationMs()).thenReturn(3600000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

            AuthResponse response = authService.registerUser("0900000000", "password123", "vtoken", "ip", "ua");

            assertThat(response.getAccessToken()).isEqualTo("reg-token");
            assertThat(response.getRefreshToken()).isEqualTo("raw-rt");
            verify(userRepository).save(argThat((User u) -> "0900000000".equals(u.getUsername())
                    && "0900000000".equals(u.getPhone())));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when phone already registered")
        void fail_phoneRegistered() {
            when(userRepository.findByUsernameGlobal("0900000000")).thenReturn(Optional.of(testUser));
            when(messageService.getMessage("error.auth.phone.registered")).thenReturn("Phone registered");

            assertThatThrownBy(() -> authService.registerUser("0900000000", "pw", "vtoken", "ip", "ua"))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // ─── requestPasswordReset ────────────────────────────────────────────────────

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("Should silently complete (no-op)")
        void success_noOp() {
            authService.requestPasswordReset("0900000000");

            verifyNoInteractions(userRepository);
        }
    }
}
