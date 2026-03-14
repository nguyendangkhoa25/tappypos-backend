package com.knp.config;

import com.knp.multitenant.TenantContext;
import com.knp.multitenant.TenantInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for Spring Security and JWT-related beans
 * Provides all necessary mocked and configured beans for @WebMvcTest
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableSpringDataWebSupport
public class TestSecurityConfig {

    // MessageSource beans are provided by production LocaleConfig
    // messageSource and messageService will be auto-wired from there

    // PasswordEncoder is provided by production SecurityConfig
    // No need to define it here in test config

    // ...existing code...

    /**
     * JWT Token Provider - mocked for tests
     * Provides token generation and validation functionality
     */
    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        return mock(JwtTokenProvider.class);
    }

    /**
     * Authentication context for storing current user info
     */
    @Bean
    @Primary
    public AuthContext authContext() {
        return new AuthContext();
    }

    /**
     * JWT Authentication Filter - mocked for tests
     * Extracts and validates JWT tokens from requests
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthContext authContext) {
        return mock(JwtAuthenticationFilter.class);
    }

    // TenantRepository is auto-created by JPA, no need to mock it here

    /**
     * Tenant Context - ThreadLocal holder for current tenant in request
     * Essential for multi-tenant data routing
     */
    @Bean
    @Primary
    public TenantContext tenantContext() {
        return new TenantContext();
    }

    /**
     * Tenant Interceptor - mocked for tests
     * Handles multi-tenant context extraction
     */
    @Bean
    @Primary
    public TenantInterceptor tenantInterceptor() {
        return mock(TenantInterceptor.class);
    }

    // DataSource beans are defined in TestDatasouceConfig to avoid bean definition conflicts
    // masterDataSource and routingDataSource are configured there

    /**
     * Handler method argument resolver for Pageable parameter
     * Enables Spring Data pagination support in tests
     */
    @Bean
    public PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver() {
        return new PageableHandlerMethodArgumentResolver();
    }

    /**
     * Security filter chain for test context
     * Allows all requests without authentication for testing, with @PreAuthorize evaluation
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }

    /**
     * Override WebConfig's CORS configuration for tests
     * Prevents CORS validation errors with "*" origin and allowCredentials
     */
    @Bean
    @Primary
    public WebMvcConfigurer testWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
}
