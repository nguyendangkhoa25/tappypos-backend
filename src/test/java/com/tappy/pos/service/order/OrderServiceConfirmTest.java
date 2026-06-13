package com.tappy.pos.service.order;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.table.TableService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl QR confirm/reject Unit Tests")
class OrderServiceConfirmTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TableService tableService;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Order submittedOrder() {
        Order o = new Order();
        o.setId(5L);
        o.setOrderNumber("QR-20260613-12345");
        o.setStatus(Order.OrderStatus.SUBMITTED);
        o.setOrderType(Order.OrderType.SELL);
        o.setTableId(7L);
        o.setTableLabel("A1");
        return o;
    }

    @Test
    @DisplayName("confirmOrder moves SUBMITTED → PENDING and occupies the table")
    void confirmOrder_movesToPendingAndOccupiesTable() {
        Order order = submittedOrder();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDTO dto = service.confirmOrder(5L);

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(order.getConfirmedBy()).isEqualTo("owner");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        verify(tableService).occupyTable(7L, 5L);
    }

    @Test
    @DisplayName("confirmOrder rejects an order that is not awaiting confirmation")
    void confirmOrder_throwsWhenNotSubmitted() {
        Order order = submittedOrder();
        order.setStatus(Order.OrderStatus.COMPLETED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        lenient().when(messageService.getMessage("error.order.not.submitted")).thenReturn("not submitted");

        assertThatThrownBy(() -> service.confirmOrder(5L)).isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
        verify(tableService, never()).occupyTable(any(), any());
    }

    @Test
    @DisplayName("rejectOrder cancels a SUBMITTED order with a reason")
    void rejectOrder_cancels() {
        Order order = submittedOrder();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDTO dto = service.rejectOrder(5L, "Hết món");

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("Hết món");
        assertThat(order.getCancelledBy()).isEqualTo("owner");
        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        verify(tableService, never()).occupyTable(any(), any());
    }

    @Test
    @DisplayName("rejectOrder refuses an order that is not awaiting confirmation")
    void rejectOrder_throwsWhenNotSubmitted() {
        Order order = submittedOrder();
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        lenient().when(messageService.getMessage("error.order.not.submitted")).thenReturn("not submitted");

        assertThatThrownBy(() -> service.rejectOrder(5L, "x")).isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }
}
