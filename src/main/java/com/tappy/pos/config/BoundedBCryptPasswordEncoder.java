package com.tappy.pos.config;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt password encoder that rejects over-length passwords with a friendly, localized 400 instead of
 * Spring Security 7's raw {@code IllegalArgumentException("password cannot be more than 72 bytes")}.
 *
 * <p>BCrypt only ever hashes the first 72 bytes of a password, and Spring Security 7 now throws on
 * anything longer (it used to silently truncate). That check lives in the now-{@code final}
 * {@code AbstractValidatingPasswordEncoder.encode}, so it can't be overridden via inheritance — we wrap
 * a {@link BCryptPasswordEncoder} and pre-validate the length before delegating. Since every password
 * the app stores is encoded through this one bean, guarding {@link #encode} here turns the over-length
 * case into a normal validation error (a localized 400 via {@code GlobalExceptionHandler}) on every
 * path — registration, reset, admin user creation, password change, etc. — without touching each call
 * site. {@link #matches}/{@link #upgradeEncoding} delegate unchanged.
 */
public class BoundedBCryptPasswordEncoder implements PasswordEncoder {

    /** BCrypt only considers the first 72 bytes of the UTF-8 encoded password. */
    static final int MAX_BYTES = 72;

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();
    private final MessageService messageService;

    public BoundedBCryptPasswordEncoder(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword != null
                && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BadRequestException(messageService.getMessage("error.password.too.long"));
        }
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }
}
