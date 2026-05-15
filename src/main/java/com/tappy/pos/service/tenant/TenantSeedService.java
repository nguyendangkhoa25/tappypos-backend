package com.tappy.pos.service.tenant;

import com.tappy.pos.model.enums.ShopType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes shop-type DML seed files in a single transaction.
 *
 * Each statement runs with a SAVEPOINT so a non-fatal failure (e.g., a bank
 * code that was already seeded by a previous tenant) is rolled back
 * individually and execution continues.  The session variable
 * app.current_tenant must already be set on the connection — TenantRlsAspect
 * does this automatically because this method is @Transactional.
 */
@Service
@Slf4j
public class TenantSeedService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<ShopType, String> DML_FILES = Map.ofEntries(
        Map.entry(ShopType.PAWN_SHOP,          "db/tenant/pawn_store.sql"),
        Map.entry(ShopType.CONVENIENCE_STORE,  "db/tenant/convenience_store.sql"),
        Map.entry(ShopType.JEWELRY,            "db/tenant/jewelry_store.sql"),
        Map.entry(ShopType.BARBER_SHOP,        "db/tenant/barber_shop.sql"),
        Map.entry(ShopType.BARBER_SHOP_MEN,    "db/tenant/barber_shop_men.sql"),
        Map.entry(ShopType.HAIR_SALON,         "db/tenant/hair_salon.sql"),
        Map.entry(ShopType.NAIL_SHOP,          "db/tenant/nail_shop.sql"),
        Map.entry(ShopType.LASH_PMU_STUDIO,    "db/tenant/lash_pmu_studio.sql"),
        Map.entry(ShopType.SPA_SHOP,           "db/tenant/spa_shop.sql"),
        Map.entry(ShopType.MASSAGE_SHOP,       "db/tenant/massage_shop.sql"),
        Map.entry(ShopType.BEAUTY_CLINIC,      "db/tenant/beauty_clinic.sql"),
        Map.entry(ShopType.MAKEUP_STUDIO,      "db/tenant/makeup_studio.sql"),
        Map.entry(ShopType.RESTAURANT,         "db/tenant/restaurant.sql"),
        Map.entry(ShopType.COFFEE_SHOP,        "db/tenant/coffee_shop.sql"),
        Map.entry(ShopType.PUB,                "db/tenant/pub.sql"),
        Map.entry(ShopType.PUB_SEAFOOD,        "db/tenant/pub_seafood.sql"),
        Map.entry(ShopType.PUB_GOAT,           "db/tenant/pub_goat.sql"),
        Map.entry(ShopType.PUB_BEEF,           "db/tenant/pub_beef.sql")
    );
    private static final String DEFAULT_DML = "db/tenant/general.sql";

    /** Service-type shops get a "Phiếu dịch vụ" POS receipt template. */
    private static final Set<ShopType> SERVICE_SHOP_TYPES = EnumSet.of(
        ShopType.BARBER_SHOP, ShopType.BARBER_SHOP_MEN, ShopType.HAIR_SALON,
        ShopType.NAIL_SHOP, ShopType.LASH_PMU_STUDIO, ShopType.SPA_SHOP,
        ShopType.MASSAGE_SHOP, ShopType.BEAUTY_CLINIC, ShopType.MAKEUP_STUDIO,
        ShopType.COFFEE_SHOP, ShopType.FOOD_BEVERAGE, ShopType.RESTAURANT,
        ShopType.PUB, ShopType.PUB_SEAFOOD, ShopType.PUB_GOAT, ShopType.PUB_BEEF
    );

    /**
     * Inserts shop-type-specific named print templates for a newly provisioned
     * tenant. Must be called after {@link #seed(ShopType)} so the tenant context
     * (and therefore RLS) is already set on the current connection.
     */
    @Transactional
    public void seedShopTypeTemplates(ShopType shopType) {
        if (shopType == null) return;

        String templateName;
        String configJson;

        if (SERVICE_SHOP_TYPES.contains(shopType)) {
            templateName = "Phiếu dịch vụ";
            configJson = "{\"headerText\":\"\",\"footerText\":\"Cảm ơn quý khách!\\nHẹn gặp lại!\",\"showAddress\":true,\"showTaxId\":false,\"showOrderNumber\":true,\"showDateTime\":true,\"showCustomer\":true,\"showTaxBreakdown\":false,\"showCashDetails\":true,\"paperWidth\":\"80mm\",\"autoClose\":true,\"showVietQr\":false}";
        } else if (shopType == ShopType.PHARMACY) {
            templateName = "Hóa đơn thuốc";
            configJson = "{\"headerText\":\"\",\"footerText\":\"Cảm ơn quý khách!\\nChúc bạn mau hồi phục!\",\"showAddress\":true,\"showTaxId\":true,\"showOrderNumber\":true,\"showDateTime\":true,\"showCustomer\":true,\"showTaxBreakdown\":true,\"showCashDetails\":true,\"paperWidth\":\"80mm\",\"autoClose\":true,\"showVietQr\":false}";
        } else if (shopType == ShopType.CONVENIENCE_STORE) {
            templateName = "Hóa đơn siêu thị";
            configJson = "{\"headerText\":\"\",\"footerText\":\"Cảm ơn quý khách!\\nHẹn gặp lại!\",\"showAddress\":true,\"showTaxId\":false,\"showOrderNumber\":true,\"showDateTime\":true,\"showCustomer\":false,\"showTaxBreakdown\":false,\"showCashDetails\":true,\"paperWidth\":\"80mm\",\"autoClose\":true,\"showVietQr\":true}";
        } else if (shopType == ShopType.FASHION || shopType == ShopType.ELECTRONICS) {
            templateName = "Phiếu bảo hành";
            configJson = "{\"headerText\":\"\",\"footerText\":\"Cảm ơn quý khách!\\nVui lòng giữ hóa đơn để bảo hành.\",\"showAddress\":true,\"showTaxId\":true,\"showOrderNumber\":true,\"showDateTime\":true,\"showCustomer\":true,\"showTaxBreakdown\":true,\"showCashDetails\":true,\"paperWidth\":\"80mm\",\"autoClose\":true,\"showVietQr\":false}";
        } else {
            // No type-specific template for other shop types
            return;
        }

        final String tName = templateName;
        final String tConfig = configJson;

        Session session = entityManager.unwrap(Session.class);
        session.doWork(conn -> {
            Savepoint sp = conn.setSavepoint();
            try (Statement st = conn.createStatement()) {
                st.execute(
                    "INSERT INTO print_templates " +
                    "(tenant_id, template_type, name, config_json, is_default, deleted, created_at, updated_at) " +
                    "VALUES (" +
                    "  current_setting('app.current_tenant', true)," +
                    "  'POS_RECEIPT'," +
                    "  '" + tName.replace("'", "''") + "'," +
                    "  '" + tConfig.replace("'", "''") + "'," +
                    "  FALSE, FALSE, NOW(), NOW()" +
                    ") ON CONFLICT (template_type, name, tenant_id) DO NOTHING"
                );
                conn.releaseSavepoint(sp);
                log.info("Seeded shop-type print template '{}' for shopType={}", tName, shopType);
            } catch (Exception e) {
                conn.rollback(sp);
                log.warn("Could not seed print template for shopType {}: {}", shopType, e.getMessage());
            }
        });
    }

    @Transactional
    public void seed(ShopType shopType) {
        String path = DML_FILES.getOrDefault(shopType, DEFAULT_DML);
        log.info("Seeding shop-type DML: {} (shopType={})", path, shopType);
        List<String> stmts;
        try {
            String sql = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
            stmts = splitStatements(sql);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read DML seed file: " + path, e);
        }

        int[] counts = {0, 0};
        Session session = entityManager.unwrap(Session.class);
        session.doWork(conn -> {
            for (String stmt : stmts) {
                Savepoint sp = conn.setSavepoint();
                try (Statement st = conn.createStatement()) {
                    st.execute(stmt);
                    conn.releaseSavepoint(sp);
                    counts[0]++;
                } catch (Exception e) {
                    conn.rollback(sp);
                    counts[1]++;
                    log.debug("Statement skipped in {}: {}", path, e.getMessage());
                }
            }
        });
        log.info("DML seeded: {}/{} executed, {} skipped ({})", counts[0], stmts.size(), counts[1], path);
    }

    private static List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("--")) continue;
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String stmt = current.toString().strip();
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).strip();
                }
                if (!stmt.isEmpty()) {
                    result.add(stmt);
                }
                current.setLength(0);
            }
        }
        String remaining = current.toString().strip();
        if (!remaining.isEmpty()) {
            result.add(remaining);
        }
        return result;
    }
}
