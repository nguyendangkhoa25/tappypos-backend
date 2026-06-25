package com.tappy.pos.service.auth;

import com.tappy.pos.model.entity.integration.ZaloZnsCredential;
import com.tappy.pos.repository.integration.ZaloZnsCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformZaloTokenService Unit Tests")
class PlatformZaloTokenServiceTest {

    @Mock private ZaloZnsCredentialRepository repository;
    @Mock private RestTemplate restTemplate;

    private PlatformZaloTokenService service;

    @BeforeEach
    void setUp() {
        service = new PlatformZaloTokenService(repository, restTemplate);
    }

    private void setBootstrapEnv(String appId, String appSecret, String refreshToken) {
        ReflectionTestUtils.setField(service, "bootstrapAppId", appId);
        ReflectionTestUtils.setField(service, "bootstrapAppSecret", appSecret);
        ReflectionTestUtils.setField(service, "bootstrapRefreshToken", refreshToken);
    }

    private ZaloZnsCredential cred(String access, LocalDateTime expiry, String refresh) {
        return ZaloZnsCredential.builder()
                .appId("app1").appSecret("secret")
                .accessToken(access).tokenExpiry(expiry).refreshToken(refresh)
                .build();
    }

    @Test
    @DisplayName("returns the stored token unchanged when it is valid and not near expiry")
    void returnsStoredTokenWhenFresh() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("good-tok", LocalDateTime.now().plusHours(2), "r1")));

        String token = service.getAccessToken();

        assertThat(token).isEqualTo("good-tok");
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("refreshes near-expiry token, persists rotated refresh token + new expiry")
    void refreshesNearExpiry() {
        ZaloZnsCredential stored = cred("old-tok", LocalDateTime.now().plusSeconds(30), "r1");
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(stored));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "access_token", "new-tok",
                        "refresh_token", "r2", "expires_in", 90000));

        String token = service.getAccessToken();

        assertThat(token).isEqualTo("new-tok");
        assertThat(stored.getAccessToken()).isEqualTo("new-tok");
        assertThat(stored.getRefreshToken()).isEqualTo("r2");
        assertThat(stored.getTokenExpiry()).isAfter(LocalDateTime.now().plusHours(1));
        verify(repository).save(stored);
    }

    @Test
    @DisplayName("bootstraps from env when no row exists, then refreshes")
    void bootstrapsThenRefreshes() {
        setBootstrapEnv("app1", "secret", "boot-refresh");
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ZaloZnsCredential.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "access_token", "boot-tok",
                        "refresh_token", "r2", "expires_in", 90000));

        String token = service.getAccessToken();

        assertThat(token).isEqualTo("boot-tok");
        verify(repository).saveAndFlush(any(ZaloZnsCredential.class));
        verify(repository).save(any(ZaloZnsCredential.class));
    }

    @Test
    @DisplayName("returns null when not configured (no row, no bootstrap env)")
    void nullWhenNotConfigured() {
        setBootstrapEnv("", "", "");
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(service.getAccessToken()).isNull();
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("returns null when Zalo rejects the refresh (error != 0)")
    void nullOnZaloError() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("old", LocalDateTime.now().plusSeconds(10), "r1")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", -216, "message", "refresh token expired"));

        assertThat(service.getAccessToken()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("returns null when the refresh call throws")
    void nullOnException() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("old", LocalDateTime.now().plusSeconds(10), "r1")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));

        assertThat(service.getAccessToken()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("returns null when stored credential has no refresh token to refresh with")
    void nullWhenNoRefreshToken() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("old", LocalDateTime.now().minusSeconds(10), null)));

        assertThat(service.getAccessToken()).isNull();
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("returns null when Zalo returns an empty (null) response body")
    void nullWhenResponseBodyNull() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("old", LocalDateTime.now().plusSeconds(10), "r1")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        assertThat(service.getAccessToken()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("returns null when the refresh response carries no access_token")
    void nullWhenAccessTokenMissing() {
        when(repository.findFirstByDeletedFalseOrderByIdAsc())
                .thenReturn(Optional.of(cred("old", LocalDateTime.now().plusSeconds(10), "r1")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0)); // success but no token

        assertThat(service.getAccessToken()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("parses a String expires_in and applies it to token_expiry")
    void parsesStringExpiresIn() {
        ZaloZnsCredential stored = cred("old", LocalDateTime.now().plusSeconds(10), "r1");
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(stored));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "access_token", "new", "refresh_token", "r2", "expires_in", "3600"));

        assertThat(service.getAccessToken()).isEqualTo("new");
        assertThat(stored.getTokenExpiry())
                .isAfter(LocalDateTime.now().plusSeconds(3500))
                .isBefore(LocalDateTime.now().plusSeconds(3700));
    }

    @Test
    @DisplayName("falls back to default TTL and keeps old refresh token when expires_in is unusable / absent")
    void defaultsTtlAndKeepsRefreshToken() {
        ZaloZnsCredential stored = cred("old", LocalDateTime.now().plusSeconds(10), "r1");
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(stored));
        // expires_in is non-numeric → DEFAULT_EXPIRES_IN (90000s); no refresh_token → keep "r1"
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "access_token", "new", "expires_in", "not-a-number"));

        assertThat(service.getAccessToken()).isEqualTo("new");
        assertThat(stored.getRefreshToken()).isEqualTo("r1");
        assertThat(stored.getTokenExpiry()).isAfter(LocalDateTime.now().plusSeconds(89_000));
    }

    @Test
    @DisplayName("does not refresh when bootstrap env is only partially configured")
    void noBootstrapWhenPartialEnv() {
        setBootstrapEnv("app1", "", "boot-refresh"); // secret blank
        when(repository.findFirstByDeletedFalseOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(service.getAccessToken()).isNull();
        verify(repository, never()).saveAndFlush(any());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }
}
