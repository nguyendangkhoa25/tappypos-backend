package com.tappy.pos.util;

import com.tappy.pos.model.enums.PawnInterestCalculation;
import com.tappy.pos.model.enums.PawnStatus;

public class PawnUtil {

    public static String getPawnInterestCalculation(String interestCalcMode) {
        return PawnInterestCalculation.fromCode(interestCalcMode).label;
    }

    public static String getPawnStatusLabel(String pawnStatus) {
        return PawnStatus.stream().filter(item -> item.code.equalsIgnoreCase(pawnStatus)).findFirst().orElseThrow().label;
    }
}
