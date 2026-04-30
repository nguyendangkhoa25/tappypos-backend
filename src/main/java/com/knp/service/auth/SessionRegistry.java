package com.knp.service.auth;

import com.knp.model.entity.auth.ActiveSession;
import com.knp.repository.auth.ActiveSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session registry scoped per tenant.
 *
 * Structure: tenantKey → (username → SessionInfo)
 *   tenantKey = tenantId from X-Tenant-ID header, or "master" for super-admin users.
 *
 * The map is the authoritative source for per-request validation (no DB hit).
 * The active_sessions table is written on every login for audit and future restart-recovery.
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
        if (tenantMap == null) return Optional.empty();
        return Optional.ofNullable(tenantMap.get(username));
    }

    /**
     * Returns true when the request should be allowed through.
     * - No sessionId claim (old token) → allow (backward compat)
     * - No registry entry (server restart) → allow (no DB reload on startup)
     * - Registry entry matches → allow
     * - Registry entry does not match → REJECT (different device)
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
        persistToDb(username, info);
    }

    public void remove(String tenantKey, String username) {
        ConcurrentHashMap<String, SessionInfo> tenantMap = sessions.get(tenantKey);
        if (tenantMap != null) {
            tenantMap.remove(username);
            log.info("Session removed: tenant={} user={}", tenantKey, username);
        }
        try {
            activeSessionRepository.deleteByUsername(username);
        } catch (Exception e) {
            log.warn("Could not remove active_sessions row for user {}: {}", username, e.getMessage());
        }
    }

    // ── DB persistence (best-effort, does not affect correctness) ─────────────

    private void persistToDb(String username, SessionInfo info) {
        try {
            ActiveSession row = activeSessionRepository.findByUsername(username)
                    .orElse(ActiveSession.builder()
                            .username(username)
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
