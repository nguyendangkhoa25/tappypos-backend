package com.knp.service.auth;

import com.knp.model.entity.auth.ActiveSession;
import com.knp.repository.auth.ActiveSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionRegistry Unit Tests")
class SessionRegistryTest {

    @Mock private ActiveSessionRepository activeSessionRepository;

    @InjectMocks
    private SessionRegistry registry;

    private SessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        sessionInfo = new SessionInfo("session-uuid-1", "192.168.1.1", "Mozilla/5.0", LocalDateTime.now());
        lenient().when(activeSessionRepository.findByUsername(any())).thenReturn(Optional.empty());
        lenient().when(activeSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ── register / getSession ─────────────────────────────────────────────────

    @Test
    @DisplayName("register: stores session and makes it retrievable")
    void register_storesSession() {
        registry.register("tenant1", "user1", sessionInfo);

        Optional<SessionInfo> result = registry.getSession("tenant1", "user1");
        assertThat(result).isPresent();
        assertThat(result.get().sessionId()).isEqualTo("session-uuid-1");
    }

    @Test
    @DisplayName("getSession: returns empty for unknown tenant")
    void getSession_unknownTenant() {
        assertThat(registry.getSession("unknown", "user1")).isEmpty();
    }

    @Test
    @DisplayName("getSession: returns empty for unknown user within known tenant")
    void getSession_unknownUser() {
        registry.register("tenant1", "user1", sessionInfo);

        assertThat(registry.getSession("tenant1", "user2")).isEmpty();
    }

    // ── isValid ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValid: returns true when sessionId matches registered session")
    void isValid_matchingSession() {
        registry.register("tenant1", "user1", sessionInfo);

        assertThat(registry.isValid("tenant1", "user1", "session-uuid-1")).isTrue();
    }

    @Test
    @DisplayName("isValid: returns false when sessionId differs (different device)")
    void isValid_differentDevice() {
        registry.register("tenant1", "user1", sessionInfo);

        assertThat(registry.isValid("tenant1", "user1", "other-session-uuid")).isFalse();
    }

    @Test
    @DisplayName("isValid: returns true when sessionId is null (backward compat for old tokens)")
    void isValid_nullSessionId() {
        registry.register("tenant1", "user1", sessionInfo);

        assertThat(registry.isValid("tenant1", "user1", null)).isTrue();
    }

    @Test
    @DisplayName("isValid: returns true when no session registered (allows after server restart)")
    void isValid_noRegisteredSession() {
        assertThat(registry.isValid("tenant1", "user1", "any-session-id")).isTrue();
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove: clears the in-memory session and DB row")
    void remove_clearsSession() {
        registry.register("tenant1", "user1", sessionInfo);
        doNothing().when(activeSessionRepository).deleteByUsername("user1");

        registry.remove("tenant1", "user1");

        assertThat(registry.getSession("tenant1", "user1")).isEmpty();
        verify(activeSessionRepository).deleteByUsername("user1");
    }

    @Test
    @DisplayName("remove: no-op when tenant has no sessions")
    void remove_noSessionForTenant() {
        doNothing().when(activeSessionRepository).deleteByUsername("user1");

        registry.remove("tenant1", "user1"); // should not throw

        assertThat(registry.getSession("tenant1", "user1")).isEmpty();
    }

    // ── isolation between tenants ─────────────────────────────────────────────

    @Test
    @DisplayName("sessions are isolated per tenant key")
    void sessions_isolatedPerTenant() {
        SessionInfo s2 = new SessionInfo("session-2", "10.0.0.1", "Chrome", LocalDateTime.now());

        registry.register("tenant1", "user1", sessionInfo);
        registry.register("tenant2", "user1", s2);

        assertThat(registry.isValid("tenant1", "user1", "session-uuid-1")).isTrue();
        assertThat(registry.isValid("tenant2", "user1", "session-2")).isTrue();
        assertThat(registry.isValid("tenant1", "user1", "session-2")).isFalse();
    }

    // ── DB persistence ────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: persists session to DB")
    void register_persistsToDb() {
        registry.register("tenant1", "user1", sessionInfo);

        verify(activeSessionRepository).save(any(ActiveSession.class));
    }

    @Test
    @DisplayName("register: updates existing DB row if one exists")
    void register_updatesExistingDbRow() {
        ActiveSession existing = ActiveSession.builder()
                .username("user1").sessionId("old-id")
                .loginAt(LocalDateTime.now()).lastActive(LocalDateTime.now()).build();
        when(activeSessionRepository.findByUsername("user1")).thenReturn(Optional.of(existing));

        registry.register("tenant1", "user1", sessionInfo);

        verify(activeSessionRepository).save(existing);
        assertThat(existing.getSessionId()).isEqualTo("session-uuid-1");
    }
}
