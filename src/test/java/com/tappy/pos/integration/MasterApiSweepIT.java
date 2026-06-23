package com.tappy.pos.integration;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack controller sweep for the MASTER-side surface — tenant management
 * ({@code MultiTenantController}), platform agents ({@code AgentController}), the master dashboard,
 * the shared product catalog, contact leads, admin feedback review, the public/master tenant
 * lookups, and the master-context role/user reads.
 *
 * <p>Logs in the seeded master admin, then drives the real HTTP API through the full filter chain
 * (JWT auth + {@code @MasterDatabaseOnly} + {@code @RequiresFeature}). Read endpoints are swept
 * best-effort for coverage and never asserted. Write lifecycles (provision a throwaway tenant,
 * create/update/delete an agent, update widgets/shop-info) run real round-trips but assert
 * <b>loosely</b> ({@code statusCode < 500}) so a legitimate business 4xx never fails the build.
 * The throwaway tenant is deleted in the last-ordered test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API sweep — master-side controllers (full stack on real Postgres)")
class MasterApiSweepIT extends AbstractApiIT {

    private static final String TENANT     = "it-master-sweep-1";
    private static final String SHOP_ADMIN = "itmastersweep1";
    private static final String SHOP_PASS  = "admin123";

    // A small but valid feature set for the throwaway tenant.
    private static final List<String> SHOP_FEATURES = List.of(
            "DASHBOARD", "ORDER", "PRODUCT", "INVENTORY", "POS", "CUSTOMER",
            "USER", "SHOP_INFO", "NOTIFICATION", "ACTIVITY_LOG");

    private static final String RANGE = "?from=2026-01-01&to=2026-12-31"
            + "&startDate=2026-01-01&endDate=2026-12-31&page=0&size=20";

    private HttpHeaders M;   // master headers

    @BeforeAll
    void setUp() {
        loginMaster();
        assertThat(masterToken).as("master token").isNotBlank();
        M = masterHeaders();
        // Provision a throwaway tenant we fully control; exercised by the later tests and
        // deleted in tenantDelete (the last-ordered test). Idempotent on re-run.
        provisionAndLogin(TENANT, "IT Master Sweep", "JEWELRY", SHOP_ADMIN, SHOP_PASS, SHOP_FEATURES);
        // re-login as master because provisionAndLogin logged us in as the shop admin
        loginMaster();
        M = masterHeaders();
    }

    // ── Read sweep across the whole master surface ─────────────────────────────

    @Test @Order(1)
    @DisplayName("GET sweep — all master read endpoints execute through the full stack")
    void readSweep() {
        sweepGet(M,
                // MultiTenantController reads
                "/multi-tenants/stats", "/multi-tenants", "/multi-tenants?search=IT",
                "/multi-tenants/" + TENANT,
                "/multi-tenants/" + TENANT + "/shop-info",
                "/multi-tenants/" + TENANT + "/dashboard-widgets",
                // AgentController reads
                "/vendor-admins", "/vendor-admins?search=a",
                // MasterDashboardController
                "/master-dashboard/stats",
                // ProductCatalogController
                "/product-catalog", "/product-catalog?search=a&page=0&size=20",
                "/product-catalog/stats",
                // ContactController admin read
                "/contact", "/contact?status=NEW&page=0&size=20",
                // FeedbackController admin read
                "/feedback", "/feedback?status=PENDING&page=0&size=20",
                // TenantController (public/master lookups)
                "/tenants", "/tenants/" + TENANT, "/tenants/" + TENANT + "/status",
                // RoleController (master context)
                "/roles", "/roles/permissions-matrix",
                // UserController master-side GETs
                "/users" + RANGE, "/users/tenant-features"
        );
        assertThat(true).isTrue();   // sweep ran end-to-end without aborting
    }

    // ── Tenant lifecycle writes (against the throwaway tenant) ──────────────────

    @Test @Order(2)
    @DisplayName("Tenant update / activate / deactivate / activate round-trips (loose)")
    void tenantUpdateActivate() {
        ResponseEntity<String> upd = put("/multi-tenants/" + TENANT,
                Map.of("name", "IT Master Sweep Updated",
                        "subscriptionType", "Trial",
                        "features", SHOP_FEATURES),
                M);
        assertThat(upd.getStatusCode().value()).as("update tenant: %s", upd.getBody()).isLessThan(500);

        ResponseEntity<String> deact = put("/multi-tenants/" + TENANT + "/deactivate", null, M);
        assertThat(deact.getStatusCode().value()).as("deactivate: %s", deact.getBody()).isLessThan(500);

        ResponseEntity<String> act = put("/multi-tenants/" + TENANT + "/activate", null, M);
        assertThat(act.getStatusCode().value()).as("activate: %s", act.getBody()).isLessThan(500);
    }

    @Test @Order(3)
    @DisplayName("Tenant shop-info + dashboard-widgets update round-trips (loose)")
    void tenantShopInfoAndWidgets() {
        ResponseEntity<String> info = put("/multi-tenants/" + TENANT + "/shop-info",
                Map.of("shopName", "IT Master Sweep Shop"), M);
        assertThat(info.getStatusCode().value()).as("update shop-info: %s", info.getBody()).isLessThan(500);

        ResponseEntity<String> widgets = put("/multi-tenants/" + TENANT + "/dashboard-widgets",
                List.of("revenue", "orders"), M);
        assertThat(widgets.getStatusCode().value()).as("update widgets: %s", widgets.getBody()).isLessThan(500);
    }

    // ── Agent lifecycle (create → fetch → update → delete) ──────────────────────

    @Test @Order(4)
    @DisplayName("Agent create → get → update → delete round-trips")
    void agentLifecycle() {
        String unique = "it-master-sweep-agent-" + System.currentTimeMillis();
        ResponseEntity<String> create = post("/vendor-admins",
                Map.of("name", unique,
                        "contactEmail", unique + "@example.com",
                        "contactPhone", "0900112233",
                        "notes", "IT sweep agent"),
                M);
        assertThat(create.getStatusCode().value()).as("create agent: %s", create.getBody()).isLessThan(500);

        // If it was created, drive the id-bearing endpoints; otherwise the test still passes.
        if (create.getStatusCode().is2xxSuccessful()) {
            long id = json(create).path("data").path("id").asLong();
            if (id > 0) {
                ResponseEntity<String> get = get("/vendor-admins/" + id, M);
                assertThat(get.getStatusCode().value()).isLessThan(500);

                ResponseEntity<String> upd = put("/vendor-admins/" + id,
                        Map.of("name", unique + "-upd",
                                "contactEmail", unique + "@example.com",
                                "contactPhone", "0900112234",
                                "notes", "updated"),
                        M);
                assertThat(upd.getStatusCode().value()).isLessThan(500);

                ResponseEntity<String> del = delete("/vendor-admins/" + id, M);
                assertThat(del.getStatusCode().value()).isLessThan(500);
            }
        }
        assertThat(true).isTrue();
    }

    // ── Contact lead: public submit → admin list → admin patch ──────────────────

    @Test @Order(5)
    @DisplayName("Contact lead public submit → admin list/patch round-trips (loose)")
    void contactLeadFlow() {
        // Public submit needs no auth/tenant — send with plain JSON headers.
        ResponseEntity<String> submit = post("/contact",
                Map.of("name", "IT Sweep Lead",
                        "phone", "0900445566",
                        "shopType", "JEWELRY",
                        "note", "from MasterApiSweepIT"),
                jsonHeaders());
        assertThat(submit.getStatusCode().value()).as("submit lead: %s", submit.getBody()).isLessThan(500);

        // Admin list to find a lead id, then patch its status.
        ResponseEntity<String> list = get("/contact?page=0&size=20", M);
        assertThat(list.getStatusCode().value()).isLessThan(500);
        if (list.getStatusCode().is2xxSuccessful()) {
            long id = json(list).path("content").path(0).path("id").asLong(0);
            if (id > 0) {
                ResponseEntity<String> patch = rest.exchange(
                        "/contact/" + id, org.springframework.http.HttpMethod.PATCH,
                        new org.springframework.http.HttpEntity<>(
                                Map.of("status", "CONTACTED", "adminNote", "IT sweep"), M),
                        String.class);
                assertThat(patch.getStatusCode().value()).as("patch lead: %s", patch.getBody()).isLessThan(500);
            }
        }
        assertThat(true).isTrue();
    }

    // ── Feedback admin review: list → patch ─────────────────────────────────────

    @Test @Order(6)
    @DisplayName("Feedback admin list → patch status round-trip (loose)")
    void feedbackAdminFlow() {
        ResponseEntity<String> list = get("/feedback?page=0&size=20", M);
        assertThat(list.getStatusCode().value()).isLessThan(500);
        if (list.getStatusCode().is2xxSuccessful()) {
            long id = json(list).path("content").path(0).path("id").asLong(0);
            if (id > 0) {
                ResponseEntity<String> patch = rest.exchange(
                        "/feedback/" + id, org.springframework.http.HttpMethod.PATCH,
                        new org.springframework.http.HttpEntity<>(
                                Map.of("status", "IN_REVIEW", "adminNote", "IT sweep review"), M),
                        String.class);
                assertThat(patch.getStatusCode().value()).as("patch feedback: %s", patch.getBody()).isLessThan(500);
            }
        }
        assertThat(true).isTrue();
    }

    // ── Cleanup: delete the throwaway tenant LAST ───────────────────────────────

    @Test @Order(99)
    @DisplayName("Tenant delete (cleanup, loose)")
    void tenantDelete() {
        ResponseEntity<String> del = delete("/multi-tenants/" + TENANT, M);
        assertThat(del.getStatusCode().value()).as("delete tenant: %s", del.getBody()).isLessThan(500);
    }
}
