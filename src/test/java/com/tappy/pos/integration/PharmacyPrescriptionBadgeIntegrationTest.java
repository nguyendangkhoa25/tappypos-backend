package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof for the pharmacy prescription badge (PHARMACY_SHOP_TYPE_PLAN.md §4d).
 *
 * <p>Boots the full stack on a real PostgreSQL (Testcontainers + Flyway), provisions a real
 * {@code PHARMACY} tenant through {@code POST /multi-tenants} — which runs {@code pharmacy.sql}
 * (incl. the §12 {@code prescription_required} attribute-value seed) — then calls
 * {@code GET /inventory}, the exact endpoint the POS grid's {@code loadInventory} uses. A drug
 * comes back with {@code prescriptionRequired=true} iff the grid would render the "Kê đơn" badge,
 * so asserting the antibiotics are flagged (and an OTC item is not) confirms the badge renders.
 *
 * <p>Requires Docker. Named {@code *IntegrationTest} so it runs under Surefire / {@code mvn test}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("Pharmacy prescription badge — full stack on real Postgres")
class PharmacyPrescriptionBadgeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tappy_pos_pharmacy_it")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration");
        r.add("spring.flyway.validate-on-migrate", () -> "false");
        r.add("app.encryption.key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        r.add("spring.jpa.properties.hibernate.connection.provider_disables_autocommit", () -> "false");
    }

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    private static final String MASTER_USER = "Administrator";
    private static final String MASTER_PASS = "12345678x@X";
    private static final String IT_TENANT   = "it-pharmacy";
    private static final String SHOP_ADMIN  = "itpharmaadmin";
    private static final String SHOP_PASS   = "admin123";

    private String login(String username, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = rest.postForEntity(
                "/auth/login/force",
                new HttpEntity<>(Map.of("username", username, "password", password), h),
                String.class);
        assertThat(res.getStatusCode()).as("login %s: %s", username, res.getBody()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(res.getBody()).path("data").path("accessToken").asText(null);
    }

    @Test
    @DisplayName("provision PHARMACY → GET /inventory flags the kê-đơn antibiotics, not the OTC items")
    void pharmacyInventory_flagsPrescriptionDrugs() throws Exception {
        // 1. Master logs in and provisions a real pharmacy (runs pharmacy.sql incl. §12).
        String masterToken = login(MASTER_USER, MASTER_PASS);
        assertThat(masterToken).as("master token").isNotBlank();

        HttpHeaders prov = new HttpHeaders();
        prov.setContentType(MediaType.APPLICATION_JSON);
        prov.set("X-Tenant-ID", "master");
        prov.setBearerAuth(masterToken);
        var body = Map.of(
                "tenantId", IT_TENANT,
                "name", "IT Pharmacy",
                "shopType", "PHARMACY",
                "adminUsername", SHOP_ADMIN,
                "adminPassword", SHOP_PASS,
                "features", List.of("DASHBOARD", "POS", "ORDER", "PRODUCT", "INVENTORY"),
                "expirationDate", "2030-12-31");
        ResponseEntity<String> provRes = rest.postForEntity(
                "/multi-tenants", new HttpEntity<>(body, prov), String.class);
        assertThat(provRes.getStatusCode().is2xxSuccessful())
                .as("provision pharmacy: %s", provRes.getBody()).isTrue();

        // 2. Shop admin logs in (JWT now carries INVENTORY) and reads the POS inventory list.
        String shopToken = login(SHOP_ADMIN, SHOP_PASS);
        assertThat(shopToken).as("shop token").isNotBlank();

        HttpHeaders h = new HttpHeaders();
        h.set("X-Tenant-ID", IT_TENANT);
        h.setBearerAuth(shopToken);
        ResponseEntity<String> invRes = rest.exchange(
                "/inventory?page=0&size=50", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(invRes.getStatusCode())
                .as("GET /inventory: %s", invRes.getBody()).isEqualTo(HttpStatus.OK);

        // 3. Map each seeded drug SKU → prescriptionRequired as the frontend grid would receive it.
        Map<String, Boolean> rxBySku = new HashMap<>();
        for (JsonNode item : mapper.readTree(invRes.getBody()).path("data").path("content")) {
            rxBySku.put(item.path("productSku").asText(), item.path("prescriptionRequired").asBoolean(false));
        }
        assertThat(rxBySku).as("seeded pharmacy inventory should be returned").isNotEmpty();

        // Antibiotics seeded with prescription_required = TRUE → badge renders.
        assertThat(rxBySku.get("PHA-DEMO-004")).as("Amoxicillin is kê đơn").isTrue();
        assertThat(rxBySku.get("PHA-DEMO-005")).as("Augmentin is kê đơn").isTrue();
        // An OTC item seeded FALSE → no badge.
        assertThat(rxBySku.get("PHA-DEMO-001")).as("Paracetamol is OTC").isFalse();
    }
}
