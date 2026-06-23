package com.tappy.pos.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.order.AddOrderItemRequest;
import com.tappy.pos.model.dto.order.CancelOrderRequest;
import com.tappy.pos.model.dto.order.ExchangeOrderItemRequest;
import com.tappy.pos.model.dto.order.MergeBillRequest;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.OrderItemDTO;
import com.tappy.pos.model.dto.order.PayAndCompleteRequest;
import com.tappy.pos.model.dto.order.SplitBillRequest;
import com.tappy.pos.model.dto.order.UpdateOrderMetaRequest;
import com.tappy.pos.model.dto.order.VoidOrderRequest;
import com.tappy.pos.model.dto.product.ProductDTO;
import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.tenant.ReceiptTemplateConfig;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.Order.OrderStatus;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.finance.BankAccountRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.customer.LoyaltyService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.service.notification.NotificationService;
import com.tappy.pos.service.product.ProductService;
import com.tappy.pos.service.table.TableService;
import com.tappy.pos.service.tenant.PrintTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Core CRUD / status-transition / scoping / split-merge / receipt unit tests for
 * {@link OrderServiceImpl}. Complements {@link OrderServicePreOrderTest} (pre-order settle) and
 * {@link OrderServiceWorkItemAnalyticsTest} (analytics + work-item queue + softDelete) — those areas
 * are intentionally not duplicated here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private InventoryService inventoryService;
    @Mock private ProductService productService;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private MessageService messageService;
    @Mock private PrintTemplateService printTemplateService;
    @Mock private ActivityLogService activityLogService;
    @Mock private NotificationService notificationService;
    @Mock private TenantContext tenantContext;
    @Mock private FeatureContext featureContext;
    @Mock private TableRepository tableRepository;
    @Mock private TableService tableService;

    // Real ObjectMapper so render()/serializeArgs()/deserializeArgs() behave like production.
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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

        // Generic message + tenant stubs so exception paths and logAsync calls don't NPE.
        lenient().when(messageService.getMessage(anyString())).thenReturn("msg");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("tenant-1");

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

    // ── helpers ─────────────────────────────────────────────────────────────────

    private OrderItem item(Long id, Long productId, Long variantId, int qty, BigDecimal unitPrice) {
        OrderItem it = new OrderItem();
        it.setId(id);
        it.setProductId(productId);
        it.setVariantId(variantId);
        it.setQuantity(qty);
        it.setUnitPrice(unitPrice);
        it.setProductName("Sản phẩm " + id);
        it.setItemType(OrderItem.ItemType.STANDARD);
        it.setStatus(OrderItem.ItemStatus.PENDING);
        return it;
    }

    private InventoryDTO inv(Long id, Long stock) {
        return InventoryDTO.builder().id(id).quantityInStock(stock).build();
    }

    private ProductVariant activeVariant(Long id, BigDecimal price) {
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setPriceOverride(price);
        v.setStatus(ProductVariant.VariantStatus.ACTIVE);
        return v;
    }

    private Order submitted(Long id, Long tableId) {
        Order o = Order.builder()
                .orderNumber("SUB-" + id)
                .status(OrderStatus.SUBMITTED)
                .orderType(Order.OrderType.SELL)
                .totalAmount(new BigDecimal("123000"))
                .createdBy("cashier01")
                .tableId(tableId)
                .orderItems(new ArrayList<>())
                .build();
        o.setId(id);
        o.setDeleted(false);
        return o;
    }

    private VoidOrderRequest voidReq(String reason, String by) {
        VoidOrderRequest r = new VoidOrderRequest();
        r.setReason(reason);
        r.setVoidedBy(by);
        return r;
    }

    // ── getAllOrders ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllOrders: no filter returns all active (view-all)")
    void testGetAllOrders_NoFilter() {
        when(orderRepository.findAllActive(pageable))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        Page<OrderDTO> result = orderService.getAllOrders(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("getAllOrders: status-only filter (view-all)")
    void testGetAllOrders_StatusOnly() {
        when(orderRepository.findAllActiveByStatus(OrderStatus.COMPLETED, pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.getAllOrders("COMPLETED", null, pageable);

        verify(orderRepository).findAllActiveByStatus(OrderStatus.COMPLETED, pageable);
    }

    @Test
    @DisplayName("getAllOrders: blank status falls through to all active")
    void testGetAllOrders_BlankStatus() {
        when(orderRepository.findAllActive(pageable))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        orderService.getAllOrders("  ", null, pageable);

        verify(orderRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("getAllOrders: status + orderType (view-all)")
    void testGetAllOrders_StatusAndType_ViewAll() {
        when(orderRepository.findAllActiveByStatusAndOrderType(
                OrderStatus.COMPLETED, Order.OrderType.SELL, pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.getAllOrders("COMPLETED", "SELL", pageable);

        verify(orderRepository).findAllActiveByStatusAndOrderType(
                OrderStatus.COMPLETED, Order.OrderType.SELL, pageable);
    }

    @Test
    @DisplayName("getAllOrders: orderType-only (view-all)")
    void testGetAllOrders_TypeOnly_ViewAll() {
        when(orderRepository.findAllActiveByOrderType(Order.OrderType.SELL, pageable))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        orderService.getAllOrders(null, "SELL", pageable);

        verify(orderRepository).findAllActiveByOrderType(Order.OrderType.SELL, pageable);
    }

    @Test
    @DisplayName("getAllOrders: no filter scoped to creator when ORDER_VIEW_ALL absent")
    void testGetAllOrders_NoFilter_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findAllActiveByCreatedBy("cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        orderService.getAllOrders(null, null, pageable);

        verify(orderRepository).findAllActiveByCreatedBy("cashier01", pageable);
        verify(orderRepository, never()).findAllActive(any());
    }

    @Test
    @DisplayName("getAllOrders: status scoped when ORDER_VIEW_ALL absent")
    void testGetAllOrders_Status_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findAllActiveByStatusAndCreatedBy(
                OrderStatus.COMPLETED, "cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.getAllOrders("COMPLETED", null, pageable);

        verify(orderRepository).findAllActiveByStatusAndCreatedBy(
                OrderStatus.COMPLETED, "cashier01", pageable);
    }

    @Test
    @DisplayName("getAllOrders: status + type scoped when ORDER_VIEW_ALL absent")
    void testGetAllOrders_StatusAndType_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findAllActiveByStatusAndOrderTypeAndCreatedBy(
                OrderStatus.COMPLETED, Order.OrderType.SELL, "cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.getAllOrders("COMPLETED", "SELL", pageable);

        verify(orderRepository).findAllActiveByStatusAndOrderTypeAndCreatedBy(
                OrderStatus.COMPLETED, Order.OrderType.SELL, "cashier01", pageable);
    }

    @Test
    @DisplayName("getAllOrders: orderType scoped when ORDER_VIEW_ALL absent")
    void testGetAllOrders_Type_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findAllActiveByOrderTypeAndCreatedBy(
                Order.OrderType.SELL, "cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        orderService.getAllOrders(null, "SELL", pageable);

        verify(orderRepository).findAllActiveByOrderTypeAndCreatedBy(
                Order.OrderType.SELL, "cashier01", pageable);
    }

    // ── searchOrders ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchOrders: view-all path")
    void testSearchOrders_ViewAll() {
        when(orderRepository.searchByKeyword("123", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.searchOrders("123", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).searchByKeyword("123", pageable);
    }

    @Test
    @DisplayName("searchOrders: scoped to creator when ORDER_VIEW_ALL absent")
    void testSearchOrders_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.searchByKeywordAndCreatedBy("abc", "cashier01", pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.searchOrders("abc", pageable);

        verify(orderRepository).searchByKeywordAndCreatedBy("abc", "cashier01", pageable);
    }

    // ── getOrderById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById: returns DTO")
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertThat(result.getOrderNumber()).isEqualTo("ORD-000001");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getOrderById: not found throws")
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById: deleted order throws")
    void testGetOrderById_Deleted() {
        pendingOrder.setDeleted(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById: owner-scoped access allowed for own order")
    void testGetOrderById_OwnerScoped_Allowed() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getOrderById: throws 404 when not owner and ORDER_VIEW_ALL absent")
    void testGetOrderById_NotOwned_Throws() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        Order other = Order.builder()
                .orderNumber("ORD-OTHER").status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO).createdBy("other_user")
                .orderItems(new ArrayList<>()).build();
        other.setId(99L);
        other.setDeleted(false);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getQuotes / convertQuote ─────────────────────────────────────────────────

    @Test
    @DisplayName("getQuotes: maps active quotes")
    void testGetQuotes() {
        pendingOrder.setQuote(true);
        when(orderRepository.findActiveQuotes()).thenReturn(List.of(pendingOrder));

        var result = orderService.getQuotes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isQuote()).isTrue();
    }

    @Test
    @DisplayName("convertQuote: not a quote throws BadRequest")
    void testConvertQuote_NotQuote() {
        pendingOrder.setQuote(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.convertQuote(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("convertQuote: completes a UNIQUE-item quote, marks sold and awards loyalty")
    void testConvertQuote_Success() {
        pendingOrder.setQuote(true);
        pendingOrder.setCustomer(customer);
        OrderItem oi = item(11L, 100L, null, 1, new BigDecimal("100000"));
        pendingOrder.setOrderItems(new ArrayList<>(List.of(oi)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(productService.getProductById(100L)).thenReturn(
                ProductDTO.builder().id(100L).name("Vòng vàng").inventoryMode("UNIQUE").build());

        OrderDTO result = orderService.convertQuote(1L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.isQuote()).isFalse();
        verify(productService).markAsSold(100L);
        verify(loyaltyService).awardPointsForOrder(eq(10L), any(), any());
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_CONFIRMED), eq("ORDER"), anyString(),
                anyString(), any(), any());
    }

    @Test
    @DisplayName("convertQuote: insufficient TRACKED stock throws before save")
    void testConvertQuote_InsufficientStock() {
        pendingOrder.setQuote(true);
        OrderItem oi = item(12L, 200L, null, 5, new BigDecimal("50000"));
        pendingOrder.setOrderItems(new ArrayList<>(List.of(oi)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(productService.getProductById(200L)).thenReturn(
                ProductDTO.builder().id(200L).name("Nhẫn").inventoryMode("TRACKED").build());
        when(inventoryService.getInventoryByProductId(eq(200L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(900L, 2L))));

        assertThatThrownBy(() -> orderService.convertQuote(1L))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    // ── startOrder ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startOrder: PENDING → IN_PROGRESS")
    void testStartOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.startOrder(1L);

        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("startOrder: non-PENDING throws BadRequest")
    void testStartOrder_NotPending() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));

        assertThatThrownBy(() -> orderService.startOrder(2L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("startOrder: not found throws")
    void testStartOrder_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.startOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── completeOrder ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeOrder: IN_PROGRESS → COMPLETED and releases table")
    void testCompleteOrder_Success() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.completeOrder(2L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(activityLogService).logAsync(anyString(), eq("cashier01"), any(),
                eq(ActivityAction.ORDER_COMPLETED), eq("ORDER"), anyString(),
                anyString(), any(), any());
    }

    @Test
    @DisplayName("completeOrder: awards loyalty when customer present")
    void testCompleteOrder_AwardsLoyalty() {
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.completeOrder(2L);

        verify(loyaltyService).awardPointsForOrder(eq(10L), any(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("completeOrder: no customer skips loyalty")
    void testCompleteOrder_NoCustomer() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.completeOrder(2L);

        verify(loyaltyService, never()).awardPointsForOrder(any(), any(), any());
    }

    @Test
    @DisplayName("completeOrder: loyalty failure does not abort")
    void testCompleteOrder_LoyaltyFailure() {
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("down")).when(loyaltyService).awardPointsForOrder(any(), any(), any());

        OrderDTO result = orderService.completeOrder(2L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("completeOrder: split-group member uses split release path")
    void testCompleteOrder_SplitGroupMember() {
        inProgressOrder.setParentOrderId(500L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findByParentOrderId(500L)).thenReturn(List.of());
        when(orderRepository.findById(500L)).thenReturn(Optional.empty());

        OrderDTO result = orderService.completeOrder(2L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("completeOrder: already COMPLETED throws")
    void testCompleteOrder_AlreadyCompleted() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.completeOrder(3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("completeOrder: CANCELLED throws")
    void testCompleteOrder_Cancelled() {
        Order cancelled = Order.builder().orderNumber("ORD-X").status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO).orderItems(new ArrayList<>()).build();
        cancelled.setId(4L);
        cancelled.setDeleted(false);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> orderService.completeOrder(4L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: PENDING cancelled with explicit cancelledBy, pushes notification")
    void testCancelOrder_Pending() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Khách đổi ý");
        req.setCancelledBy("manager01");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.cancelOrder(1L, req);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(argThat(o -> "manager01".equals(o.getCancelledBy())));
        verify(activityLogService).logAsync(anyString(), eq("manager01"), any(),
                eq(ActivityAction.ORDER_CANCELLED), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
        verify(notificationService).pushToRolesAsync(any(), any(), any(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("cancelOrder: preorder uses PREORDER_CANCELLED action")
    void testCancelOrder_Preorder() {
        pendingOrder.setPreorder(true);
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("Hủy đặt");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.cancelOrder(1L, req);

        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.PREORDER_CANCELLED), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("cancelOrder: falls back to JWT principal when cancelledBy blank")
    void testCancelOrder_PrincipalFallback() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("x");
        req.setCancelledBy(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.cancelOrder(1L, req);

        verify(orderRepository).save(argThat(o -> "cashier01".equals(o.getCancelledBy())));
    }

    @Test
    @DisplayName("cancelOrder: notification failure does not abort")
    void testCancelOrder_NotificationFailure() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("x");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("notif down")).when(notificationService)
                .pushToRolesAsync(any(), any(), any(), anyString(), any(), any(), anyString());

        OrderDTO result = orderService.cancelOrder(1L, req);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder: COMPLETED throws")
    void testCancelOrder_Completed() {
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("x");
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(3L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancelOrder: already CANCELLED throws")
    void testCancelOrder_AlreadyCancelled() {
        Order cancelled = Order.builder().orderNumber("ORD-Y").status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO).orderItems(new ArrayList<>()).build();
        cancelled.setId(5L);
        cancelled.setDeleted(false);
        CancelOrderRequest req = new CancelOrderRequest();
        req.setReason("x");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> orderService.cancelOrder(5L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── voidOrder ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("voidOrder: COMPLETED → VOIDED with explicit voidedBy")
    void testVoidOrder_Success() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.voidOrder(3L, voidReq("Tranh chấp", "manager01"));

        assertThat(result.getStatus()).isEqualTo("VOIDED");
        verify(orderRepository).save(argThat(o -> "manager01".equals(o.getVoidedBy())));
        // logAsync(tenantId, actor, fullName, action, targetType, targetId, messageKey, ip, args...)
        // — voidOrder passes two varargs (orderNumber, reason), so match all 10 positions.
        verify(activityLogService).logAsync(anyString(), eq("manager01"), any(),
                eq(ActivityAction.ORDER_VOIDED), eq("ORDER"), any(),
                anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("voidOrder: non-COMPLETED throws")
    void testVoidOrder_NotCompleted() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.voidOrder(1L, voidReq("x", "m")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("voidOrder: falls back to JWT principal when voidedBy blank")
    void testVoidOrder_PrincipalFallback() {
        VoidOrderRequest req = new VoidOrderRequest();
        req.setReason("x");
        req.setVoidedBy(null);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.voidOrder(3L, req);

        verify(orderRepository).save(argThat(o -> "cashier01".equals(o.getVoidedBy())));
    }

    @Test
    @DisplayName("voidOrder: restores stock when inventory exists")
    void testVoidOrder_RestoresStock() {
        OrderItem oi = item(14L, 400L, null, 3, new BigDecimal("10000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(400L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(902L, 5L))));

        orderService.voidOrder(3L, voidReq("Trả hàng", "manager01"));

        verify(inventoryService).addStock(eq(902L), anyLong());
    }

    @Test
    @DisplayName("voidOrder: no inventory row — completes without restore")
    void testVoidOrder_NoInventory() {
        OrderItem oi = item(15L, 401L, null, 1, new BigDecimal("10000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(401L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        OrderDTO result = orderService.voidOrder(3L, voidReq("x", "m"));

        assertThat(result.getStatus()).isEqualTo("VOIDED");
        verify(inventoryService, never()).addStock(any(), anyLong());
    }

    @Test
    @DisplayName("voidOrder: inventory failure does not abort")
    void testVoidOrder_InventoryFailure() {
        OrderItem oi = item(16L, 402L, null, 1, new BigDecimal("10000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(402L), any()))
                .thenThrow(new RuntimeException("inv down"));

        OrderDTO result = orderService.voidOrder(3L, voidReq("x", "m"));

        assertThat(result.getStatus()).isEqualTo("VOIDED");
    }

    // ── exchangeOrderItem ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("exchangeOrderItem: non-COMPLETED throws")
    void testExchange_NotCompleted() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(2L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(1L, 99L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: item not found throws")
    void testExchange_ItemNotFound() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(2L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: item without variant throws")
    void testExchange_NotVariant() {
        OrderItem oi = item(20L, 500L, null, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(2L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 20L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: same variant throws")
    void testExchange_SameVariant() {
        OrderItem oi = item(21L, 500L, 7L, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(7L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 21L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: invalid target variant throws")
    void testExchange_VariantInvalid() {
        OrderItem oi = item(22L, 500L, 7L, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.empty());
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 22L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: inactive variant throws")
    void testExchange_InactiveVariant() {
        OrderItem oi = item(27L, 500L, 7L, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        ProductVariant v = activeVariant(8L, new BigDecimal("100000"));
        v.setStatus(ProductVariant.VariantStatus.INACTIVE);
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.of(v));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 27L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: price mismatch throws")
    void testExchange_PriceMismatch() {
        OrderItem oi = item(23L, 500L, 7L, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.of(activeVariant(8L, new BigDecimal("150000"))));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 23L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: no inventory for new variant throws")
    void testExchange_NoNewInventory() {
        OrderItem oi = item(24L, 500L, 7L, 1, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.of(activeVariant(8L, new BigDecimal("100000"))));
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(500L), eq(8L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 24L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: insufficient new-variant stock throws")
    void testExchange_InsufficientStock() {
        OrderItem oi = item(25L, 500L, 7L, 3, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.of(activeVariant(8L, new BigDecimal("100000"))));
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(500L), eq(8L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(903L, 1L))));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        assertThatThrownBy(() -> orderService.exchangeOrderItem(3L, 25L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("exchangeOrderItem: success swaps variant, moves stock, logs")
    void testExchange_Success() {
        OrderItem oi = item(26L, 500L, 7L, 2, new BigDecimal("100000"));
        completedOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(8L, 500L))
                .thenReturn(Optional.of(activeVariant(8L, new BigDecimal("100000"))));
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(500L), eq(8L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(904L, 10L))));
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(500L), eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(905L, 5L))));
        ExchangeOrderItemRequest req = new ExchangeOrderItemRequest();
        req.setNewVariantId(8L);

        orderService.exchangeOrderItem(3L, 26L, req);

        assertThat(oi.getVariantId()).isEqualTo(8L);
        verify(inventoryService).removeStock(eq(904L), anyLong());
        verify(inventoryService).addStock(eq(905L), anyLong());
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_EXCHANGED), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
    }

    // ── receipt generation ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateReceipt: returns HTML")
    void testGenerateReceipt() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(printTemplateService.getReceiptConfig()).thenReturn(new ReceiptTemplateConfig());

        String html = orderService.generateReceipt(1L);

        assertThat(html).isNotNull();
    }

    @Test
    @DisplayName("generatePreviewReceipt: returns preview HTML")
    void testGeneratePreviewReceipt() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(printTemplateService.getReceiptConfig()).thenReturn(new ReceiptTemplateConfig());
        ReceiptPreviewRequest req = new ReceiptPreviewRequest();
        req.setItems(List.of());
        req.setTotalDiscount(BigDecimal.ZERO);
        req.setTotal(new BigDecimal("100000"));

        String html = orderService.generatePreviewReceipt(req);

        assertThat(html).isNotNull();
    }

    // ── customer / my-orders queries ───────────────────────────────────────────────

    @Test
    @DisplayName("getOrdersByCustomerId delegates to repository")
    void testGetOrdersByCustomerId() {
        when(orderRepository.findByCustomerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        Page<OrderDTO> result = orderService.getOrdersByCustomerId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMyPendingOrders uses current principal")
    void testGetMyPendingOrders() {
        when(orderRepository.findActiveByCreatedBy(eq("cashier01"), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        Page<OrderDTO> result = orderService.getMyPendingOrders(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMyCompletedOrders: DAY filter")
    void testGetMyCompletedOrders_Day() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        orderService.getMyCompletedOrders("DAY", null, null, null, pageable);

        verify(orderRepository).findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable));
    }

    @Test
    @DisplayName("getMyCompletedOrders: MONTH filter")
    void testGetMyCompletedOrders_Month() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getMyCompletedOrders("MONTH", null, 6, 2026, pageable);

        verify(orderRepository).findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable));
    }

    @Test
    @DisplayName("getMyCompletedOrders: YEAR filter")
    void testGetMyCompletedOrders_Year() {
        when(orderRepository.findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getMyCompletedOrders("YEAR", null, null, 2024, pageable);

        verify(orderRepository).findCompletedByCreatedByAndPeriod(eq("cashier01"), any(), any(), eq(pageable));
    }

    @Test
    @DisplayName("getMyWorkStats: populated stats")
    void testGetMyWorkStats() {
        when(orderRepository.countActiveByCreatedBy(eq("cashier01"), any())).thenReturn(3L);
        when(orderRepository.getMyCompletedStats(eq("cashier01"), any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{10L, new BigDecimal("500000")}));

        var result = orderService.getMyWorkStats("DAY", null, null, null);

        assertThat(result.getPendingCount()).isEqualTo(3L);
        assertThat(result.getCompletedCount()).isEqualTo(10L);
        assertThat(result.getCompletedRevenue()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("getMyWorkStats: empty stats → zeroes")
    void testGetMyWorkStats_Empty() {
        when(orderRepository.countActiveByCreatedBy(eq("cashier01"), any())).thenReturn(0L);
        when(orderRepository.getMyCompletedStats(eq("cashier01"), any(), any())).thenReturn(Collections.emptyList());

        var result = orderService.getMyWorkStats("DAY", null, null, null);

        assertThat(result.getCompletedCount()).isEqualTo(0L);
        assertThat(result.getCompletedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── staff / customer reporting ───────────────────────────────────────────────

    @Test
    @DisplayName("getStaffOrderSummary: null revenue defaults to zero")
    void testGetStaffOrderSummary() {
        when(orderRepository.sumRevenueByCreatedBy(eq("cashier01"), any(), any())).thenReturn(null);
        when(orderRepository.countByCreatedByAndDateRange(eq("cashier01"), any(), any())).thenReturn(0L);
        when(orderRepository.countByCreatedByAndDateRangeAndStatus(eq("cashier01"), eq("COMPLETED"), any(), any())).thenReturn(0L);
        when(orderRepository.countByCreatedByAndDateRangeAndStatus(eq("cashier01"), eq("CANCELLED"), any(), any())).thenReturn(0L);

        var result = orderService.getStaffOrderSummary("cashier01", LocalDate.now(), LocalDate.now());

        assertThat(result.get("totalRevenue")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("avgOrderValue")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getStaffOrderChart: day granularity")
    void testGetStaffOrderChart() {
        when(orderRepository.getDailyRevenueByCreatedBy(eq("cashier01"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-22", new BigDecimal("50000")}));

        var result = orderService.getStaffOrderChart("cashier01", LocalDate.now(), LocalDate.now(), "day");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("label")).isEqualTo("2026-06-22");
    }

    @Test
    @DisplayName("getStaffOrders delegates to repository")
    void testGetStaffOrders() {
        when(orderRepository.findAllByCreatedBy(eq("cashier01"), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(completedOrder)));

        var result = orderService.getStaffOrders("cashier01", null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getCustomerOrderChart: week granularity")
    void testGetCustomerOrderChart() {
        when(orderRepository.getWeeklyRevenueByCustomer(eq(10L), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-W25", new BigDecimal("75000")}));

        var result = orderService.getCustomerOrderChart(10L, LocalDate.now(), LocalDate.now(), "week");

        assertThat(result).hasSize(1);
    }

    // ── kitchen / QR confirmation ───────────────────────────────────────────────

    @Test
    @DisplayName("getKitchenOrders / getPendingConfirmationOrders map to DTOs")
    void testKitchenAndPending() {
        when(orderRepository.findAllKitchenOrders()).thenReturn(List.of(inProgressOrder));
        when(orderRepository.findAllSubmittedOrders()).thenReturn(List.of(pendingOrder));

        assertThat(orderService.getKitchenOrders()).hasSize(1);
        assertThat(orderService.getPendingConfirmationOrders()).hasSize(1);
    }

    @Test
    @DisplayName("bumpKitchenItem: PENDING → IN_PROGRESS → COMPLETED → PENDING cycle")
    void testBumpKitchenItem_Cycle() {
        OrderItem oi = item(80L, 1200L, null, 1, new BigDecimal("10000"));
        when(orderItemRepository.findById(80L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.bumpKitchenItem(80L);
        assertThat(oi.getStatus()).isEqualTo(OrderItem.ItemStatus.IN_PROGRESS);
        orderService.bumpKitchenItem(80L);
        assertThat(oi.getStatus()).isEqualTo(OrderItem.ItemStatus.COMPLETED);
        orderService.bumpKitchenItem(80L);
        assertThat(oi.getStatus()).isEqualTo(OrderItem.ItemStatus.PENDING);
    }

    @Test
    @DisplayName("confirmOrder: SUBMITTED → PENDING, occupies table, logs")
    void testConfirmOrder_Success() {
        Order o = submitted(50L, 7L);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.confirmOrder(50L);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(tableService).occupyTable(7L, 50L);
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_CONFIRMED), eq("ORDER"), anyString(),
                anyString(), any(), any());
    }

    @Test
    @DisplayName("confirmOrder: table occupy failure does not abort")
    void testConfirmOrder_TableFails() {
        Order o = submitted(51L, 8L);
        when(orderRepository.findById(51L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("busy")).when(tableService).occupyTable(8L, 51L);

        OrderDTO result = orderService.confirmOrder(51L);

        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("confirmOrder: non-SUBMITTED throws")
    void testConfirmOrder_NotSubmitted() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("rejectOrder: with reason cancels and logs")
    void testRejectOrder_WithReason() {
        Order o = submitted(52L, null);
        when(orderRepository.findById(52L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.rejectOrder(52L, "Hết món");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_REJECTED), eq("ORDER"), anyString(),
                anyString(), any(), any());
    }

    @Test
    @DisplayName("rejectOrder: blank reason uses system default")
    void testRejectOrder_DefaultReason() {
        Order o = submitted(53L, null);
        when(orderRepository.findById(53L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.rejectOrder(53L, "  ");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("rejectOrder: non-SUBMITTED throws")
    void testRejectOrder_NotSubmitted() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> orderService.rejectOrder(3L, "x"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateDeliveryStatus: non-delivery channel throws")
    void testUpdateDeliveryStatus_NotDelivery() {
        pendingOrder.setOrderChannel(Order.OrderChannel.DINE_IN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.updateDeliveryStatus(1L, Order.DeliveryStatus.DELIVERING))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateDeliveryStatus: delivery order updates and logs")
    void testUpdateDeliveryStatus_Success() {
        pendingOrder.setOrderChannel(Order.OrderChannel.DELIVERY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.updateDeliveryStatus(1L, Order.DeliveryStatus.DELIVERED);

        assertThat(result.getDeliveryStatus()).isEqualTo("DELIVERED");
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_DELIVERY_UPDATED), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
    }

    // ── IN_PROGRESS item mutations ───────────────────────────────────────────────

    @Test
    @DisplayName("addItemToOrder: non IN_PROGRESS throws")
    void testAddItemToOrder_NotInProgress() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        AddOrderItemRequest req = new AddOrderItemRequest();
        req.setProductId(600L);
        req.setQuantity(1);

        assertThatThrownBy(() -> orderService.addItemToOrder(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addItemToOrder: adds item, deducts stock, recalculates total")
    void testAddItemToOrder_Success() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productService.getProductById(600L)).thenReturn(
                ProductDTO.builder().id(600L).name("Cà phê").price(new BigDecimal("40000"))
                        .costPrice(new BigDecimal("10000")).build());
        when(inventoryService.getInventoryByProductId(eq(600L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(906L, 100L))));

        AddOrderItemRequest req = new AddOrderItemRequest();
        req.setProductId(600L);
        req.setQuantity(2);
        req.setNote("Ít đường");

        OrderItemDTO result = orderService.addItemToOrder(2L, req);

        assertThat(result.getProductName()).isEqualTo("Cà phê");
        verify(inventoryService).removeStock(eq(906L), eq(2L));
        assertThat(inProgressOrder.getTotalAmount()).isEqualByComparingTo("380000");
    }

    @Test
    @DisplayName("addItemToOrder: assigns employee when provided")
    void testAddItemToOrder_WithEmployee() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productService.getProductById(601L)).thenReturn(
                ProductDTO.builder().id(601L).name("Gội đầu").price(new BigDecimal("50000")).build());
        when(inventoryService.getInventoryByProductId(eq(601L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        Employee emp = new Employee();
        emp.setId(42L);
        emp.setFullName("Thợ B");
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(emp));

        AddOrderItemRequest req = new AddOrderItemRequest();
        req.setProductId(601L);
        req.setQuantity(1);
        req.setEmployeeId(42L);

        OrderItemDTO result = orderService.addItemToOrder(2L, req);

        assertThat(result.getAssignedEmployeeName()).isEqualTo("Thợ B");
    }

    @Test
    @DisplayName("removeItemFromOrder: removes item and restores stock")
    void testRemoveItemFromOrder_Success() {
        OrderItem oi = item(30L, 700L, null, 2, new BigDecimal("50000"));
        oi.setOrder(inProgressOrder);
        inProgressOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(30L)).thenReturn(Optional.of(oi));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(700L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(907L, 5L))));

        orderService.removeItemFromOrder(2L, 30L);

        verify(orderItemRepository).delete(oi);
        verify(inventoryService).addStock(eq(907L), anyLong());
    }

    @Test
    @DisplayName("removeItemFromOrder: item from another order throws")
    void testRemoveItemFromOrder_WrongOrder() {
        OrderItem oi = item(31L, 700L, null, 1, new BigDecimal("50000"));
        oi.setOrder(completedOrder);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(31L)).thenReturn(Optional.of(oi));

        assertThatThrownBy(() -> orderService.removeItemFromOrder(2L, 31L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("removeItemFromOrder: order not IN_PROGRESS throws")
    void testRemoveItemFromOrder_NotInProgress() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.removeItemFromOrder(1L, 30L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateItemQuantity: zero quantity throws")
    void testUpdateItemQuantity_NonPositive() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));

        assertThatThrownBy(() -> orderService.updateItemQuantity(2L, 1L, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateItemQuantity: increase deducts stock delta")
    void testUpdateItemQuantity_Increase() {
        OrderItem oi = item(32L, 800L, null, 1, new BigDecimal("20000"));
        oi.setOrder(inProgressOrder);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(32L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(800L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(908L, 50L))));

        OrderItemDTO result = orderService.updateItemQuantity(2L, 32L, 3);

        assertThat(result.getQuantity()).isEqualTo(3);
        verify(inventoryService).removeStock(eq(908L), eq(2L));
    }

    @Test
    @DisplayName("updateItemQuantity: decrease restores stock delta")
    void testUpdateItemQuantity_Decrease() {
        OrderItem oi = item(33L, 800L, null, 5, new BigDecimal("20000"));
        oi.setOrder(inProgressOrder);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(33L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryService.getInventoryByProductId(eq(800L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(909L, 50L))));

        orderService.updateItemQuantity(2L, 33L, 2);

        verify(inventoryService).addStock(eq(909L), eq(3L));
    }

    @Test
    @DisplayName("updateItemEmployee: clears employee when null")
    void testUpdateItemEmployee_Clear() {
        OrderItem oi = item(34L, 800L, null, 1, new BigDecimal("20000"));
        oi.setOrder(inProgressOrder);
        oi.setAssignedEmployeeId(99L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(34L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.updateItemEmployee(2L, 34L, null);

        assertThat(oi.getAssignedEmployeeId()).isNull();
    }

    @Test
    @DisplayName("updateItemEmployee: assigns employee by id")
    void testUpdateItemEmployee_Assign() {
        OrderItem oi = item(35L, 800L, null, 1, new BigDecimal("20000"));
        oi.setOrder(inProgressOrder);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(35L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Employee emp = new Employee();
        emp.setId(42L);
        emp.setFullName("Trần Thợ");
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(emp));

        orderService.updateItemEmployee(2L, 35L, 42L);

        assertThat(oi.getAssignedEmployeeId()).isEqualTo(42L);
        assertThat(oi.getAssignedEmployeeName()).isEqualTo("Trần Thợ");
    }

    @Test
    @DisplayName("updateItemNote: trims and sets note")
    void testUpdateItemNote() {
        OrderItem oi = item(36L, 800L, null, 1, new BigDecimal("20000"));
        oi.setOrder(inProgressOrder);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderItemRepository.findById(36L)).thenReturn(Optional.of(oi));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.updateItemNote(2L, 36L, "  Không cay  ");

        assertThat(oi.getNote()).isEqualTo("Không cay");
    }

    @Test
    @DisplayName("updateOrderMeta: updates tip, customer and payment method")
    void testUpdateOrderMeta_Success() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));

        UpdateOrderMetaRequest req = new UpdateOrderMetaRequest();
        req.setTip(new BigDecimal("20000"));
        req.setCustomerId(10L);
        req.setPaymentMethod("TRANSFER");

        OrderDTO result = orderService.updateOrderMeta(2L, req);

        assertThat(result.getPaymentMethod()).isEqualTo("TRANSFER");
        assertThat(result.getTipAmount()).isEqualByComparingTo("20000");
        assertThat(inProgressOrder.getTotalAmount()).isEqualByComparingTo("320000");
    }

    @Test
    @DisplayName("updateOrderMeta: clearCustomer removes the customer")
    void testUpdateOrderMeta_ClearCustomer() {
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        UpdateOrderMetaRequest req = new UpdateOrderMetaRequest();
        req.setClearCustomer(true);

        OrderDTO result = orderService.updateOrderMeta(2L, req);

        assertThat(result.getCustomerId()).isNull();
    }

    @Test
    @DisplayName("updateOrderMeta: non IN_PROGRESS throws")
    void testUpdateOrderMeta_NotInProgress() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.updateOrderMeta(1L, new UpdateOrderMetaRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("payAndCompleteOrder: pays, completes items, awards loyalty, logs")
    void testPayAndComplete_Success() {
        OrderItem oi = item(40L, 900L, null, 1, new BigDecimal("300000"));
        inProgressOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        inProgressOrder.setCustomer(customer);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PayAndCompleteRequest req = new PayAndCompleteRequest();
        req.setPaymentMethod("CASH");
        req.setAmountPaid(new BigDecimal("500000"));

        OrderDTO result = orderService.payAndCompleteOrder(2L, req);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getChangeAmount()).isEqualByComparingTo("200000");
        assertThat(oi.getStatus()).isEqualTo(OrderItem.ItemStatus.COMPLETED);
        verify(loyaltyService).awardPointsForOrder(eq(10L), any(), any());
    }

    @Test
    @DisplayName("payAndCompleteOrder: defaults payment method and amountPaid")
    void testPayAndComplete_Defaults() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.payAndCompleteOrder(2L, new PayAndCompleteRequest());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getPaymentMethod()).isEqualTo("CASH");
    }

    @Test
    @DisplayName("payAndCompleteOrder: non IN_PROGRESS throws")
    void testPayAndComplete_NotInProgress() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.payAndCompleteOrder(1L, new PayAndCompleteRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    // ── splitBill ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("splitBill: invalid status throws")
    void testSplitBill_InvalidStatus() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("EVEN");
        req.setSplitCount(2);

        assertThatThrownBy(() -> orderService.splitBill(3L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("splitBill: already a child throws")
    void testSplitBill_AlreadyChild() {
        inProgressOrder.setParentOrderId(99L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("EVEN");
        req.setSplitCount(2);

        assertThatThrownBy(() -> orderService.splitBill(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("splitBill EVEN: invalid count throws")
    void testSplitBill_Even_BadCount() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("EVEN");
        req.setSplitCount(1);

        assertThatThrownBy(() -> orderService.splitBill(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("splitBill EVEN: N children sum to total, source voided, logs")
    void testSplitBill_Even_Success() {
        inProgressOrder.setTotalAmount(new BigDecimal("300000"));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        SplitBillRequest req = new SplitBillRequest();
        req.setMode("EVEN");
        req.setSplitCount(3);

        List<OrderDTO> children = orderService.splitBill(2L, req);

        assertThat(children).hasSize(3);
        BigDecimal sum = children.stream().map(OrderDTO::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("300000");
        assertThat(inProgressOrder.getStatus()).isEqualTo(OrderStatus.VOIDED);
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_SPLIT), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("splitBill ITEM: no groups throws")
    void testSplitBill_Item_NoGroups() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("ITEM");
        req.setGroups(List.of());

        assertThatThrownBy(() -> orderService.splitBill(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("splitBill ITEM: moves selected qty to a child, remainder on source")
    void testSplitBill_Item_Success() {
        OrderItem oi = item(60L, 1000L, null, 4, new BigDecimal("25000"));
        oi.setOrder(inProgressOrder);
        inProgressOrder.setTotalAmount(new BigDecimal("100000"));
        inProgressOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        SplitBillRequest.SplitItem si = new SplitBillRequest.SplitItem();
        si.setItemId(60L);
        si.setQuantity(1);
        SplitBillRequest.SplitGroup group = new SplitBillRequest.SplitGroup();
        group.setItems(List.of(si));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("ITEM");
        req.setGroups(List.of(group));

        List<OrderDTO> checks = orderService.splitBill(2L, req);

        assertThat(checks).hasSize(2);
        BigDecimal sum = checks.stream().map(OrderDTO::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100000");
        assertThat(oi.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("splitBill ITEM: invalid quantity throws")
    void testSplitBill_Item_BadQuantity() {
        OrderItem oi = item(61L, 1000L, null, 2, new BigDecimal("25000"));
        oi.setOrder(inProgressOrder);
        inProgressOrder.setOrderItems(new ArrayList<>(List.of(oi)));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());

        SplitBillRequest.SplitItem si = new SplitBillRequest.SplitItem();
        si.setItemId(61L);
        si.setQuantity(5);
        SplitBillRequest.SplitGroup group = new SplitBillRequest.SplitGroup();
        group.setItems(List.of(si));
        SplitBillRequest req = new SplitBillRequest();
        req.setMode("ITEM");
        req.setGroups(List.of(group));

        assertThatThrownBy(() -> orderService.splitBill(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── mergeBill ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("mergeBill: same source and target throws")
    void testMergeBill_Same() {
        MergeBillRequest req = new MergeBillRequest();
        req.setSourceOrderId(2L);

        assertThatThrownBy(() -> orderService.mergeBill(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("mergeBill: invalid status throws")
    void testMergeBill_InvalidStatus() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        MergeBillRequest req = new MergeBillRequest();
        req.setSourceOrderId(2L);

        assertThatThrownBy(() -> orderService.mergeBill(3L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("mergeBill: moves items, voids source, releases table, logs")
    void testMergeBill_Success() {
        Order target = Order.builder().orderNumber("TGT-1").status(OrderStatus.IN_PROGRESS)
                .orderType(Order.OrderType.SELL).totalAmount(new BigDecimal("100000"))
                .createdBy("cashier01").orderItems(new ArrayList<>()).build();
        target.setId(70L);
        target.setDeleted(false);

        OrderItem srcItem = item(71L, 1100L, null, 2, new BigDecimal("30000"));
        Order source = Order.builder().orderNumber("SRC-1").status(OrderStatus.IN_PROGRESS)
                .orderType(Order.OrderType.SELL).totalAmount(new BigDecimal("60000"))
                .createdBy("cashier01").orderItems(new ArrayList<>(List.of(srcItem))).build();
        source.setId(72L);
        source.setDeleted(false);
        srcItem.setOrder(source);

        when(orderRepository.findById(70L)).thenReturn(Optional.of(target));
        when(orderRepository.findById(72L)).thenReturn(Optional.of(source));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MergeBillRequest req = new MergeBillRequest();
        req.setSourceOrderId(72L);

        OrderDTO result = orderService.mergeBill(70L, req);

        assertThat(result).isNotNull();
        assertThat(target.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(source.getStatus()).isEqualTo(OrderStatus.VOIDED);
        verify(activityLogService).logAsync(anyString(), anyString(), any(),
                eq(ActivityAction.ORDER_MERGED), eq("ORDER"), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("mergeBill: null source id throws")
    void testMergeBill_NullSource() {
        MergeBillRequest req = new MergeBillRequest();
        req.setSourceOrderId(null);

        assertThatThrownBy(() -> orderService.mergeBill(70L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── getPreOrders ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPreOrders: view-all path with status")
    void testGetPreOrders_ViewAll() {
        pendingOrder.setPreorder(true);
        when(orderRepository.findPreorders(eq(OrderStatus.PENDING), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pendingOrder)));

        Page<OrderDTO> result = orderService.getPreOrders("PENDING", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPreOrders: scoped to creator when ORDER_VIEW_ALL absent")
    void testGetPreOrders_Scoped() {
        when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
        when(orderRepository.findPreordersByCreatedBy(any(), any(), any(), eq("cashier01"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getPreOrders(null, LocalDate.now(), LocalDate.now(), pageable);

        verify(orderRepository).findPreordersByCreatedBy(any(), any(), any(), eq("cashier01"), eq(pageable));
    }

    @Test
    @DisplayName("getPreOrders: invalid status throws")
    void testGetPreOrders_InvalidStatus() {
        assertThatThrownBy(() -> orderService.getPreOrders("NOPE", null, null, pageable))
                .isInstanceOf(BadRequestException.class);
    }
}
