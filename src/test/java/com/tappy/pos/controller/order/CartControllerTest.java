package com.tappy.pos.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.model.dto.order.CartRequest;
import com.tappy.pos.model.dto.order.CartResponse;
import com.tappy.pos.model.dto.order.CheckoutRequest;
import com.tappy.pos.model.dto.order.CheckoutResponse;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.order.CartService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for CartController — covers:
 *   - Auth and @RequiresFeature("POS") enforcement at class level
 *   - PATCH /carts/{id}/items/{itemId}/commission requires COMMISSION feature (method-level override)
 *   - Cart lifecycle: initialize (201), get, add item, remove item, checkout (200)
 *   - Abandon cart returns 200 with null data
 */
@WebMvcTest(CartController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("CartController")
class CartControllerTest {

    private static final String TENANT_ID  = "shop-xyz";
    private static final String USERNAME   = "cashier01";
    private static final String SESSION_ID = "sess-001";
    private static final String CART_ID    = "cart-uuid-abc";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean CartService                  cartService;

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

    private CartResponse cartWith(String cartId) {
        return CartResponse.builder()
                .cartId(cartId).total(BigDecimal.ZERO).items(List.of()).build();
    }

    // ── Auth and feature enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Auth and feature enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post("/carts").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token without POS feature → 403 FORBIDDEN")
        void missingPosFeature_returns403() throws Exception {
            mockMvc.perform(post("/carts")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden())
                   .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("COMMISSION endpoint with POS-only token → 403 (method-level override)")
        void commissionEndpoint_posOnly_returns403() throws Exception {
            // updateItemCommission has @RequiresFeature("COMMISSION") which overrides class-level POS
            mockMvc.perform(patch("/carts/" + CART_ID + "/items/1/commission")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                   .andExpect(status().isForbidden());
        }
    }

    // ── POST /carts ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /carts")
    class InitializeCart {

        @Test
        @DisplayName("POS feature → 201 with cartId in data")
        void withPosFeature_returns201() throws Exception {
            when(cartService.initializeCart()).thenReturn(cartWith(CART_ID));

            mockMvc.perform(post("/carts")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }

    // ── GET /carts/{cartId} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /carts/{cartId}")
    class GetCart {

        @Test
        @DisplayName("existing cart → 200 with cart data")
        void found_returns200() throws Exception {
            when(cartService.getCart(CART_ID)).thenReturn(cartWith(CART_ID));

            mockMvc.perform(get("/carts/" + CART_ID)
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }

    // ── POST /carts/{cartId}/items ────────────────────────────────────────────

    @Nested
    @DisplayName("POST /carts/{cartId}/items")
    class AddItem {

        @Test
        @DisplayName("valid request → 200 with updated cart")
        void addItem_returns200() throws Exception {
            CartRequest req = CartRequest.builder().productId(10L).quantity(2).build();
            CartResponse updated = cartWith(CART_ID);
            when(cartService.addItemToCart(anyString(), any(CartRequest.class))).thenReturn(updated);

            mockMvc.perform(post("/carts/" + CART_ID + "/items")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }

    // ── DELETE /carts/{cartId}/items/{itemId} ─────────────────────────────────

    @Nested
    @DisplayName("DELETE /carts/{cartId}/items/{itemId}")
    class RemoveItem {

        @Test
        @DisplayName("remove item → 200 with updated cart")
        void removeItem_returns200() throws Exception {
            when(cartService.removeItemFromCart(CART_ID, 5L)).thenReturn(cartWith(CART_ID));

            mockMvc.perform(delete("/carts/" + CART_ID + "/items/5")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }

    // ── POST /carts/{cartId}/checkout ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /carts/{cartId}/checkout")
    class Checkout {

        @Test
        @DisplayName("valid checkout → 200 with orderNumber")
        void checkout_returns200() throws Exception {
            CheckoutResponse response = CheckoutResponse.builder()
                    .orderId(100L).orderNumber("ORD-100").total(BigDecimal.valueOf(150_000)).build();
            when(cartService.checkout(anyString(), any(CheckoutRequest.class))).thenReturn(response);

            mockMvc.perform(post("/carts/" + CART_ID + "/checkout")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.orderNumber").value("ORD-100"))
                   .andExpect(jsonPath("$.data.orderId").value(100));
        }
    }

    // ── PATCH /carts/{cartId}/items/{itemId}/commission ───────────────────────

    @Nested
    @DisplayName("PATCH /carts/{cartId}/items/{itemId}/commission")
    class UpdateCommission {

        @Test
        @DisplayName("COMMISSION feature → 200 with updated cart")
        void withCommissionFeature_returns200() throws Exception {
            when(cartService.updateItemCommission(anyString(), anyLong(), any(), any()))
                    .thenReturn(cartWith(CART_ID));

            mockMvc.perform(patch("/carts/" + CART_ID + "/items/3/commission")
                    .header("Authorization", bearerToken("COMMISSION"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"assignedEmployeeId\":7}"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }

    // ── POST /carts/{cartId}/abandon ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /carts/{cartId}/abandon")
    class AbandonCart {

        @Test
        @DisplayName("POS feature → 200 success")
        void abandon_returns200() throws Exception {
            doNothing().when(cartService).abandonCart(CART_ID);

            mockMvc.perform(post("/carts/" + CART_ID + "/abandon")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── DELETE /carts/{cartId} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /carts/{cartId}")
    class ClearCart {

        @Test
        @DisplayName("clear cart → 200 with empty cart")
        void clear_returns200() throws Exception {
            when(cartService.clearCart(CART_ID)).thenReturn(cartWith(CART_ID));

            mockMvc.perform(delete("/carts/" + CART_ID)
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.cartId").value(CART_ID));
        }
    }
}
