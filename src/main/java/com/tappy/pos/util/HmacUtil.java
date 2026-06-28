package com.tappy.pos.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMAC helpers for payment-gateway signing (MoMo: HMAC-SHA256, VNPay: HMAC-SHA512).
 */
public final class HmacUtil {

    private HmacUtil() {}

    public static String hmacSha256Hex(String data, String key) {
        return hmacHex("HmacSHA256", data, key);
    }

    public static String hmacSha512Hex(String data, String key) {
        return hmacHex("HmacSHA512", data, key);
    }

    private static String hmacHex(String algorithm, String data, String key) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute " + algorithm, e);
        }
    }

    /** Constant-time comparison for signature verification. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}
