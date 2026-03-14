package com.knp.controller;

import com.knp.model.dto.dashboard.DashboardSummaryDTO;
import com.knp.model.entity.Order;
import com.knp.repository.CustomerRepository;
import com.knp.repository.EmployeeRepository;
import com.knp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        log.info("Endpoint: GET /dashboard/summary");

        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        BigDecimal totalRevenue   = orderRepository.sumTotalRevenue();
        BigDecimal monthlyRevenue = orderRepository.sumRevenueByMonth(year, month);
        BigDecimal yearlyRevenue  = orderRepository.sumRevenueByYear(year);

        long totalOrders     = orderRepository.countCompleted();
        long pendingOrders   = orderRepository.countByDeletedFalseAndStatus(Order.OrderStatus.PENDING);
        long totalCustomers  = customerRepository.countAllActive();

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
                .recentOrders(recentDTOs)
                .topEmployees(List.of())
                .topCustomers(List.of())
                .build();

        return ResponseEntity.ok(summary);
    }
}
