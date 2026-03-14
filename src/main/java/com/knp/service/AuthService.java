package com.knp.service;

import com.knp.config.JwtTokenProvider;
import com.knp.exception.UnauthorizedException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.exception.BadRequestException;
import com.knp.model.dto.auth.AuthResponse;
import com.knp.model.dto.auth.LoginRequest;
import com.knp.model.dto.auth.UserDTO;
import com.knp.model.entity.RefreshToken;
import com.knp.model.entity.Role;
import com.knp.model.entity.User;
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

import java.util.List;
import java.util.Optional;

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
    public static final String REFRESH_TOKEN_HEADER_KEY = "refresh-token";

    /**
     * Authenticate user with username and password
     * If rememberMe is true, generate refresh token
     */
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        // Log which database context we're using
        if (tenantContext.getCurrentTenant() != null) {
            log.info("Authenticating user: {} in TENANT database: {}",
                    loginRequest.getUsername(), tenantContext.getCurrentTenant().getTenantId());
        } else {
            log.info("Authenticating user: {} in MASTER database", loginRequest.getUsername());
        }

        // Find user by username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", loginRequest.getUsername());
                    String errorMessage = messageService.getMessage("error.unauthorized");
                    return new BadRequestException(errorMessage);
                });

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginRequest.getUsername());
            String errorMessage = messageService.getMessage("error.unauthorized");
            throw new BadRequestException(errorMessage);
        }

        if (!user.getActive()) {
            log.warn("User account is inactive: {}", loginRequest.getUsername());
            String errorMessage = messageService.getMessage("error.user.inactive");
            throw new BadRequestException(errorMessage);
        }

        // Determine if user is from master database
        boolean isMasterUser = tenantContext.getCurrentTenant() == null;

        // Extract role names from user's roles
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        // Get accessible features: intersection of tenant features and role features
        // Step 1: Get features assigned to the tenant from master DB
        // Step 2: Get features assigned to the role
        // Step 3: Return matching features (intersection)
        List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);
        log.info("User {} has access to {} features (tenant + role intersection): {}",
                loginRequest.getUsername(), featureNames.size(), featureNames);

        // Generate access token with roles, features and master user flag
        String accessToken = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                user.getUsername(),
                roleNames,
                featureNames,
                isMasterUser
        );
        log.info("Access token generated for user: {} with roles: {}, features: {}, isMasterUser: {}",
                loginRequest.getUsername(), roleNames, featureNames.size(), isMasterUser);

        if (StringUtils.isNotEmpty(user.getRequireAction())) {
            log.info("User {} requires action: {}", loginRequest.getUsername(), user.getRequireAction());
            return AuthResponse.requiredAction(user.getUsername(), user.getRequireAction(),
                    accessToken, jwtTokenProvider.getTokenExpirationMs());
        }

        // Generate refresh token if rememberMe is enabled
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
        for (Cookie cookie : request.getCookies()) {
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
     * Logout user - invalidate all refresh tokens
     */
    public void logoutUser(String username) {
        log.info("Logging out user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.getMessage("error.user.not.found", username);
                    return new RuntimeException(errorMessage);
                });

        // Deactivate all refresh tokens
        refreshTokenRepository.findByUser(user).forEach(token -> {
            token.setActive(false);
            refreshTokenRepository.save(token);
        });

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

