package com.tappy.pos.service.order;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.Order.OrderStatus;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.tenant.ReceiptTemplateConfig;
import com.tappy.pos.repository.finance.BankAccountRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.tappy.pos.service.customer.LoyaltyService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.tenant.PrintTemplateService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.FeatureContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShopInfoRepository shopInfoRepository;

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private MessageService messageService;

    @Mock
    private PrintTemplateService printTemplateService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private FeatureContext featureContext;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order pendingOrder;
    private Order inProgressOrder;
    private Order completedOrder;
    private Customer customer;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cashier01", null, Collections.emptyList()));

        // Default: user can view all orders (shop-owner scenario)
        lenient().when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(true);

        customer = Customer.builder()
                .name("Nguyễn Văn A")
                .phone("0901234567")
                .build();
        customer.setId(10L);

        pendingOrder = Order.builder()
                .orderNumber("ORD-000001")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("500000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paymentMethod("CASH")
                .amountPaid(new BigDecimal("500000"))
                .changeAmount(BigDecimal.ZERO)
                .createdBy("cashier01")
                .orderItems(new ArrayList<>())
                .build();
        pendingOrder.setId(1L);
        pendingOrder.setDeleted(false);

        inProgressOrder = Order.builder()
                .orderNumber("ORD-000002")
                .status(OrderStatus.IN_PROGRESS)
                .totalAmount(new BigDecimal("300000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paymentMethod("CASH")
                .createdBy("cashier01")
                .orderItems(new ArrayList<>())
                .build();
        inProgressOrder.setId(2L);
        inProgressOrder.setDeleted(false);

        completedOrder = Order.builder()
                .orderNumber("ORD-000003")
                .status(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("750000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paymentMethod("TRANSFER")
                .createdBy("cashier01")
                .orderItems(new ArrayList<>())
                .build();
        completedOrder.setId(3L);
        completedOrder.setDeleted(false);

        pageable = PageRequest.of(0, 20);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAllOrders ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active orders when no status filter")
    void testGetAllOrders_NoFilter() {
        Page<Order> page = new PageImpl<>(java.util.List.of(pendingOrder));
        when(orderRepository.findAllActive(pageable)).thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("Should filter orders by status")
    void testGetAllOrders_WithStatusFilter() {
        Page<Order> page = new PageImpl<>(java.util.List.of(completedOrder));
        when(orderRepository.findAllActiveByStatus(OrderStatus.COMPLETED, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders("COMPLETED", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActiveByStatus(OrderStatus.COMPLETED, pageable);
    }

    @Test
    @DisplayName("Should ignore blank status and return all orders")
    void testGetAllOrders_BlankStatus() {
        Page<Order> page = new PageImpl<>(java.util.List.of(pendingOrder));
        when(orderRepository.findAllActive(pageable)).thenReturn(page);

        orderService.getAllOrders("  ", null, pageable);

        verify(orderRepository).findAllActive(pageable);
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return order DTO by id")
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-000001");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent order")
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for deleted order")
    void testGetOrderById_Deleted() {
        pendingOrder.setDeleted(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── startOrder ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should transition PENDING order to IN_PROGRESS")
    void testStartOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        OrderDTO result = orderService.startOrder(1L);

        assertThat(result).isNotNull();
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    @DisplayName("Should throw BadRequestException when starting non-PENDING order")
    void testStartOrder_NotPending() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));

        assertThatThrownBy(() -> orderService.startOrder(2L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when starting non-existent order")
    void testStartOrder_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.startOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── completeOrder ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should complete an IN_PROGRESS order")
    void testCompleteOrder_Success() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        OrderDTO result = orderService.completeOrder(2L);

        assertThat(result).isNotNull();
        verify(orderRepository).save(inProgressOrder);
    }

    @Test
    @DisplayName("Should award loyalty points when order has customer")
    void testCompleteOrder_AwardsLoyaltyPoints() {
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        orderService.completeOrder(2L);

        verify(loyaltyService).awardPointsForOrder(
                eq(customer.getId()), eq(inProgressOrder.getId()), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should not award loyalty points when order has no customer")
    void testCompleteOrder_NoCustomer_SkipsLoyalty() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        orderService.completeOrder(2L);

        verify(loyaltyService, never()).awardPointsForOrder(any(), any(), any());
    }

    @Test
    @DisplayName("Should still complete order even if loyalty service throws")
    void testCompleteOrder_LoyaltyServiceFailure_DoesNotAbort() {
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);
        doThrow(new RuntimeException("Loyalty service unavailable"))
                .when(loyaltyService).awardPointsForOrder(any(), any(), any());

        OrderDTO result = orderService.completeOrder(2L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw BadRequestException when order is already COMPLETED")
    void testCompleteOrder_AlreadyCompleted() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.completeOrder(3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw BadRequestException when order is CANCELLED")
    void testCompleteOrder_Cancelled() {
        Order cancelled = Order.builder()
                .orderNumber("ORD-000004")
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();
        cancelled.setId(4L);
        cancelled.setDeleted(false);

        when(orderRepository.findById(4L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> orderService.completeOrder(4L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should cancel a PENDING order")
    void testCancelOrder_PendingOrder() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Customer changed mind");
        req.setCancelledBy("cashier01");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        OrderDTO result = orderService.cancelOrder(1L, req);

        assertThat(result).isNotNull();
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    @DisplayName("Should cancel an IN_PROGRESS order")
    void testCancelOrder_InProgressOrder() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("System error");
        req.setCancelledBy("manager01");

        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(inProgressOrder);

        OrderDTO result = orderService.cancelOrder(2L, req);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should fall back to JWT principal when cancelledBy not in request")
    void testCancelOrder_UsesPrincipalWhenCancelledByBlank() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Test");
        req.setCancelledBy(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        orderService.cancelOrder(1L, req);

        verify(orderRepository).save(argThat(o -> "cashier01".equals(o.getCancelledBy())));
    }

    @Test
    @DisplayName("Should throw BadRequestException when cancelling a COMPLETED order")
    void testCancelOrder_CompletedOrder() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Mistake");

        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(3L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw BadRequestException when order is already CANCELLED")
    void testCancelOrder_AlreadyCancelled() {
        Order cancelled = Order.builder()
                .orderNumber("ORD-000005")
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();
        cancelled.setId(5L);
        cancelled.setDeleted(false);

        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Re-cancel");

        when(orderRepository.findById(5L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> orderService.cancelOrder(5L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── voidOrder ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should void a COMPLETED order")
    void testVoidOrder_Success() {
        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("Customer dispute");
        req.setVoidedBy("manager01");

        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);

        OrderDTO result = orderService.voidOrder(3L, req);

        assertThat(result).isNotNull();
        verify(orderRepository).save(completedOrder);
    }

    @Test
    @DisplayName("Should throw BadRequestException when voiding non-COMPLETED order")
    void testVoidOrder_NotCompleted() {
        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("Void pending");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.voidOrder(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should fall back to JWT principal when voidedBy not in request")
    void testVoidOrder_UsesPrincipalWhenVoidedByBlank() {
        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("Test");
        req.setVoidedBy(null);

        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);

        orderService.voidOrder(3L, req);

        verify(orderRepository).save(argThat(o -> "cashier01".equals(o.getVoidedBy())));
    }

    @Test
    @DisplayName("Should attempt inventory restoration for each order item when voiding")
    void testVoidOrder_TriesInventoryRestoration() {
        com.tappy.pos.model.entity.order.OrderItem item = new com.tappy.pos.model.entity.order.OrderItem();
        item.setProductId(100L);
        item.setQuantity(2);
        completedOrder.setOrderItems(java.util.List.of(item));

        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("Return");
        req.setVoidedBy("manager01");

        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);
        when(inventoryService.getInventoryByProductId(eq(100L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        orderService.voidOrder(3L, req);

        verify(inventoryService).getInventoryByProductId(eq(100L), any());
    }

    @Test
    @DisplayName("Should complete void even if inventory restoration fails")
    void testVoidOrder_InventoryFailure_DoesNotAbort() {
        com.tappy.pos.model.entity.order.OrderItem item = new com.tappy.pos.model.entity.order.OrderItem();
        item.setProductId(100L);
        item.setQuantity(1);
        completedOrder.setOrderItems(java.util.List.of(item));

        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("Return");
        req.setVoidedBy("manager01");

        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);
        when(inventoryService.getInventoryByProductId(eq(100L), any()))
                .thenThrow(new RuntimeException("Inventory service down"));

        OrderDTO result = orderService.voidOrder(3L, req);

        assertThat(result).isNotNull();
    }

    // ── searchOrders ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchOrders delegates to repository")
    void testSearchOrders() {
        when(orderRepository.searchByKeyword("123", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.searchOrders("123", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).searchByKeyword("123", pageable);
    }

    // ── ORDER_VIEW_ALL absent — scoped queries ────────────────────────────────

    @Test
    @DisplayName("getAllOrders: scoped to own orders when ORDER_VIEW_ALL absent")
    void testGetAllOrders_ScopedToOwn() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Page<Order> page = new PageImpl<>(List.of(pendingOrder));
        when(orderRepository.findAllActiveByCreatedBy("cashier01", pageable)).thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActiveByCreatedBy("cashier01", pageable);
        verify(orderRepository, never()).findAllActive(any());
    }

    @Test
    @DisplayName("getOrderById: throws 404 when user does not own the order and ORDER_VIEW_ALL absent")
    void testGetOrderById_NotOwned_ThrowsNotFound() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Order otherOrder = Order.builder()
                .orderNumber("ORD-OTHER")
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO)
                .createdBy("other_user")
                .orderItems(new ArrayList<>())
                .build();
        otherOrder.setId(99L);
        otherOrder.setDeleted(false);

        when(orderRepository.findById(99L)).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getOrdersByCustomerId ─────────────────────────────────────────────────

    @Test
    @DisplayName("getOrdersByCustomerId delegates to repository")
    void testGetOrdersByCustomerId() {
        when(orderRepository.findByCustomerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.getOrdersByCustomerId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByCustomerId(1L, pageable);
    }

    // ── getMyPendingOrders ────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyPendingOrders uses current principal as username")
    void testGetMyPendingOrders() {
        when(orderRepository.findActiveByCreatedBy(eq("cashier01"), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        Page<OrderDTO> result = orderService.getMyPendingOrders(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findActiveByCreatedBy(eq("cashier01"), any(), eq(pageable));
    }

    // ── getMyCompletedOrders ──────────────────────────────────────────────────

    @Test
    @DisplayName("getMyCompletedOrders uses DAY filter by default")
    void testGetMyCompletedOrders_DayFilter() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(
                eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.getMyCompletedOrders("DAY", null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findCompletedByCreatedByAndPeriod(
                eq("cashier01"), any(), any(), eq(pageable));
    }

    @Test
    @DisplayName("getMyCompletedOrders uses MONTH filter")
    void testGetMyCompletedOrders_MonthFilter() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(
                eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<OrderDTO> result = orderService.getMyCompletedOrders("MONTH", null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getMyCompletedOrders uses YEAR filter")
    void testGetMyCompletedOrders_YearFilter() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(
                eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getMyCompletedOrders("YEAR", null, null, 2024, pageable);

        verify(orderRepository).findCompletedByCreatedByAndPeriod(
                eq("cashier01"), any(), any(), eq(pageable));
    }

    // ── getMyWorkStats ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyWorkStats returns pending and completed counts")
    void testGetMyWorkStats() {
        when(orderRepository.countActiveByCreatedBy(eq("cashier01"), any())).thenReturn(3L);
        Object[] statsRow = new Object[]{ 10L, new BigDecimal("500000") };
        when(orderRepository.getMyCompletedStats(eq("cashier01"), any(), any())).thenReturn(Collections.singletonList(statsRow));

        var result = orderService.getMyWorkStats("DAY", null, null, null);

        assertThat(result.getPendingCount()).isEqualTo(3L);
        assertThat(result.getCompletedCount()).isEqualTo(10L);
        assertThat(result.getCompletedRevenue()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("getMyWorkStats: returns zeroes when no completed stats rows")
    void testGetMyWorkStats_EmptyStats() {
        when(orderRepository.countActiveByCreatedBy(eq("cashier01"), any())).thenReturn(0L);
        when(orderRepository.getMyCompletedStats(eq("cashier01"), any(), any()))
                .thenReturn(Collections.emptyList());

        var result = orderService.getMyWorkStats("DAY", null, null, null);

        assertThat(result.getPendingCount()).isEqualTo(0L);
        assertThat(result.getCompletedCount()).isEqualTo(0L);
        assertThat(result.getCompletedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getAllOrders: status+orderType combination ─────────────────────────────

    @Test
    @DisplayName("getAllOrders: filters by both status and orderType when both provided")
    void testGetAllOrders_StatusAndOrderType_ViewAll() {
        Page<Order> page = new PageImpl<>(List.of(completedOrder));
        when(orderRepository.findAllActiveByStatusAndOrderType(
                OrderStatus.COMPLETED, Order.OrderType.SELL, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders("COMPLETED", "SELL", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActiveByStatusAndOrderType(
                OrderStatus.COMPLETED, Order.OrderType.SELL, pageable);
    }

    @Test
    @DisplayName("getAllOrders: filters by both status and orderType scoped when no VIEW_ALL")
    void testGetAllOrders_StatusAndOrderType_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Page<Order> page = new PageImpl<>(List.of(completedOrder));
        when(orderRepository.findAllActiveByStatusAndOrderTypeAndCreatedBy(
                OrderStatus.COMPLETED, Order.OrderType.SELL, "cashier01", pageable)).thenReturn(page);

        orderService.getAllOrders("COMPLETED", "SELL", pageable);

        verify(orderRepository).findAllActiveByStatusAndOrderTypeAndCreatedBy(
                OrderStatus.COMPLETED, Order.OrderType.SELL, "cashier01", pageable);
    }

    @Test
    @DisplayName("getAllOrders: filters by orderType only when viewAll")
    void testGetAllOrders_OrderTypeOnly_ViewAll() {
        Page<Order> page = new PageImpl<>(List.of(pendingOrder));
        when(orderRepository.findAllActiveByOrderType(Order.OrderType.SELL, pageable))
                .thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders(null, "SELL", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActiveByOrderType(Order.OrderType.SELL, pageable);
    }

    @Test
    @DisplayName("getAllOrders: filters by orderType only scoped when no VIEW_ALL")
    void testGetAllOrders_OrderTypeOnly_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Page<Order> page = new PageImpl<>(List.of(pendingOrder));
        when(orderRepository.findAllActiveByOrderTypeAndCreatedBy(
                Order.OrderType.SELL, "cashier01", pageable)).thenReturn(page);

        orderService.getAllOrders(null, "SELL", pageable);

        verify(orderRepository).findAllActiveByOrderTypeAndCreatedBy(
                Order.OrderType.SELL, "cashier01", pageable);
    }

    @Test
    @DisplayName("getAllOrders: filters by status only scoped when no VIEW_ALL")
    void testGetAllOrders_StatusOnly_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Page<Order> page = new PageImpl<>(List.of(completedOrder));
        when(orderRepository.findAllActiveByStatusAndCreatedBy(
                OrderStatus.COMPLETED, "cashier01", pageable)).thenReturn(page);

        orderService.getAllOrders("COMPLETED", null, pageable);

        verify(orderRepository).findAllActiveByStatusAndCreatedBy(
                OrderStatus.COMPLETED, "cashier01", pageable);
    }

    @Test
    @DisplayName("searchOrders: scoped to own when VIEW_ALL absent")
    void testSearchOrders_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.searchByKeywordAndCreatedBy("abc", "cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.searchOrders("abc", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).searchByKeywordAndCreatedBy("abc", "cashier01", pageable);
    }

    // ── generateReceipt ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateReceipt: returns HTML string for existing order")
    void testGenerateReceipt_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        ReceiptTemplateConfig cfg = new ReceiptTemplateConfig();
        when(printTemplateService.getReceiptConfig()).thenReturn(cfg);

        String html = orderService.generateReceipt(1L);

        assertThat(html).isNotNull();
    }

    // ── generatePreviewReceipt ────────────────────────────────────────────────

    @Test
    @DisplayName("generatePreviewReceipt: returns preview HTML")
    void testGeneratePreviewReceipt() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        ReceiptTemplateConfig cfg = new ReceiptTemplateConfig();
        when(printTemplateService.getReceiptConfig()).thenReturn(cfg);

        ReceiptPreviewRequest req = new ReceiptPreviewRequest();
        req.setItems(List.of());
        req.setTotalDiscount(BigDecimal.ZERO);
        req.setTotal(new BigDecimal("100000"));

        String html = orderService.generatePreviewReceipt(req);

        assertThat(html).isNotNull();
    }
}
