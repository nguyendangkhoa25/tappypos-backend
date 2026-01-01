package com.barbershop.service;

import com.barbershop.model.dto.salary.*;
import com.barbershop.model.entity.Order;
import com.barbershop.model.entity.Salary;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.OrderItem;
import com.barbershop.repository.SalaryRepository;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import com.barbershop.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Calculate total earnings from completed orders for an employee in a given month/year
     */
    public BigDecimal calculateEmployeeEarnings(Long employeeId, Integer month, Integer year) {
        log.info("Calculating earnings for employee {} for {}/{}", employeeId, month, year);

        List<Order> completedOrders = orderRepository.findCompletedOrdersByEmployee(employeeId);

        BigDecimal totalEarnings = completedOrders.stream()
                .filter(order -> order.getCompletedAt() != null)
                .filter(order -> {
                    LocalDate completedDate = order.getCompletedAt().toLocalDate();
                    return completedDate.getMonthValue() == month && completedDate.getYear() == year;
                })
                .flatMap(order -> order.getOrderItems().stream())
                .map(orderItem -> orderItem.getCommissionAmount() != null ?
                        orderItem.getCommissionAmount() : orderItem.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total earnings calculated: {}", totalEarnings);
        return totalEarnings;
    }

    /**
     * Create a new salary record
     */
    public SalaryDTO createSalary(CreateSalaryRequest request) {
        log.info("Creating salary for employee {} for {}/{}", request.getEmployeeId(), request.getMonth(), request.getYear());

        // Validate month and year
        if (request.getMonth() < 1 || request.getMonth() > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (request.getYear() < 2000 || request.getYear() > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }

        // Check if salary already exists
        if (salaryRepository.existsByEmployeeAndMonthAndYear(request.getEmployeeId(), request.getMonth(), request.getYear())) {
            throw new IllegalArgumentException("Salary already exists for this employee in this month");
        }

        // Get employee
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Use netSalary from UI request instead of recalculating
        BigDecimal netSalary = request.getNetSalary() != null ? request.getNetSalary() : BigDecimal.ZERO;

        // Create salary record
        Salary salary = Salary.builder()
                .employee(employee)
                .month(request.getMonth())
                .year(request.getYear())
                .netSalary(netSalary)
                .commissionAmount(request.getCommissionAmount() != null ? request.getCommissionAmount() : BigDecimal.ZERO)
                .deductionAmount(request.getDeductionAmount() != null ? request.getDeductionAmount() : BigDecimal.ZERO)
                .overtimeAmount(request.getOvertimeAmount() != null ? request.getOvertimeAmount() : BigDecimal.ZERO)
                .bonusAmount(request.getBonusAmount() != null ? request.getBonusAmount() : BigDecimal.ZERO)
                .allowanceAmount(request.getAllowanceAmount() != null ? request.getAllowanceAmount() : BigDecimal.ZERO)
                .notes(request.getNotes())
                .status(Salary.SalaryStatus.SUBMITTED)
                .build();

        Salary savedSalary = salaryRepository.save(salary);
        log.info("Salary created successfully with id: {}", savedSalary.getId());

        // Mark order items as calculated and included in this salary
        List<OrderItem> uncalculatedItems = orderItemRepository.findUncalculatedItemsByEmployeeAndMonthYear(
                request.getEmployeeId(), request.getMonth(), request.getYear());

        uncalculatedItems.forEach(item -> {
            item.setSalaryCalculated(true);
            item.setIncludedInSalary(savedSalary);
        });

        orderItemRepository.saveAll(uncalculatedItems);
        log.info("Marked {} order items as calculated for salary {}", uncalculatedItems.size(), savedSalary.getId());

        return mapToDTO(savedSalary);
    }

    /**
     * Update salary adjustments (deductions, overtime, bonus, allowance, netSalary)
     */
    public SalaryDTO updateSalary(Long salaryId, UpdateSalaryRequest request) {
        log.info("Updating salary with id: {}", salaryId);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        if (request.getNetSalary() != null) {
            salary.setNetSalary(request.getNetSalary());
        }
        if (request.getCommissionAmount() != null) {
            salary.setCommissionAmount(request.getCommissionAmount());
        }
        if (request.getDeductionAmount() != null) {
            salary.setDeductionAmount(request.getDeductionAmount());
        }
        if (request.getOvertimeAmount() != null) {
            salary.setOvertimeAmount(request.getOvertimeAmount());
        }
        if (request.getBonusAmount() != null) {
            salary.setBonusAmount(request.getBonusAmount());
        }
        if (request.getAllowanceAmount() != null) {
            salary.setAllowanceAmount(request.getAllowanceAmount());
        }
        if (request.getNotes() != null) {
            salary.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            try {
                salary.setStatus(Salary.SalaryStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status provided: {}", request.getStatus());
            }
        }

        Salary updated = salaryRepository.save(salary);
        log.info("Salary updated successfully");

        return mapToDTO(updated);
    }

    /**
     * Get salary by id
     */
    public SalaryDTO getSalaryById(Long salaryId) {
        log.info("Fetching salary with id: {}", salaryId);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        return mapToDTO(salary);
    }

    /**
     * Get salary by employee, month, and year
     */
    public SalaryDTO getSalaryByEmployeeAndMonthYear(Long employeeId, Integer month, Integer year) {
        log.info("Fetching salary for employee {} for {}/{}", employeeId, month, year);

        Salary salary = salaryRepository.findByEmployeeAndMonthAndYear(employeeId, month, year)
                .orElse(null);

        return salary != null ? mapToDTO(salary) : null;
    }

    /**
     * Get all salaries for an employee with pagination
     */
    public Page<SalaryDTO> getSalariesByEmployee(Long employeeId, Pageable pageable) {
        log.info("Fetching salaries for employee {} with pagination", employeeId);

        return salaryRepository.findByEmployee(employeeId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get all salaries with pagination
     */
    public Page<SalaryDTO> getAllSalaries(Pageable pageable) {
        log.info("Fetching all salaries with pagination");

        return salaryRepository.findAllActive(pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get salaries by status
     */
    public Page<SalaryDTO> getSalariesByStatus(Salary.SalaryStatus status, Pageable pageable) {
        log.info("Fetching salaries with status: {}", status);

        return salaryRepository.findByStatus(status, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get detailed salary information including order items
     */
    public SalaryDetailDTO getSalaryDetail(Long salaryId) {
        log.info("Fetching detailed salary information for id: {}", salaryId);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        Employee employee = salary.getEmployee();
        List<Order> completedOrders = orderRepository.findCompletedOrdersByEmployee(employee.getId());

        List<SalaryDetailDTO.OrderItemEarningDTO> orderItems = completedOrders.stream()
                .filter(order -> order.getCompletedAt() != null)
                .filter(order -> {
                    LocalDate completedDate = order.getCompletedAt().toLocalDate();
                    return completedDate.getMonthValue() == salary.getMonth() && completedDate.getYear() == salary.getYear();
                })
                .flatMap(order -> order.getOrderItems().stream()
                        .map(item -> SalaryDetailDTO.OrderItemEarningDTO.builder()
                                .orderId(order.getId())
                                .orderItemId(item.getId())
                                .serviceName(item.getProductName())
                                .amount(item.getAmount())
                                .commissionAmount(item.getCommissionAmount() != null ?
                                        item.getCommissionAmount() : item.getAmount())
                                .completedAt(item.getCompletedAt())
                                .build()))
                .collect(Collectors.toList());

        return SalaryDetailDTO.builder()
                .salaryId(salary.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .employeePosition(employee.getPosition())
                .month(salary.getMonth())
                .year(salary.getYear())
                .baseSalary(employee.getBaseSalary())
                .netSalary(salary.getNetSalary())
                .commissionAmount(salary.getCommissionAmount())
                .deductionAmount(salary.getDeductionAmount())
                .overtimeAmount(salary.getOvertimeAmount())
                .bonusAmount(salary.getBonusAmount())
                .allowanceAmount(salary.getAllowanceAmount())
                .notes(salary.getNotes())
                .status(salary.getStatus().toString())
                .approvedAt(salary.getApprovedAt())
                .approvedByUserId(salary.getApprovedByUserId())
                .createdAt(salary.getCreatedAt())
                .orderItems(orderItems)
                .build();
    }

    /**
     * Delete (soft delete) a salary
     */
    public void deleteSalary(Long salaryId) {
        log.info("Deleting salary with id: {}", salaryId);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        // Get all order items included in this salary before deleting
        List<OrderItem> itemsInSalary = orderItemRepository.findItemsBySalaryId(salaryId);

        // Unmark order items as calculated
        itemsInSalary.forEach(item -> {
            item.setSalaryCalculated(false);
            item.setIncludedInSalary(null);
        });

        orderItemRepository.saveAll(itemsInSalary);
        log.info("Unmarked {} order items as calculated for deleted salary {}", itemsInSalary.size(), salaryId);

        // Soft delete the salary
        salary.softDelete();
        salaryRepository.save(salary);
        log.info("Salary deleted successfully");
    }

    /**
     * Get uncalculated order items for an employee in a given month/year
     */
    public List<OrderItemEarningDTO> getUncalculatedOrderItems(Long employeeId, Integer month, Integer year) {
        log.info("Fetching uncalculated order items for employee {} for {}/{}", employeeId, month, year);

        List<OrderItem> uncalculatedItems = orderItemRepository.findUncalculatedItemsByEmployeeAndMonthYear(
                employeeId, month, year);

        return uncalculatedItems.stream()
                .map(item -> OrderItemEarningDTO.builder()
                        .orderItemId(item.getId())
                        .orderId(item.getOrder().getId())
                        .serviceName(item.getProductName())
                        .amount(item.getAmount())
                        .commissionAmount(item.getCommissionAmount() != null ?
                                item.getCommissionAmount() : item.getAmount())
                        .completedAt(item.getCompletedAt())
                        .salaryCalculated(item.getSalaryCalculated() != null ? item.getSalaryCalculated() : false)
                        .includedInSalaryId(item.getIncludedInSalary() != null ? item.getIncludedInSalary().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get all order items included in a specific salary
     */
    public List<OrderItemEarningDTO> getOrderItemsBySalaryId(Long salaryId) {
        log.info("Fetching order items included in salary {}", salaryId);

        List<OrderItem> items = orderItemRepository.findItemsBySalaryId(salaryId);

        return items.stream()
                .map(item -> OrderItemEarningDTO.builder()
                        .orderItemId(item.getId())
                        .orderId(item.getOrder().getId())
                        .serviceName(item.getProductName())
                        .amount(item.getAmount())
                        .commissionAmount(item.getCommissionAmount() != null ?
                                item.getCommissionAmount() : item.getAmount())
                        .completedAt(item.getCompletedAt())
                        .salaryCalculated(true)
                        .includedInSalaryId(item.getIncludedInSalary().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get order items by employee, date range, and salary calculated status
     * Allows filtering by calculated flag and flexible date ranges
     */
    public Page<OrderItemEarningDTO> getOrderItemsByEmployeeAndDateRange(
            Long employeeId,
            java.time.LocalDateTime fromDate,
            java.time.LocalDateTime toDate,
            Boolean salaryCalculated,
            Pageable pageable) {

        log.info("Fetching order items for employee {} from {} to {}, salaryCalculated={}",
                employeeId, fromDate, toDate, salaryCalculated);

        Page<OrderItem> items;

        if (salaryCalculated != null) {
            // Filter by salary calculated flag
            items = orderItemRepository.findByEmployeeAndDateRangeAndSalaryCalculated(
                    employeeId, fromDate, toDate, salaryCalculated, pageable);
        } else {
            // Get both calculated and uncalculated items
            items = orderItemRepository.findByEmployeeAndDateRange(
                    employeeId, fromDate, toDate, pageable);
        }

        return items.map(item -> OrderItemEarningDTO.builder()
                .orderItemId(item.getId())
                .orderId(item.getOrder().getId())
                .serviceName(item.getProductName())
                .amount(item.getAmount())
                .commissionAmount(item.getCommissionAmount() != null ?
                        item.getCommissionAmount() : item.getAmount())
                .completedAt(item.getCompletedAt())
                .salaryCalculated(item.getSalaryCalculated() != null ? item.getSalaryCalculated() : false)
                .includedInSalaryId(item.getIncludedInSalary() != null ? item.getIncludedInSalary().getId() : null)
                .build());
    }

    /**
     * Get uncalculated order items for an employee (paginated, sorted by completed date DESC)
     * These are completed items that haven't been included in any salary yet
     */
    public Page<OrderItemEarningDTO> getUncalculatedOrderItemsPaginated(Long employeeId, Pageable pageable) {
        log.info("Fetching uncalculated order items (paginated) for employee {}", employeeId);

        Page<OrderItem> items = orderItemRepository.findUncalculatedItemsByEmployeePagedAndSorted(employeeId, pageable);

        return items.map(item -> OrderItemEarningDTO.builder()
                .orderItemId(item.getId())
                .orderId(item.getOrder().getId())
                .serviceName(item.getProductName())
                .amount(item.getAmount())
                .commissionAmount(item.getCommissionAmount() != null ?
                        item.getCommissionAmount() : item.getAmount())
                .completedAt(item.getCompletedAt())
                .salaryCalculated(item.getSalaryCalculated() != null ? item.getSalaryCalculated() : false)
                .includedInSalaryId(item.getIncludedInSalary() != null ? item.getIncludedInSalary().getId() : null)
                .build());
    }

    /**
     * Get order items for an employee with optional filtering by calculated status
     * @param employeeId Employee ID
     * @param salaryCalculated true for calculated items, false for uncalculated items
     * @param pageable Pagination info
     * @return Page of OrderItemEarningDTO
     */
    public Page<OrderItemEarningDTO> getOrderItemsByEmployeeAndCalculatedStatus(
            Long employeeId, Boolean salaryCalculated, Pageable pageable) {
        log.info("Fetching order items for employee {} with salaryCalculated={}", employeeId, salaryCalculated);

        Page<OrderItem> items = orderItemRepository.findOrderItemsByEmployeeAndCalculatedStatus(
                employeeId, salaryCalculated, pageable);

        return items.map(item -> OrderItemEarningDTO.builder()
                .orderItemId(item.getId())
                .orderId(item.getOrder().getId())
                .serviceName(item.getProductName())
                .amount(item.getAmount())
                .commissionAmount(item.getCommissionAmount() != null ?
                        item.getCommissionAmount() : item.getAmount())
                .completedAt(item.getCompletedAt())
                .salaryCalculated(item.getSalaryCalculated() != null ? item.getSalaryCalculated() : false)
                .includedInSalaryId(item.getIncludedInSalary() != null ? item.getIncludedInSalary().getId() : null)
                .build());
    }

    /**
     * Map Salary entity to DTO
     */
    private SalaryDTO mapToDTO(Salary salary) {
        return SalaryDTO.builder()
                .id(salary.getId())
                .employeeId(salary.getEmployee().getId())
                .employeeName(salary.getEmployee().getName())
                .month(salary.getMonth())
                .year(salary.getYear())
                .netSalary(salary.getNetSalary())
                .commissionAmount(salary.getCommissionAmount())
                .deductionAmount(salary.getDeductionAmount())
                .overtimeAmount(salary.getOvertimeAmount())
                .bonusAmount(salary.getBonusAmount())
                .allowanceAmount(salary.getAllowanceAmount())
                .notes(salary.getNotes())
                .status(salary.getStatus().toString())
                .approvedAt(salary.getApprovedAt())
                .approvedByUserId(salary.getApprovedByUserId())
                .createdAt(salary.getCreatedAt())
                .updatedAt(salary.getUpdatedAt())
                .build();
    }

    /**
     * Approve a salary
     */
    public SalaryDTO approveSalary(Long salaryId) {
        log.info("Approving salary with id: {}", salaryId);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        salary.setStatus(Salary.SalaryStatus.APPROVED);
        salary.setApprovedAt(java.time.LocalDateTime.now());

        Salary savedSalary = salaryRepository.save(salary);
        log.info("Salary approved successfully");

        return mapToDTO(savedSalary);
    }

    /**
     * Reject a salary
     */
    public SalaryDTO rejectSalary(Long salaryId, String reason) {
        log.info("Rejecting salary with id: {} - Reason: {}", salaryId, reason);

        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary not found"));

        salary.setStatus(Salary.SalaryStatus.REJECTED);
        salary.setNotes((salary.getNotes() != null ? salary.getNotes() + "\n" : "") +
                        "Rejected: " + (reason != null ? reason : "No reason provided"));

        Salary savedSalary = salaryRepository.save(salary);
        log.info("Salary rejected successfully");

        return mapToDTO(savedSalary);
    }
}

