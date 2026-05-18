package com.tappy.pos.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    public static String DATE_TIME_FORMAT_FULL = "HH:mm yyyy-MM-dd";

    public static LocalDateTime fromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        ZoneId gmtZoneId = ZoneId.of("GMT");
        ZonedDateTime zdtAtSystemZone = localDateTime.atZone(gmtZoneId);
        return zdtAtSystemZone.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static String parseLocalDateTime(LocalDateTime localDateTime, String format) {
        if (localDateTime == null) {
            return StringUtils.EMPTY;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    public static String localDateTimeToString(LocalDateTime localDateTime) {
        return parseLocalDateTime(localDateTime, DATE_TIME_FORMAT_FULL);
    }
}
