package com.tappy.pos.controller.auth;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.auth.AuthResponse;
import com.tappy.pos.model.dto.auth.LoginRequest;
import com.tappy.pos.model.dto.auth.UserDTO;
import com.tappy.pos.service.auth.AuthService;
import com.tappy.pos.service.auth.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Authentication endpoints
 * Public endpoints (no X-Tenant-ID header required)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;
    private final TurnstileService turnstileService;

    /**
     * POST /api/auth/login
     * Login with username and password.
     * Returns 409 DEVICE_CONFLICT if the user is already logged in on another device.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Login request for user: {}", loginRequest.getUsername());

        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (!Boolean.TRUE.equals(loginRequest.getRefreshInBody()) && !turnstileService.verify(loginRequest.getTurnstileToken(), clientIp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("TURNSTILE_FAILED", "Human verification failed. Please try again."));
        }

        AuthResponse authResponse = authService.authenticateUser(loginRequest, clientIp, userAgent);

        if (loginRequest.getRememberMe() && authResponse.getRefreshToken() != null) {
            ResponseCookie cookie = authService.getRefreshTokenResponseCookie(authResponse.getRefreshToken());
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    /**
     * POST /api/auth/login/force
     * Force-login: kicks out any existing session and creates a new one.
     * Called when the user confirms the "Switch Device?" dialog.
     */
    @PostMapping("/login/force")
    public ResponseEntity<ApiResponse<AuthResponse>> forceLogin(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Force-login request for user: {}", loginRequest.getUsername());

        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (!Boolean.TRUE.equals(loginRequest.getRefreshInBody()) && !turnstileService.verify(loginRequest.getTurnstileToken(), clientIp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("TURNSTILE_FAILED", "Human verification failed. Please try again."));
        }

        AuthResponse authResponse = authService.forceLogin(loginRequest, clientIp, userAgent);

        if (loginRequest.getRememberMe() && authResponse.getRefreshToken() != null) {
            ResponseCookie cookie = authService.getRefreshTokenResponseCookie(authResponse.getRefreshToken());
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    /**
     * POST /api/auth/refresh
     * Refresh access token using refresh token cookie or body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestParam(required = false) String username,
            @RequestBody(required = false) java.util.Map<String, String> body,
            HttpServletRequest request) {
        log.info("Refresh token request");
        String refreshToken = authService.getRefreshToken(request);
        if ((refreshToken == null || refreshToken.isBlank()) && body != null) {
            refreshToken = body.get("refreshToken");
            if (username == null || username.isBlank()) username = body.get("username");
        }
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("BAD_REQUEST", "username required"));
        }
        AuthResponse authResponse = authService.refreshAccessToken(username, refreshToken);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
    }

    /**
     * POST /api/auth/logout
     * Logout user - invalidate all refresh tokens and clear cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        String username = authContext.getCurrentUsername();

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        }

        log.info("Logout request for user: {}", username);
        authService.logoutUser(username);
        authService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    /**
     * GET /api/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile() {
        String username = authContext.getCurrentUsername();

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        }

        log.info("Profile request for user: {}", username);
        UserDTO userProfile = authService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.success(userProfile, "Profile retrieved successfully"));
    }

    @PostMapping("/phone-pin")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPin(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = body.get("username");
        String pin = body.get("pin");
        if (username == null || pin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "username and pin required"));
        }
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        AuthResponse authResponse = authService.loginWithPin(username, pin, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/pin/setup")
    public ResponseEntity<ApiResponse<Void>> setupPin(@RequestBody java.util.Map<String, String> body) {
        String pin = body.get("pin");
        if (pin == null || pin.length() != 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "PIN must be 6 digits"));
        }
        String username = authContext.getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        authService.setupPin(username, pin);
        return ResponseEntity.ok(ApiResponse.success(null, "PIN set up successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String phone = body.get("phone");
        String password = body.get("password");
        if (phone == null || password == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "phone and password required"));
        }
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        AuthResponse authResponse = authService.registerUser(phone, password, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Registration successful"));
    }

    @DeleteMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> deletePin() {
        String username = authContext.getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Not authenticated"));
        log.info("Delete PIN request for user: {}", username);
        authService.deletePin(username);
        return ResponseEntity.ok(ApiResponse.success(null, "PIN removed successfully"));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody java.util.Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null) return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "phone required"));
        authService.requestPasswordReset(phone);
        return ResponseEntity.ok(ApiResponse.success(null, "If the account exists, instructions have been sent."));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
