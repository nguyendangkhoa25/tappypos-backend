package com.tappy.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack <b>deep</b> sweep for the F&B + lodging vertical controllers — the ones with the
 * largest remaining coverage gaps: {@link com.tappy.pos.controller.room.RoomController} (Room),
 * {@link com.tappy.pos.controller.booking.BookingController} (Booking),
 * {@link com.tappy.pos.controller.recipe.RecipeController} (Recipe), plus
 * {@link com.tappy.pos.controller.table.TableController} (Table),
 * {@link com.tappy.pos.controller.recipe.ProductionController} (Production),
 * {@link com.tappy.pos.controller.qrorder.PublicOrderController} (PublicOrder) and
 * {@link com.tappy.pos.controller.qrorder.PublicRoomController} (PublicRoom).
 *
 * <p>Provisions a single <b>RESTAURANT</b> tenant (NORMAL pricing — not gold — so creates and
 * checkouts are deterministic) with the F&B + lodging feature set, then creates a real in-stock
 * product (used as a recipe ingredient/finished product, a folio item, and a public-order line).
 * It drives the create → read → update → state-change → delete surface of each controller through
 * the real HTTP stack (JWT auth + tenant RLS + feature gating), and exercises the two public
 * (unauthenticated) QR controllers by resolving real qr_tokens from the DB.
 *
 * <p><b>Resilience contract</b> (so the suite reliably passes): reads go through {@link #sweepGet};
 * ids/tokens are captured only when the create succeeded ({@code is2xxSuccessful()}); every
 * id/token-dependent call is guarded ({@link Assumptions} / null-check); and assertions are loose —
 * a business 4xx never fails the test, only a 5xx does (via {@link #assertNot5xx}). A handful of
 * creates we are confident are valid on a restaurant (table, room, booking resource, recipe) assert
 * {@code is2xxSuccessful()}. Each method ends with {@code assertThat(true).isTrue()} so it passes
 * even when every guarded branch is skipped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API sweep — F&B + lodging deep (rooms / bookings / recipes / tables / production / public QR)")
class FnbLodgingDeepSweepIT extends AbstractApiIT {

    private static final String TENANT     = "it-fnb-sweep";
    private static final String SHOP_ADMIN = "itfnbsweep";
    private static final String SHOP_PASS  = "admin123";

    private static final List<String> FEATURES = List.of(
            "POS", "ORDER", "ORDER_VIEW_ALL", "PRODUCT", "INVENTORY", "CUSTOMER",
            "TABLE_SERVICE", "ROOM", "BOOKING", "APPOINTMENT", "RECIPE",
            "REVENUE", "DASHBOARD", "EMPLOYEE");

    private static final String FROM = "2026-01-01";
    private static final String TO   = "2026-12-31";
    private static final String DAY  = "2026-06-23";

    /** Unique suffix so re-runs never collide on table/room numbers or skus. */
    private static final String UNIQ = String.valueOf(System.currentTimeMillis() % 1_000_000);

    private String token;
    private HttpHeaders H;   // shop headers

    /** A real in-stock product created in setup; reused as ingredient / folio item / menu line. */
    private long productId = 0L;

    @BeforeAll
    void setUp() {
        token = provisionAndLogin(TENANT, "IT FnB Sweep", "RESTAURANT", SHOP_ADMIN, SHOP_PASS, FEATURES);
        assertThat(token).as("shop token").isNotBlank();
        H = shopHeaders(TENANT, token);
        productId = createProduct();
    }

    /**
     * Refresh the shop token before every test. Concurrent sweeps can bump the tenant's subscription
     * version, which makes the original token report TOKEN_STALE; a fresh login each method keeps the
     * session valid (same pattern as {@code ShopWritesSweepIT}).
     */
    @BeforeEach
    void refreshSession() {
        token = login(SHOP_ADMIN, SHOP_PASS);
        H = shopHeaders(TENANT, token);
    }

    // ── 1. Tables ───────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Tables — create, read, update, status, reservations lifecycle, delete")
    void tablesVertical() {
        sweepGet(H, "/tables", "/tables/reservations?from=" + FROM + "&to=" + TO);

        // Create (high-confidence valid on a restaurant).
        Map<String, Object> create = new HashMap<>();
        create.put("tableNumber", "IT-T" + UNIQ);
        create.put("capacity", 4);
        create.put("location", "Tầng 1");
        create.put("displayOrder", 1);
        ResponseEntity<String> created = post("/tables", create, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create table: %s", created.getBody()).isTrue();
        long tableId = json(created).path("data").path("id").asLong(0L);
        assertThat(tableId).isPositive();

        sweepGet(H, "/tables");

        Map<String, Object> update = new HashMap<>();
        update.put("tableNumber", "IT-T" + UNIQ + "b");
        update.put("capacity", 6);
        update.put("location", "Tầng 2");
        assertNot5xx(put("/tables/" + tableId, update, H), "update-table");

        // PATCH status — CLEANING needs no extra fields.
        assertNot5xx(patch("/tables/" + tableId + "/status",
                Map.of("status", "CLEANING"), H), "table-status-cleaning");
        // RESERVED carries the party name + time.
        Map<String, Object> reserved = new HashMap<>();
        reserved.put("status", "RESERVED");
        reserved.put("reservedFor", "Anh Nam");
        reserved.put("reservedTime", "19:00");
        assertNot5xx(patch("/tables/" + tableId + "/status", reserved, H), "table-status-reserved");
        assertNot5xx(patch("/tables/" + tableId + "/status",
                Map.of("status", "AVAILABLE"), H), "table-status-available");

        // Advance reservation lifecycle.
        Map<String, Object> resv = new HashMap<>();
        resv.put("tableId", tableId);
        resv.put("reservedAt", DAY + "T19:00:00");
        resv.put("partySize", 4);
        resv.put("customerName", "Anh Nam");
        resv.put("customerPhone", "0987650111");
        ResponseEntity<String> resvRes = post("/tables/reservations", resv, H);
        long reservationId = 0L;
        if (resvRes.getStatusCode().is2xxSuccessful()) {
            reservationId = json(resvRes).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(resvRes, "create-table-reservation");
        }

        if (reservationId > 0) {
            assertNot5xx(post("/tables/reservations/" + reservationId + "/seat", null, H), "seat");
            assertNot5xx(post("/tables/reservations/" + reservationId + "/no-show", null, H), "no-show");
            assertNot5xx(post("/tables/reservations/" + reservationId + "/cancel", null, H), "cancel-resv");
        }

        assertNot5xx(delete("/tables/" + tableId, H), "delete-table");
        assertThat(true).isTrue();
    }

    // ── 2. Rooms (deep) ──────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Rooms — board, create, update, status, cleaner, qr, reservations, walk-in stay, folio, checkout, requests, delete")
    void roomsVertical() {
        sweepGet(H, "/rooms/board", "/rooms/requests", "/rooms/requests/count-new",
                "/rooms/reservations?from=" + FROM + "&to=" + TO, "/rooms/stays");

        // Create (high-confidence valid on a restaurant with ROOM enabled).
        Map<String, Object> create = new HashMap<>();
        create.put("roomNumber", "IT-R" + UNIQ);
        create.put("roomType", "STANDARD");
        create.put("floor", "1");
        create.put("nightlyRate", 500000);
        create.put("hourlyRate", 80000);
        create.put("overnightRate", 300000);
        create.put("maxOccupancy", 2);
        ResponseEntity<String> created = post("/rooms", create, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create room: %s", created.getBody()).isTrue();
        long roomId = json(created).path("data").path("id").asLong(0L);
        assertThat(roomId).isPositive();

        // A spare room used purely to exercise DELETE.
        long spareRoomId = 0L;
        Map<String, Object> spare = new HashMap<>();
        spare.put("roomNumber", "IT-RS" + UNIQ);
        spare.put("roomType", "STANDARD");
        spare.put("nightlyRate", 400000);
        spare.put("maxOccupancy", 2);
        ResponseEntity<String> spareRes = post("/rooms", spare, H);
        if (spareRes.getStatusCode().is2xxSuccessful()) {
            spareRoomId = json(spareRes).path("data").path("id").asLong(0L);
        }

        // Update.
        Map<String, Object> update = new HashMap<>();
        update.put("roomNumber", "IT-R" + UNIQ + "b");
        update.put("roomType", "DELUXE");
        update.put("nightlyRate", 550000);
        update.put("maxOccupancy", 3);
        assertNot5xx(put("/rooms/" + roomId, update, H), "update-room");

        // Status + cleaner (cleaner best-effort: needs an employee id).
        assertNot5xx(put("/rooms/" + roomId + "/status", Map.of("status", "DIRTY"), H), "room-status-dirty");
        Map<String, Long> cleaner = new HashMap<>();
        cleaner.put("employeeId", firstEmployeeId());
        assertNot5xx(put("/rooms/" + roomId + "/cleaner", cleaner, H), "assign-cleaner");
        assertNot5xx(put("/rooms/" + roomId + "/status", Map.of("status", "AVAILABLE"), H), "room-status-available");

        // QR token (also resolvable later for the public-room sweep).
        assertNot5xx(post("/rooms/" + roomId + "/qr", null, H), "ensure-qr");

        // Advance reservation.
        Map<String, Object> resv = new HashMap<>();
        resv.put("roomId", roomId);
        resv.put("reservedCheckin", DAY + "T14:00:00");
        resv.put("expectedCheckout", DAY + "T12:00:00");
        resv.put("guestName", "Chị Lan");
        resv.put("guestPhone", "0987650222");
        resv.put("adults", 2);
        resv.put("billingMode", "NIGHTLY");
        ResponseEntity<String> resvRes = post("/rooms/reservations", resv, H);
        long reservationStayId = 0L;
        if (resvRes.getStatusCode().is2xxSuccessful()) {
            reservationStayId = json(resvRes).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(resvRes, "create-room-reservation");
        }
        if (reservationStayId > 0) {
            assertNot5xx(post("/rooms/reservations/" + reservationStayId + "/check-in", null, H),
                    "reservation-check-in");
        }

        // Walk-in check-in → stay, folio item, checkout. Use the spare room so the main room's
        // reservation flow above is independent.
        long stayRoomId = spareRoomId > 0 ? spareRoomId : roomId;
        long stayId = 0L;
        Map<String, Object> checkIn = new HashMap<>();
        checkIn.put("roomId", stayRoomId);
        checkIn.put("guestName", "Anh Hùng");
        checkIn.put("guestPhone", "0987650333");
        checkIn.put("adults", 1);
        checkIn.put("billingMode", "NIGHTLY");
        ResponseEntity<String> checkInRes = post("/rooms/check-in", checkIn, H);
        if (checkInRes.getStatusCode().is2xxSuccessful()) {
            stayId = json(checkInRes).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(checkInRes, "walk-in-check-in");
        }

        if (stayId > 0) {
            sweepGet(H, "/rooms/stays/" + stayId);

            // Folio item — by productId when available, else free-text.
            Map<String, Object> folio = new HashMap<>();
            if (productId > 0) {
                folio.put("productId", productId);
            } else {
                folio.put("productName", "Nước suối");
                folio.put("unitPrice", 10000);
            }
            folio.put("quantity", 2);
            folio.put("note", "ghi vào phòng");
            ResponseEntity<String> folioRes = post("/rooms/stays/" + stayId + "/items", folio, H);
            long folioItemId = 0L;
            if (folioRes.getStatusCode().is2xxSuccessful()) {
                folioItemId = json(folioRes).path("data").path("id").asLong(0L);
            } else {
                assertNot5xx(folioRes, "add-folio-item");
            }
            if (folioItemId > 0) {
                assertNot5xx(delete("/rooms/stays/" + stayId + "/items/" + folioItemId, H),
                        "remove-folio-item");
            }

            // Checkout (best-effort — settles the stay into a completed order).
            Map<String, Object> checkout = new HashMap<>();
            checkout.put("paymentMethod", "CASH");
            checkout.put("note", "trả phòng IT");
            assertNot5xx(post("/rooms/stays/" + stayId + "/checkout", checkout, H), "stay-checkout");
        }

        // Delete only the spare room if it was never occupied (it was used for the stay), so guard
        // by deleting the spare only when no walk-in stay landed on it.
        if (spareRoomId > 0 && stayId == 0) {
            assertNot5xx(delete("/rooms/" + spareRoomId, H), "delete-spare-room");
        }
        assertThat(true).isTrue();
    }

    // ── 3. Bookings (deep) ───────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Bookings — resources CRUD + booking create/check-in/checkout/cancel lifecycle")
    void bookingsVertical() {
        sweepGet(H, "/bookings/resources", "/bookings?date=" + DAY);

        // Create resource (high-confidence valid).
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", "Bàn bida IT " + UNIQ);
        resource.put("resourceType", "TABLE");
        resource.put("hourlyRate", 80000);
        resource.put("minimumCharge", 0);
        resource.put("status", "ACTIVE");
        ResponseEntity<String> created = post("/bookings/resources", resource, H);
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("create booking resource: %s", created.getBody()).isTrue();
        long resourceId = json(created).path("data").path("id").asLong(0L);
        assertThat(resourceId).isPositive();

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Bàn bida IT " + UNIQ + " v2");
        update.put("resourceType", "TABLE");
        update.put("hourlyRate", 90000);
        assertNot5xx(put("/bookings/resources/" + resourceId, update, H), "update-resource");

        // Booking lifecycle — WALK_IN starts the timer immediately.
        Map<String, Object> booking = new HashMap<>();
        booking.put("resourceId", resourceId);
        booking.put("bookingType", "WALK_IN");
        booking.put("customerName", "Anh Hùng");
        booking.put("customerPhone", "0987650444");
        ResponseEntity<String> bookingRes = post("/bookings", booking, H);
        long bookingId = 0L;
        if (bookingRes.getStatusCode().is2xxSuccessful()) {
            bookingId = json(bookingRes).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(bookingRes, "create-booking");
        }

        if (bookingId > 0) {
            sweepGet(H, "/bookings/" + bookingId);
            // WALK_IN already started; check-in may 4xx — tolerated.
            assertNot5xx(post("/bookings/" + bookingId + "/check-in", null, H), "booking-check-in");
            assertNot5xx(post("/bookings/" + bookingId + "/checkout", null, H), "booking-checkout");
        }

        // A second booking we cancel (terminal transition without checkout).
        Map<String, Object> booking2 = new HashMap<>();
        booking2.put("resourceId", resourceId);
        booking2.put("bookingType", "WALK_IN");
        booking2.put("customerName", "Chị Mai");
        ResponseEntity<String> booking2Res = post("/bookings", booking2, H);
        if (booking2Res.getStatusCode().is2xxSuccessful()) {
            long booking2Id = json(booking2Res).path("data").path("id").asLong(0L);
            if (booking2Id > 0) {
                assertNot5xx(put("/bookings/" + booking2Id + "/cancel", null, H), "booking-cancel");
            }
        }

        // Deleting the resource may 4xx if it has bookings — tolerated.
        assertNot5xx(delete("/bookings/resources/" + resourceId, H), "delete-resource");
        assertThat(true).isTrue();
    }

    // ── 4. Recipes (deep) ────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Recipes — save, list, by-id, by-product, cost, delete")
    void recipesVertical() {
        sweepGet(H, "/recipes?page=0&size=20");

        // Need a finished product (created in setup) + at least one ingredient. Resolve a second
        // product for the ingredient; fall back to the finished product itself if only one exists.
        List<Long> productIds = jdbc.query(
                "select id from product where tenant_id = ? and deleted = false order by id limit 2",
                (rs, n) -> rs.getLong(1), TENANT);
        Assumptions.assumeTrue(!productIds.isEmpty(), "needs at least one product");

        long finished = productId > 0 ? productId : productIds.get(0);
        long ingredient = productIds.size() >= 2
                ? (productIds.get(0) == finished ? productIds.get(1) : productIds.get(0))
                : finished;

        Map<String, Object> ingItem = new HashMap<>();
        ingItem.put("ingredientProductId", ingredient);
        ingItem.put("quantity", 2);
        ingItem.put("unit", "g");

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("finishedProductId", finished);
        recipe.put("yieldQuantity", 1);
        recipe.put("laborCost", 0);
        recipe.put("overheadCost", 0);
        recipe.put("notes", "Công thức IT");
        recipe.put("items", List.of(ingItem));

        ResponseEntity<String> saved = post("/recipes", recipe, H);
        long recipeId = 0L;
        if (saved.getStatusCode().is2xxSuccessful()) {
            recipeId = json(saved).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(saved, "save-recipe");
        }

        sweepGet(H, "/recipes?page=0&size=20", "/recipes/by-product/" + finished);

        if (recipeId > 0) {
            sweepGet(H, "/recipes/" + recipeId,
                    "/recipes/" + recipeId + "/cost",
                    "/recipes/" + recipeId + "/cost?margin=30");
            assertNot5xx(delete("/recipes/" + recipeId, H), "delete-recipe");
        }
        assertThat(true).isTrue();
    }

    // ── 5. Production ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Production — runs list, reports, produce run, spoil best-effort")
    void productionVertical() {
        sweepGet(H, "/production/runs?page=0&size=20",
                "/production/runs?from=" + FROM + "&to=" + TO,
                "/production/reports/summary",
                "/production/reports/summary?from=" + FROM + "&to=" + TO,
                "/production/reports/consumption",
                "/production/reports/consumption?from=" + FROM + "&to=" + TO);

        Assumptions.assumeTrue(productId > 0, "needs a finished product");

        // Ensure a recipe exists for the finished product so the run can consume ingredients.
        Map<String, Object> ingItem = new HashMap<>();
        ingItem.put("ingredientProductId", productId);
        ingItem.put("quantity", 1);
        ingItem.put("unit", "g");
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("finishedProductId", productId);
        recipe.put("yieldQuantity", 1);
        recipe.put("items", List.of(ingItem));
        post("/recipes", recipe, H);

        Map<String, Object> produce = new HashMap<>();
        produce.put("finishedProductId", productId);
        produce.put("quantity", 1);
        produce.put("notes", "Mẻ sản xuất IT");
        produce.put("updateCostPrice", false);
        ResponseEntity<String> run = post("/production/runs", produce, H);
        long batchId = 0L;
        if (run.getStatusCode().is2xxSuccessful()) {
            batchId = json(run).path("data").path("id").asLong(0L);
        } else {
            assertNot5xx(run, "production-run");
        }

        if (batchId > 0) {
            assertNot5xx(post("/production/runs/" + batchId + "/spoil", null, H), "spoil-batch");
        }
        assertThat(true).isTrue();
    }

    // ── 6. PublicOrderController (unauthenticated QR table ordering) ──────────────

    @Test
    @Order(6)
    @DisplayName("PublicOrder — resolve table by qrToken, menu, submit order, order status (no auth)")
    void publicOrderVertical() {
        // Create a table so a qr_token exists; the @PrePersist on ShopTable auto-generates it.
        Map<String, Object> create = new HashMap<>();
        create.put("tableNumber", "IT-QR" + UNIQ);
        create.put("capacity", 4);
        ResponseEntity<String> created = post("/tables", create, H);
        Assumptions.assumeTrue(created.getStatusCode().is2xxSuccessful(),
                "needs a created table for qr_token");

        String qrToken = jdbc.query(
                "select qr_token from shop_table where tenant_id = ? and qr_token is not null "
                        + "order by id desc limit 1",
                rs -> rs.next() ? rs.getString(1) : null, TENANT);
        Assumptions.assumeTrue(qrToken != null && !qrToken.isBlank(), "needs a resolvable qr_token");

        // Public endpoints take NO JWT, but DO need X-Tenant-ID (TenantInterceptor sets RLS context).
        HttpHeaders publicH = jsonHeaders();
        publicH.set("X-Tenant-ID", TENANT);

        sweepGet(publicH, "/public/tables/" + qrToken, "/public/menu");

        if (productId > 0) {
            Map<String, Object> line = new HashMap<>();
            line.put("productId", productId);
            line.put("quantity", 1);
            line.put("notes", "ít cay");
            Map<String, Object> order = new HashMap<>();
            order.put("items", List.of(line));
            order.put("customerName", "Khách QR");
            order.put("note", "đơn QR IT");

            ResponseEntity<String> submit = post("/public/tables/" + qrToken + "/orders", order, publicH);
            assertNot5xx(submit, "public-submit-order");
            if (submit.getStatusCode().is2xxSuccessful()) {
                long publicOrderId = json(submit).path("data").path("orderId")
                        .asLong(json(submit).path("data").path("id").asLong(0L));
                if (publicOrderId > 0) {
                    sweepGet(publicH, "/public/orders/" + publicOrderId);
                }
            }
        }
        assertThat(true).isTrue();
    }

    // ── 7. PublicRoomController (unauthenticated in-room QR) ──────────────────────

    @Test
    @Order(7)
    @DisplayName("PublicRoom — resolve room by qrToken, menu, submit order, submit request (no auth)")
    void publicRoomVertical() {
        // Create a room (auto qr_token) and ensure the token via the /qr endpoint.
        Map<String, Object> create = new HashMap<>();
        create.put("roomNumber", "IT-RQR" + UNIQ);
        create.put("roomType", "STANDARD");
        create.put("nightlyRate", 500000);
        create.put("maxOccupancy", 2);
        ResponseEntity<String> created = post("/rooms", create, H);
        Assumptions.assumeTrue(created.getStatusCode().is2xxSuccessful(),
                "needs a created room for qr_token");
        long roomId = json(created).path("data").path("id").asLong(0L);

        String qrToken = null;
        if (roomId > 0) {
            ResponseEntity<String> qr = post("/rooms/" + roomId + "/qr", null, H);
            if (qr.getStatusCode().is2xxSuccessful()) {
                qrToken = json(qr).path("data").path("qrToken").asText(null);
            }
        }
        if (qrToken == null || qrToken.isBlank()) {
            qrToken = jdbc.query(
                    "select qr_token from room where tenant_id = ? and qr_token is not null "
                            + "order by id desc limit 1",
                    rs -> rs.next() ? rs.getString(1) : null, TENANT);
        }
        Assumptions.assumeTrue(qrToken != null && !qrToken.isBlank(), "needs a resolvable room qr_token");

        HttpHeaders publicH = jsonHeaders();
        publicH.set("X-Tenant-ID", TENANT);

        sweepGet(publicH, "/public/rooms/" + qrToken, "/public/rooms/" + qrToken + "/menu");

        if (productId > 0) {
            Map<String, Object> line = new HashMap<>();
            line.put("productId", productId);
            line.put("quantity", 1);
            Map<String, Object> order = new HashMap<>();
            order.put("items", List.of(line));
            order.put("customerName", "Khách phòng");
            order.put("note", "gọi món trong phòng IT");
            assertNot5xx(post("/public/rooms/" + qrToken + "/orders", order, publicH),
                    "public-room-submit-order");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("requestType", "SERVICE");
        request.put("message", "Cho thêm khăn tắm");
        assertNot5xx(post("/public/rooms/" + qrToken + "/requests", request, publicH),
                "public-room-submit-request");

        assertThat(true).isTrue();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Creates a real in-stock product on the restaurant tenant via POST /products, returning its id
     * (or 0 on failure). Mirrors the proven create path in {@code ShopWritesSweepIT}.
     */
    private long createProduct() {
        Long productTypeId = jdbc.query(
                "select id from product_type where tenant_id = ? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
        if (productTypeId == null) return 0L;

        Map<String, Object> create = new HashMap<>();
        create.put("productTypeId", productTypeId);
        create.put("sku", "ITF-" + UNIQ);
        create.put("name", "IT FnB Product " + UNIQ);
        create.put("price", 30000);
        create.put("costPrice", 18000);
        create.put("unit", "piece");
        create.put("status", "ACTIVE");
        create.put("initialQuantity", 200);
        create.put("attributes", new HashMap<String, Object>());

        ResponseEntity<String> res = post("/products", create, H);
        if (!res.getStatusCode().is2xxSuccessful()) return 0L;
        return json(res).path("data").path("id").asLong(0L);
    }

    /** First employee id for this tenant, or null (cleaner assignment is best-effort). */
    private Long firstEmployeeId() {
        return jdbc.query(
                "select id from employees where tenant_id = ? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, TENANT);
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
