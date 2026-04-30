package com.knp.model.enums;

import java.util.stream.Stream;

public enum PawnInterestCalculation {
    INTEREST_BY_DAY_FULL_MONTH("Tính lãi theo ngày", 30),
    INTEREST_BY_DAY_25DAY_PER_MONTH("Tính lãi theo ngày, 25 ngày/Tháng(30 ngày)", 25);

    public final String label;
    public final int code;

    PawnInterestCalculation(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public static Stream<PawnInterestCalculation> stream() {
        return Stream.of(PawnInterestCalculation.values());
    }
}
