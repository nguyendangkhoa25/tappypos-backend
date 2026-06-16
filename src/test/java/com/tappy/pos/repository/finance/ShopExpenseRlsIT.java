package com.tappy.pos.repository.finance;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test — verifies that PostgreSQL RLS (FORCE ROW LEVEL SECURITY) on
 * the {@code shop_expense} table correctly isolates each tenant's data.
 *
 * <p>Bug reproduced: user 0974862175 logged into pawn-99098 but saw expenses
 * belonging to hair-56748. Root cause investigation pointed to the backend's
 * {@code findByUsernameGlobal} returning the wrong tenant on login, which then
 * caused the JWT to carry hair-56748's tenantId — so every request sent
 * {@code X-Tenant-ID: hair-56748} and the app legitimately returned that shop's
 * data. This test confirms the DB-layer defence (RLS) is intact: even if the
 * app sends the wrong tenant-id header, each shop only ever sees its own rows.
 *
 * <p>Requires Docker (TestContainers) — skipped automatically if Docker is not
 * available (org.testcontainers.DockerClientFactory detects this at runtime).
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Data is inserted as a PostgreSQL <b>superuser</b> (bypasses RLS) so we
 *       can seed both tenants in the same setup step.
 *   <li>Queries run as <b>app_role</b>, a non-superuser. FORCE RLS applies to
 *       non-superusers, making the policy enforcement observable.
 *   <li>{@code SET LOCAL app.current_tenant} is transaction-local; the helper
 *       method sets autoCommit=false so the variable stays active for the query
 *       and is automatically cleared on rollback.
 * </ul>
 */
@Testcontainers
@DisplayName("ShopExpense RLS — cross-tenant expense isolation")
class ShopExpenseRlsIT {

    // Shared across all test methods — started once, stopped after the class.
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rlstest")
                    .withUsername("superuser")
                    .withPassword("superpass");

    /** Superuser JdbcTemplate used only for schema setup and data seeding. */
    private static JdbcTemplate superJdbc;

    // ── Schema + role setup (once per class) ─────────────────────────────────

    @BeforeAll
    static void createSchemaAndRole() {
        superJdbc = new JdbcTemplate(new SimpleDriverDataSource(
                new org.postgresql.Driver(),
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()));

        // Minimal shop_expense schema — mirrors the production table structure
        superJdbc.execute("""
                CREATE TABLE shop_expense (
                    id           BIGSERIAL    PRIMARY KEY,
                    tenant_id    VARCHAR(100) NOT NULL,
                    description  TEXT,
                    amount       NUMERIC(15,2),
                    category     VARCHAR(50),
                    expense_date DATE         NOT NULL DEFAULT CURRENT_DATE,
                    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
                )""");

        // Helper function used by the RLS policy — identical to the production one
        superJdbc.execute("""
                CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS TEXT
                LANGUAGE sql STABLE AS $$
                  SELECT NULLIF(current_setting('app.current_tenant', true), '')
                $$""");

        // Enable RLS — FORCE means even the table owner is subject to the policy
        superJdbc.execute("ALTER TABLE shop_expense ENABLE ROW LEVEL SECURITY");
        superJdbc.execute("ALTER TABLE shop_expense FORCE  ROW LEVEL SECURITY");
        superJdbc.execute("""
                CREATE POLICY tenant_isolation ON shop_expense
                USING (tenant_id = current_tenant_id())""");

        // Non-superuser role — this is what FORCE ROW LEVEL SECURITY actually applies to.
        // Superusers bypass RLS even with FORCE, so all data-query assertions must use
        // this role to observe real policy enforcement.
        superJdbc.execute("CREATE ROLE app_role LOGIN PASSWORD 'apppass'");
        superJdbc.execute("GRANT CONNECT  ON DATABASE rlstest   TO app_role");
        superJdbc.execute("GRANT USAGE    ON SCHEMA public       TO app_role");
        superJdbc.execute("GRANT SELECT   ON shop_expense        TO app_role");
        superJdbc.execute("GRANT EXECUTE  ON FUNCTION current_tenant_id() TO app_role");
    }

    // ── Data seeding (before each test) ──────────────────────────────────────

    @BeforeEach
    void seedData() {
        // Reset between tests so counts are deterministic
        superJdbc.execute("TRUNCATE shop_expense RESTART IDENTITY");

        // pawn-99098: exactly 2 expenses (mirrors the production state that triggered the bug)
        superJdbc.update(
                "INSERT INTO shop_expense (tenant_id, description, amount, category) VALUES (?,?,?,?)",
                "pawn-99098", "Tiền thuê mặt bằng", 5_000_000, "RENT");
        superJdbc.update(
                "INSERT INTO shop_expense (tenant_id, description, amount, category) VALUES (?,?,?,?)",
                "pawn-99098", "Tiền điện nước", 800_000, "UTILITIES");

        // hair-56748: 8 expenses (the "many records" the bug caused to appear in pawn's view)
        for (int i = 1; i <= 8; i++) {
            superJdbc.update(
                    "INSERT INTO shop_expense (tenant_id, description, amount, category) VALUES (?,?,?,?)",
                    "hair-56748", "Chi phí cửa hàng tóc " + i, 100_000L * i, "UTILITIES");
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Opens a fresh connection as {@code app_role}, sets {@code app.current_tenant}
     * to {@code tenantId} via {@code SET LOCAL} (transaction-scoped), runs the
     * expense query, then rolls back.
     *
     * <p>Passing {@code null} simulates a request that arrives without an
     * {@code X-Tenant-ID} header — the session variable is never set so
     * {@code current_tenant_id()} returns {@code NULL} and the RLS policy
     * {@code tenant_id = NULL} evaluates to {@code FALSE} for every row.
     */
    private List<Map<String, Object>> queryExpensesAsTenant(String tenantId) throws Exception {
        var appDs = new SimpleDriverDataSource(
                new org.postgresql.Driver(), postgres.getJdbcUrl(), "app_role", "apppass");

        try (Connection conn = appDs.getConnection()) {
            // autoCommit=false is required — SET LOCAL is only meaningful inside a transaction
            conn.setAutoCommit(false);
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Statement st = conn.createStatement()) {
                if (tenantId != null && !tenantId.isEmpty()) {
                    st.execute("SET LOCAL app.current_tenant = '"
                            + tenantId.replace("'", "''") + "'");
                }
                try (ResultSet rs = st.executeQuery(
                        "SELECT tenant_id, description FROM shop_expense " +
                        "WHERE deleted = FALSE ORDER BY id")) {
                    while (rs.next()) {
                        rows.add(Map.of(
                                "tenant_id",   rs.getString("tenant_id"),
                                "description", rs.getString("description")));
                    }
                }
            }
            conn.rollback(); // read-only — rollback resets SET LOCAL and leaves DB clean
            return rows;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("pawn-99098 → only its own 2 expenses returned; hair-56748 data must NOT leak")
    void pawnShop_onlySeesItsOwnExpenses() throws Exception {
        List<Map<String, Object>> rows = queryExpensesAsTenant("pawn-99098");

        assertThat(rows)
                .as("pawn-99098 has exactly 2 seeded expenses and must not see hair-56748 data")
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.get("tenant_id"))
                        .as("every returned row must belong to pawn-99098")
                        .isEqualTo("pawn-99098"))
                .extracting(r -> r.get("description"))
                .containsExactlyInAnyOrder("Tiền thuê mặt bằng", "Tiền điện nước");
    }

    @Test
    @DisplayName("hair-56748 → only its own 8 expenses returned; pawn-99098 data must NOT leak")
    void hairShop_onlySeesItsOwnExpenses() throws Exception {
        List<Map<String, Object>> rows = queryExpensesAsTenant("hair-56748");

        assertThat(rows)
                .as("hair-56748 has exactly 8 seeded expenses and must not see pawn-99098 data")
                .hasSize(8)
                .allSatisfy(r -> assertThat(r.get("tenant_id"))
                        .as("every returned row must belong to hair-56748")
                        .isEqualTo("hair-56748"));
    }

    @Test
    @DisplayName("no tenant context → RLS returns zero rows (NULL = NULL is always FALSE in the policy)")
    void noTenantContext_returnsNoRows() throws Exception {
        // Simulates a request where X-Tenant-ID was not sent and TenantContext is empty.
        // current_tenant_id() returns NULL; the policy USING (tenant_id = NULL) is FALSE
        // for every row because NULL comparisons are never TRUE in SQL.
        List<Map<String, Object>> rows = queryExpensesAsTenant(null);

        assertThat(rows)
                .as("without a tenant context, RLS must block all rows from all tenants")
                .isEmpty();
    }
}
