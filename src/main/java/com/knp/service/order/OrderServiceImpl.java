package com.knp.service.order;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.enums.ActivityAction;
import com.knp.multitenant.TenantContext;
import com.knp.model.dto.tenant.ReceiptPreviewRequest;
import com.knp.model.dto.order.CancelOrderRequest;
import com.knp.model.dto.order.MyWorkStatsDTO;
import com.knp.model.dto.order.OrderDTO;
import com.knp.model.dto.order.OrderItemDTO;
import com.knp.model.dto.order.VoidOrderRequest;
import com.knp.model.entity.order.Order;
import com.knp.model.entity.order.OrderItem;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.repository.order.OrderRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.util.ReceiptHtmlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.knp.service.customer.LoyaltyService;
import com.knp.service.inventory.InventoryService;
import com.knp.service.tenant.PrintTemplateService;
import com.knp.service.audit.ActivityLogService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final LoyaltyService loyaltyService;
    private final InventoryService inventoryService;
    private final MessageService messageService;
    private final PrintTemplateService printTemplateService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

    @Override
    public Page<OrderDTO> getAllOrders(String status, Pageable pageable) {
        log.info("Request: Get all orders - status: {}, page: {}, size: {}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<Order> orders;
        if (status != null && !status.isBlank()) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findAllActiveByStatus(orderStatus, pageable);
        } else {
            orders = orderRepository.findAllActive(pageable);
        }

        log.info("Retrieved {} orders", orders.getTotalElements());
        return orders.map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> searchOrders(String keyword, Pageable pageable) {
        log.info("Request: Search orders - keyword: {}, page: {}", keyword, pageable.getPageNumber());
        return orderRepository.searchByKeyword(keyword, pageable).map(this::mapToDTO);
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
        return ReceiptHtmlBuilder.build(order, shopInfo, printTemplateService.getReceiptConfig());
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
        Object[] stats = orderRepository.getMyCompletedStats(username, period[0], period[1]);
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
                .build();
    }
}
