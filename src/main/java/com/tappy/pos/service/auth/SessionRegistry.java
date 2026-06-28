package com.tappy.pos.service.auth;

import com.tappy.pos.model.entity.auth.ActiveSession;
import com.tappy.pos.repository.auth.ActiveSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session registry scoped per tenant.
 *
 * Structure: tenantKey → (username → SessionInfo)
 *   tenantKey = tenantId from X-Tenant-ID header, or "master" for super-admin users.
 *
 * The in-memory map is the fast path for per-request validation; the active_sessions table is the
 * durable source of truth. Every login/logout writes the table, and {@link #getSession} falls back
 * to it on an in-memory miss, re-hydrating the map. This makes single-device enforcement survive a
 * backend restart: after a restart the map is empty, but the first request for each user reloads
 * that user's one session from the table, so a token from a since-evicted device is still rejected.
 *
 * Backward-compat: tokens issued before this feature was deployed have no sessionId claim.
 * isValid() returns true for those to avoid kicking everyone out on deploy.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionRegistry {

    public static final String MASTER_KEY = "master";

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SessionInfo>> sessions =
            new ConcurrentHashMap<>();

    private final ActiveSessionRepository activeSessionRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    public Optional<SessionInfo> getSession(String tenantKey, String username) {
        ConcurrentHashMap<String, SessionInfo> tenantMap = sessions.get(tenantKey);
        SessionInfo cached = tenantMap == null ? null : tenantMap.get(username);
        if (cached != null) return Optional.of(cached);
        // In-memory miss — fall back to the persisted session so enforcement survives a restart.
        return loadFromDb(tenantKey, username);
    }

    /**
     * Returns true when the request should be allowed through.
     * - No sessionId claim (old token) → allow (backward compat)
     * - No session in memory or DB → allow (genuinely no active session: pre-feature token, or post-logout)
     * - Active session matches → allow
     * - Active session does not match → REJECT (different device took over)
     */
    public boolean isValid(String tenantKey, String username, String sessionId) {
        if (sessionId == null) return true;
        Optional<SessionInfo> existing = getSession(tenantKey, username);
        if (existing.isEmpty()) return true;
        return existing.get().sessionId().equals(sessionId);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public void register(String tenantKey, String username, SessionInfo info) {
        sessions.computeIfAbsent(tenantKey, k -> new ConcurrentHashMap<>())
                .put(username, info);
        log.info("Session registered: tenant={} user={} ip={}", tenantKey, username, info.ipAddress());
        persistToDb(tenantKey, username, info);
    }

    public void remove(String tenantKey, String username) {
        ConcurrentHashMap<String, SessionInfo> tenantMap = sessions.get(tenantKey);
        if (tenantMap != null) {
            tenantMap.remove(username);
            log.info("Session removed: tenant={} user={}", tenantKey, username);
        }
        try {
            String tenantId = MASTER_KEY.equals(tenantKey) ? null : tenantKey;
            activeSessionRepository.deleteByUsernameAndTenantId(username, tenantId);
        } catch (Exception e) {
            log.warn("Could not remove active_sessions row for user {}: {}", username, e.getMessage());
        }
    }

    // ── DB persistence ────────────────────────────────────────────────────────

    /**
     * Loads the persisted session for a user and re-hydrates the in-memory map so subsequent
     * requests hit the fast path. Returns empty only when no row exists (genuinely no active session).
     *
     * <p>A real DB read failure is allowed to propagate rather than being swallowed into an empty
     * Optional: {@link #isValid} treats empty as "allow", so swallowing a read error would fail
     * <em>open</em> — letting a token from a since-evicted device through during the cold-cache window
     * right after a restart. Failing closed costs nothing extra in practice, because the request's own
     * work needs the same DB and would fail anyway. (Persistence writes in {@link #persistToDb} stay
     * best-effort/swallowed — they are not on the validation path.)
     */
    private Optional<SessionInfo> loadFromDb(String tenantKey, String username) {
        String tenantId = MASTER_KEY.equals(tenantKey) ? null : tenantKey;
        return activeSessionRepository.findByUsernameAndTenantId(username, tenantId)
                .map(row -> {
                    SessionInfo info = new SessionInfo(
                            row.getSessionId(), row.getIpAddress(), row.getUserAgent(), row.getLoginAt());
                    // register() uses an unconditional put and so always wins over this hydration.
                    sessions.computeIfAbsent(tenantKey, k -> new ConcurrentHashMap<>())
                            .putIfAbsent(username, info);
                    return info;
                });
    }

    private void persistToDb(String tenantKey, String username, SessionInfo info) {
        try {
            String tenantId = MASTER_KEY.equals(tenantKey) ? null : tenantKey;
            ActiveSession row = activeSessionRepository
                    .findByUsernameAndTenantId(username, tenantId)
                    .orElse(ActiveSession.builder()
                            .username(username)
                            .tenantId(tenantId)
                            .loginAt(info.loginAt())
                            .build());
            row.setSessionId(info.sessionId());
            row.setIpAddress(info.ipAddress());
            row.setUserAgent(info.userAgent());
            row.setLoginAt(info.loginAt());
            row.setLastActive(LocalDateTime.now());
            activeSessionRepository.save(row);
        } catch (Exception e) {
            log.warn("Could not persist active_sessions for user {} (table may not exist yet): {}", username, e.getMessage());
        }
    }
}
