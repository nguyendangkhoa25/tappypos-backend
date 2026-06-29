package com.tappy.pos.util;

/**
 * Vietnamese phone-number helpers shared by the OTP / messaging flows.
 *
 * <p>The send form is the international {@code 84XXXXXXXXX} (no '+') that messaging providers expect;
 * the local form {@code 0XXXXXXXXX} is how numbers are stored at registration (kept exactly as typed).
 */
public final class PhoneUtil {

    private PhoneUtil() {
    }

    /**
     * Converts {@code 0XXXXXXXXX → 84XXXXXXXXX}.
     * Numbers already starting with 84 pass through unchanged.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("84")) return digits;
        if (digits.startsWith("0")) return "84" + digits.substring(1);
        return digits;
    }

    /**
     * Converts to the local Vietnamese format: {@code 84XXXXXXXXX → 0XXXXXXXXX}.
     * Inverse of {@link #normalizePhone}. Used to match users whose phone is stored
     * in the local "0..." form (registration persists the number exactly as typed),
     * since {@link #normalizePhone} produces the "84..." send form.
     */
    public static String localizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("84")) return "0" + digits.substring(2);
        if (digits.startsWith("0")) return digits;
        return digits;
    }

    /** Masks all but the leading and trailing digits for safe logging. */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) return "****";
        int keep = Math.min(4, phone.length() - 3);
        return phone.substring(0, keep) + "***" + phone.substring(phone.length() - 3);
    }
}
