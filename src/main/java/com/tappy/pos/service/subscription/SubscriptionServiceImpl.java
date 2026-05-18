package com.tappy.pos.service.subscription;

import com.tappy.pos.exception.OrderLimitExceededException;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.SubscriptionPlan;
import com.tappy.pos.model.enums.SubscriptionType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;

    @Override
    public Map<String, Object> getForCurrentTenant() {
        String tenantId = tenantContext.getCurrentTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElseThrow();

        String planCode = tenant.getSubscriptionType() != null
                ? tenant.getSubscriptionType().toUpperCase()
                : "TRIAL";
        SubscriptionPlan.PlanLimits limits = SubscriptionPlan.of(planCode);

        LocalDate expiration = tenant.getExpirationDate();
        String status = resolveStatus(tenant);

        LocalDate now = LocalDate.now();
        long currentMonthOrders = orderRepository.countOrdersThisMonth(now.getYear(), now.getMonthValue());

        String displayName = displayName(planCode);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plan", planCode);
        data.put("planDisplayName", displayName);
        data.put("status", status);
        data.put("startedAt", expiration != null ? expiration.minusYears(1).toString() : null);
        data.put("expiresAt", expiration != null ? expiration.toString() : null);
        data.put("maxUsers", limits.isUserUnlimited() ? null : limits.maxUsers());
        data.put("currentUsers", tenant.getMaxUsers() != null ? tenant.getMaxUsers() : 1);
        data.put("maxOrdersPerMonth", limits.isOrderUnlimited() ? null : limits.maxOrdersPerMonth());
        data.put("currentMonthOrders", currentMonthOrders);
        data.put("pricePerMonth", limits.pricePerMonth());
        data.put("features", java.util.List.of());
        return data;
    }

    @Override
    public void checkOrderLimit() {
        String tenantId = tenantContext.getCurrentTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null) return;

        // Expired tenants are blocked upstream by TenantInterceptor; skip here.
        String planCode = tenant.getSubscriptionType() != null
                ? tenant.getSubscriptionType().toUpperCase()
                : "TRIAL";
        SubscriptionPlan.PlanLimits limits = SubscriptionPlan.of(planCode);
        if (limits.isOrderUnlimited()) return;

        LocalDate now = LocalDate.now();
        long used = orderRepository.countOrdersThisMonth(now.getYear(), now.getMonthValue());
        if (used >= limits.maxOrdersPerMonth()) {
            throw new OrderLimitExceededException(
                "Bạn đã đạt giới hạn " + limits.maxOrdersPerMonth() + " đơn hàng/tháng của gói " + displayName(planCode) +
                ". Vui lòng nâng cấp gói để tiếp tục.");
        }
    }

    private String resolveStatus(Tenant tenant) {
        if (!tenant.isActive()) return "SUSPENDED";
        if (tenant.getExpirationDate() != null && tenant.getExpirationDate().isBefore(LocalDate.now())) return "EXPIRED";
        return "ACTIVE";
    }

    private String displayName(String planCode) {
        try {
            return SubscriptionType.valueOf(planCode).getDisplayName();
        } catch (IllegalArgumentException e) {
            return planCode;
        }
    }
}
