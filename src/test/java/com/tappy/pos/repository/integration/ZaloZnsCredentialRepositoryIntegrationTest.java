package com.tappy.pos.repository.integration;

import com.tappy.pos.model.entity.integration.ZaloZnsCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2-backed slice test for {@link ZaloZnsCredentialRepository#findFirstByDeletedFalseOrderByIdAsc()}.
 * Flyway is disabled; Hibernate derives the schema from the entity. The
 * EncryptedStringConverter degrades to plaintext when no EncryptionService bean
 * is present, so values round-trip unchanged in this slice.
 */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@DisplayName("ZaloZnsCredentialRepository Integration Tests")
class ZaloZnsCredentialRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ZaloZnsCredentialRepository repository;

    @BeforeEach
    void clean() {
        // This slice does not roll back between methods here, so start each test from a clean table.
        repository.deleteAll();
        repository.flush();
    }

    private ZaloZnsCredential build(String appId, boolean deleted) {
        return ZaloZnsCredential.builder()
                .appId(appId)
                .appSecret("secret-" + appId)
                .accessToken("at-" + appId)
                .refreshToken("rt-" + appId)
                .tokenExpiry(LocalDateTime.now().plusHours(1))
                .oaName("Tappy Việt Nam")
                .deleted(deleted)
                .build();
    }

    @Test
    @DisplayName("findFirstByDeletedFalseOrderByIdAsc returns the active credential row")
    void returnsActiveRow() {
        entityManager.persistAndFlush(build("app-active", false));

        Optional<ZaloZnsCredential> found = repository.findFirstByDeletedFalseOrderByIdAsc();

        assertThat(found).isPresent();
        assertThat(found.get().getAppId()).isEqualTo("app-active");
        assertThat(found.get().getAccessToken()).isEqualTo("at-app-active");
        assertThat(found.get().getRefreshToken()).isEqualTo("rt-app-active");
    }

    @Test
    @DisplayName("findFirstByDeletedFalseOrderByIdAsc ignores soft-deleted rows")
    void ignoresDeletedRows() {
        entityManager.persistAndFlush(build("app-gone", true));

        assertThat(repository.findFirstByDeletedFalseOrderByIdAsc()).isEmpty();
    }

    @Test
    @DisplayName("findFirstByDeletedFalseOrderByIdAsc returns the lowest-id row when several are active")
    void returnsFirstByIdAsc() {
        ZaloZnsCredential first = entityManager.persistAndFlush(build("app-first", false));
        entityManager.persistAndFlush(build("app-second", false));

        Optional<ZaloZnsCredential> found = repository.findFirstByDeletedFalseOrderByIdAsc();

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(first.getId());
        assertThat(found.get().getAppId()).isEqualTo("app-first");
    }

    @Test
    @DisplayName("findFirstByDeletedFalseOrderByIdAsc returns empty when no rows exist")
    void emptyWhenNoRows() {
        assertThat(repository.findFirstByDeletedFalseOrderByIdAsc()).isEmpty();
    }
}
