package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

/**
 * Shared base for full-stack controller/API integration tests.
 *
 * <p>Boots the <b>entire</b> Spring context against a <b>real PostgreSQL</b> (Testcontainers)
 * with the production Flyway migrations applied, and drives the real HTTP API through the
 * full filter chain (JWT auth, {@code TenantInterceptor}, feature gating, RLS) — exactly like
 * {@link ApiSmokeIntegrationTest}, but factored into a reusable base so each subclass can
 * provision its own tenant and sweep many controllers.
 *
 * <p><b>Singleton-container pattern:</b> one Postgres container is started once for the whole
 * JVM and every subclass reuses it. Because all subclasses declare the same dynamic properties,
 * Spring's context cache is shared too — the context boots and Flyway migrates only once for the
 * entire IT suite, no matter how many sweep classes extend this.
 *
 * <p>Requires Docker. These classes are named {@code *IT} so they run under Failsafe in the
 * {@code verify} phase (and feed the integration-coverage report), not under Surefire.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
abstract class AbstractApiIT {

    // One shared container for the whole IT suite (manual lifecycle — not @Container).
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("tappy_pos_api_it")
                .withUsername("postgres")
                .withPassword("postgres");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration");
        r.add("spring.flyway.validate-on-migrate", () -> "false");
        r.add("app.encryption.key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        r.add("spring.jpa.properties.hibernate.connection.provider_disables_autocommit", () -> "false");
    }

    @Autowired protected TestRestTemplate rest;   // base URL already includes the /api context path
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ObjectMapper mapper;

    protected static final String MASTER_USER = "Administrator";
    protected static final String MASTER_PASS = "12345678x@X";

    /** Set by {@link #loginMaster()}. */
    protected String masterToken;

    // ── Auth ──────────────────────────────────────────────────────────────────

    /** Logs in the seeded master admin (force-login, no device conflict) and caches the token. */
    protected String loginMaster() {
        masterToken = login(MASTER_USER, MASTER_PASS);
        return masterToken;
    }

    /** Logs in any user via force-login (no X-Tenant-ID — the backend resolves the tenant). */
    protected String login(String username, String password) {
        HttpHeaders h = jsonHeaders();
        ResponseEntity<String> res = rest.postForEntity(
                "/auth/login/force",
                new HttpEntity<>(Map.of("username", username, "password", password), h),
                String.class);
        return path(res, "data", "accessToken");
    }

    /**
     * Provisions a tenant via the real master API and returns a logged-in shop token.
     * Idempotent enough for re-runs: if provisioning fails because the tenant already exists,
     * it still attempts the shop login.
     */
    protected String provisionAndLogin(String tenantId, String name, String shopType,
                                       String adminUsername, String adminPassword,
                                       List<String> features) {
        if (masterToken == null) loginMaster();
        Map<String, Object> body = Map.of(
                "tenantId", tenantId,
                "name", name,
                "shopType", shopType,
                "adminUsername", adminUsername,
                "adminPassword", adminPassword,
                "features", features,
                "expirationDate", "2035-12-31");
        rest.postForEntity("/multi-tenants", new HttpEntity<>(body, masterHeaders()), String.class);
        return login(adminUsername, adminPassword);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    protected HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    protected HttpHeaders masterHeaders() {
        HttpHeaders h = jsonHeaders();
        h.set("X-Tenant-ID", "master");
        h.setBearerAuth(masterToken);
        return h;
    }

    protected HttpHeaders shopHeaders(String tenantId, String token) {
        HttpHeaders h = jsonHeaders();
        h.set("X-Tenant-ID", tenantId);
        h.setBearerAuth(token);
        return h;
    }

    protected ResponseEntity<String> get(String pathUrl, HttpHeaders h) {
        return rest.exchange(pathUrl, HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    protected ResponseEntity<String> post(String pathUrl, Object body, HttpHeaders h) {
        return rest.exchange(pathUrl, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
    }

    protected ResponseEntity<String> put(String pathUrl, Object body, HttpHeaders h) {
        return rest.exchange(pathUrl, HttpMethod.PUT, new HttpEntity<>(body, h), String.class);
    }

    protected ResponseEntity<String> delete(String pathUrl, HttpHeaders h) {
        return rest.exchange(pathUrl, HttpMethod.DELETE, new HttpEntity<>(h), String.class);
    }

    /**
     * Best-effort sweep: GET every path with the given headers, swallowing any error so one
     * bad endpoint never aborts the rest. Used purely to exercise read endpoints for coverage.
     * TestRestTemplate already returns 4xx/5xx without throwing; the try/catch only guards
     * against connection-level surprises.
     */
    protected void sweepGet(HttpHeaders h, String... paths) {
        for (String p : paths) {
            try {
                get(p, h);
            } catch (Exception ignored) {
                // intentionally ignored — coverage sweep
            }
        }
    }

    // ── JSON / JDBC helpers ─────────────────────────────────────────────────────

    protected JsonNode json(ResponseEntity<String> res) {
        try {
            return mapper.readTree(res.getBody());
        } catch (Exception e) {
            throw new RuntimeException("bad json: " + res.getBody(), e);
        }
    }

    /** Reads a nested text field, e.g. {@code path(res, "data", "accessToken")}. */
    protected String path(ResponseEntity<String> res, String... fields) {
        JsonNode n = json(res);
        for (String f : fields) n = n.path(f);
        return n.asText(null);
    }

    /** First id in a tenant table, or {@code null} if the tenant has no rows yet. */
    protected Long firstId(String table, String tenantId) {
        return jdbc.query(
                "select id from " + table + " where tenant_id = ? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, tenantId);
    }
}
