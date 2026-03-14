package com.knp.service;

import com.knp.config.JwtTokenProvider;
import com.knp.exception.UnauthorizedException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.BadRequestException;
import com.knp.exception.AccountLockedException;
import com.knp.exception.DeviceConflictException;
import com.knp.model.dto.auth.AuthResponse;
import com.knp.model.dto.auth.LoginRequest;
import com.knp.model.dto.auth.UserDTO;
import com.knp.model.entity.RefreshToken;
import com.knp.model.entity.Role;
import com.knp.model.entity.User;
import com.knp.model.enums.ActivityAction;
import com.knp.multitenant.TenantContext;
import com.knp.repository.RefreshTokenRepository;
import com.knp.repository.UserRepository;
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

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", loginRequest.getUsername());
                    return new BadRequestException(messageService.getMessage("error.unauthorized"));
                });

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
        String accessToken = jwtTokenProvider.generateTokenWithSession(
                user.getUsername(),
                roleNames,
                featureNames,
                isMasterUser,
                sessionId
        );
        log.info("Access token generated for user: {} with roles: {}, features: {}, isMasterUser: {}",
                loginRequest.getUsername(), roleNames, featureNames.size(), isMasterUser);

        sessionRegistry.register(tenantKey, user.getUsername(),
                new SessionInfo(sessionId, clientIp, userAgent, LocalDateTime.now()));

        if (!isMasterUser) {
            activityLogService.logAsync(tenantKey, user.getUsername(), user.getFullName(),
                    ActivityAction.LOGIN, null, null, "Logged in", clientIp);
        }

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
                1000L
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

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new ResourceNotFoundException(errorMessage);
                });

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
        String accessToken = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                username,
                roleNames,
                featureNames,
                isMasterUser
        );
        log.info("New access token generated for user: {} with roles: {}, features: {}, isMasterUser: {}",
                username, roleNames, featureNames.size(), isMasterUser);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getTokenExpirationMs(),
                user.getUsername(),
                10000L
        );
    }

    /**
     * Logout user - invalidate all refresh tokens and clear session registry.
     */
    public void logoutUser(String username) {
        log.info("Logging out user: {}", username);

        User user = userRepository.findByUsername(username)
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

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new RuntimeException(errorMessage);
                });

        return mapToDTO(user);
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

