package com.tappy.pos.service.order;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.MyWorkStatsDTO;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.OrderItemDTO;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.model.dto.order.WorkItemDTO;
import com.tappy.pos.model.dto.order.WorkItemSummaryDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.entity.finance.BankAccount;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.finance.BankAccountRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.util.ReceiptHtmlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tappy.pos.service.customer.LoyaltyService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.service.tenant.PrintTemplateService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.repository.table.TableRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final LoyaltyService loyaltyService;
    private final InventoryService inventoryService;
    private final MessageService messageService;
    private final PrintTemplateService printTemplateService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;
    private final FeatureContext featureContext;
    private final TableRepository tableRepository;

    @Override
    public Page<OrderDTO> getAllOrders(String status, String orderType, Pageable pageable) {
        log.info("Request: Get all orders - status: {}, orderType: {}, page: {}, size: {}",
                status, orderType, pageable.getPageNumber(), pageable.getPageSize());

        boolean canViewAll = featureContext.hasFeature("ORDER_VIEW_ALL");
        String username = getCurrentUsername();
        Page<Order> orders;

        boolean hasStatus    = status != null && !status.isBlank();
        boolean hasOrderType = orderType != null && !orderType.isBlank();

        if (hasStatus && hasOrderType) {
            Order.OrderStatus  os = Order.OrderStatus.valueOf(status.toUpperCase());
            Order.OrderType    ot = Order.OrderType.valueOf(orderType.toUpperCase());
            orders = canViewAll
                    ? orderRepository.findAllActiveByStatusAndOrderType(os, ot, pageable)
                    : orderRepository.findAllActiveByStatusAndOrderTypeAndCreatedBy(os, ot, username, pageable);
        } else if (hasStatus) {
            Order.OrderStatus os = Order.OrderStatus.valueOf(status.toUpperCase());
            orders = canViewAll
                    ? orderRepository.findAllActiveByStatus(os, pageable)
                    : orderRepository.findAllActiveByStatusAndCreatedBy(os, username, pageable);
        } else if (hasOrderType) {
            Order.OrderType ot = Order.OrderType.valueOf(orderType.toUpperCase());
            orders = canViewAll
                    ? orderRepository.findAllActiveByOrderType(ot, pageable)
                    : orderRepository.findAllActiveByOrderTypeAndCreatedBy(ot, username, pageable);
        } else {
            orders = canViewAll
                    ? orderRepository.findAllActive(pageable)
                    : orderRepository.findAllActiveByCreatedBy(username, pageable);
        }

        log.info("Retrieved {} orders (viewAll={})", orders.getTotalElements(), canViewAll);
        return orders.map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getAllOrdersFiltered(String status, String paymentMethod, java.time.LocalDate from, java.time.LocalDate to, Pageable pageable) {
        boolean canViewAll = featureContext.hasFeature("ORDER_VIEW_ALL");
        String username = getCurrentUsername();
        Page<Order> orders = canViewAll
                ? orderRepository.findAllFiltered(status, paymentMethod, from, to, pageable)
                : orderRepository.findAllFilteredByUser(username, status, paymentMethod, from, to, pageable);
        return orders.map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> searchOrders(String keyword, Pageable pageable) {
        log.info("Request: Search orders - keyword: {}, page: {}", keyword, pageable.getPageNumber());
        boolean canViewAll = featureContext.hasFeature("ORDER_VIEW_ALL");
        return canViewAll
                ? orderRepository.searchByKeyword(keyword, pageable).map(this::mapToDTO)
                : orderRepository.searchByKeywordAndCreatedBy(keyword, getCurrentUsername(), pageable).map(this::mapToDTO);
    }

    @Override
    public OrderDTO getOrderById(Long id) {
        log.info("Request: Get order by id: {}", id);
        Order order = orderRepository.findById(id)
                .filter(o -> !Boolean.TRUE.equals(o.getDeleted()))
                .orElseThrow(() -> {
                    log.error("Order not found - id: {}", id);
                    return new ResourceNotFoundException(
                            messageService.getMessage("error.order.not.found", id));
                });

        // Enforce row-level ownership when the user cannot view all orders
        if (!featureContext.hasFeature("ORDER_VIEW_ALL")) {
            String username = getCurrentUsername();
            if (!username.equals(order.getCreatedBy())) {
                log.warn("User {} attempted to access order {} owned by {}", username, id, order.getCreatedBy());
                throw new ResourceNotFoundException(messageService.getMessage("error.order.not.found", id));
            }
        }

        return mapToDTO(order);
    }

    private OrderDTO mapToDTO(Order order) {
        List<OrderItemDTO> items = order.getOrderItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : Order.OrderType.SELL.name())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .paymentMethod(order.getPaymentMethod())
                .amountPaid(order.getAmountPaid())
                .changeAmount(order.getChangeAmount())
                .notes(order.getNotes())
                .createdBy(order.getCreatedBy())
                .completedAt(order.getCompletedAt())
                .completedBy(order.getCompletedBy())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .cancelledBy(order.getCancelledBy())
                .voidedAt(order.getVoidedAt())
                .voidReason(order.getVoidReason())
                .voidedBy(order.getVoidedBy())
                .createdAt(order.getCreatedAt())
                .invoiceId(order.getInvoice() != null ? order.getInvoice().getId() : null)
                .invoiceNumber(order.getInvoice() != null ? order.getInvoice().getInvoiceNumber() : null)
                .promotionCode(order.getPromotionCode())
                .promotionDiscount(order.getPromotionDiscount())
                .loyaltyPointsRedeemed(order.getLoyaltyPointsRedeemed())
                .loyaltyDiscount(order.getLoyaltyDiscount())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public OrderDTO startOrder(Long id) {
        log.info("Request: Start order id: {}", id);
        Order order = findActiveOrder(id);

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.invalid.status.for.start", order.getStatus()));
        }

        order.start();
        Order saved = orderRepository.save(order);
        log.info("Order {} status changed to IN_PROGRESS", id);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public OrderDTO completeOrder(Long id) {
        log.info("Request: Complete order id: {}", id);
        Order order = findActiveOrder(id);

        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.cannot.modify"));
        }

        String completedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        order.complete(completedBy);
        Order saved = orderRepository.save(order);
        log.info("Order {} completed by {}", id, completedBy);
        releaseTableForOrder(saved);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), completedBy, null,
                ActivityAction.ORDER_COMPLETED, "ORDER", saved.getOrderNumber(),
                "Completed order #" + saved.getOrderNumber(), null);

        // Award loyalty points to customer if applicable
        if (saved.getCustomer() != null) {
            try {
                loyaltyService.awardPointsForOrder(
                        saved.getCustomer().getId(), saved.getId(), saved.getTotalAmount());
            } catch (Exception e) {
                log.warn("Failed to award loyalty points for order {}: {}", saved.getId(), e.getMessage());
            }
        }

        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long id, CancelOrderRequest request) {
        log.info("Request: Cancel order id: {}", id);
        Order order = findActiveOrder(id);

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.cannot.cancel.completed"));
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.cannot.modify"));
        }

        // Resolve who is cancelling: use request value, fall back to JWT principal
        String cancelledBy = (request.getCancelledBy() != null && !request.getCancelledBy().isBlank())
                ? request.getCancelledBy()
                : SecurityContextHolder.getContext().getAuthentication().getName();

        order.cancel(request.getReason(), cancelledBy);
        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled by {} — reason: {}", id, cancelledBy, request.getReason());
        releaseTableForOrder(saved);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), cancelledBy, null,
                ActivityAction.ORDER_CANCELLED, "ORDER", saved.getOrderNumber(),
                "Cancelled order #" + saved.getOrderNumber() + " — " + request.getReason(), null);

        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public OrderDTO voidOrder(Long id, VoidOrderRequest request) {
        log.info("Request: Void order id: {}", id);
        Order order = findActiveOrder(id);

        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.void.only.completed", order.getStatus()));
        }

        String voidedBy = (request.getVoidedBy() != null && !request.getVoidedBy().isBlank())
                ? request.getVoidedBy()
                : SecurityContextHolder.getContext().getAuthentication().getName();

        // Restore inventory for each item — log warnings on failure, never abort
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductId() == null) continue;
            try {
                var inventoryPage = inventoryService.getInventoryByProductId(
                        item.getProductId(), PageRequest.of(0, 1));
                if (!inventoryPage.isEmpty()) {
                    Long inventoryId = inventoryPage.getContent().get(0).getId();
                    inventoryService.addStock(inventoryId, (long) item.getQuantity());
                    log.info("Restored {} units of product {} to inventory", item.getQuantity(), item.getProductId());
                } else {
                    log.warn("No inventory found for product {} — stock not restored", item.getProductId());
                }
            } catch (Exception e) {
                log.warn("Inventory restoration failed for product {}: {}", item.getProductId(), e.getMessage());
            }
        }

        order.voidOrder(request.getReason(), voidedBy);
        Order saved = orderRepository.save(order);
        log.info("Order {} voided by {} — reason: {}", id, voidedBy, request.getReason());

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), voidedBy, null,
                ActivityAction.ORDER_VOIDED, "ORDER", saved.getOrderNumber(),
                "Voided order #" + saved.getOrderNumber() + " — " + request.getReason(), null);

        return mapToDTO(saved);
    }

    @Override
    public String generateReceipt(Long id) {
        log.info("Request: Generate receipt for order id: {}", id);
        Order order = findActiveOrder(id);
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc().orElse(null);
        var cfg = printTemplateService.getReceiptConfig();
        BankAccount bankAccount = cfg.isShowVietQr() ? bankAccountRepository.findDefault().orElse(null) : null;
        return ReceiptHtmlBuilder.build(order, shopInfo, cfg, bankAccount);
    }

    @Override
    public String generatePreviewReceipt(ReceiptPreviewRequest request) {
        log.info("Request: Generate preview receipt");
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc().orElse(null);
        return ReceiptHtmlBuilder.buildPreview(request, shopInfo, printTemplateService.getReceiptConfig());
    }

    @Override
    public Page<OrderDTO> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        log.info("Request: Get orders for customer {}", customerId);
        return orderRepository.findByCustomerId(customerId, pageable).map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getMyPendingOrders(Pageable pageable) {
        String username = getCurrentUsername();
        log.info("Request: Get my pending orders - user: {}", username);
        List<Order.OrderStatus> statuses = List.of(Order.OrderStatus.PENDING, Order.OrderStatus.IN_PROGRESS);
        return orderRepository.findActiveByCreatedBy(username, statuses, pageable).map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getMyCompletedOrders(String filterType, Integer day, Integer month, Integer year, Pageable pageable) {
        String username = getCurrentUsername();
        log.info("Request: Get my completed orders - user: {}, filter: {}", username, filterType);
        LocalDateTime[] period = resolvePeriod(filterType, day, month, year);
        return orderRepository.findCompletedByCreatedByAndPeriod(username, period[0], period[1], pageable).map(this::mapToDTO);
    }

    @Override
    public MyWorkStatsDTO getMyWorkStats(String filterType, Integer day, Integer month, Integer year) {
        String username = getCurrentUsername();
        List<Order.OrderStatus> pendingStatuses = List.of(Order.OrderStatus.PENDING, Order.OrderStatus.IN_PROGRESS);
        long pendingCount = orderRepository.countActiveByCreatedBy(username, pendingStatuses);

        LocalDateTime[] period = resolvePeriod(filterType, day, month, year);
        List<Object[]> statRows = orderRepository.getMyCompletedStats(username, period[0], period[1]);
        Object[] stats = statRows.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : statRows.get(0);
        long completedCount = ((Number) stats[0]).longValue();
        BigDecimal completedRevenue = (BigDecimal) stats[1];

        return new MyWorkStatsDTO(pendingCount, completedCount, completedRevenue);
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private LocalDateTime[] resolvePeriod(String filterType, Integer day, Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        int d = day != null ? day : now.getDayOfMonth();

        return switch (filterType != null ? filterType.toUpperCase() : "DAY") {
            case "MONTH" -> new LocalDateTime[]{
                LocalDate.of(y, m, 1).atStartOfDay(),
                LocalDate.of(y, m, 1).plusMonths(1).atStartOfDay()
            };
            case "YEAR" -> new LocalDateTime[]{
                LocalDate.of(y, 1, 1).atStartOfDay(),
                LocalDate.of(y, 1, 1).plusYears(1).atStartOfDay()
            };
            default -> {
                LocalDate date = LocalDate.of(y, m, d);
                yield new LocalDateTime[]{date.atStartOfDay(), date.plusDays(1).atStartOfDay()};
            }
        };
    }

    private void releaseTableForOrder(Order order) {
        try {
            tableRepository.findByTenantIdAndCurrentOrderId(order.getTenantId(), order.getId())
                    .ifPresent(table -> {
                        table.setStatus(com.tappy.pos.model.enums.TableStatus.AVAILABLE);
                        table.setCurrentOrderId(null);
                        tableRepository.save(table);
                        log.debug("Table {} released after order {}", table.getId(), order.getId());
                    });
        } catch (Exception e) {
            log.warn("Could not release table for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private Order findActiveOrder(Long id) {
        return orderRepository.findById(id)
                .filter(o -> !Boolean.TRUE.equals(o.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.order.not.found", id)));
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .amount(item.getAmount())
                .taxPercentage(item.getTaxPercentage())
                .taxAmount(item.getTaxAmount())
                .itemType(item.getItemType())
                .metadata(item.getMetadata())
                .assignedEmployeeId(item.getAssignedEmployeeId())
                .assignedEmployeeName(item.getAssignedEmployeeName())
                .commissionRate(item.getCommissionRate())
                .commissionAmount(item.getCommissionAmount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getOrderSummary(LocalDate from, LocalDate to, String status, String paymentMethod) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(java.time.LocalTime.MAX);
        BigDecimal totalRevenue = orderRepository.sumRevenueByDateRange(fromDt, toDt);
        long orderCount = orderRepository.countByDateRange(fromDt, toDt);
        long completedCount = orderRepository.countByDateRangeAndStatus(fromDt, toDt, Order.OrderStatus.COMPLETED);
        long cancelledCount = orderRepository.countByDateRangeAndStatus(fromDt, toDt, Order.OrderStatus.CANCELLED);
        BigDecimal avg = orderCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return java.util.Map.of("totalRevenue", totalRevenue, "orderCount", orderCount, "avgOrderValue", avg, "completedCount", completedCount, "cancelledCount", cancelledCount);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getOrderChart(LocalDate from, LocalDate to, String granularity) {
        java.time.LocalDateTime dtFrom = from.atStartOfDay();
        java.time.LocalDateTime dtTo = to.atTime(java.time.LocalTime.MAX);
        List<Object[]> rows = switch (granularity == null ? "day" : granularity) {
            case "hour"  -> orderRepository.getHourlyRevenue(dtFrom, dtTo);
            case "week"  -> orderRepository.getWeeklyRevenue(dtFrom, dtTo);
            case "month" -> orderRepository.getMonthlyRevenue(dtFrom, dtTo);
            case "year"  -> orderRepository.getYearlyRevenue(dtFrom, dtTo);
            default      -> orderRepository.getDailyRevenue(dtFrom, dtTo);
        };
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) { result.add(java.util.Map.of("label", row[0].toString(), "value", row[1])); }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getTopProducts(int limit, LocalDateTime from) {
        List<Object[]> rows = orderRepository.getTopProductsSince(from, org.springframework.data.domain.PageRequest.of(0, limit));
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            result.add(java.util.Map.of("name", row[0] != null ? row[0].toString() : "", "orderCount", ((Number) row[1]).longValue(), "revenue", row[2]));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getTopProductsByRange(int limit, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.getTopProductsByRange(from, to, org.springframework.data.domain.PageRequest.of(0, limit));
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            result.add(java.util.Map.of("name", row[0] != null ? row[0].toString() : "", "orderCount", ((Number) row[1]).longValue(), "revenue", row[2]));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getTopCustomersByRange(int limit, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.getTopCustomersByRange(from, to, org.springframework.data.domain.PageRequest.of(0, limit));
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("name", row[0] != null ? row[0].toString() : "");
            item.put("orderCount", ((Number) row[1]).longValue());
            item.put("totalSpend", row[2]);
            item.put("customerId", row[3] != null ? row[3].toString() : "");
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getTopEmployeesByRange(int limit, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.getTopEmployeesByRange(from, to, org.springframework.data.domain.PageRequest.of(0, limit));
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            result.add(java.util.Map.of("name", row[0] != null ? row[0].toString() : "", "orderCount", ((Number) row[1]).longValue(), "revenue", row[2]));
        }
        return result;
    }

    // ── Item-level work queue (MY_WORK feature) ────────────────────────────────

    @Override
    public Page<WorkItemDTO> getAvailableWorkItems(Pageable pageable) {
        log.info("Request: Get available (unassigned) work items");
        return orderItemRepository.findAvailableWorkItems(pageable).map(this::mapRowToWorkItemDTO);
    }

    @Override
    @Transactional
    public WorkItemDTO pickupWorkItem(Long itemId) {
        Employee employee = resolveCurrentEmployee();
        log.info("Request: Pickup work item {} - employeeId: {}", itemId, employee.getId());
        OrderItem item = orderItemRepository.findByIdAndAssignedEmployeeIdIsNull(itemId)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.order.item.already.assigned", "")));
        item.setAssignedEmployeeId(employee.getId());
        item.setAssignedEmployeeName(employee.getFullName());
        orderItemRepository.save(item);
        log.info("Work item {} picked up by employee {}", itemId, employee.getId());
        return loadWorkItemDTO(item);
    }

    @Override
    @Transactional
    public WorkItemDTO unpickWorkItem(Long itemId) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Unpick work item {} - employeeId: {}", itemId, employeeId);
        OrderItem item = orderItemRepository.findByIdAndAssignedEmployeeId(itemId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.order.item.not.found", itemId)));
        if (item.getStatus() != OrderItem.ItemStatus.PENDING) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.item.cannot.reassign"));
        }
        item.setAssignedEmployeeId(null);
        item.setAssignedEmployeeName(null);
        orderItemRepository.save(item);
        log.info("Work item {} unpicked by employee {}", itemId, employeeId);
        return loadWorkItemDTO(item);
    }

    @Override
    public Page<WorkItemDTO> getMyWorkItems(Pageable pageable) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Get all work items - employeeId: {}", employeeId);
        Page<Object[]> rows = orderItemRepository.findWorkItemsByEmployeeId(employeeId, pageable);
        return rows.map(this::mapRowToWorkItemDTO);
    }

    @Override
    public Page<WorkItemDTO> getMyPendingWorkItems(Pageable pageable) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Get pending work items - employeeId: {}", employeeId);
        Page<Object[]> rows = orderItemRepository.findPendingWorkItemsByEmployeeId(employeeId, pageable);
        return rows.map(this::mapRowToWorkItemDTO);
    }

    @Override
    @Transactional
    public WorkItemDTO startWorkItem(Long itemId) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Start work item {} - employeeId: {}", itemId, employeeId);
        OrderItem item = orderItemRepository.findByIdAndAssignedEmployeeId(itemId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.order.item.not.found", itemId)));
        if (item.getStatus() != OrderItem.ItemStatus.PENDING) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.item.invalid.status.for.start", item.getStatus()));
        }
        item.setStatus(OrderItem.ItemStatus.IN_PROGRESS);
        orderItemRepository.save(item);
        log.info("Work item {} started by employee {}", itemId, employeeId);
        return loadWorkItemDTO(item);
    }

    @Override
    @Transactional
    public WorkItemDTO completeWorkItem(Long itemId) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Complete work item {} - employeeId: {}", itemId, employeeId);
        OrderItem item = orderItemRepository.findByIdAndAssignedEmployeeId(itemId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.order.item.not.found", itemId)));
        if (item.getStatus() == OrderItem.ItemStatus.COMPLETED) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.item.cannot.reassign"));
        }
        item.setStatus(OrderItem.ItemStatus.COMPLETED);
        item.setCompletedAt(LocalDateTime.now());
        orderItemRepository.save(item);
        log.info("Work item {} completed by employee {}", itemId, employeeId);
        return loadWorkItemDTO(item);
    }

    @Override
    @Transactional
    public WorkItemDTO releaseWorkItem(Long itemId) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Release work item {} - employeeId: {}", itemId, employeeId);
        OrderItem item = orderItemRepository.findByIdAndAssignedEmployeeId(itemId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.order.item.not.found", itemId)));
        if (item.getStatus() != OrderItem.ItemStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    messageService.getMessage("error.order.item.not.assigned", item.getStatus()));
        }
        item.setStatus(OrderItem.ItemStatus.PENDING);
        orderItemRepository.save(item);
        log.info("Work item {} released by employee {}", itemId, employeeId);
        return loadWorkItemDTO(item);
    }

    // ── Completed work item history ────────────────────────────────────────────

    @Override
    public Page<WorkItemDTO> getMyCompletedWorkItems(String filterType, Integer day, Integer month, Integer year,
                                                      String keyword, Pageable pageable) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Get my completed work items - employeeId: {}, filter: {}", employeeId, filterType);
        LocalDateTime[] period = resolvePeriod(filterType, day, month, year);
        return orderItemRepository
                .findCompletedWorkItems(employeeId, period[0], period[1], keyword, pageable)
                .map(this::mapRowToWorkItemDTO);
    }

    @Override
    public WorkItemSummaryDTO getMyWorkItemSummary(String filterType, Integer day, Integer month, Integer year) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Get my work item summary - employeeId: {}, filter: {}", employeeId, filterType);
        LocalDateTime[] period = resolvePeriod(filterType, day, month, year);
        List<Object[]> rows = orderItemRepository.getWorkItemStats(employeeId, period[0], period[1]);
        if (rows.isEmpty()) {
            return WorkItemSummaryDTO.builder()
                    .completedCount(0).totalRevenue(BigDecimal.ZERO)
                    .totalDurationMinutes(0).totalCommission(BigDecimal.ZERO).build();
        }
        Object[] r = rows.get(0);
        return WorkItemSummaryDTO.builder()
                .completedCount(((Number) r[0]).longValue())
                .totalRevenue(toBigDecimal(r[1]))
                .totalDurationMinutes(((Number) r[2]).longValue())
                .totalCommission(toBigDecimal(r[3]))
                .build();
    }

    @Override
    public List<Map<String, Object>> getMyWorkItemTrend(String filterType, Integer day, Integer month, Integer year) {
        Long employeeId = resolveCurrentEmployeeId();
        log.info("Request: Get my work item trend - employeeId: {}, filter: {}", employeeId, filterType);
        LocalDateTime[] period = resolvePeriod(filterType, day, month, year);
        String type = filterType != null ? filterType.toUpperCase() : "DAY";

        List<Object[]> rows = switch (type) {
            case "YEAR" -> orderItemRepository.getWorkItemTrendByMonth(employeeId, period[0], period[1]);
            case "WEEK", "MONTH" -> orderItemRepository.getWorkItemTrendByDay(employeeId, period[0], period[1]);
            default -> orderItemRepository.getWorkItemTrendByHour(employeeId, period[0], period[1]);
        };

        return rows.stream().map(r -> {
            String label = switch (type) {
                case "YEAR" -> String.valueOf(((Number) r[0]).intValue());
                case "WEEK", "MONTH" -> r[0].toString();
                default -> String.format("%02d:00", ((Number) r[0]).intValue());
            };
            Map<String, Object> point = new java.util.LinkedHashMap<>();
            point.put("label", label);
            point.put("count", ((Number) r[1]).longValue());
            point.put("revenue", toBigDecimal(r[2]));
            return point;
        }).collect(Collectors.toList());
    }

    private Long resolveCurrentEmployeeId() {
        return resolveCurrentEmployee().getId();
    }

    private Employee resolveCurrentEmployee() {
        String username = getCurrentUsername();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.employee.not.found", username)));
    }

    private WorkItemDTO mapRowToWorkItemDTO(Object[] row) {
        return WorkItemDTO.builder()
                .itemId(toLong(row[0]))
                .orderId(toLong(row[1]))
                .orderNumber(toString(row[2]))
                .customerName(toString(row[3]))
                .productId(toLong(row[4]))
                .productName(toString(row[5]))
                .quantity(toInt(row[6]))
                .unitPrice(toBigDecimal(row[7]))
                .amount(toBigDecimal(row[8]))
                .durationMinutes(toInt(row[9]))
                .status(toString(row[10]))
                .completedAt(toLocalDateTime(row[11]))
                .assignedEmployeeId(toLong(row[12]))
                .assignedEmployeeName(toString(row[13]))
                .orderCreatedAt(toLocalDateTime(row[14]))
                .commissionRate(row.length > 15 ? toBigDecimal(row[15]) : null)
                .commissionAmount(row.length > 16 ? toBigDecimal(row[16]) : null)
                .build();
    }

    private WorkItemDTO loadWorkItemDTO(OrderItem item) {
        var order = item.getOrder();
        return WorkItemDTO.builder()
                .itemId(item.getId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .amount(item.getAmount())
                .durationMinutes(0)
                .status(item.getStatus().name())
                .completedAt(item.getCompletedAt())
                .assignedEmployeeId(item.getAssignedEmployeeId())
                .assignedEmployeeName(item.getAssignedEmployeeName())
                .orderCreatedAt(order.getCreatedAt())
                .build();
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    private static String toString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) return ldt;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    @Override
    @Transactional
    public void softDeleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.setDeleted(true);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getCustomerOrderSummary(Long customerId, LocalDate from, LocalDate to) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.atTime(java.time.LocalTime.MAX);
        BigDecimal totalRevenue = orderRepository.sumRevenueByCustomerAndDateRange(customerId, dtFrom, dtTo);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        long orderCount = orderRepository.countByCustomerAndDateRange(customerId, dtFrom, dtTo);
        long completedCount = orderRepository.countByCustomerAndDateRangeAndStatus(customerId, dtFrom, dtTo, "COMPLETED");
        BigDecimal avg = completedCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedCount), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return java.util.Map.of(
                "totalRevenue", totalRevenue,
                "orderCount", orderCount,
                "completedCount", completedCount,
                "avgOrderValue", avg);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getStaffOrderSummary(String createdBy, LocalDate from, LocalDate to) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.atTime(java.time.LocalTime.MAX);
        BigDecimal totalRevenue = orderRepository.sumRevenueByCreatedBy(createdBy, dtFrom, dtTo);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        long orderCount = orderRepository.countByCreatedByAndDateRange(createdBy, dtFrom, dtTo);
        long completedCount = orderRepository.countByCreatedByAndDateRangeAndStatus(createdBy, "COMPLETED", dtFrom, dtTo);
        long cancelledCount = orderRepository.countByCreatedByAndDateRangeAndStatus(createdBy, "CANCELLED", dtFrom, dtTo);
        BigDecimal avg = orderCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return java.util.Map.of("totalRevenue", totalRevenue, "orderCount", orderCount, "avgOrderValue", avg, "completedCount", completedCount, "cancelledCount", cancelledCount);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getStaffOrderChart(String createdBy, LocalDate from, LocalDate to, String granularity) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.atTime(java.time.LocalTime.MAX);
        List<Object[]> rows = switch (granularity == null ? "day" : granularity) {
            case "week"  -> orderRepository.getWeeklyRevenueByCreatedBy(createdBy, dtFrom, dtTo);
            case "month" -> orderRepository.getMonthlyRevenueByCreatedBy(createdBy, dtFrom, dtTo);
            case "year"  -> orderRepository.getYearlyRevenueByCreatedBy(createdBy, dtFrom, dtTo);
            default      -> orderRepository.getDailyRevenueByCreatedBy(createdBy, dtFrom, dtTo);
        };
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) { result.add(java.util.Map.of("label", row[0].toString(), "value", row[1])); }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getStaffOrders(String createdBy, String status, LocalDate from, LocalDate to, Pageable pageable) {
        return orderRepository.findAllByCreatedBy(createdBy, status, from, to, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getCustomerOrderChart(Long customerId, LocalDate from, LocalDate to, String granularity) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.atTime(java.time.LocalTime.MAX);
        List<Object[]> rows = switch (granularity == null ? "day" : granularity) {
            case "week"  -> orderRepository.getWeeklyRevenueByCustomer(customerId, dtFrom, dtTo);
            case "month" -> orderRepository.getMonthlyRevenueByCustomer(customerId, dtFrom, dtTo);
            case "year"  -> orderRepository.getYearlyRevenueByCustomer(customerId, dtFrom, dtTo);
            default      -> orderRepository.getDailyRevenueByCustomer(customerId, dtFrom, dtTo);
        };
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) { result.add(java.util.Map.of("label", row[0].toString(), "value", row[1])); }
        return result;
    }
}
