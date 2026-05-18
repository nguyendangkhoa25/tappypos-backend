package com.tappy.pos.service.tenant;

import com.tappy.pos.config.EncryptionService;
import com.tappy.pos.model.entity.tenant.ShopConfig;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.ShopConfigRepository;
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
    @Mock private TenantContext tenantContext;

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

    // ── getDecimal ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDecimal: returns parsed BigDecimal when key exists")
    void getDecimal_valid() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_interest_rate")
                .configValue("3.5").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_interest_rate")).thenReturn(Optional.of(cfg));

        java.math.BigDecimal result = shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE);

        assertThat(result).isEqualByComparingTo("3.5");
    }

    @Test
    @DisplayName("getDecimal: returns null when key is absent")
    void getDecimal_absent() {
        when(shopConfigRepository.findByConfigKey("pawn_interest_rate")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)).isNull();
    }

    @Test
    @DisplayName("getDecimal: returns null when stored value is not a number")
    void getDecimal_invalidNumber() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_interest_rate")
                .configValue("not-a-number").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_interest_rate")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)).isNull();
    }

    // ── set (typed overloads) ─────────────────────────────────────────────────

    @Test
    @DisplayName("set Double: stores as string representation")
    void set_double() {
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.DEFAULT_TAX_RATE, 10.0);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("10.0");
    }

    @Test
    @DisplayName("set BigDecimal: stores plain string")
    void set_bigDecimal() {
        when(shopConfigRepository.findByConfigKey("pawn_interest_rate")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.PAWN_INTEREST_RATE, new java.math.BigDecimal("2.5"));

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("2.5");
    }

    @Test
    @DisplayName("set Integer: stores as string")
    void set_integer() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.POS_MODE, (Integer) null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }

    @Test
    @DisplayName("set Boolean: stores 'true' string")
    void set_boolean() {
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, true);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("true");
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

    @Test
    @DisplayName("seedIfAbsent Double: saves when absent")
    void seedIfAbsent_double_absent() {
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 10.0);

        verify(shopConfigRepository).save(any());
    }

    @Test
    @DisplayName("seedIfAbsent Integer: saves when absent")
    void seedIfAbsent_integer_absent() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, 1);

        verify(shopConfigRepository).save(any());
    }

    @Test
    @DisplayName("seedIfAbsent Boolean: skips when already exists")
    void seedIfAbsent_boolean_exists() {
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.of(plainConfig));

        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false);

        verify(shopConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("seedIfAbsent Boolean: saves when key absent")
    void seedIfAbsent_boolean_absent() {
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, true);

        verify(shopConfigRepository).save(any());
    }

    // ── getRaw: blank configValue returns null ────────────────────────────────

    @Test
    @DisplayName("getString: returns null when stored value is blank")
    void getString_blankValue_returnsNull() {
        ShopConfig blankConfig = ShopConfig.builder()
                .configKey("pos_mode").configValue("   ").configGroup("POS").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.of(blankConfig));

        assertThat(shopConfigService.getString(ShopConfigKey.POS_MODE)).isNull();
    }

    // ── upsert: existing row update path ─────────────────────────────────────

    @Test
    @DisplayName("set String: updates existing row when key already present")
    void set_updatesExistingRow() {
        when(shopConfigRepository.findByConfigKey("pos_mode")).thenReturn(Optional.of(plainConfig));
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.POS_MODE, "TABLE");

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("TABLE");
    }

    // ── set typed overloads: non-null Integer ────────────────────────────────

    @Test
    @DisplayName("set Integer: stores non-null value as string")
    void set_integer_nonNull() {
        when(shopConfigRepository.findByConfigKey("pawn_due_date")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.PAWN_DUE_DATE, 30);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("30");
    }

    @Test
    @DisplayName("set Double: stores null as null value")
    void set_double_null() {
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.DEFAULT_TAX_RATE, (Double) null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }

    @Test
    @DisplayName("set BigDecimal: stores null as null value")
    void set_bigDecimal_null() {
        when(shopConfigRepository.findByConfigKey("pawn_interest_rate")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.PAWN_INTEREST_RATE, (java.math.BigDecimal) null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }

    @Test
    @DisplayName("set Boolean: stores null as null value")
    void set_boolean_null() {
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, (Boolean) null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }

    // ── set String: encrypted key with null/blank value skips encryption ──────

    @Test
    @DisplayName("set String: null value for encrypted key not encrypted")
    void set_encryptedKey_nullValue() {
        when(shopConfigRepository.findByConfigKey("einvoice_password")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.EINVOICE_PASSWORD, (String) null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
        verify(encryptionService, never()).encrypt(any());
    }

    @Test
    @DisplayName("set String: blank value for encrypted key not encrypted")
    void set_encryptedKey_blankValue() {
        when(shopConfigRepository.findByConfigKey("einvoice_password")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.set(ShopConfigKey.EINVOICE_PASSWORD, "   ");

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        verify(encryptionService, never()).encrypt(any());
    }

    // ── type-converting getters: missing branches ─────────────────────────────

    @Test
    @DisplayName("getInt: returns default when value is not a number")
    void getInt_invalidString() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_due_date")
                .configValue("bad").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_due_date")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 15)).isEqualTo(15);
    }

    @Test
    @DisplayName("getBoolean: parses 'false' as false")
    void getBoolean_falseValue() {
        ShopConfig cfg = ShopConfig.builder().configKey("pawn_exclude_visible_item")
                .configValue("false").configGroup("PAWN").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, true)).isFalse();
    }

    @Test
    @DisplayName("getBoolean: returns default when key absent")
    void getBoolean_absent_returnsDefault() {
        when(shopConfigRepository.findByConfigKey("pawn_exclude_visible_item")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, true)).isTrue();
    }

    @Test
    @DisplayName("getDouble: returns default when key absent")
    void getDouble_absent_returnsDefault() {
        when(shopConfigRepository.findByConfigKey("default_tax_rate")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 5.0)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("getInt: returns default when key absent")
    void getInt_absent_returnsDefault() {
        when(shopConfigRepository.findByConfigKey("pawn_due_date")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30)).isEqualTo(30);
    }

    // ── getDashboardWidgets ───────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardWidgets: returns null when key absent")
    void getDashboardWidgets_absent() {
        when(shopConfigRepository.findByConfigKey("dashboard_widgets")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getDashboardWidgets()).isNull();
    }

    @Test
    @DisplayName("getDashboardWidgets: returns null when stored value is blank")
    void getDashboardWidgets_blank() {
        ShopConfig cfg = ShopConfig.builder().configKey("dashboard_widgets")
                .configValue("   ").configGroup("DASHBOARD").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("dashboard_widgets")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getDashboardWidgets()).isNull();
    }

    @Test
    @DisplayName("getDashboardWidgets: parses CSV into list trimming whitespace")
    void getDashboardWidgets_csvList() {
        ShopConfig cfg = ShopConfig.builder().configKey("dashboard_widgets")
                .configValue("REVENUE, ORDERS ,CUSTOMERS").configGroup("DASHBOARD").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("dashboard_widgets")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getDashboardWidgets())
                .containsExactly("REVENUE", "ORDERS", "CUSTOMERS");
    }

    // ── setDashboardWidgets ───────────────────────────────────────────────────

    @Test
    @DisplayName("setDashboardWidgets: joins list to CSV and saves")
    void setDashboardWidgets_withList() {
        when(shopConfigRepository.findByConfigKey("dashboard_widgets")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.setDashboardWidgets(java.util.List.of("REVENUE", "ORDERS"));

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("REVENUE,ORDERS");
    }

    @Test
    @DisplayName("setDashboardWidgets: stores null when list is null")
    void setDashboardWidgets_null() {
        when(shopConfigRepository.findByConfigKey("dashboard_widgets")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.setDashboardWidgets(null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }

    // ── getNavConfig ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getNavConfig: returns null when key absent")
    void getNavConfig_absent() {
        when(shopConfigRepository.findByConfigKey("nav_config")).thenReturn(Optional.empty());

        assertThat(shopConfigService.getNavConfig()).isNull();
    }

    @Test
    @DisplayName("getNavConfig: returns null when stored value is blank")
    void getNavConfig_blank() {
        ShopConfig cfg = ShopConfig.builder().configKey("nav_config")
                .configValue("  ").configGroup("GENERAL").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("nav_config")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getNavConfig()).isNull();
    }

    @Test
    @DisplayName("getNavConfig: parses CSV into ordered list")
    void getNavConfig_csvList() {
        ShopConfig cfg = ShopConfig.builder().configKey("nav_config")
                .configValue("HOME,ORDER,PRODUCT").configGroup("GENERAL").encrypted(false).build();
        when(shopConfigRepository.findByConfigKey("nav_config")).thenReturn(Optional.of(cfg));

        assertThat(shopConfigService.getNavConfig()).containsExactly("HOME", "ORDER", "PRODUCT");
    }

    // ── setNavConfig ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("setNavConfig: joins list to CSV and saves")
    void setNavConfig_withList() {
        when(shopConfigRepository.findByConfigKey("nav_config")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.setNavConfig(java.util.List.of("HOME", "ORDER"));

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isEqualTo("HOME,ORDER");
    }

    @Test
    @DisplayName("setNavConfig: stores null when list is null")
    void setNavConfig_null() {
        when(shopConfigRepository.findByConfigKey("nav_config")).thenReturn(Optional.empty());
        when(shopConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shopConfigService.setNavConfig(null);

        ArgumentCaptor<ShopConfig> cap = ArgumentCaptor.forClass(ShopConfig.class);
        verify(shopConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getConfigValue()).isNull();
    }
}
