package com.knp.model.enums;

import java.util.stream.Stream;

public enum PawnStatus {
    PAWNED("Đang cầm", "PAWNED"),
    REDEEMED("Đã trả", "REDEEMED"),
    FORFEITED("Đã thanh lý", "FORFEITED"),
    CANCELLED("Đã hủy", "CANCELLED");

    public final String label;
    public final String code;

    PawnStatus(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public static Stream<PawnStatus> stream() {
        return Stream.of(PawnStatus.values());
    }
}
