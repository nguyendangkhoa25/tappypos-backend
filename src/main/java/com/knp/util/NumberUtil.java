package com.knp.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class NumberUtil {
    public static String amountToString(BigDecimal amount) {
        if (amount == null) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(amount.longValue());
    }

    public static String weightToString(BigDecimal weight) {
        if (weight == null) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(weight.doubleValue());
    }
}
