package com.tappy.pos.service.order;

import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.MyWorkStatsDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
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

    OrderDTO voidOrder(Long id, VoidOrderRequest request);

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

    List<Map<String, Object>> getTopEmployeesByRange(int limit, LocalDateTime from, LocalDateTime to);

    void softDeleteOrder(Long id);

    // ── Item-level work queue (MY_WORK feature) ────────────────────────────────
    Page<WorkItemDTO> getMyWorkItems(Pageable pageable);

    Page<WorkItemDTO> getMyPendingWorkItems(Pageable pageable);

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
}
