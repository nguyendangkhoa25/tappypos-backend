package com.tappy.pos.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON codec for deferred-render message arguments (the {@code *_args} columns used by the
 * render-at-read i18n pattern — activity log, notifications, order notes/reasons).
 *
 * <p>Pass already-formatted display strings for numbers/money so {@link java.text.MessageFormat}
 * does not re-format them per locale.
 */
public final class MessageArgs {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object[] NONE = new Object[0];

    private MessageArgs() {}

    /** Serialize args to a JSON array string, or {@code null} when there are none. */
    public static String toJson(Object... args) {
        if (args == null || args.length == 0) return null;
        try {
            return MAPPER.writeValueAsString(args);
        } catch (Exception e) {
            return null;
        }
    }

    /** Parse a {@code *_args} JSON array back into message arguments; empty array on null/blank/bad input. */
    public static Object[] fromJson(String json) {
        if (json == null || json.isBlank()) return NONE;
        try {
            return MAPPER.readValue(json, Object[].class);
        } catch (Exception e) {
            return NONE;
        }
    }
}
