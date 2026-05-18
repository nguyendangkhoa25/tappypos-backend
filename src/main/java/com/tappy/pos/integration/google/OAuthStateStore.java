package com.tappy.pos.integration.google;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived in-memory store for OAuth2 state parameters.
 * Prevents CSRF by ensuring the callback state was issued by this server
 * within the last 10 minutes.
 */
@Component
public class OAuthStateStore {

    private static final long TTL_MS = 10 * 60 * 1000L;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String generate(String tenantId) {
        purgeExpired();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        store.put(nonce, new Entry(tenantId, Instant.now().toEpochMilli()));
        return nonce;
    }

    /** Returns the tenantId for the nonce and removes it (one-time use). */
    public String consume(String nonce) {
        Entry entry = store.remove(nonce);
        if (entry == null) return null;
        if (Instant.now().toEpochMilli() - entry.createdAt > TTL_MS) return null;
        return entry.tenantId;
    }

    private void purgeExpired() {
        long now = Instant.now().toEpochMilli();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    private record Entry(String tenantId, long createdAt) {}
}
