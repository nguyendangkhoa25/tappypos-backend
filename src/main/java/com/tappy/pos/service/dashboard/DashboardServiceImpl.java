package com.tappy.pos.service.dashboard;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.dto.dashboard.DashboardKpiDTO;
import com.tappy.pos.model.dto.dashboard.DashboardSummaryDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository  customerRepository;
    private final EmployeeRepository  employeeRepository;
    private final PawnRepository      pawnRepository;
    private final FeatureContext      featureContext;

    @Override
    public DashboardSummaryDTO getSummary() {
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        BigDecimal totalRevenue   = orderRepository.sumTotalRevenue();
        BigDecimal monthlyRevenue = orderRepository.sumRevenueByMonth(year, month);
        BigDecimal yearlyRevenue  = orderRepository.sumRevenueByYear(year);

        long totalOrders   = orderRepository.countCompleted();
        long pendingOrders = orderRepository.countByDeletedFalseAndStatus(Order.OrderStatus.PENDING);
        long totalCustomers = customerRepository.countAllActive();

        Long totalItemsSold = orderItemRepository.sumTotalItemsSold();
        Long monthItemsSold = orderItemRepository.sumItemsSoldByMonth(year, month);
        Long yearItemsSold  = orderItemRepository.sumItemsSoldByYear(year);

        Long       activePawnContracts   = null;
        BigDecimal activePawnAmount      = null;
        Long       monthNewPawnContracts = null;
        BigDecimal monthNewPawnAmount    = null;
        Long       monthInterestEarned   = null;
        if (featureContext.hasFeature("PAWN")) {
            activePawnContracts   = pawnRepository.countActivePawnContracts();
            activePawnAmount      = pawnRepository.sumActivePawnAmount();
            monthNewPawnContracts = pawnRepository.countNewPawnsByMonth(year, month);
            monthNewPawnAmount    = pawnRepository.sumNewPawnAmountByMonth(year, month);
            monthInterestEarned   = pawnRepository.sumInterestEarnedByMonth(year, month);
        }

        List<DashboardSummaryDTO.RecentOrderDTO> recentDTOs = orderRepository
                .findRecentCompleted(PageRequest.of(0, 10))
                .stream()
                .map(o -> DashboardSummaryDTO.RecentOrderDTO.builder()
                        .id(o.getId())
                        .customerName(o.getCustomer() != null ? o.getCustomer().getName() : "Khách vãng lai")
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .completedAt(o.getCompletedAt() != null ? o.getCompletedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
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
    }

    @Override
    public DashboardKpiDTO getKpi(LocalDateTime from, LocalDateTime to, String fromDate, String toDate) {
        Long       itemsSold        = orderItemRepository.sumItemsSoldByDateRange(from, to);
        BigDecimal revenue          = orderRepository.sumRevenueByDateRange(from, to);
        Long       newPawnContracts = null;
        BigDecimal newPawnAmount    = null;
        Long       interestEarned   = null;
        if (featureContext.hasFeature("PAWN")) {
            newPawnContracts = pawnRepository.countNewPawnsByDateRange(from, to);
            newPawnAmount    = pawnRepository.sumNewPawnAmountByDateRange(from, to);
            interestEarned   = pawnRepository.sumInterestEarnedByDateRange(from, to);
        }

        return DashboardKpiDTO.builder()
                .itemsSold(itemsSold)
                .revenue(revenue)
                .newPawnContracts(newPawnContracts)
                .newPawnAmount(newPawnAmount)
                .interestEarned(interestEarned)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
    }
}
