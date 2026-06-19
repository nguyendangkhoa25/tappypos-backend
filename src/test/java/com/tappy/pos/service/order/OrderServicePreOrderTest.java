package com.tappy.pos.service.order;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.SettlePreOrderRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl pre-order settle Unit Tests")
class OrderServicePreOrderTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        lenient().when(messageService.getMessage(any())).thenReturn("err");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** A PENDING pre-order: total 500k, deposit 200k already paid, no items, no customer. */
    private Order pendingPreorder() {
        Order o = new Order();
        o.setId(9L);
        o.setOrderNumber("ORD-PRE-1");
        o.setStatus(Order.OrderStatus.PENDING);
        o.setOrderType(Order.OrderType.SELL);
        o.setPreorder(true);
        o.setTotalAmount(new BigDecimal("500000"));
        o.setDepositAmount(new BigDecimal("200000"));
        o.setAmountPaid(new BigDecimal("200000"));
        return o;
    }

    @Test
    @DisplayName("settlePreOrder collects balance, sets amountPaid=total and completes")
    void settle_completesAndCollectsBalance() {
        Order order = pendingPreorder();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        SettlePreOrderRequest req = new SettlePreOrderRequest();
        req.setPaymentMethod("CASH");
        req.setAmountReceived(new BigDecimal("300000")); // exact balance

        OrderDTO dto = service.settlePreOrder(9L, req);

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(order.getAmountPaid()).isEqualByComparingTo("500000");
        assertThat(order.getChangeAmount()).isEqualByComparingTo("0");
        assertThat(order.getCompletedBy()).isEqualTo("owner");
        assertThat(dto.getBalanceDue()).isEqualByComparingTo("0");
        verify(activityLogService).logAsync(eq("shop1"), eq("owner"), any(),
                eq(ActivityAction.PREORDER_SETTLED), eq("ORDER"), eq("ORD-PRE-1"), any(), any());
    }

    @Test
    @DisplayName("settlePreOrder computes change when more cash is tendered than the balance")
    void settle_computesChange() {
        Order order = pendingPreorder();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        SettlePreOrderRequest req = new SettlePreOrderRequest();
        req.setAmountReceived(new BigDecimal("350000")); // 50k change on a 300k balance

        service.settlePreOrder(9L, req);

        assertThat(order.getChangeAmount()).isEqualByComparingTo("50000");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("settlePreOrder rejects underpayment of the balance")
    void settle_rejectsUnderpayment() {
        Order order = pendingPreorder();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        SettlePreOrderRequest req = new SettlePreOrderRequest();
        req.setAmountReceived(new BigDecimal("100000")); // < 300k balance

        assertThatThrownBy(() -> service.settlePreOrder(9L, req))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("settlePreOrder refuses a normal (non-preorder) order")
    void settle_refusesNonPreorder() {
        Order order = pendingPreorder();
        order.setPreorder(false);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.settlePreOrder(9L, new SettlePreOrderRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPreOrderSummary returns deposits held + pending/today counts")
    void summary_aggregates() {
        Order todayPickup = pendingPreorder();
        todayPickup.setPickupTime(java.time.LocalDateTime.now().withHour(16).withMinute(0));
        Order laterPickup = pendingPreorder();
        laterPickup.setPickupTime(java.time.LocalDateTime.now().plusDays(3));
        when(orderRepository.sumDepositsHeld()).thenReturn(new BigDecimal("400000"));
        when(orderRepository.findPreorders(eq(Order.OrderStatus.PENDING), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(todayPickup, laterPickup),
                        org.springframework.data.domain.PageRequest.of(0, 1000), 2));

        var s = service.getPreOrderSummary();

        assertThat(s.getDepositsHeld()).isEqualByComparingTo("400000");
        assertThat(s.getPendingCount()).isEqualTo(2);
        assertThat(s.getTodayCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("settlePreOrder refuses a pre-order that is not PENDING")
    void settle_refusesNonPending() {
        Order order = pendingPreorder();
        order.setStatus(Order.OrderStatus.COMPLETED);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.settlePreOrder(9L, new SettlePreOrderRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }
}
