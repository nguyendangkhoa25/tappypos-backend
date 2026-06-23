package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
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
 * Provisioning verification for the new BUILDING_MATERIALS (VLXD) shop type and the
 * CUSTOMER_DEBT (công nợ) feature. Boots the full stack on a real Postgres (Testcontainers)
 * with all Flyway migrations (incl. V019 customer_debt + V020 product alt-unit) applied,
 * provisions a BUILDING_MATERIALS tenant via the real master API, then asserts:
 *   - the building_materials.sql seed ran (HARDWARE type, VLXD categories, sample products,
 *     HARDWARE EAV attributes),
 *   - CUSTOMER_DEBT was wired onto the SHOP_OWNER role,
 *   - a credit sale + repayment round-trips through the CustomerDebt API under tenant RLS.
 *
 * Requires Docker. Named *IntegrationTest so it runs under mvn test (Surefire).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Provisioning — BUILDING_MATERIALS shop type + CUSTOMER_DEBT on real Postgres")
class BuildingMaterialsProvisioningIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tappy_pos_vlxd_it")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration");
        r.add("spring.flyway.validate-on-migrate", () -> "false");
        r.add("app.encryption.key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        r.add("spring.jpa.properties.hibernate.connection.provider_disables_autocommit", () -> "false");
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private static final String MASTER_USER = "Administrator";
    private static final String MASTER_PASS = "12345678x@X";
    private static final String TENANT      = "it-vlxd-shop";
    private static final String SHOP_ADMIN  = "itvlxdadmin";
    private static final String SHOP_PASS   = "admin123";

    static String masterToken;
    static String shopToken;

    private HttpHeaders masterHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Tenant-ID", "master");
        h.setBearerAuth(masterToken);
        return h;
    }

    private HttpHeaders shopHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Tenant-ID", TENANT);
        h.setBearerAuth(shopToken);
        return h;
    }

    @Test
    @Order(1)
    @DisplayName("V019/V020 migrations applied: customer_debt + debt_payment tables and product alt-unit columns exist")
    void migrationsApplied() {
        assertThat(tableExists("customer_debt")).isTrue();
        assertThat(tableExists("debt_payment")).isTrue();
        assertThat(columnExists("debt_payment", "updated_at")).isTrue();   // BaseEntity column (BLOCKING-1 fix)
        assertThat(columnExists("debt_payment", "deleted")).isTrue();
        assertThat(columnExists("product", "alt_unit")).isTrue();
        assertThat(columnExists("product", "alt_unit_factor")).isTrue();
        // V021 — sell-in-alt-unit line columns.
        assertThat(columnExists("cart_items", "sell_unit")).isTrue();
        assertThat(columnExists("cart_items", "unit_factor")).isTrue();
        assertThat(columnExists("order_items", "sell_unit")).isTrue();
        assertThat(columnExists("order_items", "unit_factor")).isTrue();
        // CUSTOMER_DEBT feature row seeded by V019.
        Integer feats = jdbc.queryForObject(
                "select count(*) from features where name = 'CUSTOMER_DEBT'", Integer.class);
        assertThat(feats).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("Master login → JWT")
    void masterLogin() throws Exception {
        var body = Map.of("username", MASTER_USER, "password", MASTER_PASS);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = rest.postForEntity(
                "/auth/login/force", new HttpEntity<>(body, h), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        masterToken = mapper.readTree(res.getBody()).path("data").path("accessToken").asText(null);
        assertThat(masterToken).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("POST /multi-tenants provisions BUILDING_MATERIALS, runs the seed, wires CUSTOMER_DEBT")
    void provisionBuildingMaterials() {
        assertThat(masterToken).as("requires master login").isNotBlank();

        var body = Map.of(
                "tenantId", TENANT,
                "name", "IT VLXD Shop",
                "shopType", "BUILDING_MATERIALS",
                "adminUsername", SHOP_ADMIN,
                "adminPassword", SHOP_PASS,
                "features", List.of("DASHBOARD", "POS", "ORDER", "PRODUCT", "INVENTORY",
                        "CUSTOMER", "CUSTOMER_DEBT", "REVENUE"),
                "expirationDate", "2030-12-31");

        ResponseEntity<String> res = rest.postForEntity(
                "/multi-tenants", new HttpEntity<>(body, masterHeaders()), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful())
                .as("create tenant: %s", res.getBody()).isTrue();

        // Tenant persisted with CUSTOMER_DEBT in its feature CSV.
        String features = jdbc.queryForObject(
                "select features from tenants where tenant_id = ?", String.class, TENANT);
        assertThat(features).contains("CUSTOMER_DEBT");

        // CUSTOMER_DEBT mapped onto SHOP_OWNER (full ROLE_FEATURES → role_features pipeline).
        Integer roleFeat = jdbc.queryForObject("""
                select count(*) from role_features rf
                join roles r    on r.id = rf.role_id
                join features f on f.id = rf.feature_id
                where r.tenant_id = ? and r.name = 'SHOP_OWNER' and f.name = 'CUSTOMER_DEBT'
                """, Integer.class, TENANT);
        assertThat(roleFeat).as("CUSTOMER_DEBT mapped to SHOP_OWNER").isEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("building_materials.sql seeded HARDWARE type, VLXD categories, sample products, attributes")
    void seedApplied() {
        // HARDWARE product type for this tenant.
        Integer hw = jdbc.queryForObject(
                "select count(*) from product_type where tenant_id = ? and code = 'HARDWARE'",
                Integer.class, TENANT);
        assertThat(hw).isEqualTo(1);

        // Parent VLXD categories (Xi măng, Sắt thép, …) — at least 8 parents seeded.
        Integer cats = jdbc.queryForObject(
                "select count(*) from category where tenant_id = ? and parent_id is null", Integer.class, TENANT);
        assertThat(cats).isGreaterThanOrEqualTo(8);

        // Sample products (VLXD-DEMO-*).
        Integer prods = jdbc.queryForObject(
                "select count(*) from product where tenant_id = ? and sku like 'VLXD-DEMO-%'", Integer.class, TENANT);
        assertThat(prods).isGreaterThanOrEqualTo(20);

        // HARDWARE EAV attribute seeded (e.g. standard_grade / mác).
        Integer attr = jdbc.queryForObject("""
                select count(*) from attribute_definition ad
                join product_type pt on pt.id = ad.product_type_id
                where ad.tenant_id = ? and pt.code = 'HARDWARE' and ad.code = 'standard_grade'
                """, Integer.class, TENANT);
        assertThat(attr).isEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("Công nợ round-trip: shop login → create debt → record payment under tenant RLS")
    void debtRoundTrip() throws Exception {
        // Shop admin login (no X-Tenant-ID on login; backend resolves tenant).
        var loginBody = Map.of("username", SHOP_ADMIN, "password", SHOP_PASS);
        HttpHeaders lh = new HttpHeaders();
        lh.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> loginRes = rest.postForEntity(
                "/auth/login/force", new HttpEntity<>(loginBody, lh), String.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        shopToken = mapper.readTree(loginRes.getBody()).path("data").path("accessToken").asText(null);
        assertThat(shopToken).isNotBlank();

        // The seeded walk-in customer ("Khách lẻ", phone 0000000000) exists for the tenant.
        Long customerId = jdbc.queryForObject(
                "select id from customers where tenant_id = ? order by id limit 1", Long.class, TENANT);
        assertThat(customerId).isNotNull();

        // Create a debt (ghi nợ) of 1,000,000đ.
        var debtBody = Map.of("customerId", customerId, "amount", 1_000_000, "note", "IT credit sale");
        ResponseEntity<String> createRes = rest.postForEntity(
                "/customer-debts", new HttpEntity<>(debtBody, shopHeaders()), String.class);
        assertThat(createRes.getStatusCode().is2xxSuccessful())
                .as("create debt: %s", createRes.getBody()).isTrue();

        // Total outstanding = 1,000,000.
        ResponseEntity<String> totalRes = rest.exchange(
                "/customer-debts/total", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        JsonNode total = mapper.readTree(totalRes.getBody()).path("data");
        assertThat(total.asDouble()).isEqualTo(1_000_000d);

        // Record a partial repayment (thu nợ) of 400,000.
        var payBody = Map.of("customerId", customerId, "amount", 400_000, "method", "CASH");
        ResponseEntity<String> payRes = rest.postForEntity(
                "/customer-debts/payments", new HttpEntity<>(payBody, shopHeaders()), String.class);
        assertThat(payRes.getStatusCode().is2xxSuccessful())
                .as("record payment: %s", payRes.getBody()).isTrue();

        // Outstanding now 600,000, and the debt row is PARTIAL.
        ResponseEntity<String> total2 = rest.exchange(
                "/customer-debts/total", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        assertThat(mapper.readTree(total2.getBody()).path("data").asDouble()).isEqualTo(600_000d);

        String status = jdbc.queryForObject(
                "select status from customer_debt where tenant_id = ? and customer_id = ?",
                String.class, TENANT, customerId);
        assertThat(status).isEqualTo("PARTIAL");
    }

    @Test
    @Order(6)
    @DisplayName("Báo giá: save a quote (no stock deducted, hidden from orders) → convert (deducts stock, appears in orders)")
    void quoteFlow() throws Exception {
        assertThat(shopToken).as("requires shop login from the debt test").isNotBlank();

        // A seeded HARDWARE product with tracked stock.
        Long productId = jdbc.queryForObject(
                "select id from product where tenant_id = ? and sku = 'VLXD-DEMO-005'", Long.class, TENANT);
        assertThat(productId).isNotNull();
        Long stockBefore = jdbc.queryForObject(
                "select quantity_in_stock from inventory where tenant_id = ? and product_id = ? and deleted = false",
                Long.class, TENANT, productId);
        assertThat(stockBefore).isNotNull();

        // Build a cart with one unit of the product.
        ResponseEntity<String> initRes = rest.postForEntity(
                "/carts", new HttpEntity<>(shopHeaders()), String.class);
        String cartId = mapper.readTree(initRes.getBody()).path("data").path("cartId").asText();
        assertThat(cartId).isNotBlank();
        rest.postForEntity("/carts/" + cartId + "/items",
                new HttpEntity<>(Map.of("productId", productId, "quantity", 1), shopHeaders()), String.class);

        // Save as a quote (báo giá) — no payment.
        ResponseEntity<String> coRes = rest.postForEntity("/carts/" + cartId + "/checkout",
                new HttpEntity<>(Map.of("paymentMethod", "CASH", "amountPaid", 0, "quote", true), shopHeaders()), String.class);
        assertThat(coRes.getStatusCode().is2xxSuccessful()).as("save quote: %s", coRes.getBody()).isTrue();

        // The quote appears in /orders/quotes and carries the is_quote flag.
        ResponseEntity<String> quotesRes = rest.exchange(
                "/orders/quotes", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        JsonNode quotes = mapper.readTree(quotesRes.getBody()).path("data");
        assertThat(quotes.isArray()).isTrue();
        assertThat(quotes.size()).isEqualTo(1);
        long quoteId = quotes.get(0).path("id").asLong();
        assertThat(quotes.get(0).path("quote").asBoolean()).isTrue();

        // The quote stays PENDING — it must NOT be auto-completed (fix #1: no order.complete / stamp accrual).
        String quoteStatus = jdbc.queryForObject(
                "select status from orders where id = ? and tenant_id = ?", String.class, quoteId, TENANT);
        assertThat(quoteStatus).as("a saved quote must remain PENDING, not COMPLETED").isEqualTo("PENDING");

        // It is NOT in the normal orders list (the leak fix).
        ResponseEntity<String> ordersRes = rest.exchange(
                "/orders?page=0&size=50", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        JsonNode orderContent = mapper.readTree(ordersRes.getBody()).path("data").path("content");
        boolean quoteInOrders = false;
        for (JsonNode o : orderContent) if (o.path("id").asLong() == quoteId) quoteInOrders = true;
        assertThat(quoteInOrders).as("quote must be hidden from the orders list").isFalse();

        // Stock is unchanged while it is only a quote.
        Long stockDuringQuote = jdbc.queryForObject(
                "select quantity_in_stock from inventory where tenant_id = ? and product_id = ? and deleted = false",
                Long.class, TENANT, productId);
        assertThat(stockDuringQuote).isEqualTo(stockBefore);

        // Convert the quote → real order: stock is deducted and it now appears in the orders list.
        ResponseEntity<String> convRes = rest.postForEntity(
                "/orders/" + quoteId + "/convert-quote", new HttpEntity<>(shopHeaders()), String.class);
        assertThat(convRes.getStatusCode().is2xxSuccessful()).as("convert quote: %s", convRes.getBody()).isTrue();

        Long stockAfter = jdbc.queryForObject(
                "select quantity_in_stock from inventory where tenant_id = ? and product_id = ? and deleted = false",
                Long.class, TENANT, productId);
        assertThat(stockAfter).isEqualTo(stockBefore - 1);

        // No longer a quote; appears in the orders list.
        ResponseEntity<String> quotes2 = rest.exchange(
                "/orders/quotes", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        assertThat(mapper.readTree(quotes2.getBody()).path("data").size()).isEqualTo(0);

        ResponseEntity<String> orders2 = rest.exchange(
                "/orders?page=0&size=50", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        JsonNode content2 = mapper.readTree(orders2.getBody()).path("data").path("content");
        boolean nowInOrders = false;
        for (JsonNode o : content2) if (o.path("id").asLong() == quoteId) nowInOrders = true;
        assertThat(nowInOrders).as("converted quote must appear in the orders list").isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("Báo giá vượt tồn: a quote above stock saves, but converting it is rejected (no oversell)")
    void quoteOverStockCannotConvert() throws Exception {
        assertThat(shopToken).as("requires shop login from the debt test").isNotBlank();

        Long productId = jdbc.queryForObject(
                "select id from product where tenant_id = ? and sku = 'VLXD-DEMO-006'", Long.class, TENANT);
        assertThat(productId).isNotNull();
        Long stockBefore = jdbc.queryForObject(
                "select quantity_in_stock from inventory where tenant_id = ? and product_id = ? and deleted = false",
                Long.class, TENANT, productId);
        assertThat(stockBefore).isNotNull();

        // Quote a quantity far beyond stock — allowed at quote time (báo giá).
        ResponseEntity<String> initRes = rest.postForEntity(
                "/carts", new HttpEntity<>(shopHeaders()), String.class);
        String cartId = mapper.readTree(initRes.getBody()).path("data").path("cartId").asText();
        long overQty = stockBefore + 100;
        ResponseEntity<String> addRes = rest.postForEntity("/carts/" + cartId + "/items",
                new HttpEntity<>(Map.of("productId", productId, "quantity", overQty, "quote", true), shopHeaders()), String.class);
        assertThat(addRes.getStatusCode().is2xxSuccessful())
                .as("a quote line may exceed stock: %s", addRes.getBody()).isTrue();

        ResponseEntity<String> coRes = rest.postForEntity("/carts/" + cartId + "/checkout",
                new HttpEntity<>(Map.of("paymentMethod", "CASH", "amountPaid", 0, "quote", true), shopHeaders()), String.class);
        assertThat(coRes.getStatusCode().is2xxSuccessful()).as("save over-stock quote: %s", coRes.getBody()).isTrue();

        ResponseEntity<String> quotesRes = rest.exchange(
                "/orders/quotes", HttpMethod.GET, new HttpEntity<>(shopHeaders()), String.class);
        long quoteId = mapper.readTree(quotesRes.getBody()).path("data").get(0).path("id").asLong();

        // Converting must be rejected (re-validates stock) — not silently completed with no deduction.
        ResponseEntity<String> convRes = rest.postForEntity(
                "/orders/" + quoteId + "/convert-quote", new HttpEntity<>(shopHeaders()), String.class);
        assertThat(convRes.getStatusCode().is4xxClientError())
                .as("converting an over-stock quote must be rejected: %s", convRes.getBody()).isTrue();

        // Stock untouched and the order is still a pending quote (no oversell).
        Long stockAfter = jdbc.queryForObject(
                "select quantity_in_stock from inventory where tenant_id = ? and product_id = ? and deleted = false",
                Long.class, TENANT, productId);
        assertThat(stockAfter).as("stock must be unchanged after a rejected conversion").isEqualTo(stockBefore);
        String status = jdbc.queryForObject(
                "select status from orders where id = ? and tenant_id = ?", String.class, quoteId, TENANT);
        assertThat(status).isEqualTo("PENDING");
        Boolean stillQuote = jdbc.queryForObject(
                "select is_quote from orders where id = ? and tenant_id = ?", Boolean.class, quoteId, TENANT);
        assertThat(stillQuote).isTrue();
    }

    private boolean tableExists(String name) {
        Integer c = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = ?", Integer.class, name);
        return c != null && c > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer c = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = ? and column_name = ?",
                Integer.class, table, column);
        return c != null && c > 0;
    }
}
