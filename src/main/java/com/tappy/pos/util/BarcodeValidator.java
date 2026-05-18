package com.tappy.pos.util;

/**
 * Validates EAN-8, EAN-13, and UPC-A barcodes using the GS1 checksum algorithm.
 */
public final class BarcodeValidator {

    private BarcodeValidator() {}

    /**
     * Returns true if the barcode is a valid EAN-8, EAN-13, or UPC-A barcode.
     * Validates both format (all digits, correct length) and GS1 checksum.
     */
    public static boolean isValid(String barcode) {
        if (barcode == null) return false;
        String code = barcode.trim();
        if (!code.matches("\\d+")) return false;
        int len = code.length();
        if (len != 8 && len != 12 && len != 13) return false;
        return hasValidChecksum(code);
    }

    /**
     * GS1 checksum: digit just left of the check digit always has weight 3,
     * then alternates 1, 3, 1, 3 going further left. Works for EAN-8/13 and UPC-A.
     */
    private static boolean hasValidChecksum(String code) {
        int n = code.length();
        int sum = 0;
        for (int i = 0; i < n - 1; i++) {
            int digit = code.charAt(i) - '0';
            boolean isOddDistanceFromCheck = ((n - 1 - i) % 2 == 1);
            sum += digit * (isOddDistanceFromCheck ? 3 : 1);
        }
        int expected = (10 - (sum % 10)) % 10;
        return expected == (code.charAt(n - 1) - '0');
    }
}
