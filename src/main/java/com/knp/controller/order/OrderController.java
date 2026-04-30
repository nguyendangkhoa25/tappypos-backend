package com.knp.controller.order;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.tenant.ReceiptPreviewRequest;
import com.knp.model.dto.order.CancelOrderRequest;
import com.knp.model.dto.order.MyWorkStatsDTO;
import com.knp.model.dto.order.OrderDTO;
import com.knp.model.dto.order.VoidOrderRequest;
import com.knp.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.knp.annotation.RequiresFeature;

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

    /**
     * GET /api/orders
     * Returns a paginated list of all orders, optionally filtered by status.
     *
     * Query params:
     *   page   (default 0)
     *   size   (default 20)
     *   status (optional) — PENDING | IN_PROGRESS | COMPLETED | CANCELLED
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /orders - status: {}, page: {}, size: {}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getAllOrders(status, pageable);
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
}
