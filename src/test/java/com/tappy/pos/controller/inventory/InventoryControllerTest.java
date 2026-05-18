package com.tappy.pos.controller.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.model.dto.inventory.CreateInventoryRequest;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.inventory.InventoryService;
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
 * @WebMvcTest for InventoryController — covers:
 *   - Auth and @RequiresFeature("INVENTORY") enforcement at class level
 *   - GET /inventory and GET /inventory/search have method-level @RequiresFeature({"INVENTORY","POS"})
 *     — both INVENTORY and POS tokens grant access to those endpoints
 *   - Other endpoints (class-level only) require exactly INVENTORY — POS alone gives 403
 *   - Stock adjustment PATCH endpoints (add-stock, remove-stock)
 *   - Alert endpoints (low-stock, expired, expiring-soon)
 */
@WebMvcTest(InventoryController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("InventoryController")
class InventoryControllerTest {

    private static final String TENANT_ID  = "shop-xyz";
    private static final String USERNAME   = "staff01";
    private static final String SESSION_ID = "sess-200";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean InventoryService             inventoryService;

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

    private InventoryDTO inventoryWith(Long id, Long productId) {
        return InventoryDTO.builder().id(id).productId(productId)
                .quantityInStock(100L).warehouseLocation("A-01").build();
    }

    // ── Auth and feature enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Auth and feature enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/inventory").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INVENTORY feature → 403 from class-level annotation on non-list endpoint")
        void inventoryFeature_classLevelEndpoint_returns200() throws Exception {
            when(inventoryService.getInventoryById(1L)).thenReturn(inventoryWith(1L, 10L));

            mockMvc.perform(get("/inventory/1")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POS-only token on class-level-only endpoint → 403")
        void posToken_classLevelEndpoint_returns403() throws Exception {
            // GET /inventory/{id} uses only class-level @RequiresFeature("INVENTORY") — POS alone is denied
            mockMvc.perform(get("/inventory/1")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POS-only token on GET /inventory satisfies method-level OR annotation → 200")
        void posToken_listEndpoint_returns200() throws Exception {
            when(inventoryService.getAllInventory(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/inventory")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk());
        }
    }

    // ── POST /inventory ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /inventory")
    class CreateInventory {

        @Test
        @DisplayName("valid body → 201 with inventory data")
        void create_returns201() throws Exception {
            CreateInventoryRequest req = CreateInventoryRequest.builder()
                    .productId(5L).quantityInStock(50L).reorderLevel(10L)
                    .reorderQuantity(20L).unitCost(BigDecimal.valueOf(15_000))
                    .warehouseLocation("B-02").build();
            when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
                    .thenReturn(inventoryWith(1L, 5L));

            mockMvc.perform(post("/inventory")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.productId").value(5));
        }

        @Test
        @DisplayName("missing required fields → 400 VALIDATION_ERROR")
        void missingRequiredFields_returns400() throws Exception {
            String body = "{\"notes\":\"test\"}"; // productId, quantityInStock, etc. missing

            mockMvc.perform(post("/inventory")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    // ── GET /inventory ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /inventory")
    class GetAllInventory {

        @Test
        @DisplayName("INVENTORY feature → 200 with paginated list")
        void inventoryToken_returns200() throws Exception {
            Page<InventoryDTO> page = new PageImpl<>(List.of(inventoryWith(1L, 10L)));
            when(inventoryService.getAllInventory(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/inventory")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.content[0].id").value(1))
                   .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    // ── GET /inventory/search ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /inventory/search")
    class SearchInventory {

        @Test
        @DisplayName("?keyword= with POS feature satisfies OR annotation → 200")
        void posToken_returns200() throws Exception {
            Page<InventoryDTO> page = new PageImpl<>(List.of(inventoryWith(2L, 20L)));
            when(inventoryService.searchInventory(anyString(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/inventory/search")
                    .param("keyword", "panadol")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.content[0].id").value(2));
        }
    }

    // ── PATCH stock adjustment ────────────────────────────────────────────────

    @Nested
    @DisplayName("Stock adjustment endpoints")
    class StockAdjustment {

        @Test
        @DisplayName("PATCH /inventory/{id}/add-stock?quantity= → 200 with updated inventory")
        void addStock_returns200() throws Exception {
            InventoryDTO updated = inventoryWith(1L, 10L);
            when(inventoryService.addStock(1L, 30L)).thenReturn(updated);

            mockMvc.perform(patch("/inventory/1/add-stock")
                    .param("quantity", "30")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("PATCH /inventory/{id}/remove-stock?quantity= → 200 with updated inventory")
        void removeStock_returns200() throws Exception {
            InventoryDTO updated = inventoryWith(1L, 10L);
            when(inventoryService.removeStock(1L, 10L)).thenReturn(updated);

            mockMvc.perform(patch("/inventory/1/remove-stock")
                    .param("quantity", "10")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.id").value(1));
        }
    }

    // ── Alert endpoints ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Alert endpoints")
    class Alerts {

        @Test
        @DisplayName("GET /inventory/alerts/low-stock → 200 with list")
        void lowStock_returns200() throws Exception {
            when(inventoryService.getLowStockItems()).thenReturn(List.of(inventoryWith(3L, 30L)));

            mockMvc.perform(get("/inventory/alerts/low-stock")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data[0].id").value(3));
        }

        @Test
        @DisplayName("GET /inventory/alerts/expired → 200 with list")
        void expired_returns200() throws Exception {
            when(inventoryService.getExpiredItems()).thenReturn(List.of());

            mockMvc.perform(get("/inventory/alerts/expired")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("GET /inventory/alerts/expiring-soon → 200 with list")
        void expiringSoon_returns200() throws Exception {
            when(inventoryService.getExpiringSoon()).thenReturn(List.of(inventoryWith(4L, 40L)));

            mockMvc.perform(get("/inventory/alerts/expiring-soon")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data[0].id").value(4));
        }
    }

    // ── DELETE /inventory/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /inventory/{id}")
    class DeleteInventory {

        @Test
        @DisplayName("INVENTORY feature → 200 success")
        void delete_returns200() throws Exception {
            doNothing().when(inventoryService).deleteInventory(1L);

            mockMvc.perform(delete("/inventory/1")
                    .header("Authorization", bearerToken("INVENTORY"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true));
        }
    }
}
