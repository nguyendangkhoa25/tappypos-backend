package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.tenant.ConnectZaloOaRequest;
import com.tappy.pos.model.dto.tenant.ZaloOaStatusDTO;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ZaloOaService Unit Tests")
class ZaloOaServiceTest {

    @Mock private ShopConfigService shopConfigService;
    @Mock private RestTemplate restTemplate;
    @Mock private MessageService messageService;

    @InjectMocks
    private ZaloOaService service;

    @BeforeEach
    void setUp() {
        when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    private ConnectZaloOaRequest request() {
        ConnectZaloOaRequest r = new ConnectZaloOaRequest();
        r.setAppId(" app123 ");
        r.setAppSecret(" secret ");
        r.setOaName(" My OA ");
        r.setOaId(" oa1 ");
        return r;
    }

    @SuppressWarnings("unchecked")
    private void stubToken(Map<String, Object> resp) {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(resp);
    }

    // ── connect ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect persists credentials and returns connected status")
    void connect_ok() {
        stubToken(Map.of("access_token", "AT", "refresh_token", "RT", "expires_in", 7776000));

        ZaloOaStatusDTO dto = service.connect(request());

        assertThat(dto.isConnected()).isTrue();
        assertThat(dto.getAppId()).isEqualTo("app123");
        verify(shopConfigService).set(ShopConfigKey.ZALO_APP_ID, "app123");
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_ACCESS_TOKEN, "AT");
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_NAME, "My OA");
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_ID, "oa1");
    }

    @Test
    @DisplayName("connect surfaces Zalo error responses as IllegalArgumentException")
    void connect_zaloError() {
        stubToken(Map.of("error", -201, "message", "invalid secret"));
        assertThatThrownBy(() -> service.connect(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid secret");
    }

    @Test
    @DisplayName("connect converts a network error into a clean IllegalArgumentException")
    void connect_networkError() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("timeout"));
        assertThatThrownBy(() -> service.connect(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.zaloOa.authFailed");
    }

    @Test
    @DisplayName("connect throws when Zalo returns an empty body")
    void connect_emptyResponse() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(null);
        assertThatThrownBy(() -> service.connect(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── disconnect ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect clears all stored Zalo keys")
    void disconnect() {
        service.disconnect();
        verify(shopConfigService).set(ShopConfigKey.ZALO_APP_ID, (String) null);
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_ACCESS_TOKEN, (String) null);
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_NAME, (String) null);
    }

    // ── getStatus ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStatus reports not connected when no appId is stored")
    void getStatus_notConfigured() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn(null);
        assertThat(service.getStatus().isConnected()).isFalse();
    }

    @Test
    @DisplayName("getStatus reports connected when token valid and unexpired")
    void getStatus_connected() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY))
                .thenReturn(LocalDateTime.now().plusDays(10).toString());
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn("AT");

        ZaloOaStatusDTO dto = service.getStatus();

        assertThat(dto.isConnected()).isTrue();
        assertThat(dto.getAppId()).isEqualTo("app123");
    }

    @Test
    @DisplayName("getStatus reports disconnected when the stored token is expired")
    void getStatus_expired() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY))
                .thenReturn(LocalDateTime.now().minusDays(1).toString());
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn("AT");

        assertThat(service.getStatus().isConnected()).isFalse();
    }

    // ── getAccessToken ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccessToken returns null when not configured")
    void getAccessToken_notConfigured() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn(null);
        assertThat(service.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("getAccessToken returns the stored token when comfortably unexpired")
    void getAccessToken_stillValid() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY))
                .thenReturn(LocalDateTime.now().plusDays(10).toString());
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn("AT");

        assertThat(service.getAccessToken()).isEqualTo("AT");
    }

    @Test
    @DisplayName("getAccessToken refreshes via refresh_token when near expiry")
    void getAccessToken_refreshWithRefreshToken() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY))
                .thenReturn(LocalDateTime.now().plusSeconds(60).toString()); // within buffer
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn("OLD");
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_SECRET)).thenReturn("secret");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_REFRESH_TOKEN)).thenReturn("RT");
        stubToken(Map.of("access_token", "NEW", "refresh_token", "RT2", "expires_in", 7776000));

        assertThat(service.getAccessToken()).isEqualTo("NEW");
        verify(shopConfigService).set(ShopConfigKey.ZALO_OA_ACCESS_TOKEN, "NEW");
    }

    @Test
    @DisplayName("getAccessToken re-grants via app flow when no refresh token present")
    void getAccessToken_refreshViaApp() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_SECRET)).thenReturn("secret");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_REFRESH_TOKEN)).thenReturn(null);
        stubToken(Map.of("access_token", "FRESH", "expires_in", 7776000));

        assertThat(service.getAccessToken()).isEqualTo("FRESH");
    }

    @Test
    @DisplayName("getAccessToken returns null when appSecret missing")
    void getAccessToken_secretMissing() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_SECRET)).thenReturn(null);

        assertThat(service.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("getAccessToken returns null when refresh call fails")
    void getAccessToken_refreshFails() {
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_ID)).thenReturn("app123");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.ZALO_APP_SECRET)).thenReturn("secret");
        when(shopConfigService.getString(ShopConfigKey.ZALO_OA_REFRESH_TOKEN)).thenReturn("RT");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        assertThat(service.getAccessToken()).isNull();
    }
}
