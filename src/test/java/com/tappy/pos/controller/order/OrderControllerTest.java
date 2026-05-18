package com.tappy.pos.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.MyWorkStatsDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.order.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for OrderController — covers:
 *   - Auth and @RequiresFeature("ORDER") enforcement
 *   - Pagination and filter parameters forwarded to service
 *   - Order lifecycle transitions (start, complete, cancel, void)
 *   - Receipt endpoint returns text/html content type
 *   - Service ResourceNotFoundException maps to 404
 *   - Optional request bodies (cancel/void accept absent body without NPE)
 *
 * TenantInterceptor is @MockBean so path-prefix differences in WebMvcTest
 * don't falsely block requests.
 */
@WebMvcTest(OrderController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("OrderController")
class OrderControllerTest {

    private static final String TENANT_ID  = "shop-xyz";
    private static final String USERNAME   = "staff01";
    private static final String SESSION_ID = "sess-002";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean OrderService                 orderService;

    @BeforeEach
    void setUp() throws Exception {
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

    private String bearerToken(String... features) {
        return "Bearer " + jwtTokenProvider.generateTokenWithSession(
                USERNAME, List.of("SHOP_OWNER"), Arrays.asList(features),
                false, SESSION_ID, null, TENANT_ID);
    }

    private OrderDTO orderWith(Long id, String status) {
        return OrderDTO.builder().id(id).orderNumber("ORD-" + id).status(status).build();
    }

    // ── Auth and feature enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Auth and feature enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/orders").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token without ORDER feature → 403 FORBIDDEN from FeatureAccessAspect")
        void missingFeature_returns403() throws Exception {
            mockMvc.perform(get("/orders")
                    .header("Authorization", bearerToken("PRODUCT")) // PRODUCT, not ORDER
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden())
                   .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }

    // ── GET /orders ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /orders")
    class GetAllOrders {

        @Test
        @DisplayName("ORDER feature → 200 with paginated order list")
        void withFeature_returns200() throws Exception {
            Page<OrderDTO> page = new PageImpl<>(List.of(orderWith(1L, "PENDING")));
            when(orderService.getAllOrders(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/orders")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-1"))
                   .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("?status=COMPLETED&orderType=SELL forwarded to service")
        void filterParams_passedToService() throws Exception {
            when(orderService.getAllOrders(eq("COMPLETED"), eq("SELL"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/orders")
                    .param("status", "COMPLETED")
                    .param("orderType", "SELL")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk());
        }
    }

    // ── GET /orders/{id} ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /orders/{id}")
    class GetOrderById {

        @Test
        @DisplayName("order exists → 200 with order data")
        void found_returns200() throws Exception {
            when(orderService.getOrderById(55L)).thenReturn(orderWith(55L, "COMPLETED"));

            mockMvc.perform(get("/orders/55")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.id").value(55))
                   .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("service throws ResourceNotFoundException → 404 RESOURCE_NOT_FOUND")
        void notFound_returns404() throws Exception {
            when(orderService.getOrderById(999L))
                    .thenThrow(new ResourceNotFoundException("Đơn hàng không tồn tại"));

            mockMvc.perform(get("/orders/999")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
        }
    }

    // ── Order lifecycle transitions ────────────────────────────────────────────

    @Nested
    @DisplayName("Order lifecycle transitions")
    class OrderTransitions {

        @Test
        @DisplayName("PUT /orders/{id}/start — PENDING → IN_PROGRESS → 200")
        void startOrder_returns200() throws Exception {
            when(orderService.startOrder(10L)).thenReturn(orderWith(10L, "IN_PROGRESS"));

            mockMvc.perform(put("/orders/10/start")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("PUT /orders/{id}/complete — IN_PROGRESS → COMPLETED → 200")
        void completeOrder_returns200() throws Exception {
            when(orderService.completeOrder(10L)).thenReturn(orderWith(10L, "COMPLETED"));

            mockMvc.perform(put("/orders/10/complete")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("POST /orders/{id}/cancel with reason body → 200 CANCELLED")
        void cancelOrder_withBody_returns200() throws Exception {
            CancelOrderRequest req = new CancelOrderRequest();
            req.setReason("Khách đổi ý");
            when(orderService.cancelOrder(eq(20L), any(CancelOrderRequest.class)))
                    .thenReturn(orderWith(20L, "CANCELLED"));

            mockMvc.perform(post("/orders/20/cancel")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("POST /orders/{id}/cancel without body → 200 (controller handles null body)")
        void cancelOrder_noBody_returns200() throws Exception {
            when(orderService.cancelOrder(eq(20L), any(CancelOrderRequest.class)))
                    .thenReturn(orderWith(20L, "CANCELLED"));

            // @RequestBody(required = false) — no body is valid
            mockMvc.perform(post("/orders/20/cancel")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /orders/{id}/void with reason body → 200 VOIDED")
        void voidOrder_returns200() throws Exception {
            VoidOrderRequest req = new VoidOrderRequest();
            req.setReason("Nhập sai đơn");
            when(orderService.voidOrder(eq(30L), any(VoidOrderRequest.class)))
                    .thenReturn(orderWith(30L, "VOIDED"));

            mockMvc.perform(post("/orders/30/void")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.status").value("VOIDED"));
        }
    }

    // ── Receipt endpoints ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Receipt endpoints")
    class ReceiptEndpoints {

        @Test
        @DisplayName("GET /orders/{id}/receipt → 200 with text/html content type")
        void getReceipt_returnsHtml() throws Exception {
            when(orderService.generateReceipt(5L)).thenReturn("<html><body>Receipt</body></html>");

            mockMvc.perform(get("/orders/5/receipt")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        }
    }

    // ── My-work endpoints ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("My-work endpoints")
    class MyWork {

        @Test
        @DisplayName("GET /orders/my-work/stats → 200 with stats DTO")
        void getMyWorkStats_returns200() throws Exception {
            MyWorkStatsDTO stats = new MyWorkStatsDTO(3L, 12L, BigDecimal.valueOf(1_500_000));
            when(orderService.getMyWorkStats(anyString(), any(), any(), any())).thenReturn(stats);

            mockMvc.perform(get("/orders/my-work/stats")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.pendingCount").value(3))
                   .andExpect(jsonPath("$.data.completedCount").value(12));
        }

        @Test
        @DisplayName("GET /orders/my-work/pending → 200 with paginated orders")
        void getMyPendingOrders_returns200() throws Exception {
            Page<OrderDTO> page = new PageImpl<>(List.of(orderWith(7L, "PENDING")));
            when(orderService.getMyPendingOrders(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/orders/my-work/pending")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.content[0].id").value(7));
        }
    }
}
