package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack API smoke integration test.
 *
 * <p>Unlike the {@code @WebMvcTest} controller slices (which mock the service layer and
 * never touch a database), this test boots the <b>entire</b> Spring context against a
 * <b>real PostgreSQL</b> (Testcontainers) with the production <b>Flyway V001</b> migration
 * applied, and drives the real HTTP API through the full filter chain (JWT auth,
 * {@code TenantInterceptor}, feature gating, RLS).
 *
 * <p>It is the regression guard for the consolidated {@code V001__initial_schema.sql}:
 * if the migration, entity mappings, or app wiring break, the context fails to start or
 * these endpoints stop working.
 *
 * <p>Requires Docker. Named {@code *IntegrationTest} (not {@code *IT}) so it runs under
 * Surefire / {@code mvn test} rather than only Failsafe.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Spring Boot 4 no longer auto-registers TestRestTemplate from @SpringBootTest alone — opt in.
@AutoConfigureTestRestTemplate
@ActiveProfiles("dev")   // supplies sinvoice.*, jwt.secret, encryption key, etc.; datasource is overridden below
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API smoke — full stack on real Postgres + Flyway V001")
class ApiSmokeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tappy_pos_it")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        // Override the H2 defaults from src/test/resources/application.properties so the
        // full stack runs on a real Postgres with the real Flyway migration.
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration");
        r.add("spring.flyway.validate-on-migrate", () -> "false");
        // The test application.properties shadows the main one (which supplies the default),
        // so provide the AES-256 field-encryption key here (Base64 of a 32-byte value).
        r.add("app.encryption.key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        // The H2 test props set provider_disables_autocommit=true, which breaks commits on a
        // real Postgres/Hikari ("Cannot commit when autoCommit is enabled"). Match production.
        r.add("spring.jpa.properties.hibernate.connection.provider_disables_autocommit", () -> "false");
    }

    @Autowired TestRestTemplate rest;   // base URL already includes the /api context path
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private static final String MASTER_USER = "Administrator";
    private static final String MASTER_PASS = "12345678x@X";
    private static final String IT_TENANT   = "it-smoke-jewelry";
    private static final String SHOP_ADMIN  = "itsmokeadmin";
    private static final String SHOP_PASS   = "admin123";

    static String masterToken;

    private boolean tableExists(String name) {
        Integer c = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = ?",
                Integer.class, name);
        return c != null && c > 0;
    }

    // ── 1. Migration ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Flyway applied V001 bootstrap plus the incremental migrations and seeded the schema")
    void migrationApplied() {
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);
        // V001 is the single consolidated bootstrap: all pre-production migrations (the former
        // V002–V044) were folded into it (ALTERs merged into their CREATE TABLE, new tables +
        // RLS appended, seed data merged, pure data-migrations dropped). Post-baseline schema
        // changes are added as sequential V0XX migrations on top, so assert V001 is the FIRST
        // applied (the baseline) rather than the only one — an exact-list check would break on
        // every new migration.
        assertThat(versions).isNotEmpty();
        assertThat(versions.get(0)).isEqualTo("001");

        // Tables created by the bootstrap (formerly separate booking/cash-drawer migrations).
        assertThat(tableExists("tenants")).isTrue();
        assertThat(tableExists("bookings")).isTrue();
        assertThat(tableExists("booking_resources")).isTrue();
        assertThat(tableExists("cash_drawer_close")).isTrue();

        // Feature rows that were folded in by name (the V004/V005/V006 fixes).
        Integer feats = jdbc.queryForObject(
                "select count(*) from features where name in ('UTILITIES','BOOKING')", Integer.class);
        assertThat(feats).isEqualTo(2);

        // Columns folded into CREATE TABLE.
        assertThat(columnExists("orders", "confirmed_at")).isTrue();
        assertThat(columnExists("shop_table", "qr_token")).isTrue();
        assertThat(columnExists("inventory", "variant_id")).isTrue();
    }

    private boolean columnExists(String table, String column) {
        Integer c = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = ? and column_name = ?",
                Integer.class, table, column);
        return c != null && c > 0;
    }

    // ── 2. Health ────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("GET /actuator/health → 200 UP")
    void health() {
        ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("UP");
    }

    // ── 3. Master auth ───────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST /auth/login/force as seeded master admin → JWT")
    void masterLogin() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of("username", MASTER_USER, "password", MASTER_PASS);
        ResponseEntity<String> res = rest.postForEntity(
                "/auth/login/force", new HttpEntity<>(body, h), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = mapper.readTree(res.getBody()).path("data");
        masterToken = data.path("accessToken").asText(null);
        assertThat(masterToken).as("master access token").isNotBlank();
    }

    // ── 4. Tenant provisioning + feature wiring ──────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /multi-tenants provisions a JEWELRY shop with UTILITIES wired through")
    void provisionTenant() {
        assertThat(masterToken).as("requires master login first").isNotBlank();

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Tenant-ID", "master");
        h.setBearerAuth(masterToken);
        var body = Map.of(
                "tenantId", IT_TENANT,
                "name", "IT Smoke Jewelry",
                "shopType", "JEWELRY",
                "adminUsername", SHOP_ADMIN,
                "adminPassword", SHOP_PASS,
                "features", List.of("DASHBOARD", "POS", "ORDER", "CUSTOMER", "PAWN", "UTILITIES"),
                "expirationDate", "2030-12-31");

        ResponseEntity<String> res = rest.postForEntity(
                "/multi-tenants", new HttpEntity<>(body, h), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful())
                .as("create tenant: %s", res.getBody()).isTrue();

        // Tenant row persisted with UTILITIES in its feature CSV.
        String features = jdbc.queryForObject(
                "select features from tenants where tenant_id = ?", String.class, IT_TENANT);
        assertThat(features).contains("UTILITIES");

        // Provisioning mapped UTILITIES onto the SHOP_OWNER role (the full feature pipeline,
        // including the FeatureEnum + TenantProvisioningService wiring added for the tools hub).
        Integer roleFeat = jdbc.queryForObject("""
                select count(*) from role_features rf
                join roles r    on r.id = rf.role_id
                join features f on f.id = rf.feature_id
                where r.tenant_id = ? and r.name = 'SHOP_OWNER' and f.name = 'UTILITIES'
                """, Integer.class, IT_TENANT);
        assertThat(roleFeat).as("UTILITIES mapped to SHOP_OWNER").isEqualTo(1);
    }

    // ── 5. Security: unauthenticated shop endpoint is blocked ─────────────────────

    @Test
    @Order(5)
    @DisplayName("Shop endpoint without JWT → 4xx (auth enforced)")
    void unauthenticatedBlocked() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Tenant-ID", IT_TENANT);
        ResponseEntity<String> res = rest.exchange(
                "/products", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(res.getStatusCode().is4xxClientError())
                .as("unauthenticated request should be rejected, got %s", res.getStatusCode())
                .isTrue();
    }
}
