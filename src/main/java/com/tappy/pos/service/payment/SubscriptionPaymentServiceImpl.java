package com.tappy.pos.service.payment;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.payment.CheckoutRequest;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.model.enums.SubscriptionPlan;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.payment.SubscriptionPaymentRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {

    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final Map<PaymentProvider, PaymentGateway> gateways = new EnumMap<>(PaymentProvider.class);

    public SubscriptionPaymentServiceImpl(TenantContext tenantContext,
                                          TenantRepository tenantRepository,
                                          SubscriptionPaymentRepository paymentRepository,
                                          MessageService messageService,
                                          ActivityLogService activityLogService,
                                          List<PaymentGateway> gatewayBeans) {
        this.tenantContext = tenantContext;
        this.tenantRepository = tenantRepository;
        this.paymentRepository = paymentRepository;
        this.messageService = messageService;
        this.activityLogService = activityLogService;
        for (PaymentGateway g : gatewayBeans) {
            gateways.put(g.provider(), g);
        }
    }

    /**
     * Best-effort actor for the activity log. A user-initiated checkout runs with an authenticated
     * shop user in the SecurityContext; an activation triggered by a provider webhook has none, so we
     * fall back to "system".
     */
    private String currentActorOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "system";
    }

    @Override
    // Deliberately NOT @Transactional: createCheckout() calls gateway.createCheckout(), which for
    // MoMo makes a blocking outbound HTTP request (up to ~25s on connect+read timeouts). Holding a
    // DB transaction — and therefore a pooled connection — across that call would let a slow payment
    // provider exhaust the connection pool and stall unrelated requests. The only DB write here is
    // the PENDING row, which paymentRepository.save() persists in its own short transaction;
    // subscription_payment is a no-RLS master table so it needs no tenant context on the connection.
    public CheckoutResponse createCheckout(CheckoutRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new BadRequestException(messageService.getMessage("error.tenant.header.required"));
        }

        // A checkout must name a concrete plan. isValid() deliberately accepts null/blank (it maps to
        // the default on the READ path), so we reject blank explicitly here — otherwise an empty
        // planCode would silently be billed and activated as the default plan.
        String planCode = request.getPlanCode() != null ? request.getPlanCode().trim().toUpperCase() : null;
        if (planCode == null || planCode.isBlank() || !SubscriptionPlan.isValid(planCode)) {
            throw new BadRequestException(messageService.getMessage("error.subscription.invalid", planCode));
        }
        BillingCycle cycle = request.getBillingCycle() != null ? request.getBillingCycle() : BillingCycle.MONTHLY;

        PaymentProvider method = request.getMethod();
        PaymentGateway gateway = method != null ? gateways.get(method) : null;
        if (gateway == null) {
            throw new BadRequestException(messageService.getMessage("error.payment.method_unsupported"));
        }

        // Amount is ALWAYS derived from the plan — never trusted from the client.
        SubscriptionPlan.PlanLimits limits = SubscriptionPlan.of(planCode);
        long amount = cycle.amountFor(limits);

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .tenantId(tenantId)
                .provider(method)
                .planCode(planCode)
                .billingCycle(cycle)
                .amount(amount)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .providerTxnRef(generateTxnRef())
                .description("Gia hạn gói " + planCode + " (" + cycle + ")")
                .build();
        // Persist the PENDING row in its own short transaction…
        payment = paymentRepository.save(payment);

        // …then make the (possibly slow, blocking) provider call OUTSIDE any open transaction.
        PaymentGateway.CheckoutResult result = gateway.createCheckout(payment);

        activityLogService.logAsync(tenantId, currentActorOrSystem(), null,
                ActivityAction.SUBSCRIPTION_CHECKOUT, "SUBSCRIPTION", payment.getProviderTxnRef(),
                "activity.subscription.checkout", null, planCode);

        return CheckoutResponse.builder()
                .txnRef(payment.getProviderTxnRef())
                .provider(method.name())
                .planCode(planCode)
                .billingCycle(cycle.name())
                .amount(amount)
                .type(result.type())
                .payUrl(result.payUrl())
                .qrContent(result.qrContent())
                .bankAccount(result.bankAccount())
                .bankName(result.bankName())
                .accountName(result.accountName())
                .transferNote(result.transferNote())
                .timingMessage(timingMessage(method))
                .build();
    }

    @Override
    @Transactional
    public void handleCallback(PaymentProvider provider, Map<String, String> params) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            log.warn("Callback for unsupported provider {}", provider);
            return;
        }
        PaymentGateway.CallbackResult cr = gateway.handleCallback(params);
        // Never act on an unverified callback: an attacker who knows a txnRef could otherwise POST an
        // unsigned "failed" callback and flip a victim's PENDING payment to FAILED. Only a callback
        // whose provider signature verified may read or mutate a payment row.
        if (!cr.signatureValid()) {
            log.warn("Rejecting {} callback for txnRef {} — invalid signature", provider, cr.txnRef());
            return;
        }
        SubscriptionPayment payment = paymentRepository.findByProviderTxnRef(cr.txnRef()).orElse(null);
        if (payment == null) {
            log.warn("Callback {} for unknown txnRef {}", provider, cr.txnRef());
            return;
        }
        payment.setRawPayload(params.toString());

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Duplicate callback for already-paid txnRef {} — ignoring", cr.txnRef());
            return; // idempotent
        }
        if (!cr.success()) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} failed via {}", cr.txnRef(), provider);
            return;
        }
        activate(payment);
    }

    @Override
    @Transactional
    public void confirmManual(String txnRef) {
        SubscriptionPayment payment = paymentRepository.findByProviderTxnRef(txnRef)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.payment.not_found")));
        if (payment.getStatus() == PaymentStatus.PAID) return; // idempotent
        activate(payment);
    }

    /** Mark paid and extend the tenant's subscription. Caller guarantees not-already-paid. */
    private void activate(SubscriptionPayment payment) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Tenant tenant = tenantRepository.findByTenantId(payment.getTenantId()).orElse(null);
        if (tenant == null) {
            log.error("Paid payment {} but tenant {} not found", payment.getProviderTxnRef(), payment.getTenantId());
            return;
        }

        // Extend from the later of today / current expiry, so paying early doesn't lose remaining days.
        LocalDate today = LocalDate.now();
        LocalDate base = (tenant.getExpirationDate() != null && tenant.getExpirationDate().isAfter(today))
                ? tenant.getExpirationDate() : today;
        LocalDate newExpiry = payment.getBillingCycle().extend(base);

        SubscriptionPlan.PlanLimits limits = SubscriptionPlan.of(payment.getPlanCode());
        tenant.setExpirationDate(newExpiry);
        tenant.setSubscriptionType(payment.getPlanCode());
        tenant.setMaxUsers(limits.maxUsers());
        tenantRepository.save(tenant);

        // Invalidate live tokens so the client refreshes — this also clears read-only mode instantly.
        tenantRepository.bumpFeaturesVersion(tenant.getTenantId());

        log.info("Activated {} {} for tenant {} → expires {}",
                payment.getPlanCode(), payment.getBillingCycle(), tenant.getTenantId(), newExpiry);

        // Audit on the shop's own timeline. tenantId is passed explicitly because the webhook thread
        // carries no TenantContext; actor is "system" for webhook activations or the master admin for
        // a manual confirm.
        activityLogService.logAsync(tenant.getTenantId(), currentActorOrSystem(), null,
                ActivityAction.SUBSCRIPTION_RENEWED, "SUBSCRIPTION", payment.getProviderTxnRef(),
                "activity.subscription.renewed", null, payment.getPlanCode(), newExpiry.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> historyForCurrentTenant() {
        String tenantId = tenantContext.getCurrentTenantId();
        List<Map<String, Object>> out = new ArrayList<>();
        for (SubscriptionPayment p : paymentRepository.findTop50ByTenantIdOrderByCreatedAtDesc(tenantId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("txnRef", p.getProviderTxnRef());
            m.put("provider", p.getProvider().name());
            m.put("planCode", p.getPlanCode());
            m.put("billingCycle", p.getBillingCycle().name());
            m.put("amount", p.getAmount());
            m.put("status", p.getStatus().name());
            m.put("createdAt", p.getCreatedAt());
            m.put("paidAt", p.getPaidAt());
            out.add(m);
        }
        return out;
    }

    private String timingMessage(PaymentProvider provider) {
        String key = switch (provider) {
            case APPLE_IAP, GOOGLE_IAP -> "payment.timing.instant";
            case VIETQR -> "payment.timing.vietqr";
            default -> "payment.timing.fast"; // MOMO, VNPAY
        };
        return messageService.getMessage(key);
    }

    private String generateTxnRef() {
        return "TPOS" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
