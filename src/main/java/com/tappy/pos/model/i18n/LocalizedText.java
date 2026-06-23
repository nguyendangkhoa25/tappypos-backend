package com.tappy.pos.model.i18n;

/**
 * A deferred-render piece of user-facing text: an i18n message key plus its arguments.
 *
 * <p>Used so that system-generated text (notifications, …) is stored as key + args and rendered in
 * the <b>reader's</b> locale at read time, instead of being frozen into one language at write time.
 * Pass already-formatted display strings as {@code args} for numbers/money so
 * {@link java.text.MessageFormat} does not re-format them per locale.
 */
public record LocalizedText(String key, Object[] args) {

    private static final Object[] NO_ARGS = new Object[0];

    public static LocalizedText of(String key, Object... args) {
        return new LocalizedText(key, args == null ? NO_ARGS : args);
    }
}
