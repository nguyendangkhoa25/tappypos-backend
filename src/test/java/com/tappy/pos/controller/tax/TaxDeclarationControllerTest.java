package com.tappy.pos.controller.tax;

import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.model.dto.tax.TaxEstimateDTO;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.tax.TaxDeclarationService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for TaxDeclarationController — verifies the @RequiresFeature("TAX_DECLARATION")
 * gate: no token → 401, token without the feature → 403, token with it → 200.
 */
@WebMvcTest(TaxDeclarationController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("TaxDeclarationController")
class TaxDeclarationControllerTest {

    private static final String TENANT_ID  = "shop-xyz";
    private static final String USERNAME   = "owner01";
    private static final String SESSION_ID = "sess-001";

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @MockitoBean TenantInterceptor           tenantInterceptor;
    @MockitoBean SessionRegistry             sessionRegistry;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean MessageService              messageService;
    @MockitoBean TaxDeclarationService       taxDeclarationService;

    @BeforeEach
    void setUp() throws Exception {
        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));

        doAnswerUnauthorized();
    }

    private void doAnswerUnauthorized() throws Exception {
        org.mockito.Mockito.doAnswer(inv -> {
            HttpServletResponse resp = (HttpServletResponse) inv.getArgument(1);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.getWriter().write("{\"success\":false,\"error\":\"UNAUTHORIZED\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private String bearerToken(String... features) {
        return "Bearer " + jwtTokenProvider.generateTokenWithSession(
                USERNAME, List.of("SHOP_OWNER"), Arrays.asList(features),
                false, SESSION_ID, null, TENANT_ID);
    }

    @Test
    @DisplayName("no token → 401")
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/tax-declarations").header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token without TAX_DECLARATION → 403 FORBIDDEN")
    void missingFeature_returns403() throws Exception {
        mockMvc.perform(get("/tax-declarations")
                .header("Authorization", bearerToken("REVENUE"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("token with TAX_DECLARATION → 200")
    void withFeature_returns200() throws Exception {
        Page<?> empty = new PageImpl<>(List.of());
        when(taxDeclarationService.list(any(), any())).thenReturn((Page) empty);

        mockMvc.perform(get("/tax-declarations")
                .header("Authorization", bearerToken("TAX_DECLARATION"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("estimate endpoint also gated → 403 without feature")
    void estimate_missingFeature_returns403() throws Exception {
        mockMvc.perform(get("/tax-declarations/estimate")
                .param("year", "2026").param("number", "2")
                .header("Authorization", bearerToken("REVENUE"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isForbidden());
    }
}
