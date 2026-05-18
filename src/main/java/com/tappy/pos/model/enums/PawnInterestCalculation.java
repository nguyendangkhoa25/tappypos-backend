package com.tappy.pos.model.enums;

import java.util.stream.Stream;

public enum PawnInterestCalculation {

    // Count every actual held day; monthly rate normalised to 30 days.
    DAILY_30("Tính theo ngày (30 ngày/tháng)"),

    // Same day-counting but cap chargeable days at 25 per 30-day period.
    DAILY_25("Tính theo ngày (tối đa 25 ngày/tháng)"),

    // Round partial months up to the next full month.
    // 1–30 days → 1 month, 31–60 days → 2 months, etc.
    MONTHLY("Tính theo tháng tròn"),

    // Round partial 15-day periods up to the next half-month.
    // 1–15 days → 0.5 month, 16–30 days → 1 month, etc.
    BIWEEKLY("Tính theo nửa tháng (15 ngày)");

    public final String label;

    PawnInterestCalculation(String label) {
        this.label = label;
    }

    public static PawnInterestCalculation fromCode(String code) {
        if (code == null) return DAILY_30;
        try {
            return PawnInterestCalculation.valueOf(code);
        } catch (IllegalArgumentException e) {
            return DAILY_30;
        }
    }

    public static Stream<PawnInterestCalculation> stream() {
        return Stream.of(PawnInterestCalculation.values());
    }
}
