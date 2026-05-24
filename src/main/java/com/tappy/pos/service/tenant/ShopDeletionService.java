package com.tappy.pos.service.tenant;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ForbiddenException;
import com.tappy.pos.model.dto.tenant.DeleteShopRequest;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.tenant.ShopDeletionLog;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.ShopDeletionLogRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ShopDeletionService — handles the "delete my shop" flow.
 *
 * <p>When a SHOP_OWNER requests deletion:</p>
 * <ol>
 *   <li>Validate: the user must hold the SHOP_OWNER role in this tenant.</li>
 *   <li>Validate: the request must carry confirmToken == "DELETE" (case-insensitive).</li>
 *   <li>Record an audit row in {@code shop_deletion_log}.</li>
 *   <li>Remove all user↔role assignments scoped to this tenant (FK safety).</li>
 *   <li>Bulk-unlink every user from the tenant (tenant_id → NULL).</li>
 *   <li>Soft-delete the tenant: set {@code deleted_at}, {@code deleted_by}, {@code active = false}.</li>
 * </ol>
 *
 * <p>After this, any subsequent request with the old X-Tenant-ID will receive 410 Gone from
 * {@link com.tappy.pos.multitenant.TenantInterceptor}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopDeletionService {

    private static final String CONFIRM_TOKEN = "DELETE";

    private final TenantRepository        tenantRepository;
    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final ShopDeletionLogRepository deletionLogRepository;
    private final TenantContext           tenantContext;
    private final AuthContext             authContext;

    @Transactional
    public void deleteShop(DeleteShopRequest request) {
        // 1. Validate confirmation token
        if (!CONFIRM_TOKEN.equalsIgnoreCase(request.getConfirmToken())) {
            throw new BadRequestException("Mã xác nhận không hợp lệ. Vui lòng nhập 'DELETE' để xác nhận.");
        }

        // 2. Get current tenant from context (set by TenantInterceptor)
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new BadRequestException("Không tìm thấy thông tin cửa hàng.");
        }

        // 3. Verify caller has SHOP_OWNER authority
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isShopOwner = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("SHOP_OWNER"::equals);
        if (!isShopOwner) {
            throw new ForbiddenException("Chỉ chủ cửa hàng mới có thể xoá cửa hàng.");
        }

        String tenantId   = tenant.getTenantId();
        String shopName   = tenant.getName();
        String deletedBy  = authContext.getCurrentUsername();
        if (deletedBy == null) deletedBy = "unknown";

        log.info("Shop deletion initiated: tenantId={}, shopName={}, requestedBy={}", tenantId, shopName, deletedBy);

        // 4. Count users for audit
        int userCount = userRepository.countByTenantId(tenantId);

        // 5. Write audit log first (safe even if later steps fail)
        ShopDeletionLog auditLog = ShopDeletionLog.builder()
                .tenantId(tenantId)
                .shopName(shopName)
                .deletedBy(deletedBy)
                .deletedAt(LocalDateTime.now())
                .reason(request.getReason())
                .userCount(userCount)
                .build();
        deletionLogRepository.save(auditLog);

        // 6. Remove user↔role join rows for this tenant's roles (FK safety)
        int removedAssignments = roleRepository.deleteUserRoleAssignmentsByTenantId(tenantId);
        log.info("Removed {} user-role assignments for tenant {}", removedAssignments, tenantId);

        // 7. Unlink all users from the tenant (tenant_id → NULL)
        int unlinkedUsers = userRepository.unlinkAllFromTenant(tenantId);
        log.info("Unlinked {} users from tenant {}", unlinkedUsers, tenantId);

        // 8. Soft-delete the tenant
        tenant.setDeletedAt(LocalDateTime.now());
        tenant.setDeletedBy(deletedBy);
        tenant.setActive(false);
        tenantRepository.save(tenant);

        log.info("Shop {} ({}) soft-deleted by {}", shopName, tenantId, deletedBy);
    }
}
