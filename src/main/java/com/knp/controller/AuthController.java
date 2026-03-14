package com.knp.controller;

import com.knp.config.AuthContext;
import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.auth.AuthResponse;
import com.knp.model.dto.auth.LoginRequest;
import com.knp.model.dto.auth.UserDTO;
import com.knp.service.AuthService;
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

    /**
     * POST /api/auth/login
     * Login with username and password
     * If rememberMe is true, refresh token is generated
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        log.info("Login request for user: {}", loginRequest.getUsername());

        AuthResponse authResponse = authService.authenticateUser(loginRequest);

        if (loginRequest.getRememberMe() && authResponse.getRefreshToken() != null) {
            log.info("Remember me is enabled, refresh token generated");
            ResponseCookie cookie = authService.getRefreshTokenResponseCookie(authResponse.getRefreshToken());
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "Login successful")
        );
    }


    /**
     * POST /api/auth/refresh
     * Refresh access token using refresh token
     * Used when access token expires but refresh token is still valid
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String username, HttpServletRequest request) {

        log.info("User {} attempt to refresh the token!", username);
        String refreshToken = authService.getRefreshToken(request);

        AuthResponse authResponse = authService.refreshAccessToken(username, refreshToken);
        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "Token refreshed successfully")
        );
    }

    /**
     * POST /api/auth/logout
     * Logout user - invalidate all refresh tokens and clear cookie
     * Requires authentication
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

        // Clear the refresh token cookie
        authService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Logout successful")
        );
    }

    /**
     * GET /api/auth/profile
     * Get current user profile
     * Requires authentication
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

        return ResponseEntity.ok(
                ApiResponse.success(userProfile, "Profile retrieved successfully")
        );
    }
}

