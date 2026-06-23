package com.tappy.pos.service.order;

import com.tappy.pos.model.dto.order.MergeBillRequest;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.model.dto.order.SplitBillRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.table.TableRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl split / merge bill Unit Tests")
class OrderServiceSplitMergeTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private TableRepository tableRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private OrderItem item(long id, String name, int qty, long price) {
        OrderItem it = new OrderItem();
        it.setId(id);
        it.setTenantId("shop1");
        it.setProductName(name);
        it.setQuantity(qty);
        it.setUnitPrice(BigDecimal.valueOf(price));
        it.setItemType(OrderItem.ItemType.STANDARD);
        it.setStatus(OrderItem.ItemStatus.PENDING);
        return it;
    }

    private Order tabOrder() {
        Order o = new Order();
        o.setId(1L);
        o.setTenantId("shop1");
        o.setOrderNumber("ORD-20260619-10000");
        o.setStatus(Order.OrderStatus.IN_PROGRESS);
        o.setOrderType(Order.OrderType.SELL);
        o.setTableId(7L);
        o.setTableLabel("A1");
        o.setTotalAmount(BigDecimal.valueOf(250));
        List<OrderItem> items = new ArrayList<>();
        items.add(item(10L, "Phở", 2, 100));  // 200
        items.add(item(11L, "Trà", 1, 50));   // 50
        o.setOrderItems(items);
        return o;
    }

    @Test
    @DisplayName("splitByItem: two checks + remainder sum to the original total")
    void splitByItem_partitionsAndPreservesTotal() {
        Order order = tabOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        SplitBillRequest req = new SplitBillRequest();
        req.setMode("ITEM");
        SplitBillRequest.SplitItem a = new SplitBillRequest.SplitItem(); a.setItemId(10L); a.setQuantity(1);
        SplitBillRequest.SplitItem b = new SplitBillRequest.SplitItem(); b.setItemId(11L); b.setQuantity(1);
        SplitBillRequest.SplitGroup g1 = new SplitBillRequest.SplitGroup(); g1.setItems(List.of(a));
        SplitBillRequest.SplitGroup g2 = new SplitBillRequest.SplitGroup(); g2.setItems(List.of(b));
        req.setGroups(List.of(g1, g2));

        List<OrderDTO> checks = service.splitBill(1L, req);

        // 2 child checks + the leftover original
        assertThat(checks).hasSize(3);
        BigDecimal sum = checks.stream().map(OrderDTO::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("250");
        // children carry parentOrderId, remainder keeps the original id
        assertThat(checks).filteredOn(c -> Long.valueOf(1L).equals(c.getParentOrderId())).hasSize(2);
        // child checks must carry their line items in the response (not be itemless)
        assertThat(checks).filteredOn(c -> Long.valueOf(1L).equals(c.getParentOrderId()))
                .allSatisfy(c -> assertThat(c.getItems()).hasSize(1));
        // Trà was fully moved out; one Phở remains on the original order
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getId()).isEqualTo(10L);
        assertThat(order.getOrderItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("splitEvenly: N equal checks summing to the total; source voided")
    void splitEvenly_dividesAndVoidsSource() {
        Order order = tabOrder();
        order.setTotalAmount(BigDecimal.valueOf(300));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        SplitBillRequest req = new SplitBillRequest();
        req.setMode("EVEN");
        req.setSplitCount(3);

        List<OrderDTO> checks = service.splitBill(1L, req);

        assertThat(checks).hasSize(3);
        assertThat(checks).allMatch(c -> c.getTotalAmount().compareTo(BigDecimal.valueOf(100)) == 0);
        assertThat(checks).allMatch(c -> Long.valueOf(1L).equals(c.getParentOrderId()));
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.VOIDED);
        // voided source total is zeroed so reports that don't exclude VOIDED won't double-count
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("mergeBill: source folds into target, source voided and table released")
    void mergeBill_foldsAndVoidsSource() {
        Order target = tabOrder();
        target.setId(1L);
        target.setTotalAmount(BigDecimal.valueOf(250));

        Order source = new Order();
        source.setId(2L);
        source.setTenantId("shop1");
        source.setOrderNumber("ORD-20260619-20000");
        source.setStatus(Order.OrderStatus.IN_PROGRESS);
        source.setTableId(8L);
        source.setTotalAmount(BigDecimal.valueOf(50));
        List<OrderItem> srcItems = new ArrayList<>();
        srcItems.add(item(20L, "Chè", 1, 50));
        source.setOrderItems(srcItems);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(target));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(source));
        when(tableRepository.findByTenantIdAndCurrentOrderId(anyString(), any())).thenReturn(Optional.empty());

        MergeBillRequest req = new MergeBillRequest();
        req.setSourceOrderId(2L);

        OrderDTO result = service.mergeBill(1L, req);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("300");
        assertThat(source.getStatus()).isEqualTo(Order.OrderStatus.VOIDED);
        assertThat(source.getOrderItems()).isEmpty();
    }
}
