package com.tappy.pos.config;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthContext - Thread-safe holder for authenticated user information
 * Similar to TenantContext but for authentication
 */
@Component
@Slf4j
public class AuthContext {

    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();

    /**
     * Set the current authenticated username
     */
    public void setCurrentUsername(String username) {
        log.debug("Setting auth context for user: {}", username);
        currentUsername.set(username);
    }

    /**
     * Get the current authenticated username
     */
    public String getCurrentUsername() {
        return currentUsername.get();
    }

    /**
     * Clear the auth context
     */
    public void clear() {
        log.debug("Clearing auth context");
        currentUsername.remove();
    }
}

