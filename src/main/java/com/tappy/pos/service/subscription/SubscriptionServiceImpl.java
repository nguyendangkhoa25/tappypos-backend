package com.tappy.pos.service.subscription;

import com.tappy.pos.exception.OrderLimitExceededException;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.SubscriptionPlan;
import com.tappy.pos.model.enums.SubscriptionType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public Map<String, Object> getForCurrentTenant() {
        String tenantId = tenantContext.getCurrentTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElseThrow();

        String planCode = tenant.getSubscriptionType() != null
                ? tenant.getSubscriptionType().toUpperCase()
                : SubscriptionPlan.DEFAULT_PLAN;
        SubscriptionPlan.PlanLimits limits = resolveLimits(planCode);

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
        data.put("currentUsers", userRepository.countByTenantId(tenantId));
        data.put("maxOrdersPerMonth", limits.isOrderUnlimited() ? null : limits.maxOrdersPerMonth());
        data.put("currentMonthOrders", currentMonthOrders);
        data.put("pricePerMonth", limits.pricePerMonth());
        data.put("pricePerYear", limits.pricePerYear());
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
                : SubscriptionPlan.DEFAULT_PLAN;
        SubscriptionPlan.PlanLimits limits = resolveLimits(planCode);
        if (limits.isOrderUnlimited()) return;

        LocalDate now = LocalDate.now();
        long used = orderRepository.countOrdersThisMonth(now.getYear(), now.getMonthValue());
        if (used >= limits.maxOrdersPerMonth()) {
            throw new OrderLimitExceededException(
                "Bạn đã đạt giới hạn " + limits.maxOrdersPerMonth() + " đơn hàng/tháng của gói " + displayName(planCode) +
                ". Vui lòng nâng cấp gói để tiếp tục.");
        }
    }

    /**
     * Resolve plan limits without ever throwing on the read/checkout hot paths. A bad/legacy
     * subscription_type (one that slipped past write-time validation, e.g. a manual DB edit)
     * degrades to the default plan with a warning instead of 500-ing the subscription screen or
     * blocking the cash register. Bad codes are rejected loudly at write time in TenantService.
     */
    private SubscriptionPlan.PlanLimits resolveLimits(String planCode) {
        try {
            return SubscriptionPlan.of(planCode);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown subscription plan code '{}' — treating as {}", planCode, SubscriptionPlan.DEFAULT_PLAN);
            return SubscriptionPlan.of(SubscriptionPlan.DEFAULT_PLAN);
        }
    }

    private String resolveStatus(Tenant tenant) {
        if (!tenant.isActive()) return "SUSPENDED";
        if (tenant.isExpired()) return "EXPIRED";
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
