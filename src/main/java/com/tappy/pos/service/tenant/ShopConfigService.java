package com.tappy.pos.service.tenant;

import com.tappy.pos.config.EncryptionService;
import com.tappy.pos.model.entity.tenant.ShopConfig;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.ShopConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShopConfigService {

    private final ShopConfigRepository shopConfigRepository;
    private final EncryptionService encryptionService;
    private final TenantContext tenantContext;

    // ── Read helpers ────────────────────────────────────────────────────────────

    public String getString(ShopConfigKey key) {
        return getRaw(key).orElse(null);
    }

    public String getString(ShopConfigKey key, String defaultValue) {
        return getRaw(key).orElse(defaultValue);
    }

    public Double getDouble(ShopConfigKey key, Double defaultValue) {
        return getRaw(key).map(v -> {
            try { return Double.parseDouble(v); }
            catch (NumberFormatException e) { return defaultValue; }
        }).orElse(defaultValue);
    }

    public BigDecimal getDecimal(ShopConfigKey key) {
        return getRaw(key).map(v -> {
            try { return new BigDecimal(v); }
            catch (NumberFormatException e) { return null; }
        }).orElse(null);
    }

    public Integer getInt(ShopConfigKey key, Integer defaultValue) {
        return getRaw(key).map(v -> {
            try { return Integer.parseInt(v); }
            catch (NumberFormatException e) { return defaultValue; }
        }).orElse(defaultValue);
    }

    public Boolean getBoolean(ShopConfigKey key, Boolean defaultValue) {
        return getRaw(key).map(v -> "true".equalsIgnoreCase(v) || "1".equals(v))
                .orElse(defaultValue);
    }

    // ── Write helpers ───────────────────────────────────────────────────────────

    public void set(ShopConfigKey key, String value) {
        String stored = (key.isEncrypted() && value != null && !value.isBlank())
                ? encryptionService.encrypt(value)
                : value;
        upsert(key, stored);
    }

    public void set(ShopConfigKey key, Double value) {
        upsert(key, value == null ? null : value.toString());
    }

    public void set(ShopConfigKey key, BigDecimal value) {
        upsert(key, value == null ? null : value.toPlainString());
    }

    public void set(ShopConfigKey key, Integer value) {
        upsert(key, value == null ? null : value.toString());
    }

    public void set(ShopConfigKey key, Boolean value) {
        upsert(key, value == null ? null : value.toString());
    }

    // ── Dashboard widgets ───────────────────────────────────────────────────────

    public List<String> getDashboardWidgets() {
        String raw = getString(ShopConfigKey.DASHBOARD_WIDGETS);
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void setDashboardWidgets(List<String> widgetIds) {
        set(ShopConfigKey.DASHBOARD_WIDGETS, widgetIds == null ? null : String.join(",", widgetIds));
    }

    // ── Mobile navigation config ────────────────────────────────────────────────

    public List<String> getNavConfig() {
        String raw = getString(ShopConfigKey.NAV_CONFIG);
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void setNavConfig(List<String> items) {
        set(ShopConfigKey.NAV_CONFIG, items == null ? null : String.join(",", items));
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private Optional<String> getRaw(ShopConfigKey key) {
        return shopConfigRepository.findByConfigKey(key.getKey())
                .map(cfg -> {
                    String val = cfg.getConfigValue();
                    if (val == null || val.isBlank()) return null;
                    if (key.isEncrypted()) {
                        try { return encryptionService.decrypt(val); }
                        catch (Exception e) {
                            log.warn("Failed to decrypt config key '{}' — returning null", key.getKey());
                            return null;
                        }
                    }
                    return val;
                });
    }

    private void upsert(ShopConfigKey key, String rawValue) {
        ShopConfig cfg = shopConfigRepository.findByConfigKey(key.getKey())
                .orElseGet(() -> ShopConfig.builder()
                        .tenantId(tenantContext.getCurrentTenantId())
                        .configKey(key.getKey())
                        .configGroup(key.getGroup())
                        .encrypted(key.isEncrypted())
                        .build());
        cfg.setConfigValue(rawValue);
        cfg.setConfigGroup(key.getGroup());
        cfg.setEncrypted(key.isEncrypted());
        shopConfigRepository.save(cfg);
    }

    // ── Convenience: seed defaults if key is absent ─────────────────────────────

    public void seedIfAbsent(ShopConfigKey key, String value) {
        if (shopConfigRepository.findByConfigKey(key.getKey()).isEmpty()) {
            set(key, value);
        }
    }

    public void seedIfAbsent(ShopConfigKey key, Double value) {
        if (shopConfigRepository.findByConfigKey(key.getKey()).isEmpty()) {
            set(key, value);
        }
    }

    public void seedIfAbsent(ShopConfigKey key, Integer value) {
        if (shopConfigRepository.findByConfigKey(key.getKey()).isEmpty()) {
            set(key, value);
        }
    }

    public void seedIfAbsent(ShopConfigKey key, Boolean value) {
        if (shopConfigRepository.findByConfigKey(key.getKey()).isEmpty()) {
            set(key, value);
        }
    }
}
