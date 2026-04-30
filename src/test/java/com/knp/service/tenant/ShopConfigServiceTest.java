package com.knp.service.tenant;

import com.knp.config.EncryptionService;
import com.knp.model.entity.tenant.ShopConfig;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.tenant.ShopConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopConfigService Unit Tests")
class ShopConfigServiceTest {

    @Mock private ShopConfigRepository shopConfigRepository;
    @Mock private EncryptionService encryptionService;

    @InjectMocks
    private ShopConfigService shopConfigService;

    private ShopConfig plainConfig;

    @BeforeEach
    void setUp() {
        plainConfig = ShopConfig.builder()
                .configKey("pos_mode")
                .configValue("STANDARD")
                .configGroup("POS")
                .encrypted(false)
                .build();
    }

    // ── getString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getString: returns stored value for existing key")
    void getString_found() {
        when(shopConfigRepository.findByConfigKey("pos_mode"))
                .thenReturn(Optional.of(plainConfig));

        assertThat(shopConfigService.getString(ShopConfigKey.POS_MODE)).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("getString: returns null when key not found")
    void getString_notFound() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getString(ShopConfigKey.POS_MODE)).isNull();
    }

    @Test
    @DisplayName("getString with default: returns default when key absent")
    void getString_defaultValue() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getString(ShopConfigKey.POS_MODE, "TABLE")).isEqualTo("TABLE");
    }

    // ── encrypted key ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getString: decrypts value for encrypted key")
    void getString_decryptsEncryptedKey() {
        ShopConfig encryptedConfig = ShopConfig.builder()
                .configKey("einvoice_password")
                .configValue("encrypted-blob")
                .configGroup("EINVOICE")
                .encrypted(true)
                .build();
        when(shopConfigRepository.findByConfigKey("einvoice_password"))
                .thenReturn(Optional.of(encryptedConfig));
        when(encryptionService.decrypt("encrypted-blob")).thenReturn("plain-password");

        assertThat(shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD)).isEqualTo("plain-password");
    }

    @Test
    @DisplayName("getString: returns null when decryption fails")
    void getString_decryptionFails() {
        ShopConfig encryptedConfig = ShopConfig.builder()
                .configKey("einvoice_password").configValue("bad-data")
                .configGroup("EINVOICE").encrypted(true).build();
        when(shopConfigRepository.findByConfigKey("einvoice_password"))
                .thenReturn(Optional.of(encryptedConfig));
        when(encryptionService.decrypt("bad-data")).thenThrow(new RuntimeException("bad key"));

        assertThat(shopConfigService.getString(ShopConfigKey.EINVOICE_PASSWORD)).isNull();
    }

    // ── type-converting getters ───────────────────────────────────────────────

    @Test
    @DisplayName("getDouble: parses valid string value")
    void getDouble_valid() {
        ShopConfig cfg = ShopConfig.builder().configKey("default_tax_rate")
                .configValue("10.5").configGroup("GENERAL").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 0.0)).isEqualTo(10.5);
    }

    @Test
    @DisplayName("getDouble: returns default for unparseable value")
    void getDouble_invalidString() {
        ShopConfig cfg = ShopConfig.builder().configKey("default_tax_rate")
                .configValue("not-a-number").configGroup("GENERAL").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 5.0)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("getInt: parses valid integer string")
    void getInt_valid() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_due_date")
                .configValue("30").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_due_date")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 15)).isEqualTo(30);
    }

    @Test
    @DisplayName("getBoolean: parses 'true' correctly")
    void getBoolean_trueValue() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_exclude_visible_item")
                .configValue("true").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false)).isTrue();
    }

    @Test
    @DisplayName("getBoolean: parses '1' as true")
    void getBoolean_oneValue() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_exclude_visible_item")
                .configValue("1").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false)).isTrue();
    }

    // ── set ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("set String: upserts new row when absent")
    void set_createsNewRow() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.POS_MODE, "TABLE");

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("TABLE");
        assertThat(cap.getValue().getConfigKey()).isEqualTo("pos_mode");
    }

    @Test
    @DisplayName("set String: encrypts value for encrypted key")
    void set_encryptsValue() {
        when(shopConfigRepository.findByConfigKey("einvoice_password")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("plain")).thenReturn("encrypted-blob");
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.EINVOICE_PASSWORD, "plain");

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("encrypted-blob");
    }

    // ── seedIfAbsent ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("seedIfAbsent: saves when key does not exist")
    void seedIfAbsent_doesNotExist() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, "STANDARD");

        verify(shopConfigRepository).save(any());
    }

    @Test
    @DisplayName("seedIfAbsent: skips when key already exists")
    void seedIfAbsent_alreadyExists() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.of(plainConfig));

        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, "TABLE");

        verify(shopConfigRepository, never()).save(any());
    }
}
