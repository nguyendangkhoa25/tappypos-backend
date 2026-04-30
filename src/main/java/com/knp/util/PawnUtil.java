package com.knp.util;

import com.knp.model.enums.PawnInterestCalculation;
import com.knp.model.enums.PawnStatus;

import static com.knp.model.enums.PawnInterestCalculation.INTEREST_BY_DAY_FULL_MONTH;

public class PawnUtil {

    public static String getPawnInterestCalculation(Integer interestDaysPerMonth) {
        return PawnInterestCalculation.stream().filter(item -> item.code == interestDaysPerMonth).findFirst().orElse(INTEREST_BY_DAY_FULL_MONTH).label;
    }

    public static String getPawnStatusLabel(String pawnStatus) {
        return PawnStatus.stream().filter(item -> item.code.equalsIgnoreCase(pawnStatus)).findFirst().orElseThrow().label;
    }
}
