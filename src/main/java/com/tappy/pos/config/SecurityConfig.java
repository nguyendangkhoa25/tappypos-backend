package com.tappy.pos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Configuration for security components
 * Configures Spring Security with JWT authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final com.tappy.pos.service.MessageService messageService;

    /**
     * Configure security filter chain
     * - Public endpoints: /api/tenants, /api/auth/**
     * - Protected endpoints: all others (require X-Tenant-ID header)
     * - JWT filter: validates token on all requests
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/tenants/**").permitAll()
                        .requestMatchers("/banks/**").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/login/force").permitAll()
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/register/send-otp").permitAll()
                        .requestMatchers("/auth/register/resend-otp").permitAll()
                        .requestMatchers("/auth/register/verify-otp").permitAll()
                        .requestMatchers("/auth/refresh").permitAll()
                        .requestMatchers("/auth/logout").permitAll()
                        .requestMatchers("/auth/profile").permitAll()
                        .requestMatchers("/auth/password-reset/request").permitAll()
                        .requestMatchers("/auth/password-reset/verify").permitAll()
                        .requestMatchers("/auth/password-reset/reset").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/app/version").permitAll()
                        .requestMatchers("/gold-prices/price-board").permitAll()
                        .requestMatchers("/contact").permitAll()
                        .requestMatchers("/integrations/oauth/callback").permitAll()
                        .requestMatchers("/shop-types").permitAll()
                        .requestMatchers("/product-templates").permitAll()
                        .requestMatchers("/expense-suggestions").permitAll()
                        .requestMatchers("/public/**").permitAll()   // QR customer ordering (no JWT; tenant-gated in service)
                        .requestMatchers("/payments/webhook/**").permitAll()  // payment provider callbacks (signature-verified)
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Prevent Spring Boot from also registering {@link JwtAuthenticationFilter} as a standalone
     * servlet filter. As a {@code @Component} extending {@code OncePerRequestFilter} it would
     * otherwise run twice: once auto-registered in the servlet chain and once inside the Spring
     * Security chain (via {@code addFilterBefore} above). Because it is a {@code OncePerRequestFilter},
     * whichever copy runs first marks the request "already filtered" and the other is skipped — and if
     * the standalone copy wins, {@code SecurityContextHolderFilter} then resets the context before
     * authorization runs, dropping the authentication (surfaced as spurious 401s under MockMvc). The
     * filter must run only within the security chain, so disable the auto-registration here.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Password encoder bean for encoding user passwords
     * Uses BCrypt with 10 rounds
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt, but rejects > 72-byte passwords with a friendly localized 400 instead of
        // Spring Security 7's raw "password cannot be more than 72 bytes" exception.
        return new BoundedBCryptPasswordEncoder(messageService);
    }
}

