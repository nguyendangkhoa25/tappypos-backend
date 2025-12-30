package com.barbershop.service;

import com.barbershop.config.JwtTokenProvider;
import com.barbershop.model.dto.auth.AuthResponse;
import com.barbershop.model.dto.auth.LoginRequest;
import com.barbershop.model.dto.auth.UserDTO;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.RefreshToken;
import com.barbershop.model.entity.User;
import com.barbershop.multitenant.TenantContext;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.RefreshTokenRepository;
import com.barbershop.repository.UserRepository;
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
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
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
                    return new RuntimeException("Invalid username or password");
                });

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginRequest.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getActive()) {
            log.warn("User account is inactive: {}", loginRequest.getUsername());
            throw new RuntimeException("User account is inactive");
        }
        // Generate access token
        String accessToken = jwtTokenProvider.generateToken(user.getUsername());
        log.info("Access token generated for user: {}", loginRequest.getUsername());

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

        // Get employee ID if user has an associated employee
        Long employeeId = null;
        Optional<Employee> employeeOpt = employeeRepository.findByUserId(user.getId());
        if (employeeOpt.isPresent()) {
            employeeId = employeeOpt.get().getId();
            log.info("Employee ID {} found for user: {}", employeeId, loginRequest.getUsername());
        }

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getTokenExpirationMs(),
                user.getUsername(),
                employeeId
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
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (StringUtils.isNotEmpty(user.getRequireAction())) {
            throw new RuntimeException("User is required to perform action");
        }

        List<RefreshToken> tokenEntities = refreshTokenRepository.findAllByUserAndActive(user, System.currentTimeMillis());
        if (tokenEntities.isEmpty()) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        // Check if token is expired
        tokenEntities.stream()
                .filter(rt -> passwordEncoder.matches(refreshToken, rt.getToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Refresh token is invalid or expired"));

        // Generate new access token
        String accessToken = jwtTokenProvider.generateToken(username);
        log.info("New access token generated for user: {}", username);

        // Get employee ID if user has an associated employee
        Long employeeId = null;
        Optional<Employee> employeeOpt = employeeRepository.findByUserId(user.getId());
        if (employeeOpt.isPresent()) {
            employeeId = employeeOpt.get().getId();
            log.info("Employee ID {} found for user during token refresh: {}", employeeId, username);
        }

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getTokenExpirationMs(),
                user.getUsername(),
                employeeId
        );
    }

    /**
     * Logout user - invalidate all refresh tokens
     */
    public void logoutUser(String username) {
        log.info("Logging out user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Deactivate all refresh tokens
        refreshTokenRepository.findByUser(user).forEach(token -> {
            token.setActive(false);
            refreshTokenRepository.save(token);
        });

        log.info("User logged out: {}", username);
    }

    /**
     * Get user profile
     */
    public UserDTO getUserProfile(String username) {
        log.info("Fetching user profile: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

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

