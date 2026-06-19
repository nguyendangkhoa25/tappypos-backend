package com.tappy.pos.service.order;

import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.order.AddOrderItemRequest;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.MyWorkStatsDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.OrderItemDTO;
import com.tappy.pos.model.dto.order.PayAndCompleteRequest;
import com.tappy.pos.model.dto.order.SettlePreOrderRequest;
import com.tappy.pos.model.dto.order.UpdateOrderMetaRequest;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.model.dto.order.WorkItemDTO;
import com.tappy.pos.model.dto.order.WorkItemSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderService {

    Page<OrderDTO> getAllOrders(String status, String orderType, Pageable pageable);
    Page<OrderDTO> getAllOrdersFiltered(String status, String paymentMethod, LocalDate from, LocalDate to, Pageable pageable);

    Page<OrderDTO> searchOrders(String keyword, Pageable pageable);

    OrderDTO getOrderById(Long id);

    OrderDTO startOrder(Long id);

    OrderDTO completeOrder(Long id);

    OrderDTO cancelOrder(Long id, CancelOrderRequest request);

    // ── Pre-order / deposit (đặt hàng + tiền cọc) — Phase 2 ─────────────────────
    /** Pickup queue: pre-orders (optionally by status / pickup window), sorted by pickup time. */
    Page<OrderDTO> getPreOrders(String status, LocalDate from, LocalDate to, Pageable pageable);

    /** Collect the remaining balance on a PENDING pre-order at pickup, deduct stock, complete it. */
    OrderDTO settlePreOrder(Long id, SettlePreOrderRequest request);

    /** Dashboard summary: deposits held + upcoming pickup counts (đơn đặt). */
    com.tappy.pos.model.dto.order.PreOrderSummaryDTO getPreOrderSummary();

    OrderDTO voidOrder(Long id, VoidOrderRequest request);

    /** Size/color swap on a completed order line: return the item's variant, issue another of the same product. */
    OrderDTO exchangeOrderItem(Long orderId, Long itemId, com.tappy.pos.model.dto.order.ExchangeOrderItemRequest request);

    String generateReceipt(Long id);

    String generatePreviewReceipt(ReceiptPreviewRequest request);

    Page<OrderDTO> getOrdersByCustomerId(Long customerId, Pageable pageable);

    Page<OrderDTO> getMyPendingOrders(Pageable pageable);

    Page<OrderDTO> getMyCompletedOrders(String filterType, Integer day, Integer month, Integer year, Pageable pageable);

    MyWorkStatsDTO getMyWorkStats(String filterType, Integer day, Integer month, Integer year);

    Map<String, Object> getOrderSummary(LocalDate from, LocalDate to, String status, String paymentMethod);

    List<Map<String, Object>> getOrderChart(LocalDate from, LocalDate to, String granularity);

    List<Map<String, Object>> getTopProducts(int limit, LocalDateTime from);

    List<Map<String, Object>> getTopProductsByRange(int limit, LocalDateTime from, LocalDateTime to);

    List<Map<String, Object>> getTopCustomersByRange(int limit, LocalDateTime from, LocalDateTime to);

    /** Same as getTopCustomersByRange but sorted by visit count, not spend. */
    List<Map<String, Object>> getTopCustomersByFrequency(int limit, LocalDateTime from, LocalDateTime to);

    /** Returns { total, newCount, returningCount } for the period. */
    Map<String, Object> getCustomerStats(LocalDateTime from, LocalDateTime to);

    List<Map<String, Object>> getTopEmployeesByRange(int limit, LocalDateTime from, LocalDateTime to);

    void softDeleteOrder(Long id);

    // ── IN_PROGRESS order mutation endpoints ──────────────────────────────────
    OrderItemDTO addItemToOrder(Long orderId, AddOrderItemRequest request);

    void removeItemFromOrder(Long orderId, Long itemId);

    OrderItemDTO updateItemQuantity(Long orderId, Long itemId, int quantity);

    OrderItemDTO updateItemEmployee(Long orderId, Long itemId, Long employeeId);

    /** Update (or clear) the per-item note. Passing null or blank clears the note. */
    OrderItemDTO updateItemNote(Long orderId, Long itemId, String note);

    OrderDTO updateOrderMeta(Long orderId, UpdateOrderMetaRequest request);

    OrderDTO payAndCompleteOrder(Long orderId, PayAndCompleteRequest request);

    // ── Item-level work queue (MY_WORK feature) ────────────────────────────────
    Page<WorkItemDTO> getMyWorkItems(Pageable pageable);

    Page<WorkItemDTO> getMyPendingWorkItems(Pageable pageable);

    /** All-staff oversight board: PENDING + IN_PROGRESS items assigned to any employee (ORDER_VIEW_ALL). */
    Page<WorkItemDTO> getAllPendingWorkItems(Pageable pageable);

    Page<WorkItemDTO> getAvailableWorkItems(Pageable pageable);

    WorkItemDTO pickupWorkItem(Long itemId);

    WorkItemDTO unpickWorkItem(Long itemId);

    WorkItemDTO startWorkItem(Long itemId);

    WorkItemDTO completeWorkItem(Long itemId);

    WorkItemDTO releaseWorkItem(Long itemId);

    // ── Completed work item history ────────────────────────────────────────────
    Page<WorkItemDTO> getMyCompletedWorkItems(String filterType, Integer day, Integer month, Integer year, String keyword, Pageable pageable);

    WorkItemSummaryDTO getMyWorkItemSummary(String filterType, Integer day, Integer month, Integer year);

    List<Map<String, Object>> getMyWorkItemTrend(String filterType, Integer day, Integer month, Integer year);

    // ── Customer-scoped analytics ──────────────────────────────────────────────
    Map<String, Object> getCustomerOrderSummary(Long customerId, LocalDate from, LocalDate to);

    List<Map<String, Object>> getCustomerOrderChart(Long customerId, LocalDate from, LocalDate to, String granularity);

    // ── Staff-scoped analytics (orders created by a specific username) ─────────
    Map<String, Object> getStaffOrderSummary(String createdBy, LocalDate from, LocalDate to);

    List<Map<String, Object>> getStaffOrderChart(String createdBy, LocalDate from, LocalDate to, String granularity);

    Page<OrderDTO> getStaffOrders(String createdBy, String status, LocalDate from, LocalDate to, Pageable pageable);

    // ── Kitchen Display ────────────────────────────────────────────────────────

    /** Returns all PENDING + IN_PROGRESS orders (unfiltered by user) for the kitchen view. */
    List<OrderDTO> getKitchenOrders();

    /**
     * Cycle kitchen item status: PENDING → IN_PROGRESS → COMPLETED.
     * Returns the updated item's parent order.
     */
    OrderItemDTO bumpKitchenItem(Long itemId);

    // ── QR customer-order confirmation ───────────────────────────────────────────

    /** Customer-submitted (SUBMITTED) orders awaiting owner confirmation, oldest first. */
    List<OrderDTO> getPendingConfirmationOrders();

    /** Owner confirms a SUBMITTED order → PENDING (enters kitchen) and occupies its table. */
    OrderDTO confirmOrder(Long orderId);

    /** Owner rejects a SUBMITTED order → CANCELLED with an optional reason. */
    OrderDTO rejectOrder(Long orderId, String reason);
}
