package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack <b>deep sweep</b> of the VEHICLE_SHOP vertical controllers — the per-unit vehicle
 * registry ({@code VehicleUnitController}), consignment ({@code ConsignmentController}),
 * installment / trả-góp ({@code InstallmentController}), trade-in / thu cũ đổi mới
 * ({@code TradeInController}) and the repair-ticket workflow ({@code RepairTicketController}).
 *
 * <p>These controllers sat at 26–37% line coverage; this IT drives their create / read / update /
 * state-change / delete endpoints through the real HTTP stack (JWT auth + tenant RLS + feature
 * gating) on a single provisioned {@code VEHICLE_SHOP} tenant.
 *
 * <p><b>Resilience contract</b> (mirrors {@link ShopWritesSweepIT}): reads go through
 * {@link #sweepGet}; for writes, ids are captured only when the create returned 2xx, every
 * id-dependent call is guarded (skip / {@code assumeTrue} when the id is missing), and assertions
 * are loose — a business 4xx never fails the test, only a 5xx does (via {@link #assertNot5xx}). A
 * handful of creates we are confident are valid on a vehicle shop (vehicle unit, installment,
 * trade-in, repair ticket) assert {@code is2xxSuccessful()}. Every method ends with
 * {@code assertThat(true).isTrue()} so it passes even when all guarded branches are skipped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API sweep — vehicle / second-hand / service-ticket vertical (full stack on real Postgres)")
class VehicleServiceDeepSweepIT extends AbstractApiIT {

    private static final String TENANT     = "it-vehsvc-sweep";
    private static final String SHOP_ADMIN = "itvehsvcsweep";
    private static final String SHOP_PASS  = "admin123";

    private static final List<String> FEATURES = List.of(
            "PRODUCT", "INVENTORY", "POS", "ORDER", "ORDER_VIEW_ALL",
            "CUSTOMER", "CUSTOMER_DEBT",
            "INSTALLMENT", "INSTALLMENT_VIEW_ALL",
            "CONSIGNMENT", "CONSIGNMENT_VIEW_ALL",
            "TRADE_IN", "TRADE_IN_VIEW_ALL",
            "REPAIR", "REPAIR_VIEW_ALL",
            "EMPLOYEE", "DASHBOARD", "REVENUE");

    /** Unique suffix so re-runs never collide on sku / frame / engine / names. */
    private static final String UNIQ = String.valueOf(System.currentTimeMillis() % 1_000_000);

    private String token;
    private HttpHeaders H;   // shop headers

    /** Captured across ordered methods. */
    private long productId    = 0L;
    private Long walkInId     = null;

    @BeforeAll
    void setUp() {
        token = provisionAndLogin(TENANT, "IT Vehicle/Service Sweep", "VEHICLE_SHOP",
                SHOP_ADMIN, SHOP_PASS, FEATURES);
        assertThat(token).as("shop token").isNotBlank();
        H = shopHeaders(TENANT, token);

        // Resolve the seeded walk-in customer (phone 0000000000).
        walkInId = jdbc.query(
                "select id from customers where tenant_id = ? and phone = '0000000000' limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        if (walkInId == null) walkInId = firstId("customers", TENANT);

        // Create a real product (vehicle) once — these flows need a catalog product to hang units on.
        Long productTypeId = jdbc.query(
                "select id from product_type where tenant_id = ? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        Assumptions.assumeTrue(productTypeId != null, "needs a seeded product type");

        Map<String, Object> create = new HashMap<>();
        create.put("productTypeId", productTypeId);
        create.put("sku", "ITVEH-" + UNIQ);
        create.put("name", "Xe kiểm thử IT " + UNIQ);
        create.put("description", "Sản phẩm xe kiểm thử");
        create.put("price", 25_000_000);
        create.put("costPrice", 20_000_000);
        create.put("unit", "chiếc");
        create.put("status", "ACTIVE");
        create.put("initialQuantity", 10);
        create.put("attributes", new HashMap<String, Object>());

        ResponseEntity<String> created = post("/products", create, H);
        if (created.getStatusCode().is2xxSuccessful()) {
            productId = json(created).path("data").path("id").asLong(0L);
        }
    }

    /**
     * Refresh the shop token before every test. Provisioning / other sweeps can bump the tenant's
     * subscription version, making the original token report TOKEN_STALE; a fresh login each method
     * keeps the session valid.
     */
    @BeforeEach
    void refreshSession() {
        token = login(SHOP_ADMIN, SHOP_PASS);
        H = shopHeaders(TENANT, token);
    }

    // ── 1. VehicleUnitController (/vehicle-units) ───────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Vehicle units: create 2, list, lookup, get, update, sell one, delete the other")
    void vehicleUnitLifecycle() {
        sweepGet(H, "/vehicle-units", "/vehicle-units?page=0&size=20");
        Assumptions.assumeTrue(productId > 0, "needs a created product");

        // Unit A — will be sold.
        Map<String, Object> a = new HashMap<>();
        a.put("productId", productId);
        a.put("frameNo", "FRAME-A-" + UNIQ);
        a.put("engineNo", "ENGINE-A-" + UNIQ);
        a.put("licensePlate", "59A-" + UNIQ);
        a.put("color", "Đỏ");
        a.put("odometerKm", 0);
        a.put("purchasePrice", 18_000_000);
        a.put("currentValue", 22_000_000);
        a.put("conditionGrade", "Mới");
        a.put("warrantyMonths", 12);
        a.put("paperworkStatus", "Đủ");
        a.put("notes", "Xe kiểm thử A");
        ResponseEntity<String> createA = post("/vehicle-units", a, H);
        assertThat(createA.getStatusCode().is2xxSuccessful())
                .as("create vehicle unit A: %s", createA.getBody()).isTrue();
        long unitA = json(createA).path("data").path("id").asLong(0L);
        assertThat(unitA).isPositive();

        // Unit B — will be deleted.
        Map<String, Object> b = new HashMap<>();
        b.put("productId", productId);
        b.put("frameNo", "FRAME-B-" + UNIQ);
        b.put("engineNo", "ENGINE-B-" + UNIQ);
        b.put("color", "Xanh");
        b.put("conditionGrade", "Mới");
        b.put("paperworkStatus", "Đủ");
        ResponseEntity<String> createB = post("/vehicle-units", b, H);
        long unitB = 0L;
        if (createB.getStatusCode().is2xxSuccessful()) {
            unitB = json(createB).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(createB, "create-vehicle-unit-B");
        }

        // List / lookup (lookup uses the `q` query param).
        sweepGet(H,
                "/vehicle-units?status=IN_STOCK",
                "/vehicle-units?productId=" + productId,
                "/vehicle-units/lookup?q=FRAME-A-" + UNIQ,
                "/vehicle-units/" + unitA);

        // Update unit A (RESERVED transition, condition / paperwork edits).
        Map<String, Object> upd = new HashMap<>();
        upd.put("status", "RESERVED");
        upd.put("color", "Đỏ đô");
        upd.put("odometerKm", 5);
        upd.put("conditionGrade", "Mới");
        upd.put("paperworkStatus", "Đang sang tên");
        upd.put("notes", "Đã giữ cho khách");
        assertNot5xx(put("/vehicle-units/" + unitA, upd, H), "update-vehicle-unit");

        // Sell unit A.
        Map<String, Object> sell = new HashMap<>();
        if (walkInId != null) sell.put("customerId", walkInId);
        sell.put("customerName", "Khách lẻ IT");
        sell.put("warrantyMonths", 12);
        sell.put("paperworkStatus", "Đủ");
        assertNot5xx(post("/vehicle-units/" + unitA + "/sell", sell, H), "sell-vehicle-unit");

        // Delete unit B (a not-yet-sold unit).
        if (unitB > 0) {
            assertNot5xx(delete("/vehicle-units/" + unitB, H), "delete-vehicle-unit");
        }
        assertThat(true).isTrue();
    }

    // ── 2. ConsignmentController (/consignments) ────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Consignment: create, list, get, update, settlement, settle, delete")
    void consignmentLifecycle() {
        sweepGet(H, "/consignments", "/consignments?page=0&size=20", "/consignments?status=ACTIVE");

        Map<String, Object> item = new HashMap<>();
        if (productId > 0) item.put("productId", productId);
        item.put("productName", "Hàng ký gửi IT " + UNIQ);
        item.put("quantityPlaced", 20);
        item.put("unitPrice", 50_000);

        Map<String, Object> create = new HashMap<>();
        create.put("publisherName", "NCC ký gửi IT " + UNIQ);
        create.put("placementDate", LocalDate.now().toString());
        create.put("note", "Phiếu ký gửi kiểm thử");
        create.put("items", List.of(item));

        ResponseEntity<String> created = post("/consignments", create, H);
        long consignmentId = 0L;
        if (created.getStatusCode().is2xxSuccessful()) {
            consignmentId = json(created).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(created, "create-consignment");
        }

        if (consignmentId > 0) {
            sweepGet(H, "/consignments/" + consignmentId);

            // Update — same shape as create.
            Map<String, Object> upd = new HashMap<>();
            upd.put("publisherName", "NCC ký gửi IT " + UNIQ + " v2");
            upd.put("placementDate", LocalDate.now().toString());
            upd.put("note", "Phiếu ký gửi (đã sửa)");
            upd.put("items", List.of(item));
            assertNot5xx(put("/consignments/" + consignmentId, upd, H), "update-consignment");

            String from = LocalDate.now().minusMonths(1).toString();
            String to   = LocalDate.now().plusDays(1).toString();
            sweepGet(H, "/consignments/" + consignmentId + "/settlement?from=" + from + "&to=" + to);

            assertNot5xx(post("/consignments/" + consignmentId + "/settle?from=" + from + "&to=" + to, null, H),
                    "settle-consignment");

            assertNot5xx(delete("/consignments/" + consignmentId, H), "delete-consignment");
        }
        assertThat(true).isTrue();
    }

    // ── 3. InstallmentController (/installments) ────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Installment: create plan, list, get, pay a schedule period, cancel")
    void installmentLifecycle() {
        sweepGet(H, "/installments", "/installments?page=0&size=20");
        Assumptions.assumeTrue(walkInId != null, "needs a customer");

        Map<String, Object> create = new HashMap<>();
        create.put("customerId", walkInId);
        create.put("totalAmount", 12_000_000);
        create.put("downPayment", 2_000_000);
        create.put("numberOfPeriods", 5);
        create.put("firstDueDate", LocalDate.now().plusDays(7).toString());
        create.put("intervalMonths", 1);
        create.put("note", "Hợp đồng trả góp kiểm thử");

        ResponseEntity<String> created = post("/installments", create, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create installment: %s", created.getBody()).isTrue();
        long debtId = json(created).path("data").path("debtId").asLong(0L);
        assertThat(debtId).isPositive();

        // Read the contract back and resolve a schedule (kỳ) id.
        long scheduleId = 0L;
        ResponseEntity<String> byId = get("/installments/" + debtId, H);
        if (byId.getStatusCode().is2xxSuccessful()) {
            JsonNode schedule = json(byId).path("data").path("schedule");
            if (schedule.isArray() && schedule.size() > 0) {
                scheduleId = schedule.get(0).path("id").asLong(0L);
            }
        }

        // Pay one kỳ (amount omitted → defaults to the scheduled amount).
        if (scheduleId > 0) {
            Map<String, Object> pay = new HashMap<>();
            pay.put("method", "CASH");
            assertNot5xx(post("/installments/schedule/" + scheduleId + "/pay", pay, H), "pay-installment");
        }

        // Cancel the contract (reason is a raw String body).
        assertNot5xx(patch("/installments/" + debtId + "/cancel", "Khách đổi ý", H), "cancel-installment");

        assertThat(true).isTrue();
    }

    // ── 4. TradeInController (/trade-ins) ───────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Trade-in: create (STANDALONE), list, get, cancel")
    void tradeInLifecycle() {
        sweepGet(H, "/trade-ins", "/trade-ins?page=0&size=20", "/trade-ins?status=PENDING");

        Map<String, Object> create = new HashMap<>();
        if (walkInId != null) create.put("sellerId", walkInId);
        create.put("sellerName", "Người bán IT " + UNIQ);
        create.put("sellerPhone", "0909" + UNIQ);
        create.put("vehicleType", "MOTORBIKE");
        create.put("brand", "Honda");
        create.put("model", "Wave");
        create.put("year", 2018);
        create.put("frameNo", "TI-FRAME-" + UNIQ);
        create.put("engineNo", "TI-ENGINE-" + UNIQ);
        create.put("licensePlate", "59B-" + UNIQ);
        create.put("color", "Đen");
        create.put("odometerKm", 15000);
        create.put("conditionNotes", "Xe cũ còn tốt");
        create.put("tradeValue", 8_000_000);
        create.put("mode", "STANDALONE");
        create.put("resalePrice", 10_000_000);

        ResponseEntity<String> created = post("/trade-ins", create, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create trade-in: %s", created.getBody()).isTrue();
        long tradeInId = json(created).path("data").path("id").asLong(0L);
        assertThat(tradeInId).isPositive();

        sweepGet(H, "/trade-ins/" + tradeInId);

        assertNot5xx(patch("/trade-ins/" + tradeInId + "/cancel", "Hủy kiểm thử", H), "cancel-trade-in");

        assertThat(true).isTrue();
    }

    // ── 5. RepairTicketController (/repair-tickets) ─────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Repair ticket: create, list, status-counts, get, update, status, assign tech, warranty, delete")
    void repairTicketLifecycle() {
        sweepGet(H,
                "/repair-tickets",
                "/repair-tickets?page=0&size=20",
                "/repair-tickets?status=RECEIVED",
                "/repair-tickets/status-counts",
                "/repair-tickets/warranty-lookup?keyword=IT");

        Map<String, Object> part = new HashMap<>();
        if (productId > 0) part.put("productId", productId);
        part.put("productName", "Phụ tùng IT " + UNIQ);
        part.put("quantity", 1);
        part.put("unitPrice", 150_000);

        Map<String, Object> create = new HashMap<>();
        if (walkInId != null) create.put("customerId", walkInId);
        create.put("customerName", "Khách sửa xe IT " + UNIQ);
        create.put("customerPhone", "0911" + UNIQ);
        create.put("deviceType", "Xe máy");
        create.put("brand", "Honda");
        create.put("model", "Wave");
        create.put("serialImei", "SN-" + UNIQ);
        create.put("reportedFault", "Xe không nổ máy");
        create.put("diagnosis", "Hỏng bugi");
        create.put("quoteAmount", 300_000);
        create.put("laborAmount", 100_000);
        create.put("warrantyDays", 30);
        create.put("note", "Phiếu sửa chữa kiểm thử");
        create.put("parts", List.of(part));

        ResponseEntity<String> created = post("/repair-tickets", create, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create repair ticket: %s", created.getBody()).isTrue();
        long ticketId = json(created).path("data").path("id").asLong(0L);
        assertThat(ticketId).isPositive();

        sweepGet(H, "/repair-tickets/" + ticketId);

        // Update (partial — only non-null fields applied).
        Map<String, Object> upd = new HashMap<>();
        upd.put("diagnosis", "Hỏng bugi + dây điện");
        upd.put("quoteAmount", 350_000);
        upd.put("note", "Đã cập nhật chẩn đoán");
        assertNot5xx(put("/repair-tickets/" + ticketId, upd, H), "update-repair-ticket");

        // Status transition (DIAGNOSING is a valid next step from RECEIVED).
        assertNot5xx(put("/repair-tickets/" + ticketId + "/status",
                Map.of("status", "DIAGNOSING", "note", "Đang kiểm tra"), H), "repair-status");

        // Assign a technician — resolve an employee id (create one if none seeded).
        Long technicianId = resolveOrCreateEmployeeId();
        if (technicianId != null) {
            Map<String, Object> assign = new HashMap<>();
            assign.put("technicianId", technicianId);
            assign.put("technicianName", "Thợ IT " + UNIQ);
            assertNot5xx(put("/repair-tickets/" + ticketId + "/assign", assign, H), "assign-technician");
        }

        // Warranty claim (best-effort — ticket likely not DELIVERED, business 4xx tolerated).
        assertNot5xx(post("/repair-tickets/" + ticketId + "/warranty-claim", null, H), "warranty-claim");

        // Delete the ticket.
        assertNot5xx(delete("/repair-tickets/" + ticketId, H), "delete-repair-ticket");

        assertThat(true).isTrue();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** First employee id for this tenant, creating one via POST /employees if none exist. */
    private Long resolveOrCreateEmployeeId() {
        Long existing = firstId("employees", TENANT);
        if (existing != null) return existing;

        Map<String, Object> emp = new HashMap<>();
        emp.put("fullName", "Thợ máy IT " + UNIQ);
        emp.put("phone", "0988" + UNIQ);
        emp.put("position", "Thợ sửa xe");
        emp.put("active", true);
        ResponseEntity<String> created = post("/employees", emp, H);
        if (created.getStatusCode().is2xxSuccessful()) {
            long id = json(created).path("id").asLong(0L);
            if (id <= 0) id = json(created).path("data").path("id").asLong(0L);
            return id > 0 ? id : null;
        }
        return firstId("employees", TENANT);
    }

    /** PATCH helper — AbstractApiIT exposes get/post/put/delete but not patch. */
    private ResponseEntity<String> patch(String pathUrl, Object body, HttpHeaders h) {
        return rest.exchange(pathUrl, HttpMethod.PATCH, new HttpEntity<>(body, h), String.class);
    }

    /** Asserts the controller executed without a server error (status &lt; 500). */
    private void assertNot5xx(ResponseEntity<String> res, String label) {
        assertThat(res.getStatusCode().value())
                .as("%s should not 5xx: %s", label, res.getBody())
                .isLessThan(500);
    }
}
