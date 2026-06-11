package com.tappy.pos.service.tenant;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ForbiddenException;
import com.tappy.pos.model.dto.tenant.DeleteShopRequest;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.ShopDeletionLogRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopDeletionService Unit Tests")
class ShopDeletionServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ShopDeletionLogRepository deletionLogRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private MessageService messageService;

    @InjectMocks
    private ShopDeletionService service;

    private void authAs(String username, String... authorities) {
        var grants = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, grants));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private DeleteShopRequest request(String token) {
        DeleteShopRequest r = new DeleteShopRequest();
        r.setConfirmToken(token);
        r.setReason("đóng cửa");
        return r;
    }

    @Test
    @DisplayName("deleteShop: SHOP_OWNER with valid token soft-deletes the tenant")
    void deleteShop_success() {
        authAs("owner", "SHOP_OWNER");
        Tenant tenant = mock(Tenant.class);
        lenient().when(tenant.getTenantId()).thenReturn("shop-1");
        lenient().when(tenant.getName()).thenReturn("Tiệm A");
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        when(authContext.getCurrentUsername()).thenReturn("owner");
        when(userRepository.countByTenantId("shop-1")).thenReturn(3);
        when(roleRepository.deleteUserRoleAssignmentsByTenantId("shop-1")).thenReturn(2);
        when(userRepository.unlinkAllFromTenant("shop-1")).thenReturn(3);

        service.deleteShop(request("DELETE"));

        verify(deletionLogRepository).save(any());
        verify(tenant).setActive(false);
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("deleteShop: invalid confirmation token → BadRequestException")
    void deleteShop_invalidToken() {
        authAs("owner", "SHOP_OWNER");
        when(messageService.getMessage("error.shop.delete.token.invalid")).thenReturn("mã sai");

        assertThatThrownBy(() -> service.deleteShop(request("nope")))
                .isInstanceOf(BadRequestException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteShop: no tenant context → BadRequestException")
    void deleteShop_noTenant() {
        authAs("owner", "SHOP_OWNER");
        when(tenantContext.getCurrentTenant()).thenReturn(null);
        when(messageService.getMessage("error.shop.delete.tenant.notFound")).thenReturn("không tìm thấy");

        assertThatThrownBy(() -> service.deleteShop(request("DELETE")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("deleteShop: non-owner → ForbiddenException")
    void deleteShop_notOwner() {
        authAs("staff", "TECHNICIAN");
        Tenant tenant = mock(Tenant.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        when(messageService.getMessage("error.shop.delete.owner.only")).thenReturn("chỉ chủ tiệm");

        assertThatThrownBy(() -> service.deleteShop(request("DELETE")))
                .isInstanceOf(ForbiddenException.class);
        verify(tenantRepository, never()).save(any());
    }
}
