package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.order.CancelOrderRequest;
import com.knp.model.dto.order.OrderDTO;
import com.knp.model.dto.order.VoidOrderRequest;
import com.knp.model.entity.Customer;
import com.knp.model.entity.Order;
import com.knp.model.entity.Order.OrderStatus;
import com.knp.repository.OrderRepository;
import com.knp.repository.ShopInfoRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

        Page<OrderDTO> result = orderService.getAllOrders(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("Should filter orders by status")
    void testGetAllOrders_WithStatusFilter() {
        Page<Order> page = new PageImpl<>(java.util.List.of(completedOrder));
        when(orderRepository.findAllActiveByStatus(OrderStatus.COMPLETED, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderService.getAllOrders("COMPLETED", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActiveByStatus(OrderStatus.COMPLETED, pageable);
    }

    @Test
    @DisplayName("Should ignore blank status and return all orders")
    void testGetAllOrders_BlankStatus() {
        Page<Order> page = new PageImpl<>(java.util.List.of(pendingOrder));
        when(orderRepository.findAllActive(pageable)).thenReturn(page);

        orderService.getAllOrders("  ", pageable);

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
        com.knp.model.entity.OrderItem item = new com.knp.model.entity.OrderItem();
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
        com.knp.model.entity.OrderItem item = new com.knp.model.entity.OrderItem();
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
}
