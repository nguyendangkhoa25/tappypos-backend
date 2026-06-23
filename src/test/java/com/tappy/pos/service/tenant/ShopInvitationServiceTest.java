package com.tappy.pos.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.GenerateInvitationRequest;
import com.tappy.pos.model.dto.tenant.InvitationCodeResponse;
import com.tappy.pos.model.dto.tenant.InvitationPreviewResponse;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.tenant.ShopInvitation;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ShopType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.ShopInvitationRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.auth.RoleFeatureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ShopInvitationService Unit Tests")
class ShopInvitationServiceTest {

    @Mock private ShopInvitationRepository invitationRepo;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantService tenantService;
    @Mock private RoleFeatureService roleFeatureService;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private TenantProvisioningService tenantProvisioningService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    private ShopInvitationService service;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new ShopInvitationService(invitationRepo, roleRepository, userRepository, tenantService,
                roleFeatureService, tenantFeatureService, tenantProvisioningService, jwtTokenProvider,
                new ObjectMapper(), tenantContext, messageService);

        when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");

        tenant = Tenant.builder().tenantId("shop1").name("Nail A").shopType(ShopType.NAIL_SHOP).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "x"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ShopInvitation invitation() {
        return ShopInvitation.builder()
                .id(1L).tenantId("shop1").code("ABC234").roleName("CASHIER")
                .features("[\"POS\",\"ORDER\"]")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    // ── generate ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generate creates a code using role default features when none supplied")
    void generate_roleDefaults() {
        when(roleRepository.existsByNameAndTenantId("CASHIER", "shop1")).thenReturn(true);
        when(roleFeatureService.getActiveFeatureNamesByRoleName("CASHIER")).thenReturn(List.of("POS", "ORDER"));
        when(invitationRepo.existsByCode(anyString())).thenReturn(false);

        GenerateInvitationRequest req = new GenerateInvitationRequest();
        req.setRoleName("CASHIER");

        InvitationCodeResponse resp = service.generate(req);

        assertThat(resp.getCode()).hasSize(6);
        assertThat(resp.getRoleName()).isEqualTo("CASHIER");
        assertThat(resp.getFeatures()).containsExactly("POS", "ORDER");
        assertThat(resp.getSecondsRemaining()).isGreaterThan(0);
        verify(invitationRepo).save(any(ShopInvitation.class));
    }

    @Test
    @DisplayName("generate uses the request's feature override when provided")
    void generate_featureOverride() {
        when(roleRepository.existsByNameAndTenantId("CASHIER", "shop1")).thenReturn(true);
        when(invitationRepo.existsByCode(anyString())).thenReturn(false);

        GenerateInvitationRequest req = new GenerateInvitationRequest();
        req.setRoleName("CASHIER");
        req.setFeatures(List.of("CUSTOMER"));

        InvitationCodeResponse resp = service.generate(req);

        assertThat(resp.getFeatures()).containsExactly("CUSTOMER");
        verify(roleFeatureService, org.mockito.Mockito.never()).getActiveFeatureNamesByRoleName(anyString());
    }

    @Test
    @DisplayName("generate rejects an unknown role")
    void generate_unknownRole() {
        when(roleRepository.existsByNameAndTenantId("GHOST", "shop1")).thenReturn(false);
        GenerateInvitationRequest req = new GenerateInvitationRequest();
        req.setRoleName("GHOST");
        assertThatThrownBy(() -> service.generate(req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("generate retries on code collision then succeeds")
    void generate_codeCollision() {
        when(roleRepository.existsByNameAndTenantId("CASHIER", "shop1")).thenReturn(true);
        when(roleFeatureService.getActiveFeatureNamesByRoleName(anyString())).thenReturn(List.of("POS"));
        when(invitationRepo.existsByCode(anyString())).thenReturn(true, false);

        GenerateInvitationRequest req = new GenerateInvitationRequest();
        req.setRoleName("CASHIER");

        assertThat(service.generate(req).getCode()).hasSize(6);
        verify(invitationRepo, org.mockito.Mockito.times(2)).existsByCode(anyString());
    }

    // ── preview ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("preview returns shop info without consuming the code")
    void preview_ok() {
        when(invitationRepo.findValidByCode(eq("ABC234"), any())).thenReturn(Optional.of(invitation()));
        when(tenantService.getTenantEntity("shop1")).thenReturn(tenant);

        InvitationPreviewResponse resp = service.preview("abc234");

        assertThat(resp.getShopName()).isEqualTo("Nail A");
        assertThat(resp.getShopType()).isEqualTo("NAIL_SHOP");
        assertThat(resp.getRoleName()).isEqualTo("CASHIER");
        verify(invitationRepo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("preview falls back to OTHER for a tenant with no shop type")
    void preview_nullShopType() {
        tenant.setShopType(null);
        when(invitationRepo.findValidByCode(anyString(), any())).thenReturn(Optional.of(invitation()));
        when(tenantService.getTenantEntity("shop1")).thenReturn(tenant);

        assertThat(service.preview("ABC234").getShopType()).isEqualTo("OTHER");
    }

    @Test
    @DisplayName("preview throws for an invalid/expired code")
    void preview_invalid() {
        when(invitationRepo.findValidByCode(anyString(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.preview("BADX23")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── join ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("join assigns the role, seeds the staff employee, marks code used, issues a token")
    void join_ok() {
        ShopInvitation inv = invitation();
        User user = new User();
        user.setUsername("newbie");
        Role role = Role.builder().name("CASHIER").build();

        when(invitationRepo.findValidByCode(eq("ABC234"), any())).thenReturn(Optional.of(inv));
        when(userRepository.findByUsernameAndNullTenant("newbie")).thenReturn(Optional.of(user));
        when(tenantService.getTenantEntity("shop1")).thenReturn(tenant);
        when(roleRepository.findByNameAndTenantId("CASHIER", "shop1")).thenReturn(Optional.of(role));
        when(tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(List.of("CASHIER")))
                .thenReturn(List.of("POS", "ORDER"));
        when(jwtTokenProvider.generateTokenWithRolesAndFeatures(eq("newbie"), eq(List.of("CASHIER")),
                eq(List.of("POS", "ORDER")), eq(false), eq("NAIL_SHOP"), eq("shop1"), any())).thenReturn("JWT");

        Map<String, Object> result = service.join("abc234", "newbie");

        assertThat(result).containsEntry("accessToken", "JWT").containsEntry("tenantId", "shop1");
        assertThat(user.getTenantId()).isEqualTo("shop1");
        assertThat(user.getRoles()).contains(role);
        assertThat(inv.getUsedBy()).isEqualTo("newbie");
        assertThat(inv.getUsedAt()).isNotNull();
        verify(tenantProvisioningService).seedJoinedStaffEmployee(user, "shop1", "CASHIER");
        verify(tenantContext).setCurrentTenant(tenant);
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("join rejects a user who already belongs to a tenant")
    void join_alreadyMember() {
        when(invitationRepo.findValidByCode(anyString(), any())).thenReturn(Optional.of(invitation()));
        when(userRepository.findByUsernameAndNullTenant("newbie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join("ABC234", "newbie")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("join throws for an invalid code")
    void join_invalidCode() {
        when(invitationRepo.findValidByCode(anyString(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.join("BADX23", "newbie")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("join clears tenant context even when role lookup fails")
    void join_roleMissingClearsContext() {
        ShopInvitation inv = invitation();
        User user = new User();
        user.setUsername("newbie");
        when(invitationRepo.findValidByCode(anyString(), any())).thenReturn(Optional.of(inv));
        when(userRepository.findByUsernameAndNullTenant("newbie")).thenReturn(Optional.of(user));
        when(tenantService.getTenantEntity("shop1")).thenReturn(tenant);
        when(roleRepository.findByNameAndTenantId("CASHIER", "shop1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join("ABC234", "newbie")).isInstanceOf(BadRequestException.class);
        verify(tenantContext).clear();
    }

    // ── JSON (de)serialization helpers ───────────────────────────────────────────────

    @Test
    @DisplayName("fromJson parses a stored feature list via the TypeReference")
    void fromJson_parsesList() {
        @SuppressWarnings("unchecked")
        List<String> parsed = (List<String>) ReflectionTestUtils.invokeMethod(
                service, "fromJson", "[\"POS\",\"ORDER\",\"CUSTOMER\"]");
        assertThat(parsed).containsExactly("POS", "ORDER", "CUSTOMER");
    }

    @Test
    @DisplayName("fromJson returns an empty list for malformed JSON")
    void fromJson_malformedReturnsEmpty() {
        @SuppressWarnings("unchecked")
        List<String> parsed = (List<String>) ReflectionTestUtils.invokeMethod(
                service, "fromJson", "not-json");
        assertThat(parsed).isEmpty();
    }

    @Test
    @DisplayName("toJson serializes a feature list and round-trips with fromJson")
    void toJson_roundTrip() {
        String json = ReflectionTestUtils.invokeMethod(service, "toJson", List.of("POS", "INVENTORY"));
        assertThat(json).isEqualTo("[\"POS\",\"INVENTORY\"]");

        @SuppressWarnings("unchecked")
        List<String> parsed = (List<String>) ReflectionTestUtils.invokeMethod(service, "fromJson", json);
        assertThat(parsed).containsExactly("POS", "INVENTORY");
    }
}
