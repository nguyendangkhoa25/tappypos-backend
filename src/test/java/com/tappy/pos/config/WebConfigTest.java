package com.tappy.pos.config;

import com.tappy.pos.multitenant.TenantInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig Unit Tests")
class WebConfigTest {

    @Mock private TenantInterceptor tenantInterceptor;
    @Mock private CorsRegistry corsRegistry;
    @Mock private CorsRegistration corsRegistration;
    @Mock private InterceptorRegistry interceptorRegistry;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig(tenantInterceptor);
        // Lenient so tests that don't call addCorsMappings don't fail
        lenient().when(corsRegistry.addMapping(anyString())).thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedOriginPatterns(any(String[].class))).thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedMethods(any(String[].class))).thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedHeaders(anyString())).thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowCredentials(anyBoolean())).thenReturn(corsRegistration);
        lenient().when(corsRegistration.maxAge(anyLong())).thenReturn(corsRegistration);
    }

    @Test
    @DisplayName("Should register tenant interceptor")
    void addInterceptors_registersTenantInterceptor() {
        webConfig.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry).addInterceptor(tenantInterceptor);
    }

    @Test
    @DisplayName("Should configure CORS for all endpoints with multiple origins")
    void addCorsMappings_multipleOrigins() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "http://localhost:3000,http://localhost:5173");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        verify(corsRegistration).allowedHeaders("*");
        verify(corsRegistration).allowCredentials(true);
        verify(corsRegistration).maxAge(3600);
    }

    @Test
    @DisplayName("Should configure CORS with single origin")
    void addCorsMappings_singleOrigin() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "https://tappypos.vn");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOriginPatterns(new String[]{"https://tappypos.vn"});
    }

    @Test
    @DisplayName("Should trim whitespace around origin entries")
    void addCorsMappings_trimsOriginWhitespace() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "  http://localhost:3000  ,  http://localhost:4000  ");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        // Two origins after trimming
        verify(corsRegistration).allowedOriginPatterns(new String[]{"http://localhost:3000", "http://localhost:4000"});
    }

    @Test
    @DisplayName("Should filter out empty entries in comma-separated origin list")
    void addCorsMappings_filtersEmptyEntries() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "http://localhost:3000,,,http://localhost:5173,,");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOriginPatterns(new String[]{"http://localhost:3000", "http://localhost:5173"});
    }

    @Test
    @DisplayName("Should configure CORS with wildcard origin")
    void addCorsMappings_wildcardOrigin() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "*");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOriginPatterns(new String[]{"*"});
    }

    @Test
    @DisplayName("Should handle all-blank origins list resulting in empty array")
    void addCorsMappings_allBlankEntries() {
        ReflectionTestUtils.setField(webConfig, "allowedOriginConfig", "  ,  ,  ");

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOriginPatterns(new String[]{});
    }

    @Test
    @DisplayName("WebConfig is annotated with @Configuration")
    void webConfig_hasConfigurationAnnotation() {
        assertThat(WebConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class)).isTrue();
    }
}
