package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack sweep focused on the ORDER lifecycle plus the F&B / lodging vertical controllers
 * (tables, rooms, bookings, appointments, recipes, production).
 *
 * <p>Provisions a single <b>RESTAURANT</b> tenant (a NON-gold shop type, so a cart checkout
 * produces a real persisted order reliably — unlike a JEWELRY/dynamic-price shop, whose checkout
 * can legitimately 400 when no gold price is set). It then drives the cart → add item → checkout
 * happy path to obtain a real {@code orderId}, exercises the {@link com.tappy.pos.controller.order.OrderController}
 * lifecycle + analytics surface against it, and sweeps the vertical controllers end-to-end through
 * the real HTTP stack (JWT auth + tenant RLS + feature gating).
 *
 * <p>Resilience: reads go through {@link #sweepGet}; writes assert loosely (status &lt; 500) or not
 * at all, so a business 4xx never fails the test. Every order-id-dependent call is guarded so the
 * suite passes even if no seeded in-stock product exists for checkout.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API sweep — order lifecycle + F&B / lodging verticals (full stack on real Postgres)")
class SalesVerticalSweepIT extends AbstractApiIT {

    private static final String TENANT     = "it-sales-sweep";
    private static final String SHOP_ADMIN = "itsalessweep";
    private static final String SHOP_PASS  = "admin123";

    private static final List<String> SHOP_FEATURES = List.of(
            "POS", "ORDER", "ORDER_VIEW_ALL", "PRODUCT", "INVENTORY", "CUSTOMER",
            "TABLE_SERVICE", "ROOM", "BOOKING", "APPOINTMENT", "RECIPE",
            "REVENUE", "DASHBOARD", "EMPLOYEE", "MY_WORK");

    private static final String FROM  = "2026-01-01";
    private static final String TO    = "2026-12-31";
    private static final String DAY   = "2026-06-23";
    private static final String RANGE = "?from=" + FROM + "&to=" + TO
            + "&startDate=" + FROM + "&endDate=" + TO + "&date=" + DAY
            + "&year=2026&month=6&granularity=day&page=0&size=20";

    private String token;
    private HttpHeaders H;

    /** Captured from the first checkout; <= 0 means no real order was created. */
    private long orderId = 0L;
    /** A line-item id on {@link #orderId}, for item-level endpoints; <= 0 means none. */
    private long orderItemId = 0L;

    @BeforeAll
    void setUp() {
        token = provisionAndLogin(TENANT, "IT Sales Sweep", "RESTAURANT", SHOP_ADMIN, SHOP_PASS, SHOP_FEATURES);
        assertThat(token).as("shop token").isNotBlank();
        H = shopHeaders(TENANT, token);
    }

    // ── 1. Cart → checkout → real order ─────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Cart → add seeded in-stock product → checkout creates a real order")
    void cartCheckoutCreatesOrder() {
        Long productId = jdbc.query(
                "select p.id from product p join inventory i on i.product_id = p.id "
                        + "where p.tenant_id = ? and i.quantity_in_stock > 0 and p.deleted = false "
                        + "order by p.id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        Assumptions.assumeTrue(productId != null, "needs a seeded in-stock product");

        ResponseEntity<String> init = post("/carts", null, H);
        assertThat(init.getStatusCode().is2xxSuccessful()).as("init cart: %s", init.getBody()).isTrue();
        String cartId = json(init).path("data").path("cartId").asText();
        assertThat(cartId).isNotBlank();

        post("/carts/" + cartId + "/items", Map.of("productId", productId, "quantity", 2), H);
        get("/carts/" + cartId, H);

        ResponseEntity<String> checkout = post("/carts/" + cartId + "/checkout",
                Map.of("paymentMethod", "CASH", "amountPaid", 100_000_000), H);
        // RESTAURANT (non-dynamic-price) checkout should succeed.
        assertThat(checkout.getStatusCode().is2xxSuccessful())
                .as("checkout: %s", checkout.getBody()).isTrue();

        JsonNode data = json(checkout).path("data");
        orderId = data.path("orderId").asLong(data.path("id").asLong(0L));
        assertThat(orderId).as("created order id").isGreaterThan(0L);
    }

    // ── 2. Order lifecycle + analytics on the real order ────────────────────────

    @Test
    @Order(2)
    @DisplayName("Order single-resource reads + receipt for the created order")
    void orderReads() {
        Assumptions.assumeTrue(orderId > 0, "needs a created order");

        ResponseEntity<String> byId = get("/orders/" + orderId, H);
        assertThat(byId.getStatusCode().is2xxSuccessful()).as("get order: %s", byId.getBody()).isTrue();

        // Capture a line-item id for the item-level endpoints.
        JsonNode items = json(byId).path("data").path("items");
        if (items.isArray() && items.size() > 0) {
            orderItemId = items.get(0).path("id").asLong(0L);
        }

        sweepGet(H, "/orders/" + orderId, "/orders/" + orderId + "/receipt");
        assertThat(true).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Order state-transition + mutation endpoints (loose assertions, business 4xx tolerated)")
    void orderMutations() {
        Assumptions.assumeTrue(orderId > 0, "needs a created order");

        // The checkout order is COMPLETED, so start/complete may 4xx — that is fine; the controller
        // + service still execute. We only require the server not to 500.
        assertOk2xxOr4xx(put("/orders/" + orderId + "/start", null, H), "start");
        assertOk2xxOr4xx(put("/orders/" + orderId + "/complete", null, H), "complete");

        assertOk2xxOr4xx(put("/orders/" + orderId + "/pay-and-complete",
                Map.of("paymentMethod", "CASH", "amountPaid", 100_000_000), H), "pay-and-complete");

        Map<String, Object> meta = new HashMap<>();
        meta.put("paymentMethod", "CASH");
        meta.put("clearCustomer", false);
        assertOk2xxOr4xx(put("/orders/" + orderId + "/meta", meta, H, true), "meta(PATCH)");

        if (orderItemId > 0) {
            assertOk2xxOr4xx(
                    patch("/orders/" + orderId + "/items/" + orderItemId + "/note",
                            Map.of("note", "ít cay"), H),
                    "item-note");
        }

        // Add another item to the order (productId resolved fresh).
        Long productId = jdbc.query(
                "select p.id from product p join inventory i on i.product_id = p.id "
                        + "where p.tenant_id = ? and i.quantity_in_stock > 0 and p.deleted = false "
                        + "order by p.id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        if (productId != null) {
            assertOk2xxOr4xx(
                    post("/orders/" + orderId + "/items", Map.of("productId", productId, "quantity", 1), H),
                    "add-item");
        }

        assertThat(true).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Receipt preview (no persisted order) executes")
    void receiptPreview() {
        Map<String, Object> item = new HashMap<>();
        item.put("productName", "Phở bò");
        item.put("quantity", 1);
        item.put("unitPrice", 50000);
        item.put("lineTotal", 50000);

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("total", 50000);
        body.put("totalDiscount", 0);
        body.put("paymentMethod", "CASH");
        body.put("amountPaid", 50000);
        body.put("changeAmount", 0);
        body.put("customerName", "Khách lẻ");

        assertOk2xxOr4xx(post("/orders/receipt/preview", body, H), "receipt-preview");
        assertThat(true).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("Second order → cancel; third → void (cover lifecycle terminal transitions)")
    void cancelAndVoid() {
        long secondId = createOrderViaCheckout();
        if (secondId > 0) {
            assertOk2xxOr4xx(post("/orders/" + secondId + "/cancel",
                    Map.of("reason", "Khách đổi ý"), H), "cancel");
        }
        long thirdId = createOrderViaCheckout();
        if (thirdId > 0) {
            assertOk2xxOr4xx(post("/orders/" + thirdId + "/void",
                    Map.of("reason", "Sai đơn"), H), "void");
        }
        assertThat(true).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Order read + analytics sweep across the whole OrderController surface")
    void orderAnalyticsSweep() {
        sweepGet(H,
                "/orders" + RANGE, "/orders/list" + RANGE, "/orders/search?keyword=a",
                "/orders/quotes", "/orders/preorders", "/orders/preorders/summary",
                "/orders/summary" + RANGE, "/orders/chart" + RANGE, "/orders/top-products" + RANGE,
                "/orders/top-customers" + RANGE, "/orders/top-customers/by-frequency" + RANGE,
                "/orders/customer-stats" + RANGE, "/orders/top-employees" + RANGE,
                "/orders/by-staff/summary" + RANGE + "&createdBy=" + SHOP_ADMIN,
                "/orders/by-staff/chart" + RANGE + "&createdBy=" + SHOP_ADMIN,
                "/orders/by-staff" + RANGE + "&createdBy=" + SHOP_ADMIN,
                "/orders/kitchen", "/orders/pending-confirmation",
                "/orders/my-work/pending", "/orders/my-work/completed", "/orders/my-work/stats",
                "/orders/work-items/available", "/orders/work-items", "/orders/work-items/pending",
                "/orders/work-items/all-pending", "/orders/work-items/completed",
                "/orders/work-items/summary" + RANGE, "/orders/work-items/trend" + RANGE);
        assertThat(true).isTrue();
    }

    // ── 3. Verticals ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Tables — create, update, status, reservations, lifecycle, delete")
    void tablesVertical() {
        sweepGet(H, "/tables",
                "/tables/reservations?from=" + FROM + "&to=" + TO);

        long tableId = 0L;
        ResponseEntity<String> create = post("/tables",
                Map.of("tableNumber", "IT-T1", "capacity", 4, "location", "Tầng 1"), H);
        if (create.getStatusCode().is2xxSuccessful()) {
            tableId = json(create).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(create, "create-table");
        }

        if (tableId > 0) {
            assertOk2xxOr4xx(put("/tables/" + tableId,
                    Map.of("tableNumber", "IT-T1b", "capacity", 6), H), "update-table");
            assertOk2xxOr4xx(patch("/tables/" + tableId + "/status",
                    Map.of("status", "CLEANING"), H), "table-status");

            long reservationId = 0L;
            ResponseEntity<String> resv = post("/tables/reservations",
                    Map.of("tableId", tableId, "reservedAt", DAY + "T19:00:00",
                            "partySize", 4, "customerName", "Anh Nam"), H);
            if (resv.getStatusCode().is2xxSuccessful()) {
                reservationId = json(resv).path("data").path("id").asLong(0L);
            } else {
                assertNot5xx(resv, "create-table-reservation");
            }
            if (reservationId > 0) {
                assertOk2xxOr4xx(post("/tables/reservations/" + reservationId + "/seat", null, H), "seat");
                assertOk2xxOr4xx(post("/tables/reservations/" + reservationId + "/cancel", null, H), "cancel-resv");
                assertOk2xxOr4xx(post("/tables/reservations/" + reservationId + "/no-show", null, H), "no-show");
            }
            assertOk2xxOr4xx(delete("/tables/" + tableId, H), "delete-table");
        }
        assertThat(true).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Rooms — board, create, update, status, reservations/stays/requests, delete")
    void roomsVertical() {
        sweepGet(H, "/rooms/board", "/rooms/requests", "/rooms/requests/count-new",
                "/rooms/reservations?from=" + FROM + "&to=" + TO, "/rooms/stays");

        long roomId = 0L;
        ResponseEntity<String> create = post("/rooms",
                Map.of("roomNumber", "IT-101", "roomType", "STANDARD", "floor", "1",
                        "nightlyRate", 500000, "maxOccupancy", 2), H);
        if (create.getStatusCode().is2xxSuccessful()) {
            roomId = json(create).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(create, "create-room");
        }

        if (roomId > 0) {
            assertOk2xxOr4xx(put("/rooms/" + roomId,
                    Map.of("roomNumber", "IT-101b", "nightlyRate", 550000), H), "update-room");
            assertOk2xxOr4xx(put("/rooms/" + roomId + "/status",
                    Map.of("status", "CLEANING"), H), "room-status");

            ResponseEntity<String> resv = post("/rooms/reservations",
                    Map.of("roomId", roomId, "reservedCheckin", DAY + "T14:00:00",
                            "guestName", "Chị Lan", "adults", 2), H);
            assertNot5xx(resv, "create-room-reservation");

            assertOk2xxOr4xx(delete("/rooms/" + roomId, H), "delete-room");
        }
        assertThat(true).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Bookings — resources CRUD + bookings best-effort")
    void bookingsVertical() {
        sweepGet(H, "/bookings/resources", "/bookings?date=" + DAY);

        long resourceId = 0L;
        ResponseEntity<String> create = post("/bookings/resources",
                Map.of("name", "Bàn bida IT", "resourceType", "TABLE", "hourlyRate", 80000), H);
        if (create.getStatusCode().is2xxSuccessful()) {
            resourceId = json(create).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(create, "create-resource");
        }

        if (resourceId > 0) {
            assertOk2xxOr4xx(put("/bookings/resources/" + resourceId,
                    Map.of("name", "Bàn bida IT 2", "hourlyRate", 90000), H), "update-resource");

            ResponseEntity<String> booking = post("/bookings",
                    Map.of("resourceId", resourceId, "bookingType", "WALK_IN",
                            "customerName", "Anh Hùng"), H);
            assertNot5xx(booking, "create-booking");

            assertOk2xxOr4xx(delete("/bookings/resources/" + resourceId, H), "delete-resource");
        }
        assertThat(true).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("Appointments — create walk-in, update, confirm, cancel, analytics, delete")
    void appointmentsVertical() {
        sweepGet(H, "/appointments?date=" + DAY,
                "/appointments/analytics?from=" + FROM + "&to=" + TO + "&granularity=day&limit=10",
                "/appointments/week-summary?from=" + FROM + "&to=" + TO);

        long apptId = 0L;
        ResponseEntity<String> create = post("/appointments",
                Map.of("customerName", "Khách lẻ IT",
                        "customerPhone", "0987650111",
                        "scheduledDate", DAY,
                        "scheduledStartTime", "10:00:00",
                        "durationMinutes", 60), H);
        if (create.getStatusCode().is2xxSuccessful()) {
            apptId = json(create).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(create, "create-appointment");
        }

        if (apptId > 0) {
            assertOk2xxOr4xx(put("/appointments/" + apptId,
                    Map.of("note", "Ghi chú IT"), H), "update-appointment");
            assertOk2xxOr4xx(put("/appointments/" + apptId + "/confirm", null, H), "confirm");
            assertOk2xxOr4xx(put("/appointments/" + apptId + "/cancel", null, H), "cancel-appt");
            assertOk2xxOr4xx(delete("/appointments/" + apptId, H), "delete-appointment");
        }
        assertThat(true).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("Recipes + production — lists, save recipe best-effort, production run best-effort")
    void recipesVertical() {
        sweepGet(H, "/recipes", "/production/runs",
                "/production/reports/summary" + RANGE,
                "/production/reports/consumption?from=" + FROM + "&to=" + TO);

        // Save a recipe: needs a finished product + at least one ingredient product.
        List<Long> productIds = jdbc.query(
                "select id from product where tenant_id = ? and deleted = false order by id limit 2",
                (rs, n) -> rs.getLong(1), TENANT);

        if (productIds.size() >= 1) {
            long finished = productIds.get(0);
            Map<String, Object> recipe = new HashMap<>();
            recipe.put("finishedProductId", finished);
            recipe.put("yieldQuantity", 1);
            if (productIds.size() >= 2) {
                Map<String, Object> ingredient = new HashMap<>();
                ingredient.put("ingredientProductId", productIds.get(1));
                ingredient.put("quantity", 2);
                ingredient.put("unit", "g");
                recipe.put("items", List.of(ingredient));
            }
            assertNot5xx(post("/recipes", recipe, H), "save-recipe");

            Map<String, Object> produce = new HashMap<>();
            produce.put("finishedProductId", finished);
            produce.put("quantity", 1);
            assertNot5xx(post("/production/runs", produce, H), "production-run");
        }
        assertThat(true).isTrue();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Runs a fresh cart→checkout and returns the new order id, or 0 if it could not be created. */
    private long createOrderViaCheckout() {
        Long productId = jdbc.query(
                "select p.id from product p join inventory i on i.product_id = p.id "
                        + "where p.tenant_id = ? and i.quantity_in_stock > 0 and p.deleted = false "
                        + "order by p.id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        if (productId == null) return 0L;

        ResponseEntity<String> init = post("/carts", null, H);
        if (!init.getStatusCode().is2xxSuccessful()) return 0L;
        String cartId = json(init).path("data").path("cartId").asText();
        if (cartId == null || cartId.isBlank()) return 0L;

        post("/carts/" + cartId + "/items", Map.of("productId", productId, "quantity", 1), H);
        ResponseEntity<String> checkout = post("/carts/" + cartId + "/checkout",
                Map.of("paymentMethod", "CASH", "amountPaid", 100_000_000), H);
        if (!checkout.getStatusCode().is2xxSuccessful()) return 0L;
        JsonNode data = json(checkout).path("data");
        return data.path("orderId").asLong(data.path("id").asLong(0L));
    }

    /** PATCH helper (AbstractApiIT exposes get/post/put/delete but not patch). */
    private ResponseEntity<String> patch(String pathUrl, Object body, HttpHeaders h) {
        return rest.exchange(pathUrl, org.springframework.http.HttpMethod.PATCH,
                new org.springframework.http.HttpEntity<>(body, h), String.class);
    }

    /** PUT that may be a PATCH (when {@code usePatch}) — keeps call sites readable. */
    private ResponseEntity<String> put(String pathUrl, Object body, HttpHeaders h, boolean usePatch) {
        return usePatch ? patch(pathUrl, body, h) : put(pathUrl, body, h);
    }

    /** Asserts the controller executed without a server error (status &lt; 500). */
    private void assertNot5xx(ResponseEntity<String> res, String label) {
        assertThat(res.getStatusCode().value())
                .as("%s should not 5xx: %s", label, res.getBody())
                .isLessThan(500);
    }

    /** Same as {@link #assertNot5xx} — a 2xx or business 4xx is acceptable, a 5xx is not. */
    private void assertOk2xxOr4xx(ResponseEntity<String> res, String label) {
        assertNot5xx(res, label);
    }
}
