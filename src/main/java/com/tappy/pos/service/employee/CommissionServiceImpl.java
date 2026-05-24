package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.*;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommissionServiceImpl implements CommissionService {

    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository  employeeRepository;
    private final UserRepository      userRepository;
    private final MessageService      messageService;

    @Override
    public MyCommissionDTO getMyCommission(String username, int month, int year) {
        validateMonthYear(month, year);

        Long userId = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("employee.not.found")))
                .getId();

        Employee emp = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.commission.employee.not.linked")));

        BigDecimal total = orderItemRepository.sumAllCommissionByEmployeeAndMonth(emp.getId(), month, year);
        if (total == null) total = BigDecimal.ZERO;

        Long count = orderItemRepository.countCommissionItemsByEmployeeAndMonth(emp.getId(), month, year);
        if (count == null) count = 0L;

        List<Object[]> rows = orderItemRepository.findCommissionDetailByEmployeeAndMonth(emp.getId(), month, year);
        List<CommissionItemDTO> items = rows.stream()
                .map(this::mapToCommissionItem)
                .collect(Collectors.toList());

        log.info("Fetched commission for employee {} month={}/{}: total={}, items={}",
                emp.getId(), month, year, total, items.size());

        return MyCommissionDTO.builder()
                .employeeId(emp.getId())
                .employeeName(emp.getFullName())
                .month(month)
                .year(year)
                .totalCommission(total)
                .itemCount(count)
                .items(items)
                .build();
    }

    @Override
    public CommissionReportDTO getCommissionReport(int month, int year) {
        validateMonthYear(month, year);

        List<Object[]> rows = orderItemRepository.findAllEmployeesCommissionSummaryByMonth(month, year);

        List<EmployeeCommissionDTO> employees = rows.stream()
                .map(r -> EmployeeCommissionDTO.builder()
                        .employeeId(((Number) r[0]).longValue())
                        .employeeName((String) r[1])
                        .totalCommission(toBigDecimal(r[2]))
                        .itemCount(((Number) r[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        BigDecimal grandTotal = employees.stream()
                .map(EmployeeCommissionDTO::getTotalCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalItems = employees.stream()
                .mapToLong(EmployeeCommissionDTO::getItemCount)
                .sum();

        log.info("Commission report month={}/{}: {} employees, total={}", month, year, employees.size(), grandTotal);

        return CommissionReportDTO.builder()
                .month(month)
                .year(year)
                .totalCommission(grandTotal)
                .totalItemCount(totalItems)
                .employees(employees)
                .build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CommissionItemDTO mapToCommissionItem(Object[] r) {
        return CommissionItemDTO.builder()
                .orderItemId(((Number) r[0]).longValue())
                .orderNumber((String) r[1])
                .productName((String) r[2])
                .quantity(r[3] != null ? ((Number) r[3]).intValue() : 0)
                .amount(toBigDecimal(r[4]))
                .commissionRate(toBigDecimal(r[5]))
                .commissionAmount(toBigDecimal(r[6]))
                .completedAt(r[7] instanceof java.sql.Timestamp
                        ? ((java.sql.Timestamp) r[7]).toLocalDateTime()
                        : (LocalDateTime) r[7])
                .build();
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }

    private void validateMonthYear(int month, int year) {
        if (month < 1 || month > 12)
            throw new BadRequestException(messageService.getMessage("error.salary.month.invalid"));
        if (year < 2000 || year > 2100)
            throw new BadRequestException(messageService.getMessage("error.salary.year.invalid"));
    }
}
