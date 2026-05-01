package com.knp.service.tenant;

import com.knp.model.enums.ShopType;
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
import java.util.List;
import java.util.Map;

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
        Map.entry(ShopType.PAWN_SHOP,          "db/tenant/pawn_shop.sql"),
        Map.entry(ShopType.CONVENIENCE_STORE,  "db/tenant/convenience_store.sql"),
        Map.entry(ShopType.JEWELRY,            "db/tenant/jewelry.sql")
    );
    private static final String DEFAULT_DML = "db/tenant/general.sql";

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
