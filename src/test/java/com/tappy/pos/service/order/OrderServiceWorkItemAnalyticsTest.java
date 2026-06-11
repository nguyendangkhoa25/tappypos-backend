package com.tappy.pos.service.order;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.order.WorkItemDTO;
import com.tappy.pos.model.dto.order.WorkItemSummaryDTO;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.finance.BankAccountRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.customer.LoyaltyService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.service.notification.NotificationService;
import com.tappy.pos.service.product.ProductService;
import com.tappy.pos.service.tenant.PrintTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the analytics and item-level work-queue (MY_WORK) methods of {@link OrderServiceImpl}
 * that are not exercised by {@link OrderServiceImplTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl — analytics & work-item unit tests")
class OrderServiceWorkItemAnalyticsTest {

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
    @Mock private MessageService messageService;
    @Mock private PrintTemplateService printTemplateService;
    @Mock private ActivityLogService activityLogService;
    @Mock private NotificationService notificationService;
    @Mock private TenantContext tenantContext;
    @Mock private FeatureContext featureContext;
    @Mock private TableRepository tableRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cashier01", null, java.util.Collections.emptyList()));
        pageable = PageRequest.of(0, 20);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Stubs userRepository → employeeRepository so resolveCurrentEmployee() returns employee #99.
    private Employee stubCurrentEmployee() {
        User user = User.builder().username("cashier01").build();
        user.setId(7L);
        Employee employee = Employee.builder().fullName("Thợ A").build();
        employee.setId(99L);
        when(userRepository.findByUsername("cashier01")).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.of(employee));
        return employee;
    }

    private OrderItem workItem(Long id, OrderItem.ItemStatus status, Long assignedEmployeeId) {
        Order order = Order.builder().orderNumber("ORD-1").build();
        order.setId(50L);
        order.setCreatedAt(LocalDateTime.now());
        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(11L)
                .productName("Cắt tóc")
                .quantity(1)
                .unitPrice(new BigDecimal("100000"))
                .amount(new BigDecimal("100000"))
                .status(status)
                .durationMinutes(30)
                .assignedEmployeeId(assignedEmployeeId)
                .assignedEmployeeName(assignedEmployeeId != null ? "Thợ A" : null)
                .build();
        item.setId(id);
        return item;
    }

    // 18-column row matching OrderServiceImpl.mapRowToWorkItemDTO contract.
    private Object[] workItemRow() {
        return new Object[]{
                1L, 50L, "ORD-1", "Khách A", 11L, "Cắt tóc", 1,
                new BigDecimal("100000"), new BigDecimal("100000"), 30, "PENDING",
                null, 99L, "Thợ A", null, null, null, "ghi chú"
        };
    }

    // ── Analytics ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analytics")
    class Analytics {

        @Test
        @DisplayName("getOrderSummary: aggregates revenue, counts and computes average")
        void getOrderSummary() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 31);
            when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(new BigDecimal("1000000"));
            when(orderRepository.countByDateRange(any(), any())).thenReturn(4L);
            when(orderRepository.countByDateRangeAndStatus(any(), any(), eq(Order.OrderStatus.COMPLETED))).thenReturn(3L);
            when(orderRepository.countByDateRangeAndStatus(any(), any(), eq(Order.OrderStatus.CANCELLED))).thenReturn(1L);

            Map<String, Object> result = orderService.getOrderSummary(from, to, null, null);

            assertThat(result.get("totalRevenue")).isEqualTo(new BigDecimal("1000000"));
            assertThat(result.get("orderCount")).isEqualTo(4L);
            assertThat(result.get("avgOrderValue")).isEqualTo(new BigDecimal("250000"));
            assertThat(result.get("completedCount")).isEqualTo(3L);
            assertThat(result.get("cancelledCount")).isEqualTo(1L);
        }

        @Test
        @DisplayName("getOrderSummary: zero orders → average is zero")
        void getOrderSummary_zeroOrders() {
            when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(BigDecimal.ZERO);
            when(orderRepository.countByDateRange(any(), any())).thenReturn(0L);
            when(orderRepository.countByDateRangeAndStatus(any(), any(), any())).thenReturn(0L);

            Map<String, Object> result = orderService.getOrderSummary(LocalDate.now(), LocalDate.now(), null, null);

            assertThat(result.get("avgOrderValue")).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getOrderChart: default granularity uses daily revenue")
        void getOrderChart_daily() {
            when(orderRepository.getDailyRevenue(any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"2026-01-01", new BigDecimal("500000")}));

            List<Map<String, Object>> chart = orderService.getOrderChart(LocalDate.now(), LocalDate.now(), null);

            assertThat(chart).hasSize(1);
            assertThat(chart.get(0).get("label")).isEqualTo("2026-01-01");
            assertThat(chart.get(0).get("value")).isEqualTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("getOrderChart: hourly granularity uses hourly revenue")
        void getOrderChart_hourly() {
            when(orderRepository.getHourlyRevenue(any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"09", new BigDecimal("120000")}));

            List<Map<String, Object>> chart = orderService.getOrderChart(LocalDate.now(), LocalDate.now(), "hour");

            assertThat(chart).hasSize(1);
            assertThat(chart.get(0).get("label")).isEqualTo("09");
        }

        @Test
        @DisplayName("getTopProducts: maps repository rows to product entries")
        void getTopProducts() {
            when(orderRepository.getTopProductsSince(any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"Cà phê", 5L, 12L, new BigDecimal("300000")}));

            List<Map<String, Object>> top = orderService.getTopProducts(10, LocalDateTime.now());

            assertThat(top).hasSize(1);
            assertThat(top.get(0).get("name")).isEqualTo("Cà phê");
            assertThat(top.get(0).get("productId")).isEqualTo("5");
            assertThat(top.get(0).get("orderCount")).isEqualTo(12L);
            assertThat(top.get(0).get("revenue")).isEqualTo(new BigDecimal("300000"));
        }

        @Test
        @DisplayName("getTopProductsByRange: maps repository rows")
        void getTopProductsByRange() {
            when(orderRepository.getTopProductsByRange(any(), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"Trà", 2L, 7L, new BigDecimal("70000")}));

            List<Map<String, Object>> top = orderService.getTopProductsByRange(10, LocalDateTime.now(), LocalDateTime.now());

            assertThat(top.get(0).get("name")).isEqualTo("Trà");
            assertThat(top.get(0).get("orderCount")).isEqualTo(7L);
        }

        @Test
        @DisplayName("getTopCustomersByRange: maps repository rows")
        void getTopCustomersByRange() {
            when(orderRepository.getTopCustomersByRange(any(), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"Khách A", 3L, new BigDecimal("450000"), 10L}));

            List<Map<String, Object>> top = orderService.getTopCustomersByRange(10, LocalDateTime.now(), LocalDateTime.now());

            assertThat(top.get(0).get("name")).isEqualTo("Khách A");
            assertThat(top.get(0).get("orderCount")).isEqualTo(3L);
            assertThat(top.get(0).get("customerId")).isEqualTo("10");
        }

        @Test
        @DisplayName("getTopCustomersByFrequency: maps repository rows")
        void getTopCustomersByFrequency() {
            when(orderRepository.getTopCustomersByFrequency(any(), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"Khách B", 8L, new BigDecimal("800000"), 11L}));

            List<Map<String, Object>> top = orderService.getTopCustomersByFrequency(10, LocalDateTime.now(), LocalDateTime.now());

            assertThat(top.get(0).get("orderCount")).isEqualTo(8L);
        }

        @Test
        @DisplayName("getCustomerStats: returning = total - new (floored at 0)")
        void getCustomerStats() {
            when(orderRepository.countActiveCustomers(any(), any())).thenReturn(10L);
            when(orderRepository.countNewCustomers(any(), any())).thenReturn(4L);

            Map<String, Object> stats = orderService.getCustomerStats(LocalDateTime.now(), LocalDateTime.now());

            assertThat(stats.get("total")).isEqualTo(10L);
            assertThat(stats.get("newCount")).isEqualTo(4L);
            assertThat(stats.get("returningCount")).isEqualTo(6L);
        }

        @Test
        @DisplayName("getTopEmployeesByRange: maps repository rows")
        void getTopEmployeesByRange() {
            when(orderRepository.getTopEmployeesByRange(any(), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"Thợ A", 99L, 6L, new BigDecimal("600000")}));

            List<Map<String, Object>> top = orderService.getTopEmployeesByRange(5, LocalDateTime.now(), LocalDateTime.now());

            assertThat(top.get(0).get("name")).isEqualTo("Thợ A");
            assertThat(top.get(0).get("userId")).isEqualTo("99");
            assertThat(top.get(0).get("orderCount")).isEqualTo(6L);
        }

        @Test
        @DisplayName("getCustomerOrderSummary: null revenue defaults to zero")
        void getCustomerOrderSummary_nullRevenue() {
            when(orderRepository.sumRevenueByCustomerAndDateRange(eq(10L), any(), any())).thenReturn(null);
            when(orderRepository.countByCustomerAndDateRange(eq(10L), any(), any())).thenReturn(0L);
            when(orderRepository.countByCustomerAndDateRangeAndStatus(eq(10L), any(), any(), eq("COMPLETED"))).thenReturn(0L);
            when(orderRepository.findLastVisitDateByCustomer(10L)).thenReturn(null);

            Map<String, Object> summary = orderService.getCustomerOrderSummary(10L, LocalDate.now(), LocalDate.now());

            assertThat(summary.get("totalRevenue")).isEqualTo(BigDecimal.ZERO);
            assertThat(summary.get("daysSinceLastVisit")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("getAllOrdersFiltered: ORDER_VIEW_ALL uses unscoped query")
        void getAllOrdersFiltered_viewAll() {
            when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(true);
            when(orderRepository.findAllFiltered(any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(Page.empty());

            orderService.getAllOrdersFiltered("COMPLETED", "CASH", LocalDate.now(), LocalDate.now(), pageable);

            verify(orderRepository).findAllFiltered(any(), any(), any(), any(), eq(pageable));
            verify(orderRepository, never()).findAllFilteredByUser(anyString(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("getAllOrdersFiltered: without ORDER_VIEW_ALL scopes to current user")
        void getAllOrdersFiltered_scoped() {
            when(featureContext.hasFeature("ORDER_VIEW_ALL")).thenReturn(false);
            when(orderRepository.findAllFilteredByUser(eq("cashier01"), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(Page.empty());

            orderService.getAllOrdersFiltered(null, null, LocalDate.now(), LocalDate.now(), pageable);

            verify(orderRepository).findAllFilteredByUser(eq("cashier01"), any(), any(), any(), any(), eq(pageable));
        }
    }

    // ── Work-item queue ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("work-item queue")
    class WorkItems {

        @Test
        @DisplayName("getAvailableWorkItems: maps unassigned rows")
        void getAvailableWorkItems() {
            when(orderItemRepository.findAvailableWorkItems(pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(workItemRow())));

            Page<WorkItemDTO> result = orderService.getAvailableWorkItems(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getItemId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getNote()).isEqualTo("ghi chú");
        }

        @Test
        @DisplayName("pickupWorkItem: assigns the item to the current employee")
        void pickupWorkItem_success() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.PENDING, null);
            when(orderItemRepository.findByIdAndAssignedEmployeeIdIsNull(5L)).thenReturn(Optional.of(item));

            WorkItemDTO dto = orderService.pickupWorkItem(5L);

            assertThat(item.getAssignedEmployeeId()).isEqualTo(99L);
            assertThat(item.getAssignedEmployeeName()).isEqualTo("Thợ A");
            assertThat(dto.getItemId()).isEqualTo(5L);
            verify(orderItemRepository).save(item);
        }

        @Test
        @DisplayName("pickupWorkItem: already assigned → BadRequestException")
        void pickupWorkItem_alreadyAssigned() {
            stubCurrentEmployee();
            when(orderItemRepository.findByIdAndAssignedEmployeeIdIsNull(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.pickupWorkItem(5L))
                    .isInstanceOf(BadRequestException.class);
            verify(orderItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("unpickWorkItem: clears assignment for a PENDING item")
        void unpickWorkItem_success() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.PENDING, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            orderService.unpickWorkItem(5L);

            assertThat(item.getAssignedEmployeeId()).isNull();
            assertThat(item.getAssignedEmployeeName()).isNull();
            verify(orderItemRepository).save(item);
        }

        @Test
        @DisplayName("unpickWorkItem: non-PENDING item → BadRequestException")
        void unpickWorkItem_notPending() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.IN_PROGRESS, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));
            when(messageService.getMessage("error.order.item.cannot.reassign")).thenReturn("không thể");

            assertThatThrownBy(() -> orderService.unpickWorkItem(5L))
                    .isInstanceOf(BadRequestException.class);
            verify(orderItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("unpickWorkItem: not found → ResourceNotFoundException")
        void unpickWorkItem_notFound() {
            stubCurrentEmployee();
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.unpickWorkItem(5L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("startWorkItem: PENDING → IN_PROGRESS")
        void startWorkItem_success() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.PENDING, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            orderService.startWorkItem(5L);

            assertThat(item.getStatus()).isEqualTo(OrderItem.ItemStatus.IN_PROGRESS);
            verify(orderItemRepository).save(item);
        }

        @Test
        @DisplayName("startWorkItem: not PENDING → BadRequestException")
        void startWorkItem_invalidStatus() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.COMPLETED, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> orderService.startWorkItem(5L))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("completeWorkItem: sets COMPLETED and completedAt")
        void completeWorkItem_success() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.IN_PROGRESS, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            orderService.completeWorkItem(5L);

            assertThat(item.getStatus()).isEqualTo(OrderItem.ItemStatus.COMPLETED);
            assertThat(item.getCompletedAt()).isNotNull();
            verify(orderItemRepository).save(item);
        }

        @Test
        @DisplayName("completeWorkItem: already COMPLETED → BadRequestException")
        void completeWorkItem_alreadyCompleted() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.COMPLETED, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));
            when(messageService.getMessage("error.order.item.cannot.reassign")).thenReturn("không thể");

            assertThatThrownBy(() -> orderService.completeWorkItem(5L))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("releaseWorkItem: IN_PROGRESS → PENDING")
        void releaseWorkItem_success() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.IN_PROGRESS, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            orderService.releaseWorkItem(5L);

            assertThat(item.getStatus()).isEqualTo(OrderItem.ItemStatus.PENDING);
            verify(orderItemRepository).save(item);
        }

        @Test
        @DisplayName("releaseWorkItem: not IN_PROGRESS → BadRequestException")
        void releaseWorkItem_notInProgress() {
            stubCurrentEmployee();
            OrderItem item = workItem(5L, OrderItem.ItemStatus.PENDING, 99L);
            when(orderItemRepository.findByIdAndAssignedEmployeeId(5L, 99L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> orderService.releaseWorkItem(5L))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("getMyWorkItems: maps rows for the current employee")
        void getMyWorkItems() {
            stubCurrentEmployee();
            when(orderItemRepository.findWorkItemsByEmployeeId(99L, pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(workItemRow())));

            Page<WorkItemDTO> result = orderService.getMyWorkItems(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAssignedEmployeeId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("getMyPendingWorkItems: maps rows for the current employee")
        void getMyPendingWorkItems() {
            stubCurrentEmployee();
            when(orderItemRepository.findPendingWorkItemsByEmployeeId(99L, pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(workItemRow())));

            Page<WorkItemDTO> result = orderService.getMyPendingWorkItems(pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("getMyCompletedWorkItems: maps completed rows over the resolved period")
        void getMyCompletedWorkItems() {
            stubCurrentEmployee();
            when(orderItemRepository.findCompletedWorkItems(eq(99L), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(workItemRow())));

            Page<WorkItemDTO> result = orderService.getMyCompletedWorkItems("DAY", null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("resolveCurrentEmployee: user not found → ResourceNotFoundException")
        void resolve_userNotFound() {
            when(userRepository.findByUsername("cashier01")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getMyWorkItems(pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("resolveCurrentEmployee: employee record missing → ResourceNotFoundException")
        void resolve_employeeNotFound() {
            User user = User.builder().username("cashier01").build();
            user.setId(7L);
            when(userRepository.findByUsername("cashier01")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getMyWorkItems(pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Work-item summary & trend ────────────────────────────────────────────────

    @Nested
    @DisplayName("work-item summary & trend")
    class SummaryAndTrend {

        @Test
        @DisplayName("getMyWorkItemSummary: empty stats → zeroed summary")
        void summary_empty() {
            stubCurrentEmployee();
            when(orderItemRepository.getWorkItemStats(eq(99L), any(), any())).thenReturn(List.of());

            WorkItemSummaryDTO summary = orderService.getMyWorkItemSummary("DAY", null, null, null);

            assertThat(summary.getCompletedCount()).isZero();
            assertThat(summary.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getMyWorkItemSummary: maps aggregated stats row")
        void summary_populated() {
            stubCurrentEmployee();
            when(orderItemRepository.getWorkItemStats(eq(99L), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{5L, new BigDecimal("500000"), 150L, new BigDecimal("50000")}));

            WorkItemSummaryDTO summary = orderService.getMyWorkItemSummary("MONTH", null, 6, 2026);

            assertThat(summary.getCompletedCount()).isEqualTo(5L);
            assertThat(summary.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(summary.getTotalDurationMinutes()).isEqualTo(150L);
            assertThat(summary.getTotalCommission()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("getMyWorkItemTrend: DAY → hourly buckets labelled HH:00")
        void trend_day() {
            stubCurrentEmployee();
            when(orderItemRepository.getWorkItemTrendByHour(eq(99L), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{9, 3L, new BigDecimal("90000")}));

            List<Map<String, Object>> trend = orderService.getMyWorkItemTrend("DAY", null, null, null);

            assertThat(trend).hasSize(1);
            assertThat(trend.get(0).get("label")).isEqualTo("09:00");
            assertThat(trend.get(0).get("count")).isEqualTo(3L);
        }

        @Test
        @DisplayName("getMyWorkItemTrend: YEAR → monthly buckets")
        void trend_year() {
            stubCurrentEmployee();
            when(orderItemRepository.getWorkItemTrendByMonth(eq(99L), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{6, 12L, new BigDecimal("1200000")}));

            List<Map<String, Object>> trend = orderService.getMyWorkItemTrend("YEAR", null, null, 2026);

            assertThat(trend.get(0).get("label")).isEqualTo("6");
            assertThat(trend.get(0).get("count")).isEqualTo(12L);
        }

        @Test
        @DisplayName("getMyWorkItemTrend: WEEK → daily buckets keep raw label")
        void trend_week() {
            stubCurrentEmployee();
            when(orderItemRepository.getWorkItemTrendByDay(eq(99L), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", 4L, new BigDecimal("400000")}));

            List<Map<String, Object>> trend = orderService.getMyWorkItemTrend("WEEK", null, null, null);

            assertThat(trend.get(0).get("label")).isEqualTo("2026-06-01");
        }
    }

    // ── softDeleteOrder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("softDeleteOrder: flags the order deleted and saves it")
    void softDeleteOrder_success() {
        Order order = Order.builder().orderNumber("ORD-9").build();
        order.setId(9L);
        order.setDeleted(false);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        orderService.softDeleteOrder(9L);

        assertThat(order.getDeleted()).isTrue();
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("softDeleteOrder: not found → ResourceNotFoundException")
    void softDeleteOrder_notFound() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.softDeleteOrder(9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }
}
