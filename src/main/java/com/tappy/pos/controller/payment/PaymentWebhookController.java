package com.tappy.pos.controller.payment;

import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.service.payment.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * PUBLIC server-to-server payment callbacks (no JWT, no tenant header). Permitted in SecurityConfig
 * (`/payments/webhook/**`) and TenantInterceptor public paths. The callback — not the browser
 * redirect — is the source of truth; each handler verifies the provider signature before activating.
 */
@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final SubscriptionPaymentService paymentService;

    /**
     * MoMo IPN (JSON POST). MoMo expects a 204 acknowledgement and stops retrying once it sees one.
     * We therefore ACK only when the callback was processed successfully; on a processing error we
     * return 500 (no ack) so MoMo retries the IPN — a transient failure must never leave a paid
     * subscription un-activated.
     */
    @PostMapping("/momo")
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> body) {
        Map<String, String> params = new HashMap<>();
        body.forEach((k, v) -> params.put(k, v == null ? null : String.valueOf(v)));
        try {
            paymentService.handleCallback(PaymentProvider.MOMO, params);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("MoMo IPN handling failed — not acknowledging so MoMo retries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * VNPay IPN (GET with query params). VNPay expects an {RspCode, Message} ack and retries the IPN
     * until it receives RspCode "00". We return "00" only on successful processing; on a processing
     * error we return "99" (Unknown error) so VNPay retries rather than treating the payment as
     * reconciled while the subscription is still un-activated.
     */
    @GetMapping("/vnpay")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleCallback(PaymentProvider.VNPAY, params);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            log.error("VNPay IPN handling failed — signalling retry to VNPay", e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }
}
