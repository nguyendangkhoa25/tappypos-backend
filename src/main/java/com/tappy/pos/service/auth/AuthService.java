package com.tappy.pos.service.auth;

import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.exception.UnauthorizedException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.AccountLockedException;
import com.tappy.pos.exception.DeviceConflictException;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.model.dto.auth.AuthResponse;
import com.tappy.pos.model.dto.auth.LoginRequest;
import com.tappy.pos.model.dto.auth.UserDTO;
import com.tappy.pos.model.entity.auth.RefreshToken;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RefreshTokenRepository;
import com.tappy.pos.repository.auth.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tappy.pos.service.tenant.TenantFeatureService;
import com.tappy.pos.service.tenant.TenantService;
import com.tappy.pos.service.audit.ActivityLogService;

/**
 * AuthService - Authentication and token management business logic
 * Uses MessageService for i18n error messages
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
    private final RoleFeatureService roleFeatureService;
    private final TenantFeatureService tenantFeatureService;
    private final TenantService tenantService;
    private final MessageService messageService;
    private final SessionRegistry sessionRegistry;
    private final ActivityLogService activityLogService;
    public static final String REFRESH_TOKEN_HEADER_KEY = "refresh-token";
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Authenticate user with username and password.
     * Throws {@link DeviceConflictException} when a session is already active on another device.
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest, String clientIp, String userAgent) {
        return doAuthenticate(loginRequest, clientIp, userAgent, false);
    }

    /**
     * Force-login: same as authenticateUser but silently kicks out the existing session first.
     */
    public AuthResponse forceLogin(LoginRequest loginRequest, String clientIp, String userAgent) {
        return doAuthenticate(loginRequest, clientIp, userAgent, true);
    }

    private AuthResponse doAuthenticate(LoginRequest loginRequest, String clientIp, String userAgent, boolean force) {
        if (tenantContext.getCurrentTenant() != null) {
            log.info("Authenticating user: {} in TENANT database: {}",
                    loginRequest.getUsername(), tenantContext.getCurrentTenant().getTenantId());
        } else {
            log.info("Authenticating user: {} in MASTER database", loginRequest.getUsername());
        }

        User user;
        if (tenantContext.getCurrentTenant() != null) {
            user = userRepository.findByUsernameTenantScoped(loginRequest.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", loginRequest.getUsername());
                        return new BadRequestException(messageService.getMessage("error.unauthorized"));
                    });
        } else {
            // No X-Tenant-ID header — global lookup for mobile/web login without known tenant
            user = userRepository.findByUsernameGlobal(loginRequest.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found (global): {}", loginRequest.getUsername());
                        return new BadRequestException(messageService.getMessage("error.unauthorized"));
                    });
            if (user.getTenantId() != null) {
                // Shop user — restore tenant context so feature resolution works correctly
                try {
                    tenantContext.setCurrentTenant(tenantService.getTenantEntity(user.getTenantId()));
                } catch (Exception e) {
                    log.warn("Could not restore tenant context for user {}: {}", loginRequest.getUsername(), e.getMessage());
                }
            } else {
                // No tenantId — only valid for master/agent roles
                boolean isMasterRole = user.getRoles().stream()
                        .anyMatch(r -> RoleEnum.MASTER_TENANT.getCode().equals(r.getName())
                                    || RoleEnum.AGENT.getCode().equals(r.getName()));
                if (!isMasterRole) {
                    log.warn("User {} has no tenant and no master role — rejecting login", loginRequest.getUsername());
                    throw new BadRequestException(messageService.getMessage("error.user.no.tenant"));
                }
            }
        }

        if (Boolean.FALSE.equals(user.getAccountNonLocked())) {
            log.warn("Login attempt on locked account: {}", loginRequest.getUsername());
            throw new AccountLockedException(messageService.getMessage("error.account.locked"));
        }

        if (!user.getActive()) {
            log.warn("User account is inactive: {}", loginRequest.getUsername());
            throw new BadRequestException(messageService.getMessage("error.user.inactive"));
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
                userRepository.save(user);
                log.warn("Account locked after {} failed attempts: {}", attempts, loginRequest.getUsername());
                throw new AccountLockedException(messageService.getMessage("error.account.locked"));
            }

            userRepository.save(user);
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            log.warn("Invalid password for user: {}. Attempts: {}/{}.", loginRequest.getUsername(), attempts, MAX_FAILED_ATTEMPTS);
            throw new BadRequestException(messageService.getMessage("error.invalid.credentials.remaining", remaining));
        }

        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        boolean isMasterUser = tenantContext.getCurrentTenant() == null;
        String tenantKey = isMasterUser
                ? SessionRegistry.MASTER_KEY
                : tenantContext.getCurrentTenant().getTenantId();

        // Single-device check
        Optional<SessionInfo> existingSession = sessionRegistry.getSession(tenantKey, user.getUsername());
        if (existingSession.isPresent()) {
            if (!force) {
                throw new DeviceConflictException(existingSession.get());
            }
            log.info("Force-login: evicting existing session for tenant={} user={}", tenantKey, user.getUsername());
        }

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);
        log.info("User {} has access to {} features (tenant + role intersection): {}",
                loginRequest.getUsername(), featureNames.size(), featureNames);

        String sessionId = UUID.randomUUID().toString();
        String shopType = tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().getShopType() != null
                ? tenantContext.getCurrentTenant().getShopType().name()
                : null;
        String tenantId = isMasterUser ? "master" : tenantContext.getCurrentTenant().getTenantId();
        String accessToken = jwtTokenProvider.generateTokenWithSession(
                user.getUsername(),
                roleNames,
                featureNames,
                isMasterUser,
                sessionId,
                shopType,
                tenantId
        );
        log.info("Access token generated for user: {} with roles: {}, features: {}, isMasterUser: {}",
                loginRequest.getUsername(), roleNames, featureNames.size(), isMasterUser);

        sessionRegistry.register(tenantKey, user.getUsername(),
                new SessionInfo(sessionId, clientIp, userAgent, LocalDateTime.now()));

        activityLogService.logAsync(tenantKey, user.getUsername(), user.getFullName(),
                ActivityAction.LOGIN, null, null, "Đăng nhập", clientIp);

        boolean setupComplete = isMasterUser
                || (tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().isSetupComplete());

        if (StringUtils.isNotEmpty(user.getRequireAction())) {
            log.info("User {} requires action: {}", loginRequest.getUsername(), user.getRequireAction());
            return AuthResponse.requiredAction(user.getUsername(), user.getRequireAction(),
                    accessToken, jwtTokenProvider.getTokenExpirationMs());
        }

        String refreshToken = null;
        if (loginRequest.getRememberMe() != null && loginRequest.getRememberMe()) {
            refreshToken = generateRefreshToken(user);
            log.info("Refresh token generated for user: {}", loginRequest.getUsername());
        }

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getTokenExpirationMs(),
                user.getUsername(),
                1000L,
                setupComplete,
                tenantId
        );
    }

    public ResponseCookie getRefreshTokenResponseCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_HEADER_KEY, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000)
                .build();
    }

    public String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return StringUtils.EMPTY;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(REFRESH_TOKEN_HEADER_KEY)) {
                return cookie.getValue();
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Generate and store refresh token
     */
    private String generateRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(passwordEncoder.encode(token))
                .expiryDate(System.currentTimeMillis() + jwtTokenProvider.getRefreshTokenExpirationMs())
                .active(true)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token saved for user: {}", user.getUsername());
        return token;
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshAccessToken(String username, String refreshToken) {
        log.info("Refreshing access token for user: {}", username);

        if (StringUtils.isBlank(refreshToken)) {
            log.warn("Refresh token is missing for user: {}", username);
            throw new UnauthorizedException(messageService.getMessage("error.refresh.token.invalid"));
        }

        // If no X-Tenant-ID was sent (e.g. mobile refresh without header), do a global lookup
        // and restore tenant context so feature intersection works correctly.
        User user;
        if (tenantContext.getCurrentTenant() != null) {
            user = userRepository.findByUsernameTenantScoped(username)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.user.not.found", username)));
        } else {
            user = userRepository.findByUsernameGlobal(username)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.user.not.found", username)));
            if (user.getTenantId() != null) {
                try {
                    tenantContext.setCurrentTenant(tenantService.getTenantEntity(user.getTenantId()));
                } catch (Exception e) {
                    log.warn("Could not restore tenant context during token refresh for user {}: {}",
                            username, e.getMessage());
                }
            } else {
                boolean isMasterRole = user.getRoles().stream()
                        .anyMatch(r -> RoleEnum.MASTER_TENANT.getCode().equals(r.getName())
                                    || RoleEnum.AGENT.getCode().equals(r.getName()));
                if (!isMasterRole) {
                    throw new UnauthorizedException(messageService.getMessage("error.user.no.tenant"));
                }
            }
        }

        if (StringUtils.isNotEmpty(user.getRequireAction())) {
            String errorMessage = messageService.getMessage("error.refresh.token.required");
            throw new BadRequestException(errorMessage);
        }

        List<RefreshToken> tokenEntities = refreshTokenRepository.findAllByUserAndActive(user, System.currentTimeMillis());
        if (tokenEntities.isEmpty()) {
            String errorMessage = messageService.getMessage("error.refresh.token.invalid");
            throw new UnauthorizedException(errorMessage);
        }

        // Check if token is expired
        tokenEntities.stream()
                .filter(rt -> passwordEncoder.matches(refreshToken, rt.getToken()))
                .findFirst()
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.refresh.token.invalid");
                    return new UnauthorizedException(errorMessage);
                });

        // Determine if user is from master database
        boolean isMasterUser = tenantContext.getCurrentTenant() == null;

        // Extract role names from user's roles
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        // Get accessible features: intersection of tenant features and role features
        List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);
        log.info("User {} has access to {} features during token refresh (tenant + role intersection): {}",
                username, featureNames.size(), featureNames);

        // Generate new access token with roles, features and master user flag
        String refreshShopType = tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().getShopType() != null
                ? tenantContext.getCurrentTenant().getShopType().name()
                : null;
        String refreshTenantId = isMasterUser ? "master" : tenantContext.getCurrentTenant().getTenantId();
        String accessToken = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                username,
                roleNames,
                featureNames,
                isMasterUser,
                refreshShopType,
                refreshTenantId
        );
        log.info("New access token generated for user: {} with roles: {}, features: {}, isMasterUser: {}",
                username, roleNames, featureNames.size(), isMasterUser);

        boolean refreshSetupComplete = isMasterUser
                || (tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().isSetupComplete());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getTokenExpirationMs(),
                user.getUsername(),
                10000L,
                refreshSetupComplete,
                refreshTenantId
        );
    }

    /**
     * Logout user - invalidate all refresh tokens and clear session registry.
     */
    public void logoutUser(String username) {
        log.info("Logging out user: {}", username);

        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new RuntimeException(errorMessage);
                });

        refreshTokenRepository.findByUser(user).forEach(token -> {
            token.setActive(false);
            refreshTokenRepository.save(token);
        });

        boolean isMasterUser = tenantContext.getCurrentTenant() == null;
        String tenantKey = isMasterUser
                ? SessionRegistry.MASTER_KEY
                : tenantContext.getCurrentTenant().getTenantId();
        sessionRegistry.remove(tenantKey, username);

        activityLogService.logAsync(tenantKey, username, user.getFullName(),
                ActivityAction.LOGOUT, null, null, "Đã đăng xuất", null);

        log.info("User logged out: {}", username);
    }

    /**
     * Clear refresh token cookie from browser
     */
    public void clearRefreshTokenCookie(jakarta.servlet.http.HttpServletResponse response) {
        log.info("Clearing refresh token cookie");

        // Create an expired cookie to clear it from the browser
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_HEADER_KEY, "")
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Get user profile
     */
    public UserDTO getUserProfile(String username) {
        log.info("Fetching user profile: {}", username);

        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new RuntimeException(errorMessage);
                });

        return mapToDTO(user);
    }

    /**
     * Login with PIN (mobile only — no turnstile required)
     */
    public AuthResponse loginWithPin(String username, String pin, String clientIp, String userAgent) {
        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.unauthorized")));
        if (Boolean.FALSE.equals(user.getAccountNonLocked())) throw new AccountLockedException(messageService.getMessage("error.account.locked"));
        if (!user.getActive()) throw new BadRequestException(messageService.getMessage("error.user.inactive"));
        if (user.getPinHash() == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
            }
            userRepository.save(user);
            throw new BadRequestException("Sai mã PIN.");
        }
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        boolean isMasterUser = tenantContext.getCurrentTenant() == null;
        List<String> roleNames = user.getRoles().stream()
                .map(com.tappy.pos.model.entity.auth.Role::getName)
                .toList();
        List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);
        String sessionId = UUID.randomUUID().toString();
        String shopType = tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().getShopType() != null
                ? tenantContext.getCurrentTenant().getShopType().name() : null;
        String tenantId = isMasterUser ? "master" : tenantContext.getCurrentTenant().getTenantId();
        String accessToken = jwtTokenProvider.generateTokenWithSession(user.getUsername(), roleNames, featureNames, isMasterUser, sessionId, shopType, tenantId);
        String tenantKey = isMasterUser ? SessionRegistry.MASTER_KEY : tenantContext.getCurrentTenant().getTenantId();
        sessionRegistry.register(tenantKey, user.getUsername(), new SessionInfo(sessionId, clientIp, userAgent, java.time.LocalDateTime.now()));
        String refreshToken = generateRefreshToken(user);
        boolean pinSetupComplete = !isMasterUser
                && tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().isSetupComplete();
        return AuthResponse.of(accessToken, refreshToken, jwtTokenProvider.getTokenExpirationMs(), user.getUsername(), null, pinSetupComplete, tenantId);
    }

    /**
     * Set up PIN for a user
     */
    public void setupPin(String username, String pin) {
        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.unauthorized")));
        user.setPinHash(passwordEncoder.encode(pin));
        userRepository.save(user);
    }

    /**
     * Register a new user (mobile self-registration — stub)
     */
    public AuthResponse registerUser(String phone, String password, String clientIp, String userAgent) {
        if (userRepository.findByUsernameGlobal(phone).isPresent()) {
            throw new DuplicateResourceException("Số điện thoại đã được đăng ký.");
        }
        User user = User.builder()
                .username(phone)
                .password(passwordEncoder.encode(password))
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction("")
                .failedLoginAttempts(0)
                .build();
        userRepository.save(user);
        boolean isMasterUser = tenantContext.getCurrentTenant() == null;
        List<String> roleNames = user.getRoles().stream()
                .map(com.tappy.pos.model.entity.auth.Role::getName)
                .toList();
        List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);
        String shopType = tenantContext.getCurrentTenant() != null && tenantContext.getCurrentTenant().getShopType() != null
                ? tenantContext.getCurrentTenant().getShopType().name() : null;
        String tenantId = isMasterUser ? "master" : tenantContext.getCurrentTenant().getTenantId();
        String accessToken = jwtTokenProvider.generateTokenWithRolesAndFeatures(user.getUsername(), roleNames, featureNames, isMasterUser, shopType, tenantId);
        String refreshToken = generateRefreshToken(user);
        // Registration creates a user with no shop yet; routing will direct them to the onboarding wizard.
        return AuthResponse.of(accessToken, refreshToken, jwtTokenProvider.getTokenExpirationMs(), user.getUsername(), null, false, null);
    }

    /**
     * Remove PIN for a user (disables PIN login)
     */
    public void deletePin(String username) {
        User user = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.unauthorized")));
        user.setPinHash(null);
        userRepository.save(user);
    }

    /**
     * Request password reset (silent no-op — don't reveal if account exists)
     */
    public void requestPasswordReset(String phone) {
        log.info("Password reset requested for: {}", phone);
        // Silent no-op — don't reveal if account exists
    }

    /**
     * Map User entity to UserDTO
     */
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .build();
    }
}

