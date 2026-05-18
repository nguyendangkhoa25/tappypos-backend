package com.tappy.pos.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.exception.DeviceConflictException;
import com.tappy.pos.model.dto.auth.AuthResponse;
import com.tappy.pos.model.dto.auth.LoginRequest;
import com.tappy.pos.model.dto.auth.UserDTO;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.AuthService;
import com.tappy.pos.service.auth.SessionInfo;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.auth.TurnstileService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for AuthController — covers:
 *   - Public endpoints (permitAll in SecurityConfig) are reachable without a token
 *   - Turnstile verification failure → 400 TURNSTILE_FAILED (returned by controller)
 *   - Successful login → 200 with access token in the response body
 *   - Device conflict on login → 409 DEVICE_CONFLICT with existing session details
 *   - Force-login → 200 (same flow but calls forceLogin on AuthService)
 *   - /auth/logout and /auth/profile: no token → controller returns 401 via AuthContext null-check
 *   - /auth/logout and /auth/profile: valid JWT → JwtAuthenticationFilter populates AuthContext → 200
 *
 * Two setup nuances:
 *   1. TenantInterceptor is @MockBean so path-prefix differences (/api/auth vs /auth) don't block.
 *   2. AuthContext (ThreadLocal) is explicitly cleared in @BeforeEach because JwtAuthenticationFilter
 *      only clears FeatureContext in its finally block — without this, a test with a valid JWT would
 *      leak the username into the next "no-token" test and cause it to get 200 instead of 401.
 */
@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class
})
@DisplayName("AuthController")
class AuthControllerTest {

    private static final String TENANT_ID  = "shop-abc";
    private static final String USERNAME   = "owner01";
    private static final String SESSION_ID = "sess-999";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;
    @Autowired AuthContext   authContext;        // injected to clear ThreadLocal between tests

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean AuthService                  authService;
    @MockBean TurnstileService             turnstileService;

    @BeforeEach
    void setUp() throws Exception {
        // Clear ThreadLocal state: JwtAuthenticationFilter sets AuthContext but never clears it,
        // so a prior test with a valid JWT would pollute the next "no-token" test.
        authContext.clear();

        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(true);

        when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                 .thenAnswer(inv -> inv.getArgument(0));

        doAnswer(inv -> {
            HttpServletResponse resp = (HttpServletResponse) inv.getArgument(1);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.getWriter().write("{\"success\":false,\"error\":\"UNAUTHORIZED\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    // Produces a JWT that JwtAuthenticationFilter validates and uses to populate
    // AuthContext.currentUsername for the duration of the request.
    private String bearerToken(String... features) {
        return "Bearer " + jwtTokenProvider.generateTokenWithSession(
                USERNAME, List.of("SHOP_OWNER"), Arrays.asList(features),
                false, SESSION_ID, null, TENANT_ID);
    }

    private LoginRequest loginRequest(String username, String password) {
        return LoginRequest.builder()
                .username(username).password(password)
                .rememberMe(false).turnstileToken("cf-token").build();
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("Turnstile verification fails → 400 TURNSTILE_FAILED")
        void turnstileFailure_returns400() throws Exception {
            when(turnstileService.verify(anyString(), anyString())).thenReturn(false);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest(USERNAME, "wrong"))))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.success").value(false))
                   .andExpect(jsonPath("$.error").value("TURNSTILE_FAILED"));
        }

        @Test
        @DisplayName("valid credentials + Turnstile pass → 200 with accessToken and username")
        void validCredentials_returns200WithToken() throws Exception {
            when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
            AuthResponse authResp = AuthResponse.of("jwt-token-abc", null, 86400L, USERNAME, 1L, true, TENANT_ID);
            // Use any() (not anyString()) for IP and User-Agent: MockMvc may pass null User-Agent
            when(authService.authenticateUser(any(LoginRequest.class), any(), any()))
                    .thenReturn(authResp);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest(USERNAME, "correct-pw"))))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.accessToken").value("jwt-token-abc"))
                   .andExpect(jsonPath("$.data.username").value(USERNAME));
        }

        @Test
        @DisplayName("user already logged in on another device → 409 DEVICE_CONFLICT with session IP")
        void deviceConflict_returns409() throws Exception {
            when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
            SessionInfo existing = new SessionInfo(
                    "old-session", "192.168.1.100", "Chrome/Mac",
                    LocalDateTime.now().minusHours(2));
            when(authService.authenticateUser(any(LoginRequest.class), any(), any()))
                    .thenThrow(new DeviceConflictException(existing));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest(USERNAME, "pw"))))
                   .andExpect(status().isConflict())
                   .andExpect(jsonPath("$.error").value("DEVICE_CONFLICT"))
                   .andExpect(jsonPath("$.data.ipAddress").value("192.168.1.100"));
        }
    }

    // ── POST /auth/login/force ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login/force")
    class ForceLogin {

        @Test
        @DisplayName("Turnstile fails → 400 TURNSTILE_FAILED")
        void turnstileFailure_returns400() throws Exception {
            when(turnstileService.verify(anyString(), anyString())).thenReturn(false);

            mockMvc.perform(post("/auth/login/force")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest(USERNAME, "pw"))))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.error").value("TURNSTILE_FAILED"));
        }

        @Test
        @DisplayName("Turnstile passes → 200 (existing session evicted by AuthService)")
        void success_returns200() throws Exception {
            when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
            AuthResponse authResp = AuthResponse.of("new-jwt-token", null, 86400L, USERNAME, 1L, true, TENANT_ID);
            when(authService.forceLogin(any(LoginRequest.class), any(), any()))
                    .thenReturn(authResp);

            mockMvc.perform(post("/auth/login/force")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest(USERNAME, "pw"))))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.accessToken").value("new-jwt-token"));
        }
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("no Authorization header → AuthContext empty → controller returns 401")
        void noToken_returns401() throws Exception {
            // /auth/logout is permitAll() so Spring Security won't block it.
            // The controller checks authContext.getCurrentUsername() == null → 401.
            // AuthContext.clear() in @BeforeEach ensures no leftover from prior tests.
            mockMvc.perform(post("/auth/logout"))
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("valid JWT → JwtAuthenticationFilter sets AuthContext → 200")
        void validToken_returns200() throws Exception {
            doNothing().when(authService).logoutUser(USERNAME);
            doNothing().when(authService).clearRefreshTokenCookie(any());

            mockMvc.perform(post("/auth/logout")
                    .header("Authorization", bearerToken())
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── GET /auth/profile ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /auth/profile")
    class GetProfile {

        @Test
        @DisplayName("no Authorization header → AuthContext empty → controller returns 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/auth/profile"))
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("valid JWT → 200 with user profile data")
        void validToken_returns200WithProfile() throws Exception {
            UserDTO profile = UserDTO.builder()
                    .id(1L).username(USERNAME).fullName("Nguyễn Văn A").active(true).build();
            when(authService.getUserProfile(USERNAME)).thenReturn(profile);

            mockMvc.perform(get("/auth/profile")
                    .header("Authorization", bearerToken())
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.username").value(USERNAME))
                   .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"));
        }
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("valid ?username param and refresh cookie → 200 with new accessToken")
        void success_returns200() throws Exception {
            AuthResponse refreshed = AuthResponse.of("refreshed-jwt", null, 86400L, USERNAME, 1L, true, TENANT_ID);
            when(authService.getRefreshToken(any())).thenReturn("some-refresh-token");
            when(authService.refreshAccessToken(eq(USERNAME), anyString())).thenReturn(refreshed);

            mockMvc.perform(post("/auth/refresh").param("username", USERNAME))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.accessToken").value("refreshed-jwt"));
        }
    }
}
