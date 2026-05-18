package com.tappy.pos.controller.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.customer.CreateCustomerRequest;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.customer.CustomerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for CustomerController — covers:
 *   - Auth and @RequiresFeature("CUSTOMER") enforcement at class level
 *   - GET /customers/walkin has method-level @RequiresFeature({"POS","PAWN","ORDER","CUSTOMER"})
 *     which overrides the class-level annotation — any of those four features grants access
 *   - CRUD lifecycle: create (201), list (200 paginated), get by ID, check-phone, orders, delete
 *   - Service ResourceNotFoundException → 404
 *
 * Two service dependencies: CustomerService + OrderService (both must be @MockBean).
 */
@WebMvcTest(CustomerController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("CustomerController")
class CustomerControllerTest {

    private static final String TENANT_ID  = "shop-abc";
    private static final String USERNAME   = "staff01";
    private static final String SESSION_ID = "sess-100";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean CustomerService              customerService;
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

    private CustomerDTO customerWith(Long id, String name, String phone) {
        return CustomerDTO.builder().id(id).name(name).phone(phone).build();
    }

    // ── Auth and feature enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Auth and feature enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/customers").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token without CUSTOMER feature → 403 FORBIDDEN")
        void missingCustomerFeature_returns403() throws Exception {
            mockMvc.perform(get("/customers")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden())
                   .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }

    // ── GET /customers/walkin ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /customers/walkin")
    class GetWalkin {

        @Test
        @DisplayName("POS feature satisfies method-level OR annotation → 200")
        void posToken_returns200() throws Exception {
            CustomerDTO walkin = customerWith(1L, "Khách lẻ", "0000000000");
            when(customerService.getWalkinCustomer()).thenReturn(walkin);

            mockMvc.perform(get("/customers/walkin")
                    .header("Authorization", bearerToken("POS"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.phone").value("0000000000"));
        }

        @Test
        @DisplayName("ORDER feature satisfies method-level OR annotation → 200")
        void orderToken_returns200() throws Exception {
            CustomerDTO walkin = customerWith(1L, "Khách lẻ", "0000000000");
            when(customerService.getWalkinCustomer()).thenReturn(walkin);

            mockMvc.perform(get("/customers/walkin")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PRODUCT feature not in walkin OR list → 403")
        void productToken_returns403() throws Exception {
            mockMvc.perform(get("/customers/walkin")
                    .header("Authorization", bearerToken("PRODUCT"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden());
        }
    }

    // ── POST /customers ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /customers")
    class CreateCustomer {

        @Test
        @DisplayName("valid body → 201 with customer data")
        void create_returns201() throws Exception {
            CreateCustomerRequest req = CreateCustomerRequest.builder()
                    .name("Nguyễn Thị B").phone("0987654321").build();
            CustomerDTO created = customerWith(10L, "Nguyễn Thị B", "0987654321");
            when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(created);

            mockMvc.perform(post("/customers")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.id").value(10))
                   .andExpect(jsonPath("$.data.name").value("Nguyễn Thị B"));
        }
    }

    // ── GET /customers ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /customers")
    class GetAllCustomers {

        @Test
        @DisplayName("CUSTOMER feature → 200 with paginated list")
        void withFeature_returns200() throws Exception {
            Page<CustomerDTO> page = new PageImpl<>(List.of(
                    customerWith(1L, "Trần Văn A", "0912345678")));
            when(customerService.getAllCustomers(any(), anyString(), anyString(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/customers")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true))
                   .andExpect(jsonPath("$.data.content[0].name").value("Trần Văn A"))
                   .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    // ── GET /customers/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /customers/{id}")
    class GetCustomerById {

        @Test
        @DisplayName("customer exists → 200 with customer data")
        void found_returns200() throws Exception {
            when(customerService.getCustomerById(42L)).thenReturn(customerWith(42L, "Lê Thị C", "0901234567"));

            mockMvc.perform(get("/customers/42")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.id").value(42))
                   .andExpect(jsonPath("$.data.name").value("Lê Thị C"));
        }

        @Test
        @DisplayName("service throws ResourceNotFoundException → 404")
        void notFound_returns404() throws Exception {
            when(customerService.getCustomerById(999L))
                    .thenThrow(new ResourceNotFoundException("Không tìm thấy khách hàng"));

            mockMvc.perform(get("/customers/999")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
        }
    }

    // ── GET /customers/check-phone ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /customers/check-phone")
    class CheckPhone {

        @Test
        @DisplayName("?phone= → 200 with matched customer")
        void found_returns200() throws Exception {
            when(customerService.findCustomerByPhone("0912345678"))
                    .thenReturn(customerWith(5L, "Phạm Văn D", "0912345678"));

            mockMvc.perform(get("/customers/check-phone")
                    .param("phone", "0912345678")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.phone").value("0912345678"));
        }
    }

    // ── GET /customers/{id}/orders ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /customers/{id}/orders")
    class GetCustomerOrders {

        @Test
        @DisplayName("CUSTOMER feature → 200 with paginated orders via OrderService")
        void found_returns200() throws Exception {
            Page<OrderDTO> page = new PageImpl<>(List.of(
                    OrderDTO.builder().id(1L).orderNumber("ORD-001").build()));
            when(orderService.getOrdersByCustomerId(anyLong(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/customers/5/orders")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-001"));
        }
    }

    // ── DELETE /customers/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /customers/{id}")
    class DeleteCustomer {

        @Test
        @DisplayName("CUSTOMER feature → 200 success")
        void delete_returns200() throws Exception {
            doNothing().when(customerService).deleteCustomer(3L);

            mockMvc.perform(delete("/customers/3")
                    .header("Authorization", bearerToken("CUSTOMER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.success").value(true));
        }
    }
}
