package com.barbershop.service;

import com.barbershop.model.dto.dashboard.DashboardSummaryDTO;
import com.barbershop.model.entity.Customer;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Order;
import com.barbershop.repository.CustomerRepository;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for dashboard statistics and summaries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;

    /**
     * Get comprehensive dashboard summary
     */
    public DashboardSummaryDTO getDashboardSummary() {
        log.info("Generating dashboard summary");

        // Get all active entities using pagination to get all results
        PageRequest allRecordsPageable = PageRequest.of(0, 10000);
        List<Order> allOrders = orderRepository.findAllActive(allRecordsPageable).getContent();
        List<Employee> allEmployees = employeeRepository.findAllActive(allRecordsPageable).getContent();
        List<Customer> allCustomers = customerRepository.findAllActive(allRecordsPageable).getContent();

        // Count active and inactive employees
        long activeEmployees = allEmployees.stream()
                .filter(emp -> emp.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .count();
        long inactiveEmployees = allEmployees.size() - activeEmployees;

        // Calculate completed orders
        List<Order> completedOrders = allOrders.stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.COMPLETED)
                .toList();

        // Calculate total revenue
        BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate monthly revenue (current month)
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal monthlyRevenue = completedOrders.stream()
                .filter(order -> order.getCompletedAt() != null)
                .filter(order -> {
                    LocalDateTime completedAt = order.getCompletedAt();
                    return !completedAt.isBefore(startOfMonth) && !completedAt.isAfter(endOfMonth);
                })
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate yearly revenue (current year)
        int currentYear = LocalDateTime.now().getYear();
        BigDecimal yearlyRevenue = completedOrders.stream()
                .filter(order -> order.getCompletedAt() != null)
                .filter(order -> order.getCompletedAt().getYear() == currentYear)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get recent orders - ALL orders (not just completed) sorted by creation date DESC (last 50 to allow load more in frontend)
        List<DashboardSummaryDTO.RecentOrderDTO> recentOrders = allOrders.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(50)
                .map(order -> {
                    // Get assigned employee from first order item
                    String employeeName = order.getOrderItems().stream()
                            .filter(item -> item.getAssignedEmployee() != null)
                            .findFirst()
                            .map(item -> item.getAssignedEmployee().getName())
                            .orElse("Chưa gán");

                    return DashboardSummaryDTO.RecentOrderDTO.builder()
                            .id(order.getId())
                            .customerName(order.getCustomer() != null ? order.getCustomer().getName() : "N/A")
                            .assignedEmployeeName(employeeName)
                            .totalAmount(order.getTotalAmount())
                            .completedAt(order.getCompletedAt() != null ? order.getCompletedAt().toString() : order.getCreatedAt().toString())
                            .status(order.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate top employees based on completed order items
        Map<Long, DashboardSummaryDTO.TopEmployeeDTO> employeeStats = new HashMap<>();

        log.debug("Completed orders count: {}", completedOrders.size());

        // Iterate through completed orders and their items
        completedOrders.forEach(order -> {
            order.getOrderItems().stream()
                    .filter(item -> item.getAssignedEmployee() != null)
                    .filter(item -> item.getStatus() == com.barbershop.model.entity.OrderItem.ItemStatus.COMPLETED)
                    .forEach(item -> {
                        Employee emp = item.getAssignedEmployee();
                        Long empId = emp.getId();
                        DashboardSummaryDTO.TopEmployeeDTO stats = employeeStats.getOrDefault(empId,
                                DashboardSummaryDTO.TopEmployeeDTO.builder()
                                        .id(empId)
                                        .name(emp.getName())
                                        .orderCount(0L)
                                        .revenue(BigDecimal.ZERO)
                                        .build());

                        stats.setOrderCount(stats.getOrderCount() + 1);
                        stats.setRevenue(stats.getRevenue().add(item.getCommissionAmount() != null ? item.getCommissionAmount() : BigDecimal.ZERO));
                        employeeStats.put(empId, stats);
                    });
        });

        List<DashboardSummaryDTO.TopEmployeeDTO> topEmployees = employeeStats.values().stream()
                .sorted((a, b) -> b.getOrderCount().compareTo(a.getOrderCount()))
                .limit(5)
                .collect(Collectors.toList());

        log.info("Top employees count: {}", topEmployees.size());

        // Count pending and in-progress orders (not completed or cancelled)
        long pendingOrders = allOrders.stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.PENDING ||
                                 order.getStatus() == Order.OrderStatus.IN_PROGRESS)
                .count();

        return DashboardSummaryDTO.builder()
                .totalOrders((long) allOrders.size())
                .completedOrders((long) completedOrders.size())
                .pendingOrders(pendingOrders)
                .totalEmployees((long) allEmployees.size())
                .activeEmployees(activeEmployees)
                .inactiveEmployees(inactiveEmployees)
                .totalCustomers((long) allCustomers.size())
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .yearlyRevenue(yearlyRevenue)
                .recentOrders(recentOrders)
                .topEmployees(topEmployees)
                .build();
    }
}

