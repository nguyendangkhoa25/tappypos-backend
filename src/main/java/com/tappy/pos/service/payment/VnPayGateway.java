package com.tappy.pos.service.payment;

import com.tappy.pos.config.PaymentProperties;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * VNPay redirect gateway. The checkout URL is built and HMAC-SHA512 signed entirely server-side
 * (no API call), then the browser is redirected to it. The IPN callback is verified by recomputing
 * the signature. Config-gated: an unconfigured merchant rejects checkout with a friendly error.
 *
 * NOTE: the exact field set / encoding follows VNPay's documented merchant spec — verify against the
 * VNPay sandbox before go-live.
 */
@Component
@RequiredArgsConstructor
public class VnPayGateway implements PaymentGateway {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentProperties props;
    private final MessageService messageService;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.VNPAY;
    }

    @Override
    public CheckoutResult createCheckout(SubscriptionPayment payment) {
        PaymentProperties.Vnpay cfg = props.getVnpay();
        if (!cfg.isConfigured()) {
            throw new BadRequestException(messageService.getMessage("error.payment.not_configured"));
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", cfg.getTmnCode());
        params.put("vnp_Amount", String.valueOf(payment.getAmount() * 100)); // VNPay uses amount ×100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getProviderTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan goi " + payment.getPlanCode() + " " + payment.getBillingCycle());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", cfg.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", LocalDateTime.now(VN_ZONE).format(DATE_FMT));

        // Sorted, URL-encoded hashData == query; sign hashData with HMAC-SHA512.
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String encName = URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII);
            String encVal = URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII);
            if (hashData.length() > 0) { hashData.append('&'); query.append('&'); }
            hashData.append(encName).append('=').append(encVal);
            query.append(encName).append('=').append(encVal);
        }
        String secureHash = HmacUtil.hmacSha512Hex(hashData.toString(), cfg.getHashSecret());
        String payUrl = cfg.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

        return new CheckoutResult(CheckoutResponse.Type.REDIRECT, payUrl, null, null, null, null, null);
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>(params);
        String received = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        TreeMap<String, String> sorted = new TreeMap<>(fields);
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (hashData.length() > 0) hashData.append('&');
            hashData.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        String expected = HmacUtil.hmacSha512Hex(hashData.toString(), props.getVnpay().getHashSecret());

        boolean signatureValid = HmacUtil.constantTimeEquals(expected, received);
        boolean paid = signatureValid
                && "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        return new CallbackResult(params.get("vnp_TxnRef"), signatureValid, paid);
    }
}
