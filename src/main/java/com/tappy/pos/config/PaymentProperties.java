package com.tappy.pos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Payment provider configuration (bound from `payment.*` in application.properties).
 * Secrets come from environment variables in production. A provider whose required
 * credentials are blank is treated as "not configured" and its checkout is rejected with
 * a friendly error. See docs/SUBSCRIPTION_PAYMENTS.md.
 */
@Component
@ConfigurationProperties(prefix = "payment")
@Getter
@Setter
public class PaymentProperties {

    private final Momo momo = new Momo();
    private final Vnpay vnpay = new Vnpay();
    private final Vietqr vietqr = new Vietqr();

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @Getter
    @Setter
    public static class Momo {
        private String partnerCode;
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private String redirectUrl;
        private String ipnUrl;

        public boolean isConfigured() {
            return notBlank(partnerCode) && notBlank(accessKey) && notBlank(secretKey) && notBlank(endpoint);
        }
    }

    @Getter
    @Setter
    public static class Vnpay {
        private String tmnCode;
        private String hashSecret;
        private String payUrl;
        private String returnUrl;

        public boolean isConfigured() {
            return notBlank(tmnCode) && notBlank(hashSecret) && notBlank(payUrl);
        }
    }

    @Getter
    @Setter
    public static class Vietqr {
        private String bankBin;
        private String bankName;
        private String accountNo;
        private String accountName;

        public boolean isConfigured() {
            return notBlank(bankBin) && notBlank(accountNo) && notBlank(accountName);
        }
    }
}
