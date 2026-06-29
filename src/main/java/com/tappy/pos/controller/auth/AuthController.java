package com.tappy.pos.controller.auth;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.auth.AuthResponse;
import com.tappy.pos.model.dto.auth.LoginRequest;
import com.tappy.pos.model.dto.auth.OtpRequestBody;
import com.tappy.pos.model.dto.auth.OtpRequestResponse;
import com.tappy.pos.model.dto.auth.OtpVerifyRequest;
import com.tappy.pos.model.dto.auth.OtpVerifyResponse;
import com.tappy.pos.model.dto.auth.RegisterOtpRequest;
import com.tappy.pos.model.dto.auth.RegisterOtpResendRequest;
import com.tappy.pos.model.dto.auth.RegisterOtpResponse;
import com.tappy.pos.model.dto.auth.RegisterOtpVerifyRequest;
import com.tappy.pos.model.dto.auth.RegisterOtpVerifyResponse;
import com.tappy.pos.model.dto.auth.RegisterRequest;
import com.tappy.pos.model.dto.auth.ResetPasswordRequest;
import com.tappy.pos.model.dto.auth.UserDTO;
import com.tappy.pos.service.auth.AuthService;
import com.tappy.pos.service.auth.PasswordResetService;
import com.tappy.pos.service.auth.PhoneVerificationService;
import com.tappy.pos.service.auth.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    private final PasswordResetService passwordResetService;
    private final PhoneVerificationService phoneVerificationService;

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

        if (Boolean.TRUE.equals(loginRequest.getRememberMe()) && authResponse.getRefreshToken() != null) {
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

        if (Boolean.TRUE.equals(loginRequest.getRememberMe()) && authResponse.getRefreshToken() != null) {
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

    /**
     * POST /api/auth/register/send-otp
     * Registration step 1: submit phone → receive an OTP via Zalo.
     * Returns 409 if the phone is already registered (UI routes to login).
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<ApiResponse<RegisterOtpResponse>> sendRegisterOtp(
            @Valid @RequestBody RegisterOtpRequest body,
            HttpServletRequest request) {
        RegisterOtpResponse result = phoneVerificationService.sendOtp(body.getPhone(), resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(result, "Mã OTP đã được gửi qua Zalo"));
    }

    /**
     * POST /api/auth/register/resend-otp
     * Registration step 1b: reissue the OTP for an existing verification (subject to a cooldown).
     */
    @PostMapping("/register/resend-otp")
    public ResponseEntity<ApiResponse<RegisterOtpResponse>> resendRegisterOtp(
            @Valid @RequestBody RegisterOtpResendRequest body,
            HttpServletRequest request) {
        RegisterOtpResponse result = phoneVerificationService.resendOtp(body.getVerificationId(), resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(result, "Mã OTP đã được gửi lại qua Zalo"));
    }

    /**
     * POST /api/auth/register/verify-otp
     * Registration step 2: submit OTP → receive a single-use verificationToken (15 min).
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<RegisterOtpVerifyResponse>> verifyRegisterOtp(
            @Valid @RequestBody RegisterOtpVerifyRequest body) {
        String verificationToken = phoneVerificationService.verifyOtp(body.getVerificationId(), body.getCode());
        return ResponseEntity.ok(ApiResponse.success(
                new RegisterOtpVerifyResponse(verificationToken), "OTP hợp lệ"));
    }

    /**
     * POST /api/auth/register
     * Registration step 3: create the account with the verified phone.
     * Requires the verificationToken from step 2.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        AuthResponse authResponse = authService.registerUser(
                body.getPhone(), body.getPassword(), body.getVerificationToken(), clientIp, userAgent);
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

    /**
     * POST /api/auth/password-reset/request
     * Step 1: submit phone → receive OTP via Zalo ZNS.
     * Always returns 200 (never reveals if phone is registered).
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<OtpRequestResponse>> requestPasswordReset(
            @Valid @RequestBody OtpRequestBody body,
            HttpServletRequest request) {
        String maskedPhone = passwordResetService.requestOtp(body.getPhone(), resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(
                new OtpRequestResponse(maskedPhone),
                "Mã OTP đã được gửi qua Zalo"));
    }

    /**
     * POST /api/auth/password-reset/verify
     * Step 2: submit OTP → receive a short-lived resetToken (10 min, single-use).
     */
    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        String resetToken = passwordResetService.verifyOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(
                new OtpVerifyResponse(resetToken),
                "OTP hợp lệ"));
    }

    /**
     * POST /api/auth/password-reset/reset
     * Step 3: submit resetToken + newPassword → password updated.
     */
    @PostMapping("/password-reset/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest body) {
        passwordResetService.resetPassword(body.getResetToken(), body.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Mật khẩu đã được đặt lại thành công"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
