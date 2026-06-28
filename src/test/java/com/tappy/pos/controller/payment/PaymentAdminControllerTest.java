package com.tappy.pos.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.model.dto.dashboard.MasterBillingStatsDTO;
import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.payment.MasterBillingService;
import com.tappy.pos.service.payment.SubscriptionPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for PaymentAdminController — covers:
 *   - @RequiresFeature("BILLING_MGMT") enforcement (no token → 401; wrong feature → 403; agents lack
 *     BILLING_MGMT so are excluded; granted → 200)
 *   - GET /payments query-param parsing (status/provider → enums, dates → day-bounded range)
 *   - refund / record-offline delegation to SubscriptionPaymentService
 */
@WebMvcTest(PaymentAdminController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("PaymentAdminController")
class PaymentAdminControllerTest {

    private static final String TENANT_ID  = "master";
    private static final String USERNAME   = "admin";
    private static final String SESSION_ID = "sess-001";

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TenantInterceptor           tenantInterceptor;
    @MockitoBean SessionRegistry             sessionRegistry;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean MessageService              messageService;
    @MockitoBean SubscriptionPaymentService  paymentService;
    @MockitoBean MasterBillingService        billingService;

    @BeforeEach
    void setUp() throws Exception {
        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(true);
        when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                 .thenAnswer(inv -> inv.getArgument(0));
        when(billingService.getStats()).thenReturn(MasterBillingStatsDTO.builder().build());
        lenient().when(billingService.getPayments(any(), any(), any(), any(), any(), any(), any()))
                 .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        doAnswer(inv -> {
            HttpServletResponse resp = (HttpServletResponse) inv.getArgument(1);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.getWriter().write("{\"success\":false,\"error\":\"UNAUTHORIZED\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private String bearerToken(String... features) {
        return "Bearer " + jwtTokenProvider.generateTokenWithSession(
                USERNAME, List.of("MASTER_TENANT"), Arrays.asList(features),
                true, SESSION_ID, null, TENANT_ID);
    }

    // ── Auth + feature enforcement ─────────────────────────────────────────────

    @Test
    @DisplayName("no token → 401")
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/payments/admin/stats").header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token without BILLING_MGMT (e.g. an AGENT/master without it) → 403")
    void missingBillingFeature_returns403() throws Exception {
        mockMvc.perform(get("/payments/admin/stats")
                .header("Authorization", bearerToken("MASTER_DASHBOARD", "TENANT_MGMT"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /stats with BILLING_MGMT → 200 and delegates to the billing service")
    void stats_withFeature_returns200() throws Exception {
        mockMvc.perform(get("/payments/admin/stats")
                .header("Authorization", bearerToken("BILLING_MGMT"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
        verify(billingService).getStats();
    }

    // ── Ledger query-param parsing ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /payments parses status/provider to enums and dates to a day-bounded range")
    void payments_parsesFilters() throws Exception {
        mockMvc.perform(get("/payments/admin/payments")
                .header("Authorization", bearerToken("BILLING_MGMT"))
                .header("X-Tenant-ID", TENANT_ID)
                .param("status", "paid")        // lower-case → normalized
                .param("provider", "momo")
                .param("from", "2026-01-01")
                .param("to", "2026-01-31"))
               .andExpect(status().isOk());

        verify(billingService).getPayments(
                eq(PaymentStatus.PAID),
                eq(PaymentProvider.MOMO),
                isNull(),                // plan not supplied
                isNull(),                // tenantId not supplied
                eq(LocalDate.parse("2026-01-01").atStartOfDay()),
                eq(LocalDate.parse("2026-01-31").plusDays(1).atStartOfDay()), // 'to' is inclusive → +1 day
                any());
    }

    @Test
    @DisplayName("GET /payments treats an unrecognised status value as no filter (null)")
    void payments_invalidStatusIgnored() throws Exception {
        mockMvc.perform(get("/payments/admin/payments")
                .header("Authorization", bearerToken("BILLING_MGMT"))
                .header("X-Tenant-ID", TENANT_ID)
                .param("status", "bogus"))
               .andExpect(status().isOk());

        verify(billingService).getPayments(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    // ── Mutations delegate to the payment service ──────────────────────────────

    @Test
    @DisplayName("POST /refund/{txnRef} delegates to the payment service")
    void refund_delegates() throws Exception {
        mockMvc.perform(post("/payments/admin/refund/TXN1")
                .header("Authorization", bearerToken("BILLING_MGMT"))
                .header("X-Tenant-ID", TENANT_ID))
               .andExpect(status().isOk());
        verify(paymentService).refund("TXN1");
    }

    @Test
    @DisplayName("POST /record-offline delegates to the payment service")
    void recordOffline_delegates() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "tenantId", "shop1", "planCode", "BASIC", "billingCycle", "MONTHLY", "note", "cash"));
        mockMvc.perform(post("/payments/admin/record-offline")
                .header("Authorization", bearerToken("BILLING_MGMT"))
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
               .andExpect(status().isOk());
        verify(paymentService).recordOfflinePayment(eq("shop1"), eq("BASIC"), eq(BillingCycle.MONTHLY), eq("cash"));
    }
}
