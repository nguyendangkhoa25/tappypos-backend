package com.tappy.pos.controller.order;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.MyWorkStatsDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.model.dto.order.WorkItemDTO;
import com.tappy.pos.model.dto.order.WorkItemSummaryDTO;
import com.tappy.pos.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tappy.pos.annotation.RequiresFeature;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@RequiresFeature("ORDER")
public class OrderController {

    private final OrderService orderService;

    /**
     * GET /api/orders/my-work/pending
     * Returns paginated PENDING + IN_PROGRESS orders created by the current user.
     */
    @GetMapping("/my-work/pending")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyPendingOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/my-work/pending - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getMyPendingOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "My pending orders retrieved successfully"));
    }

    /**
     * GET /api/orders/my-work/completed
     * Returns paginated COMPLETED orders created by the current user, filtered by period.
     *
     * Query params:
     *   filterType  DAY | MONTH | YEAR  (default DAY)
     *   day         (optional, default today's day)
     *   month       (optional, default current month)
     *   year        (optional, default current year)
     */
    @GetMapping("/my-work/completed")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyCompletedOrders(
            @RequestParam(defaultValue = "DAY") String filterType,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/my-work/completed - filter: {}, page: {}", filterType, page);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getMyCompletedOrders(filterType, day, month, year, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "My completed orders retrieved successfully"));
    }

    /**
     * GET /api/orders/my-work/stats
     * Returns pending count, completed count and total revenue for the current user/period.
     */
    @GetMapping("/my-work/stats")
    public ResponseEntity<ApiResponse<MyWorkStatsDTO>> getMyWorkStats(
            @RequestParam(defaultValue = "DAY") String filterType,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        log.info("Endpoint: GET /orders/my-work/stats - filter: {}", filterType);
        MyWorkStatsDTO stats = orderService.getMyWorkStats(filterType, day, month, year);
        return ResponseEntity.ok(ApiResponse.success(stats, "My work stats retrieved successfully"));
    }

    // ── Item-level work queue (MY_WORK feature) ────────────────────────────────

    /**
     * GET /api/orders/work-items/available
     * Returns unassigned PENDING items in active orders — the pool any employee can pick from.
     */
    @GetMapping("/work-items/available")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<Page<WorkItemDTO>>> getAvailableWorkItems(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/work-items/available - page: {}, size: {}", page, size);
        Page<WorkItemDTO> items = orderService.getAvailableWorkItems(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(items, "Available work items retrieved successfully"));
    }

    /**
     * PUT /api/orders/work-items/{itemId}/pickup
     * Claim an unassigned item — assign it to the current employee.
     */
    @PutMapping("/work-items/{itemId}/pickup")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemDTO>> pickupWorkItem(@PathVariable Long itemId) {
        log.info("Endpoint: PUT /orders/work-items/{}/pickup", itemId);
        WorkItemDTO item = orderService.pickupWorkItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(item, "Work item picked up successfully"));
    }

    /**
     * PUT /api/orders/work-items/{itemId}/unpick
     * Release a PENDING item back to the unassigned pool.
     */
    @PutMapping("/work-items/{itemId}/unpick")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemDTO>> unpickWorkItem(@PathVariable Long itemId) {
        log.info("Endpoint: PUT /orders/work-items/{}/unpick", itemId);
        WorkItemDTO item = orderService.unpickWorkItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(item, "Work item released to pool successfully"));
    }

    /**
     * GET /api/orders/work-items
     * Returns all order items assigned to the current employee (all statuses).
     */
    @GetMapping("/work-items")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<Page<WorkItemDTO>>> getMyWorkItems(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/work-items - page: {}, size: {}", page, size);
        Page<WorkItemDTO> items = orderService.getMyWorkItems(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(items, "Work items retrieved successfully"));
    }

    /**
     * GET /api/orders/work-items/pending
     * Returns PENDING + IN_PROGRESS items assigned to the current employee.
     */
    @GetMapping("/work-items/pending")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<Page<WorkItemDTO>>> getMyPendingWorkItems(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/work-items/pending - page: {}, size: {}", page, size);
        Page<WorkItemDTO> items = orderService.getMyPendingWorkItems(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(items, "Pending work items retrieved successfully"));
    }

    /**
     * PUT /api/orders/work-items/{itemId}/start
     * Transition item PENDING → IN_PROGRESS.
     */
    @PutMapping("/work-items/{itemId}/start")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemDTO>> startWorkItem(@PathVariable Long itemId) {
        log.info("Endpoint: PUT /orders/work-items/{}/start", itemId);
        WorkItemDTO item = orderService.startWorkItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(item, "Work item started successfully"));
    }

    /**
     * PUT /api/orders/work-items/{itemId}/complete
     * Transition item IN_PROGRESS → COMPLETED.
     */
    @PutMapping("/work-items/{itemId}/complete")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemDTO>> completeWorkItem(@PathVariable Long itemId) {
        log.info("Endpoint: PUT /orders/work-items/{}/complete", itemId);
        WorkItemDTO item = orderService.completeWorkItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(item, "Work item completed successfully"));
    }

    /**
     * PUT /api/orders/work-items/{itemId}/release
     * Transition item IN_PROGRESS → PENDING.
     */
    @PutMapping("/work-items/{itemId}/release")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemDTO>> releaseWorkItem(@PathVariable Long itemId) {
        log.info("Endpoint: PUT /orders/work-items/{}/release", itemId);
        WorkItemDTO item = orderService.releaseWorkItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(item, "Work item released successfully"));
    }

    // ── Completed work item history ────────────────────────────────────────────

    /**
     * GET /api/orders/work-items/completed
     * Paginated list of the current employee's completed items, filterable by period and searchable.
     *
     * filterType: DAY | WEEK | MONTH | YEAR  (default DAY)
     * keyword: optional search across product name, order number, customer name
     */
    @GetMapping("/work-items/completed")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<Page<WorkItemDTO>>> getMyCompletedWorkItems(
            @RequestParam(defaultValue = "DAY") String filterType,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/work-items/completed - filter: {}, keyword: {}", filterType, keyword);
        Page<WorkItemDTO> items = orderService.getMyCompletedWorkItems(
                filterType, day, month, year, keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(items, "Completed work items retrieved successfully"));
    }

    /**
     * GET /api/orders/work-items/summary
     * Summary stats (count, revenue, total duration minutes) for the current employee over a period.
     */
    @GetMapping("/work-items/summary")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<WorkItemSummaryDTO>> getMyWorkItemSummary(
            @RequestParam(defaultValue = "DAY") String filterType,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        log.info("Endpoint: GET /orders/work-items/summary - filter: {}", filterType);
        WorkItemSummaryDTO summary = orderService.getMyWorkItemSummary(filterType, day, month, year);
        return ResponseEntity.ok(ApiResponse.success(summary, "Work item summary retrieved successfully"));
    }

    /**
     * GET /api/orders/work-items/trend
     * Bar chart data for the current employee's completed items.
     *
     * filterType DAY   → data points by hour  (label: "08:00")
     * filterType WEEK  → data points by day   (label: "2026-05-12")
     * filterType MONTH → data points by day   (label: "2026-05-12")
     * filterType YEAR  → data points by month (label: "5")
     */
    @GetMapping("/work-items/trend")
    @RequiresFeature("MY_WORK")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyWorkItemTrend(
            @RequestParam(defaultValue = "DAY") String filterType,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        log.info("Endpoint: GET /orders/work-items/trend - filter: {}", filterType);
        List<Map<String, Object>> trend = orderService.getMyWorkItemTrend(filterType, day, month, year);
        return ResponseEntity.ok(ApiResponse.success(trend, "Work item trend retrieved successfully"));
    }

    /**
     * GET /api/orders
     * Returns a paginated list of all orders, optionally filtered by status and/or orderType.
     *
     * Query params:
     *   page      (default 0)
     *   size      (default 20)
     *   status    (optional) — PENDING | IN_PROGRESS | COMPLETED | CANCELLED | VOIDED
     *   orderType (optional) — SELL | BUY | EXCHANGE
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders - status: {}, orderType: {}, page: {}, size: {}", status, orderType, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getAllOrders(status, orderType, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    /**
     * GET /api/orders/list
     * Filtered, paginated order list for the Report screen.
     * Supports from, to (date range on createdAt), status, and paymentMethod.
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrdersList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/list - status:{} paymentMethod:{} from:{} to:{} page:{}", status, paymentMethod, from, to, page);
        Page<OrderDTO> orders = orderService.getAllOrdersFiltered(status, paymentMethod, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    /**
     * GET /api/orders/search
     * Full-text search across orderNumber and customer name.
     *
     * Query params:
     *   keyword (required)
     *   page    (default 0)
     *   size    (default 20)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders/search - keyword: {}, page: {}, size: {}", keyword, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.searchOrders(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    /**
     * GET /api/orders/{id}
     * Returns a single order with its items.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        log.info("Endpoint: GET /orders/{}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    /**
     * PUT /api/orders/{id}/start
     * Transition PENDING → IN_PROGRESS.
     */
    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<OrderDTO>> startOrder(@PathVariable Long id) {
        log.info("Endpoint: PUT /orders/{}/start", id);
        OrderDTO order = orderService.startOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order started successfully"));
    }

    /**
     * PUT /api/orders/{id}/complete
     * Transition IN_PROGRESS (or PENDING) → COMPLETED.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderDTO>> completeOrder(@PathVariable Long id) {
        log.info("Endpoint: PUT /orders/{}/complete", id);
        OrderDTO order = orderService.completeOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order completed successfully"));
    }

    /**
     * POST /api/orders/{id}/cancel
     * Transition PENDING | IN_PROGRESS → CANCELLED.
     * Body: { reason, cancelledBy }
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request) {
        log.info("Endpoint: POST /orders/{}/cancel", id);
        if (request == null) request = new CancelOrderRequest();
        OrderDTO order = orderService.cancelOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    /**
     * POST /api/orders/{id}/void
     * Transition COMPLETED → VOIDED. Restores inventory for all items.
     * Body: { reason, voidedBy }
     */
    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponse<OrderDTO>> voidOrder(
            @PathVariable Long id,
            @RequestBody(required = false) VoidOrderRequest request) {
        log.info("Endpoint: POST /orders/{}/void", id);
        if (request == null) request = new VoidOrderRequest();
        OrderDTO order = orderService.voidOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order voided successfully"));
    }

    /**
     * GET /api/orders/{id}/receipt
     * Returns a self-contained HTML receipt (80 mm thermal format) for the given order.
     */
    @GetMapping(value = "/{id}/receipt", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReceipt(@PathVariable Long id) {
        log.info("Endpoint: GET /orders/{}/receipt", id);
        String html = orderService.generateReceipt(id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(html);
    }

    /**
     * POST /api/orders/receipt/preview
     * Returns a self-contained HTML receipt from pre-checkout cart data (no persisted order needed).
     */
    @PostMapping(value = "/receipt/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewReceipt(@RequestBody ReceiptPreviewRequest request) {
        log.info("Endpoint: POST /orders/receipt/preview");
        String html = orderService.generatePreviewReceipt(request);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(html);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod) {
        log.info("Endpoint: GET /orders/summary from={} to={}", from, to);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderSummary(from, to, status, paymentMethod), "OK"));
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrderChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        log.info("Endpoint: GET /orders/chart from={} to={} granularity={}", from, to, granularity);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderChart(from, to, granularity), "OK"));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "30") int days) {
        log.info("Endpoint: GET /orders/top-products limit={} from={} to={} days={}", limit, from, to, days);
        if (from != null && to != null) {
            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt = to.atTime(LocalTime.MAX);
            return ResponseEntity.ok(ApiResponse.success(orderService.getTopProductsByRange(limit, fromDt, toDt), "OK"));
        }
        LocalDateTime fromDt = LocalDateTime.now().minusDays(days);
        return ResponseEntity.ok(ApiResponse.success(orderService.getTopProducts(limit, fromDt), "OK"));
    }

    @GetMapping("/top-customers")
    @RequiresFeature("CUSTOMER")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopCustomers(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /orders/top-customers limit={} from={} to={}", limit, from, to);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        return ResponseEntity.ok(ApiResponse.success(orderService.getTopCustomersByRange(limit, fromDt, toDt), "OK"));
    }

    @GetMapping("/top-employees")
    @RequiresFeature("ORDER_VIEW_ALL")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopEmployees(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /orders/top-employees limit={} from={} to={}", limit, from, to);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        return ResponseEntity.ok(ApiResponse.success(orderService.getTopEmployeesByRange(limit, fromDt, toDt), "OK"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        log.info("Endpoint: DELETE /orders/{}", id);
        orderService.softDeleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Order deleted"));
    }

    // ── Staff performance endpoints (requires ORDER_VIEW_ALL) ──────────────────

    @GetMapping("/by-staff/summary")
    @RequiresFeature("ORDER_VIEW_ALL")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStaffOrderSummary(
            @RequestParam String createdBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /orders/by-staff/summary createdBy={} from={} to={}", createdBy, from, to);
        return ResponseEntity.ok(ApiResponse.success(orderService.getStaffOrderSummary(createdBy, from, to), "OK"));
    }

    @GetMapping("/by-staff/chart")
    @RequiresFeature("ORDER_VIEW_ALL")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStaffOrderChart(
            @RequestParam String createdBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        log.info("Endpoint: GET /orders/by-staff/chart createdBy={} from={} to={} granularity={}", createdBy, from, to, granularity);
        return ResponseEntity.ok(ApiResponse.success(orderService.getStaffOrderChart(createdBy, from, to, granularity), "OK"));
    }

    @GetMapping("/by-staff")
    @RequiresFeature("ORDER_VIEW_ALL")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getStaffOrders(
            @RequestParam String createdBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Endpoint: GET /orders/by-staff createdBy={} status={} page={}", createdBy, status, page);
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getStaffOrders(createdBy, status, from, to, PageRequest.of(page, size)), "OK"));
    }
}
