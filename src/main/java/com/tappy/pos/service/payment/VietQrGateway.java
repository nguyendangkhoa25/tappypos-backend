package com.tappy.pos.service.payment;

import com.tappy.pos.config.PaymentProperties;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * VietQR (napas247) bank-transfer gateway. Builds a scannable EMVCo QR payload to Tappy's business
 * bank account with the txn reference as the transfer memo. Activation is manual / via a bank-feed
 * reconciliation (Casso/SePay) → there is no synchronous callback here; the master admin confirms
 * the transfer (see SubscriptionPaymentService.confirmManual). Fully offline — no merchant API.
 */
@Component
@RequiredArgsConstructor
public class VietQrGateway implements PaymentGateway {

    private final PaymentProperties props;
    private final MessageService messageService;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.VIETQR;
    }

    @Override
    public CheckoutResult createCheckout(SubscriptionPayment payment) {
        PaymentProperties.Vietqr cfg = props.getVietqr();
        if (!cfg.isConfigured()) {
            throw new BadRequestException(messageService.getMessage("error.payment.not_configured"));
        }
        String qr = buildVietQrPayload(cfg.getBankBin(), cfg.getAccountNo(), payment.getAmount(), payment.getProviderTxnRef());
        return new CheckoutResult(
                CheckoutResponse.Type.QR,
                null,
                qr,
                cfg.getAccountNo(),
                cfg.getBankName(),
                cfg.getAccountName(),
                payment.getProviderTxnRef());
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> params) {
        // VietQR has no synchronous provider callback in this build — payments are confirmed manually
        // by the master admin or by a bank-feed reconciliation job. Use confirmManual() instead.
        throw new UnsupportedOperationException("VietQR is confirmed manually, not via callback");
    }

    /** Build an EMVCo (napas247) VietQR payload string for a dynamic (amount-bearing) transfer. */
    static String buildVietQrPayload(String bankBin, String accountNo, long amount, String addInfo) {
        String merchantAccount =
                tlv("00", "A000000727")
                + tlv("01", tlv("00", bankBin) + tlv("01", accountNo))
                + tlv("02", "QRIBFTTA"); // transfer to account
        String payload =
                tlv("00", "01")                 // payload format indicator
                + tlv("01", "12")               // dynamic QR (amount present)
                + tlv("38", merchantAccount)
                + tlv("53", "704")              // currency VND
                + tlv("54", String.valueOf(amount))
                + tlv("58", "VN")
                + tlv("62", tlv("08", addInfo)) // additional data → purpose / memo
                + "6304";                       // CRC tag+len, value computed over the above
        return payload + crc16(payload);
    }

    private static String tlv(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    /** CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF), 4 upper-hex chars — per the EMVCo QR spec. */
    private static String crc16(String s) {
        int crc = 0xFFFF;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) : (crc << 1);
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
