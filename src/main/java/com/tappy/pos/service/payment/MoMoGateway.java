package com.tappy.pos.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.config.PaymentProperties;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MoMo AIO (captureWallet) gateway. createCheckout signs the request (HMAC-SHA256), POSTs to MoMo's
 * create endpoint, and returns the {@code payUrl} for redirect/QR. The IPN callback is verified by
 * recomputing the signature. Config-gated.
 *
 * NOTE: field order in the signature strings follows MoMo's v2 docs — verify against the MoMo
 * sandbox before go-live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoMoGateway implements PaymentGateway {

    private final PaymentProperties props;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOMO;
    }

    @Override
    public CheckoutResult createCheckout(SubscriptionPayment payment) {
        PaymentProperties.Momo cfg = props.getMomo();
        if (!cfg.isConfigured()) {
            throw new BadRequestException(messageService.getMessage("error.payment.not_configured"));
        }

        String requestId = payment.getProviderTxnRef();
        String orderId = payment.getProviderTxnRef();
        String amount = String.valueOf(payment.getAmount());
        String orderInfo = "Thanh toan goi " + payment.getPlanCode();
        String extraData = "";
        String requestType = "captureWallet";

        String rawSignature = "accessKey=" + cfg.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + cfg.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + cfg.getPartnerCode()
                + "&redirectUrl=" + cfg.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        String signature = HmacUtil.hmacSha256Hex(rawSignature, cfg.getSecretKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", cfg.getPartnerCode());
        body.put("partnerName", "Tappy POS");
        body.put("storeId", "TappyPOS");
        body.put("requestId", requestId);
        body.put("amount", payment.getAmount());
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", cfg.getRedirectUrl());
        body.put("ipnUrl", cfg.getIpnUrl());
        body.put("lang", "vi");
        body.put("requestType", requestType);
        body.put("autoCapture", true);
        body.put("extraData", extraData);
        body.put("signature", signature);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getEndpoint()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            int resultCode = json.path("resultCode").asInt(-1);
            String payUrl = json.path("payUrl").asText(null);
            if (resultCode != 0 || payUrl == null) {
                log.warn("MoMo create failed: resultCode={} message={}", resultCode, json.path("message").asText());
                throw new BadRequestException(messageService.getMessage("error.payment.create_failed"));
            }
            return new CheckoutResult(CheckoutResponse.Type.REDIRECT, payUrl, null, null, null, null, null);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("MoMo create error", e);
            throw new BadRequestException(messageService.getMessage("error.payment.create_failed"));
        }
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> p) {
        PaymentProperties.Momo cfg = props.getMomo();
        String rawSignature = "accessKey=" + cfg.getAccessKey()
                + "&amount=" + p.get("amount")
                + "&extraData=" + p.getOrDefault("extraData", "")
                + "&message=" + p.get("message")
                + "&orderId=" + p.get("orderId")
                + "&orderInfo=" + p.get("orderInfo")
                + "&orderType=" + p.get("orderType")
                + "&partnerCode=" + p.get("partnerCode")
                + "&payType=" + p.get("payType")
                + "&requestId=" + p.get("requestId")
                + "&responseTime=" + p.get("responseTime")
                + "&resultCode=" + p.get("resultCode")
                + "&transId=" + p.get("transId");
        String expected = HmacUtil.hmacSha256Hex(rawSignature, cfg.getSecretKey());

        boolean signatureValid = HmacUtil.constantTimeEquals(expected, p.get("signature"));
        boolean paid = signatureValid && "0".equals(p.get("resultCode"));
        return new CallbackResult(p.get("orderId"), signatureValid, paid);
    }
}
