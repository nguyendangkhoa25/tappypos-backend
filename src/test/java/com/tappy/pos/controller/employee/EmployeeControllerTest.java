package com.tappy.pos.controller.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.aspect.FeatureAccessAspect;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.config.JwtAuthenticationEntryPoint;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.config.SecurityConfig;
import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.multitenant.TenantInterceptor;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.SessionRegistry;
import com.tappy.pos.service.employee.EmployeeService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for EmployeeController — covers:
 *   - Auth and @RequiresFeature("EMPLOYEE") enforcement
 *   - IMPORTANT: EmployeeController does NOT use ApiResponse<T> wrapper — it returns
 *     ResponseEntity<Page<EmployeeDTO>> and ResponseEntity<EmployeeDTO> directly.
 *     JSON assertions target root fields (e.g. $.content, $.fullName) not $.data.
 *   - DELETE /employees/{id} returns 204 No Content (noContent().build())
 *   - POST /employees uses @Valid — missing @NotBlank fullName → 400
 */
@WebMvcTest(EmployeeController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        AuthContext.class,
        FeatureContext.class,
        FeatureAccessAspect.class
})
@EnableAspectJAutoProxy
@DisplayName("EmployeeController")
class EmployeeControllerTest {

    private static final String TENANT_ID  = "shop-abc";
    private static final String USERNAME   = "owner01";
    private static final String SESSION_ID = "sess-300";

    @Autowired MockMvc       mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TenantInterceptor            tenantInterceptor;
    @MockBean SessionRegistry              sessionRegistry;
    @MockBean JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    @MockBean MessageService               messageService;
    @MockBean EmployeeService              employeeService;

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

    private EmployeeDTO employeeWith(Long id, String fullName) {
        return EmployeeDTO.builder().id(id).fullName(fullName).active(true).build();
    }

    // ── Auth and feature enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Auth and feature enforcement")
    class AuthEnforcement {

        @Test
        @DisplayName("no token → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/employees").header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token without EMPLOYEE feature → 403 FORBIDDEN")
        void missingFeature_returns403() throws Exception {
            mockMvc.perform(get("/employees")
                    .header("Authorization", bearerToken("ORDER"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isForbidden())
                   .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }

    // ── GET /employees ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees")
    class GetAll {

        @Test
        @DisplayName("EMPLOYEE feature → 200 with Page<EmployeeDTO> (no ApiResponse wrapper)")
        void withFeature_returns200() throws Exception {
            // EmployeeController returns ResponseEntity<Page<EmployeeDTO>> directly — no $.data nesting
            var page = new PageImpl<>(List.of(employeeWith(1L, "Nguyễn Văn A")));
            when(employeeService.getAll(any(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/employees")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.content[0].fullName").value("Nguyễn Văn A"))
                   .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ── GET /employees/all ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/all")
    class GetAllActive {

        @Test
        @DisplayName("EMPLOYEE feature → 200 with List<EmployeeDTO>")
        void withFeature_returns200() throws Exception {
            when(employeeService.getAllActive())
                    .thenReturn(List.of(employeeWith(1L, "Trần Thị B"), employeeWith(2L, "Lê Văn C")));

            mockMvc.perform(get("/employees/all")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$[0].fullName").value("Trần Thị B"))
                   .andExpect(jsonPath("$[1].fullName").value("Lê Văn C"));
        }
    }

    // ── GET /employees/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}")
    class GetById {

        @Test
        @DisplayName("employee exists → 200 with EmployeeDTO (no ApiResponse wrapper)")
        void found_returns200() throws Exception {
            when(employeeService.getById(7L)).thenReturn(employeeWith(7L, "Phạm Thị D"));

            mockMvc.perform(get("/employees/7")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id").value(7))
                   .andExpect(jsonPath("$.fullName").value("Phạm Thị D"));
        }
    }

    // ── POST /employees ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /employees")
    class Create {

        @Test
        @DisplayName("valid body → 201 with created EmployeeDTO")
        void validRequest_returns201() throws Exception {
            CreateEmployeeRequest req = new CreateEmployeeRequest();
            req.setFullName("Hoàng Văn E");
            req.setPosition("Cashier");

            when(employeeService.create(any(CreateEmployeeRequest.class)))
                    .thenReturn(employeeWith(10L, "Hoàng Văn E"));

            mockMvc.perform(post("/employees")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.id").value(10))
                   .andExpect(jsonPath("$.fullName").value("Hoàng Văn E"));
        }

        @Test
        @DisplayName("missing @NotBlank fullName → 400 VALIDATION_ERROR")
        void missingFullName_returns400() throws Exception {
            String body = "{\"position\":\"Cashier\"}"; // fullName absent

            mockMvc.perform(post("/employees")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                   .andExpect(jsonPath("$.data.fullName").exists());
        }
    }

    // ── PUT /employees/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /employees/{id}")
    class Update {

        @Test
        @DisplayName("valid update → 200 with updated EmployeeDTO")
        void valid_returns200() throws Exception {
            UpdateEmployeeRequest req = new UpdateEmployeeRequest();
            req.setFullName("Hoàng Văn E (cập nhật)");

            when(employeeService.update(anyLong(), any(UpdateEmployeeRequest.class)))
                    .thenReturn(employeeWith(10L, "Hoàng Văn E (cập nhật)"));

            mockMvc.perform(put("/employees/10")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.fullName").value("Hoàng Văn E (cập nhật)"));
        }
    }

    // ── DELETE /employees/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /employees/{id}")
    class Delete {

        @Test
        @DisplayName("EMPLOYEE feature → 204 No Content")
        void delete_returns204() throws Exception {
            doNothing().when(employeeService).delete(5L);

            mockMvc.perform(delete("/employees/5")
                    .header("Authorization", bearerToken("EMPLOYEE"))
                    .header("X-Tenant-ID", TENANT_ID))
                   .andExpect(status().isNoContent());
        }
    }
}
