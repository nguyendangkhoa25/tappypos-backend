package com.knp.controller.dashboard;

import com.knp.model.dto.dashboard.DashboardKpiDTO;
import com.knp.model.dto.dashboard.DashboardSummaryDTO;
import com.knp.model.entity.order.Order;
import com.knp.repository.buyback.BuybackOrderRepository;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.employee.EmployeeRepository;
import com.knp.repository.order.OrderItemRepository;
import com.knp.repository.order.OrderRepository;
import com.knp.repository.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@RequiresFeature("DASHBOARD")
public class DashboardController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BuybackOrderRepository buybackOrderRepository;
    private final PawnRepository pawnRepository;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        log.info("Endpoint: GET /dashboard/summary");

        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        BigDecimal totalRevenue   = orderRepository.sumTotalRevenue();
        BigDecimal monthlyRevenue = orderRepository.sumRevenueByMonth(year, month);
        BigDecimal yearlyRevenue  = orderRepository.sumRevenueByYear(year);

        long totalOrders    = orderRepository.countCompleted();
        long pendingOrders  = orderRepository.countByDeletedFalseAndStatus(Order.OrderStatus.PENDING);
        long totalCustomers = customerRepository.countAllActive();

        // Items sold
        Long totalItemsSold = orderItemRepository.sumTotalItemsSold();
        Long monthItemsSold = orderItemRepository.sumItemsSoldByMonth(year, month);
        Long yearItemsSold  = orderItemRepository.sumItemsSoldByYear(year);

        // Pawn KPIs
        Long       activePawnContracts   = pawnRepository.countActivePawnContracts();
        BigDecimal activePawnAmount      = pawnRepository.sumActivePawnAmount();
        Long       monthNewPawnContracts = pawnRepository.countNewPawnsByMonth(year, month);
        BigDecimal monthNewPawnAmount    = pawnRepository.sumNewPawnAmountByMonth(year, month);
        Long       monthInterestEarned   = pawnRepository.sumInterestEarnedByMonth(year, month);

        // Buyback (purchases from customers)
        Long       totalItemsBought   = buybackOrderRepository.sumTotalItemsBought();
        Long       monthItemsBought   = buybackOrderRepository.sumItemsBoughtByMonth(year, month);
        Long       yearItemsBought    = buybackOrderRepository.sumItemsBoughtByYear(year);
        BigDecimal totalBuybackSpent  = buybackOrderRepository.sumTotalBuybackSpent();
        BigDecimal monthBuybackSpent  = buybackOrderRepository.sumBuybackSpentByMonth(year, month);
        BigDecimal yearBuybackSpent   = buybackOrderRepository.sumBuybackSpentByYear(year);
        Long       totalBuybackOrders = buybackOrderRepository.countCompletedBuyOrders();

        List<Order> recent = orderRepository.findRecentCompleted(PageRequest.of(0, 10));
        List<DashboardSummaryDTO.RecentOrderDTO> recentDTOs = recent.stream()
                .map(o -> DashboardSummaryDTO.RecentOrderDTO.builder()
                        .id(o.getId())
                        .customerName(o.getCustomer() != null ? o.getCustomer().getName() : "Khách vãng lai")
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .completedAt(o.getCompletedAt() != null ? o.getCompletedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalOrders(totalOrders)
                .completedOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalEmployees(employeeRepository.countAllActive())
                .activeEmployees(employeeRepository.countActive())
                .inactiveEmployees(employeeRepository.countInactive())
                .totalCustomers(totalCustomers)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .yearlyRevenue(yearlyRevenue)
                .totalItemsSold(totalItemsSold)
                .monthItemsSold(monthItemsSold)
                .yearItemsSold(yearItemsSold)
                .totalItemsBought(totalItemsBought)
                .monthItemsBought(monthItemsBought)
                .yearItemsBought(yearItemsBought)
                .totalBuybackSpent(totalBuybackSpent)
                .monthBuybackSpent(monthBuybackSpent)
                .yearBuybackSpent(yearBuybackSpent)
                .totalBuybackOrders(totalBuybackOrders)
                .activePawnContracts(activePawnContracts)
                .activePawnAmount(activePawnAmount)
                .monthNewPawnContracts(monthNewPawnContracts)
                .monthNewPawnAmount(monthNewPawnAmount)
                .monthInterestEarned(monthInterestEarned)
                .currentMonth(month)
                .currentYear(year)
                .recentOrders(recentDTOs)
                .topEmployees(List.of())
                .topCustomers(List.of())
                .build();

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/kpi")
    public ResponseEntity<DashboardKpiDTO> getKpi(
            @RequestParam String from,
            @RequestParam String to) {
        log.info("Endpoint: GET /dashboard/kpi from={} to={}", from, to);

        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).atTime(LocalTime.MAX);

        Long       itemsSold      = orderItemRepository.sumItemsSoldByDateRange(fromDt, toDt);
        BigDecimal revenue        = orderRepository.sumRevenueByDateRange(fromDt, toDt);
        Long       itemsBought    = buybackOrderRepository.sumItemsBoughtByDateRange(fromDt, toDt);
        BigDecimal buybackSpent   = buybackOrderRepository.sumBuybackSpentByDateRange(fromDt, toDt);
        Long       newPawnContracts  = pawnRepository.countNewPawnsByDateRange(fromDt, toDt);
        BigDecimal newPawnAmount     = pawnRepository.sumNewPawnAmountByDateRange(fromDt, toDt);
        Long       interestEarned    = pawnRepository.sumInterestEarnedByDateRange(fromDt, toDt);

        return ResponseEntity.ok(DashboardKpiDTO.builder()
                .itemsSold(itemsSold)
                .revenue(revenue)
                .itemsBought(itemsBought)
                .buybackSpent(buybackSpent)
                .newPawnContracts(newPawnContracts)
                .newPawnAmount(newPawnAmount)
                .interestEarned(interestEarned)
                .fromDate(from)
                .toDate(to)
                .build());
    }
}
