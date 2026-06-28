package com.tappy.pos.service.payment;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.payment.CheckoutRequest;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.payment.SubscriptionPaymentRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionPaymentServiceImpl Unit Tests")
class SubscriptionPaymentServiceImplTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final SubscriptionPaymentRepository paymentRepository = mock(SubscriptionPaymentRepository.class);
    private final MessageService messageService = mock(MessageService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final PaymentGateway vietqr = mock(PaymentGateway.class);

    private SubscriptionPaymentServiceImpl newService() {
        when(vietqr.provider()).thenReturn(PaymentProvider.VIETQR);
        return new SubscriptionPaymentServiceImpl(
                tenantContext, tenantRepository, paymentRepository, messageService, activityLogService, List.of(vietqr));
    }

    @Test
    @DisplayName("createCheckout derives the amount from the plan (never the client) and persists PENDING")
    void createCheckout_derivesAmount() {
        SubscriptionPaymentServiceImpl service = newService();
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vietqr.createCheckout(any())).thenReturn(
                new PaymentGateway.CheckoutResult(CheckoutResponse.Type.QR, null, "QRDATA",
                        "0123", "Vietcombank", "TAPPY", "ref"));

        CheckoutRequest req = new CheckoutRequest();
        req.setPlanCode("pro");                 // lower-case → normalized
        req.setBillingCycle(BillingCycle.MONTHLY);
        req.setMethod(PaymentProvider.VIETQR);

        CheckoutResponse res = service.createCheckout(req);

        assertThat(res.getPlanCode()).isEqualTo("PRO");
        assertThat(res.getAmount()).isEqualTo(199_000); // PRO monthly from SubscriptionPlan.LIMITS
        assertThat(res.getType()).isEqualTo(CheckoutResponse.Type.QR);
        assertThat(res.getQrContent()).isEqualTo("QRDATA");
        assertThat(res.getTxnRef()).isNotBlank();
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING && p.getAmount() == 199_000));
    }

    @Test
    @DisplayName("createCheckout rejects a blank planCode instead of billing the default plan")
    void createCheckout_rejectsBlankPlan() {
        SubscriptionPaymentServiceImpl service = newService();
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");

        CheckoutRequest req = new CheckoutRequest();
        req.setPlanCode("   ");                 // whitespace-only → must be rejected, not defaulted
        req.setBillingCycle(BillingCycle.MONTHLY);
        req.setMethod(PaymentProvider.VIETQR);

        assertThatThrownBy(() -> service.createCheckout(req))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckout YEARLY uses the annual price")
    void createCheckout_yearly() {
        SubscriptionPaymentServiceImpl service = newService();
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vietqr.createCheckout(any())).thenReturn(
                new PaymentGateway.CheckoutResult(CheckoutResponse.Type.QR, null, "Q", "0", "B", "N", "r"));

        CheckoutRequest req = new CheckoutRequest();
        req.setPlanCode("PRO");
        req.setBillingCycle(BillingCycle.YEARLY);
        req.setMethod(PaymentProvider.VIETQR);

        assertThat(service.createCheckout(req).getAmount()).isEqualTo(1_990_000); // PRO yearly
    }

    @Test
    @DisplayName("handleCallback: a successful callback activates and extends the subscription + bumps features version")
    void handleCallback_activates() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.PENDING).providerTxnRef("TXN1").build();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantId()).thenReturn("shop1");
        when(tenant.getExpirationDate()).thenReturn(null);

        when(vietqr.handleCallback(any())).thenReturn(new PaymentGateway.CallbackResult("TXN1", true, true));
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(payment));
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));

        service.handleCallback(PaymentProvider.VIETQR, Map.of("ignored", "x"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(tenant).setExpirationDate(LocalDate.now().plusMonths(1));
        verify(tenant).setSubscriptionType("PRO");
        verify(tenant).setMaxUsers(4); // PRO = 4 users
        verify(tenantRepository).bumpFeaturesVersion("shop1");
    }

    @Test
    @DisplayName("handleCallback: an already-PAID payment is idempotent (no re-activation)")
    void handleCallback_idempotent() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment paid = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.PAID).providerTxnRef("TXN1").build();
        when(vietqr.handleCallback(any())).thenReturn(new PaymentGateway.CallbackResult("TXN1", true, true));
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(paid));

        service.handleCallback(PaymentProvider.VIETQR, Map.of());

        verify(tenantRepository, never()).bumpFeaturesVersion(any());
        verify(tenantRepository, never()).findByTenantId(any());
    }

    @Test
    @DisplayName("VietQR payload: well-formed EMVCo string (format header, amount, memo, 4-hex CRC tail)")
    void vietQrPayload_wellFormed() {
        String qr = VietQrGateway.buildVietQrPayload("970436", "1234567890", 199_000, "TXN1");
        assertThat(qr).startsWith("000201");        // payload format indicator + dynamic init
        assertThat(qr).contains("970436");          // bank BIN
        assertThat(qr).contains("1234567890");      // account number
        assertThat(qr).contains("5406199000");      // amount field (tag 54, len 06, value 199000)
        assertThat(qr).contains("TXN1");            // memo
        assertThat(qr.substring(qr.length() - 8)).startsWith("6304"); // CRC tag+len before 4 hex chars
        assertThat(qr.substring(qr.length() - 4)).matches("[0-9A-F]{4}");
    }

    @Test
    @DisplayName("handleCallback: a validly-signed but declined callback marks FAILED and does not activate")
    void handleCallback_failed() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment pending = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.PENDING).providerTxnRef("TXN1").build();
        when(vietqr.handleCallback(any())).thenReturn(new PaymentGateway.CallbackResult("TXN1", true, false));
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(pending));

        service.handleCallback(PaymentProvider.VIETQR, Map.of());

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(tenantRepository, never()).bumpFeaturesVersion(any());
    }

    @Test
    @DisplayName("handleCallback: an invalid-signature callback is ignored — never looks up or mutates the payment")
    void handleCallback_invalidSignature_ignored() {
        SubscriptionPaymentServiceImpl service = newService();
        // Signature did not verify → the service must not even resolve the txnRef, so a forged
        // "failed" callback cannot flip a victim's PENDING payment to FAILED.
        when(vietqr.handleCallback(any())).thenReturn(new PaymentGateway.CallbackResult("TXN1", false, false));

        service.handleCallback(PaymentProvider.VIETQR, Map.of());

        verify(paymentRepository, never()).findByProviderTxnRef(any());
        verify(paymentRepository, never()).save(any());
        verify(tenantRepository, never()).bumpFeaturesVersion(any());
    }

    @Test
    @DisplayName("refund: a PAID payment is marked REFUNDED; tenant expiry is left unchanged")
    void refund_marksPaidRefunded() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment paid = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.PAID).providerTxnRef("TXN1").build();
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(paid));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.refund("TXN1");

        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(tenantRepository, never()).findByTenantId(any());        // expiry untouched
        verify(tenantRepository, never()).bumpFeaturesVersion(any());
    }

    @Test
    @DisplayName("refund: an already-REFUNDED payment is idempotent (no re-save)")
    void refund_idempotent() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment refunded = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.REFUNDED).providerTxnRef("TXN1").build();
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(refunded));

        service.refund("TXN1");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("refund: a non-paid (PENDING) payment cannot be refunded")
    void refund_rejectsNonPaid() {
        SubscriptionPaymentServiceImpl service = newService();
        SubscriptionPayment pending = SubscriptionPayment.builder()
                .tenantId("shop1").provider(PaymentProvider.VIETQR).planCode("PRO")
                .billingCycle(BillingCycle.MONTHLY).amount(199_000)
                .status(PaymentStatus.PENDING).providerTxnRef("TXN1").build();
        when(paymentRepository.findByProviderTxnRef("TXN1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.refund("TXN1")).isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordOfflinePayment: creates a PAID MANUAL payment (plan-derived amount) and extends the subscription")
    void recordOfflinePayment_createsPaidManualAndExtends() {
        SubscriptionPaymentServiceImpl service = newService();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantId()).thenReturn("shop1");
        when(tenant.getExpirationDate()).thenReturn(null);
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordOfflinePayment("shop1", "basic", BillingCycle.MONTHLY, "cash at counter");

        // MANUAL provider, amount derived from BASIC monthly (99,000), persisted.
        verify(paymentRepository, atLeastOnce()).save(argThat(p ->
                p.getProvider() == PaymentProvider.MANUAL
                        && "BASIC".equals(p.getPlanCode())
                        && p.getAmount() == 99_000));
        verify(tenant).setExpirationDate(LocalDate.now().plusMonths(1));
        verify(tenant).setSubscriptionType("BASIC");
        verify(tenant).setMaxUsers(2); // BASIC = 2 users
        verify(tenantRepository).bumpFeaturesVersion("shop1");
    }

    @Test
    @DisplayName("recordOfflinePayment: an unknown plan code is rejected before any write")
    void recordOfflinePayment_rejectsUnknownPlan() {
        SubscriptionPaymentServiceImpl service = newService();

        assertThatThrownBy(() -> service.recordOfflinePayment("shop1", "NOPE", BillingCycle.MONTHLY, null))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }
}
