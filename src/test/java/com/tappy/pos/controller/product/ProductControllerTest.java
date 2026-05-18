package com.tappy.pos.controller.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.product.BarcodeLookupResult;
import com.tappy.pos.model.dto.product.CreateProductRequest;
import com.tappy.pos.model.dto.product.ProductDTO;
import com.tappy.pos.model.dto.product.UpdateProductRequest;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.product.ProductService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for ProductController — covers:
 *   - Auth enforcement (no token → 401, invalid session → 401, tenant mismatch → 403)
 *   - @RequiresFeature enforcement (missing PRODUCT → 403, has PRODUCT → proceeds)
 *   - HTTP routing and status codes for every endpoint
 *   - JSON serialization shape via ApiResponse<T> wrapper
 *   - Bean Validation (@Valid) rejection → 400 VALIDATION_ERROR with field map
 *   - Service exceptions → correct HTTP error codes
 *
 * TenantInterceptor is @MockBean so that the path-prefix mismatch between
 * /api/auth (production) and /auth (WebMvcTest) doesn't falsely block requests.
 */
@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("ProductController")
class ProductControllerTest {

    private static final String TENANT_ID  = "shop-abc";
    private static final String USERNAME   = "cashier01";
    private static final String SESSION_ID = "sess-001";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;   // bypass path-prefix mismatch
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean ProductService               productService;

    @BeforeEach
    void setUp() throws Exception {
        // TenantInterceptor: always allow through (path prefixes differ in test vs production)
        when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(true);

        when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                 .thenAnswer(inv -> inv.getArgument(0));

        // Entry point: write minimal 401 body so MockMvc can assert the status
        doAnswer(inv -> {
            HttpServletResponse resp = (HttpServletResponse) inv.getArgument(1);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.getWriter().write("{\"success\":false,\"error\":\"UNAUTHORIZED\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    // Generates a signed JWT that JwtAuthenticationFilter will accept.
    // The "features" claim drives FeatureAccessAspect; "tid" locks token to TENANT_ID.
    private String bearerToken(String... features) {
        return "Bearer " + jwtTokenProvider.generateTokenWithSession(
                USERNAME, List.of("SHOP_OWNER"), Arrays.asList(features),
                false, SESSION_ID, null, TENANT_ID);
    }

    // ── Auth enforcement ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Auth enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/products").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("valid token but session invalidated (device switch) → 401")
        void invalidatedSession_returns401() throws Exception {
            when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(false);

            mockMvc.perform(get("/products")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token issued for a different shop (tenant mismatch) → 403")
        void tenantMismatch_returns403() throws Exception {
            // JWT tid=[shop-abc] but request sends X-Tenant-ID: other-shop
            mockMvc.perform(get("/products")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", "other-shop"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("token without PRODUCT feature → 403 FORBIDDEN from FeatureAccessAspect")
        void missingFeature_returns403() throws Exception {
            mockMvc.perform(get("/products")
                    .header("Authorization", bearerToken("ORDER")) // ORDER, not PRODUCT
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden())
                   .andExpect(jsonPath("$.success").value(false))
                   .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }

    // ── GET /products ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products")
    class GetAllProducts {

        @Test
        @DisplayName("PRODUCT feature → 200 with paginated ApiResponse")
        void withFeature_returns200() throws Exception {
            Page<ProductDTO> page = new PageImpl<>(List.of(
                    ProductDTO.builder().id(1L).name("Cà phê").price(BigDecimal.valueOf(25000)).build()));
            when(productService.getAllProducts(anyString(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/products")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.content[0].name").value("Cà phê"))
                   .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("?status=INACTIVE forwarded to service")
        void statusParam_passedToService() throws Exception {
            when(productService.getAllProducts(eq("INACTIVE"), any(), any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/products")
                    .param("status", "INACTIVE")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk());

            verify(productService).getAllProducts(eq("INACTIVE"), any(), any(Pageable.class));
        }
    }

    // ── POST /products ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /products")
    class CreateProduct {

        @Test
        @DisplayName("valid body → 201 with created product in data")
        void validRequest_returns201() throws Exception {
            CreateProductRequest req = CreateProductRequest.builder()
                    .productTypeId(1L).sku("SKU-001").name("Bánh mì")
                    .price(BigDecimal.valueOf(15000)).attributes(Map.of()).build();
            ProductDTO created = ProductDTO.builder().id(10L).name("Bánh mì").sku("SKU-001").build();
            when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(created);

            mockMvc.perform(post("/products")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.id").value(10))
                   .andExpect(jsonPath("$.data.sku").value("SKU-001"));
        }

        @Test
        @DisplayName("missing required fields (name, sku, productTypeId) → 400 VALIDATION_ERROR with field map")
        void missingRequiredFields_returns400() throws Exception {
            // price and attributes present; name/sku/productTypeId absent
            String body = "{\"price\": 15000, \"attributes\": {}}";

            mockMvc.perform(post("/products")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                   .andExpect(jsonPath("$.data.name").exists())
                   .andExpect(jsonPath("$.data.sku").exists())
                   .andExpect(jsonPath("$.data.productTypeId").exists());
        }
    }

    // ── GET /products/{id} ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products/{id}")
    class GetProductById {

        @Test
        @DisplayName("product exists → 200 with product data")
        void found_returns200() throws Exception {
            ProductDTO dto = ProductDTO.builder().id(42L).name("Trà sữa").sku("SKU-042").build();
            when(productService.getProductById(42L)).thenReturn(dto);

            mockMvc.perform(get("/products/42")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.id").value(42))
                   .andExpect(jsonPath("$.data.sku").value("SKU-042"));
        }

        @Test
        @DisplayName("service throws ResourceNotFoundException → 404 RESOURCE_NOT_FOUND")
        void notFound_returns404() throws Exception {
            when(productService.getProductById(99L))
                    .thenThrow(new ResourceNotFoundException("Không tìm thấy sản phẩm"));

            mockMvc.perform(get("/products/99")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.success").value(false))
                   .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("non-numeric id path variable → 400")
        void nonNumericId_returns400() throws Exception {
            mockMvc.perform(get("/products/abc")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isBadRequest());
        }
    }

    // ── PUT /products/{id} ────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("valid update body → 200 with updated product")
        void valid_returns200() throws Exception {
            UpdateProductRequest req = UpdateProductRequest.builder()
                    .name("Trà xanh premium").price(BigDecimal.valueOf(30000))
                    .status("ACTIVE")                      // @NotBlank required field
                    .attributes(Map.of()).build();
            ProductDTO updated = ProductDTO.builder().id(5L).name("Trà xanh premium").build();
            when(productService.updateProduct(eq(5L), any(UpdateProductRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/products/5")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.name").value("Trà xanh premium"));
        }
    }

    // ── DELETE /products/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("success → 200 success:true")
        void success_returns200() throws Exception {
            doNothing().when(productService).deleteProduct(1L);

            mockMvc.perform(delete("/products/1")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── GET /products/search ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products/search")
    class SearchProducts {

        @Test
        @DisplayName("?keyword=trà → 200 with matching products")
        void byKeyword_returns200() throws Exception {
            Page<ProductDTO> result = new PageImpl<>(List.of(
                    ProductDTO.builder().id(3L).name("Trà xanh").build()));
            when(productService.searchProducts(eq("trà"), any(Pageable.class))).thenReturn(result);

            mockMvc.perform(get("/products/search")
                    .param("keyword", "trà")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.content[0].name").value("Trà xanh"));
        }
    }

    // ── GET /products/barcode/{barcode} ───────────────────────────────────────

    @Nested
    @DisplayName("GET /products/barcode/{barcode}")
    class BarcodeSearch {

        @Test
        @DisplayName("barcode found in shop inventory → 200 source=SHOP")
        void found_returnsShopSource() throws Exception {
            BarcodeLookupResult result = BarcodeLookupResult.builder()
                    .source(BarcodeLookupResult.Source.SHOP)
                    .product(ProductDTO.builder().id(7L).barcode("8934678000001").build())
                    .build();
            when(productService.lookupByBarcode("8934678000001")).thenReturn(result);

            mockMvc.perform(get("/products/barcode/8934678000001")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.source").value("SHOP"))
                   .andExpect(jsonPath("$.data.product.id").value(7));
        }
    }
}
