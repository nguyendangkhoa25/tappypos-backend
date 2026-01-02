package com.barbershop.service;

import com.barbershop.model.dto.revenue.CreateRevenueRequest;
import com.barbershop.model.dto.revenue.RevenueDTO;
import com.barbershop.model.dto.revenue.RevenueCostDTO;
import com.barbershop.model.dto.revenue.RevenueSummarySalaryDTO;
import com.barbershop.model.dto.revenue.RevenueSummaryOrderDTO;
import com.barbershop.model.entity.Order;
import com.barbershop.model.entity.Revenue;
import com.barbershop.model.entity.RevenueCost;
import com.barbershop.model.entity.Salary;
import com.barbershop.repository.OrderRepository;
import com.barbershop.repository.RevenueRepository;
import com.barbershop.repository.RevenueCostRepository;
import com.barbershop.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RevenueService {

    private final RevenueRepository revenueRepository;
    private final OrderRepository orderRepository;
    private final SalaryRepository salaryRepository;
    private final RevenueCostRepository revenueCostRepository;

    /**
     * Calculate gross revenue for a given month and year
     * Sums all completed/delivered orders for that month
     */
    public BigDecimal calculateGrossRevenue(Integer year, Integer month) {
        log.info("Calculating gross revenue for {}/{}", month, year);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Order> orders = orderRepository.findByCompletedAtBetweenAndStatus(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                "COMPLETED"
        );

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Gross revenue for {}/{}: {}", month, year, totalRevenue);
        return totalRevenue;
    }

    /**
     * Calculate total employee salaries for a given month and year
     */
    public BigDecimal calculateTotalEmployeeSalaries(Integer year, Integer month) {
        log.info("Calculating total employee salaries for {}/{}", month, year);

        List<Salary> salaries = salaryRepository.findByYearAndMonthAndDeletedFalse(year, month);

        BigDecimal totalSalaries = salaries.stream()
                .map(Salary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total employee salaries for {}/{}: {}", month, year, totalSalaries);
        return totalSalaries;
    }

    /**
     * Create or update revenue for a given month
     */
    public RevenueDTO createRevenue(CreateRevenueRequest request) {
        log.info("Creating revenue for {}/{}", request.getMonth(), request.getYear());

        // Check if revenue already exists for this month/year
        Revenue existingRevenue = revenueRepository.findByYearAndMonthNotDeleted(request.getYear(), request.getMonth())
                .orElse(null);

        if (existingRevenue != null) {
            throw new IllegalArgumentException("Revenue already exists for " + request.getMonth() + "/" + request.getYear());
        }

        // Calculate gross revenue
        BigDecimal grossRevenue = calculateGrossRevenue(request.getYear(), request.getMonth());

        // Calculate total employee salaries
        BigDecimal totalEmployeeSalary = calculateTotalEmployeeSalaries(request.getYear(), request.getMonth());

        // Sum other costs
        BigDecimal otherCosts = request.getOtherCosts() != null
                ? request.getOtherCosts().stream()
                .map(CreateRevenueRequest.OtherCostInput::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        // Calculate totals
        BigDecimal totalCosts = totalEmployeeSalary.add(otherCosts);
        BigDecimal netRevenue = grossRevenue.subtract(totalCosts);

        // Create and save revenue
        Revenue revenue = Revenue.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .grossRevenue(grossRevenue)
                .totalEmployeeSalary(totalEmployeeSalary)
                .otherCosts(otherCosts)
                .totalCosts(totalCosts)
                .netRevenue(netRevenue)
                .notes(request.getNotes())
                .deleted(false)
                .build();

        Revenue savedRevenue = revenueRepository.save(revenue);

        // Save individual cost items
        if (request.getOtherCosts() != null && !request.getOtherCosts().isEmpty()) {
            List<RevenueCost> revenueCosts = request.getOtherCosts().stream()
                    .map(cost -> RevenueCost.builder()
                            .revenue(savedRevenue)
                            .description(cost.getDescription())
                            .amount(cost.getAmount())
                            .deleted(false)
                            .build())
                    .toList();
            revenueCostRepository.saveAll(revenueCosts);
        }

        log.info("Revenue created successfully for {}/{}", request.getMonth(), request.getYear());

        return mapToDTO(savedRevenue);
    }

    /**
     * Get revenue by ID
     */
    public RevenueDTO getRevenueById(Long id) {
        log.info("Fetching revenue with ID: {}", id);

        Revenue revenue = revenueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revenue not found"));

        return mapToDTO(revenue);
    }

    /**
     * Get revenue by year and month
     */
    public RevenueDTO getRevenueByYearAndMonth(Integer year, Integer month) {
        log.info("Fetching revenue for {}/{}", month, year);

        Revenue revenue = revenueRepository.findByYearAndMonthNotDeleted(year, month)
                .orElseThrow(() -> new RuntimeException("Revenue not found for " + month + "/" + year));

        return mapToDTO(revenue);
    }

    /**
     * Get all revenues with pagination
     */
    public Page<RevenueDTO> getAllRevenues(Pageable pageable) {
        log.info("Fetching all revenues");

        return revenueRepository.findAllNotDeleted(pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get revenues by year with pagination
     */
    public Page<RevenueDTO> getRevenuesByYear(Integer year, Pageable pageable) {
        log.info("Fetching revenues for year: {}", year);

        return revenueRepository.findByYearNotDeleted(year, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Delete (soft delete) revenue
     */
    public void deleteRevenue(Long id) {
        log.info("Deleting revenue with ID: {}", id);

        Revenue revenue = revenueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revenue not found"));

        revenue.setDeleted(true);
        revenueRepository.save(revenue);

        log.info("Revenue deleted successfully");
    }

    /**
     * Map Revenue entity to RevenueDTO
     */
    private RevenueDTO mapToDTO(Revenue revenue) {
        List<RevenueCostDTO> costDTOs = revenue.getRevenueCosts() != null
                ? revenue.getRevenueCosts().stream()
                .map(cost -> RevenueCostDTO.builder()
                        .id(cost.getId())
                        .description(cost.getDescription())
                        .amount(cost.getAmount())
                        .build())
                .toList()
                : new java.util.ArrayList<>();

        return RevenueDTO.builder()
                .id(revenue.getId())
                .year(revenue.getYear())
                .month(revenue.getMonth())
                .grossRevenue(revenue.getGrossRevenue())
                .totalEmployeeSalary(revenue.getTotalEmployeeSalary())
                .otherCosts(revenue.getOtherCosts())
                .totalCosts(revenue.getTotalCosts())
                .netRevenue(revenue.getNetRevenue())
                .notes(revenue.getNotes())
                .revenueCosts(costDTOs)
                .createdAt(revenue.getCreatedAt())
                .updatedAt(revenue.getUpdatedAt())
                .build();
    }

    /**
     * Get revenue summary with salaries and orders for a specific month/year
     */
    public Map<String, Object> getRevenueSummary(Integer year, Integer month) {
        log.info("Calculating revenue summary for {}/{}", month, year);

        // Get all salaries for the month
        List<Salary> salaries = salaryRepository.findByYearAndMonthAndDeletedFalse(year, month);
        BigDecimal totalEmployeeSalary = salaries.stream()
                .map(Salary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Convert salaries to DTOs
        List<RevenueSummarySalaryDTO> salaryDTOs = salaries.stream()
                .map(salary -> RevenueSummarySalaryDTO.builder()
                        .id(salary.getId())
                        .employeeName(salary.getEmployee() != null ? salary.getEmployee().getName() : "N/A")
                        .month(salary.getMonth())
                        .year(salary.getYear())
                        .status(salary.getStatus() != null ? salary.getStatus().toString() : "N/A")
                        .netSalary(salary.getNetSalary())
                        .commissionAmount(salary.getCommissionAmount())
                        .deductionAmount(salary.getDeductionAmount())
                        .overtimeAmount(salary.getOvertimeAmount())
                        .bonusAmount(salary.getBonusAmount())
                        .allowanceAmount(salary.getAllowanceAmount())
                        .build())
                .toList();

        // Get all completed orders for the month
        List<Order> orders = orderRepository.findCompletedOrdersByYearAndMonth(year, month);
        BigDecimal totalOrderAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Convert orders to DTOs
        List<RevenueSummaryOrderDTO> orderDTOs = orders.stream()
                .map(order -> RevenueSummaryOrderDTO.builder()
                        .id(order.getId())
                        .customerName(order.getCustomer() != null ? order.getCustomer().getName() : "N/A")
                        .customerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : "N/A")
                        .status(order.getStatus() != null ? order.getStatus().toString() : "N/A")
                        .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                        .completedAt(order.getCompletedAt() != null ? order.getCompletedAt().toString() : null)
                        .totalAmount(order.getTotalAmount())
                        .discountAmount(order.getDiscountAmount())
                        .taxAmount(order.getTaxAmount())
                        .commissionAmount(order.getCommissionAmount())
                        .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                        .build())
                .toList();

        // Build summary object
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("year", year);
        summary.put("month", month);
        summary.put("totalEmployeeSalary", totalEmployeeSalary);
        summary.put("totalOrderAmount", totalOrderAmount);
        summary.put("salaries", salaryDTOs);
        summary.put("orders", orderDTOs);

        return summary;
    }
}
