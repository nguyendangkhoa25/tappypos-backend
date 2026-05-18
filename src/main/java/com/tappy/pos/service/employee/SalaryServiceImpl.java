package com.tappy.pos.service.employee;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.*;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.employee.Salary;
import com.tappy.pos.model.entity.employee.SalaryAdjustment;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.SalaryStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.employee.SalaryAdjustmentRepository;
import com.tappy.pos.repository.employee.SalaryAdvanceRepository;
import com.tappy.pos.repository.employee.SalaryRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalaryServiceImpl implements SalaryService {

    private final SalaryRepository           salaryRepository;
    private final SalaryAdjustmentRepository adjustmentRepository;
    private final SalaryAdvanceRepository    advanceRepository;
    private final OrderItemRepository        orderItemRepository;
    private final EmployeeRepository         employeeRepository;
    private final UserRepository             userRepository;
    private final MessageService             messageService;
    private final TenantContext              tenantContext;
    private final FeatureContext             featureContext;
    private final ActivityLogService         activityLogService;
    private final NotificationService        notificationService;

    @Override
    public List<SalaryDTO> generatePayroll(GenerateSalaryRequest request) {
        int month = request.getMonth();
        int year  = request.getYear();
        if (month < 1 || month > 12)
            throw new BadRequestException(messageService.getMessage("error.salary.month.invalid"));
        if (year < 2000 || year > 2100)
            throw new BadRequestException(messageService.getMessage("error.salary.year.invalid"));

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Employee> employees = employeeRepository.findAllActive();
        List<SalaryDTO> result = new ArrayList<>();

        for (Employee emp : employees) {
            if (salaryRepository.existsByEmployeeIdAndMonthAndYear(emp.getId(), month, year)) {
                log.info("Salary already exists for employee {} {}/{}", emp.getId(), month, year);
                continue;
            }

            BigDecimal commission = orderItemRepository
                    .sumPendingCommissionByEmployeeAndMonth(emp.getId(), month, year);
            if (commission == null) commission = BigDecimal.ZERO;

            BigDecimal baseWage = emp.getBaseWage() != null ? emp.getBaseWage() : BigDecimal.ZERO;

            BigDecimal advance = advanceRepository.sumPendingByEmployeeAndMonth(emp.getId(), month, year);
            if (advance == null) advance = BigDecimal.ZERO;

            Salary salary = Salary.builder()
                    .tenantId(tenantContext.getCurrentTenantId())
                    .employeeId(emp.getId())
                    .employeeName(emp.getFullName())
                    .month(month)
                    .year(year)
                    .baseWage(baseWage)
                    .totalCommission(commission)
                    .advanceAmount(advance)
                    .totalAmount(baseWage.add(commission).subtract(advance))
                    .status(SalaryStatus.DRAFT)
                    .createdBy(actor)
                    .build();

            Salary saved = salaryRepository.save(salary);
            orderItemRepository.linkItemsToSalary(saved.getId(), emp.getId(), month, year);
            if (advance.compareTo(BigDecimal.ZERO) > 0) {
                advanceRepository.linkAdvancesToSalary(saved.getId(), emp.getId(), month, year);
            }
            result.add(mapToDTO(saved));
        }

        activityLogService.logAsync(
                tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.SALARY_GENERATED, "SALARY",
                null, "Tạo bảng lương tháng " + month + "/" + year, null);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalaryDTO> getSalaries(String statusStr, Integer year, Integer month, int page, int size) {
        SalaryStatus status = statusStr != null && !statusStr.isBlank()
                ? SalaryStatus.valueOf(statusStr) : null;
        PageRequest pageable = PageRequest.of(page, size);

        if (featureContext.hasFeature("SALARY_VIEW_ALL")) {
            return salaryRepository.findAllFiltered(status, year, month, pageable)
                    .map(this::mapToDTO);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByUsernameTenantScoped(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("employee.not.found")))
                .getId();
        Employee emp = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("employee.not.found")));
        return salaryRepository.findByEmployeeIdFiltered(emp.getId(), status, year, month, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryDTO getSalaryDetail(Long id) {
        Salary salary = findSalary(id);
        List<Object[]> rows = orderItemRepository.findCommissionItemsBySalaryId(id);
        List<SalaryCommissionItemDTO> items = rows.stream().map(r -> SalaryCommissionItemDTO.builder()
                .orderItemId(toLong(r[0]))
                .orderNumber(str(r[1]))
                .productName(str(r[2]))
                .quantity(toInt(r[3]))
                .amount(toBD(r[4]))
                .commissionRate(toBD(r[5]))
                .commissionAmount(toBD(r[6]))
                .completedAt(r[7] instanceof LocalDateTime ldt ? ldt : null)
                .build()).toList();

        List<SalaryAdjustmentDTO> adjustments = adjustmentRepository
                .findBySalaryIdOrderByCreatedAtAsc(id)
                .stream().map(this::mapAdjToDTO).toList();

        SalaryDTO dto = mapToDTO(salary);
        dto.setCommissionItems(items);
        dto.setAdjustments(adjustments);
        return dto;
    }

    @Override
    public SalaryDTO approve(Long id, ApproveSalaryRequest request) {
        Salary salary = findSalary(id);
        if (salary.getStatus() != SalaryStatus.DRAFT)
            throw new BadRequestException(messageService.getMessage("error.salary.cannot.approve"));
        salary.setStatus(SalaryStatus.APPROVED);
        salary.setApprovedAt(LocalDateTime.now());
        Salary saved = salaryRepository.save(salary);
        logAction(ActivityAction.SALARY_APPROVED, saved.getId(), "Phê duyệt lương " + saved.getEmployeeName());

        if (request != null && request.isSendNotification()) {
            pushApprovalNotification(saved);
        }
        return mapToDTO(saved);
    }

    @Override
    public SalaryDTO markPaid(Long id, PaySalaryRequest request) {
        Salary salary = findSalary(id);
        if (salary.getStatus() != SalaryStatus.APPROVED)
            throw new BadRequestException(messageService.getMessage("error.salary.cannot.pay"));
        salary.setStatus(SalaryStatus.PAID);
        salary.setPaidAt(LocalDateTime.now());
        Salary saved = salaryRepository.save(salary);
        orderItemRepository.markSalaryCalculated(saved.getId());
        logAction(ActivityAction.SALARY_PAID, saved.getId(), "Thanh toán lương " + saved.getEmployeeName());
        if (request != null && request.isSendNotification()) {
            pushPaymentNotification(saved);
        }
        return mapToDTO(saved);
    }

    @Override
    public void delete(Long id) {
        Salary salary = findSalary(id);
        if (salary.getStatus() != SalaryStatus.DRAFT)
            throw new BadRequestException(messageService.getMessage("error.salary.cannot.delete"));
        orderItemRepository.unlinkFromSalary(id);
        advanceRepository.unlinkFromSalary(id);
        adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(id)
                .forEach(adjustmentRepository::delete);
        salaryRepository.delete(salary);
    }

    @Override
    public SalaryAdjustmentDTO addAdjustment(Long salaryId, SalaryAdjustmentRequest req) {
        Salary salary = findSalary(salaryId);
        if (salary.getStatus() != SalaryStatus.DRAFT)
            throw new BadRequestException(messageService.getMessage("error.salary.cannot.adjust"));
        if (req.getAmount() == null || req.getAmount().signum() <= 0)
            throw new BadRequestException(messageService.getMessage("error.adjustment.amount.invalid"));
        if (!"BONUS".equals(req.getType()) && !"DEDUCTION".equals(req.getType()))
            throw new BadRequestException(messageService.getMessage("error.adjustment.type.invalid"));

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        SalaryAdjustment adj = SalaryAdjustment.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .salaryId(salaryId)
                .type(req.getType())
                .amount(req.getAmount())
                .note(req.getNote())
                .createdBy(actor)
                .build();
        SalaryAdjustment saved = adjustmentRepository.save(adj);
        recalculateTotalAmount(salary);
        salaryRepository.save(salary);
        return mapAdjToDTO(saved);
    }

    @Override
    public void removeAdjustment(Long salaryId, Long adjId) {
        Salary salary = findSalary(salaryId);
        if (salary.getStatus() != SalaryStatus.DRAFT)
            throw new BadRequestException(messageService.getMessage("error.salary.cannot.adjust"));
        adjustmentRepository.findByIdAndSalaryId(adjId, salaryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.adjustment.not.found", adjId)));
        adjustmentRepository.deleteById(adjId);
        recalculateTotalAmount(salary);
        salaryRepository.save(salary);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recalculateTotalAmount(Salary salary) {
        BigDecimal bonuses     = adjustmentRepository.sumByType(salary.getId(), "BONUS");
        BigDecimal deductions  = adjustmentRepository.sumByType(salary.getId(), "DEDUCTION");
        if (bonuses    == null) bonuses    = BigDecimal.ZERO;
        if (deductions == null) deductions = BigDecimal.ZERO;
        BigDecimal advance = salary.getAdvanceAmount() != null ? salary.getAdvanceAmount() : BigDecimal.ZERO;
        salary.setTotalAmount(salary.getBaseWage()
                .add(salary.getTotalCommission())
                .add(bonuses)
                .subtract(deductions)
                .subtract(advance));
    }

    private void pushApprovalNotification(Salary salary) {
        try {
            employeeRepository.findById(salary.getEmployeeId()).ifPresent(emp -> {
                if (emp.getUserId() == null) return;
                userRepository.findById(emp.getUserId()).ifPresent(user -> {
                    String title   = "Bảng lương đã được phê duyệt";
                    String message = "Bảng lương tháng " + salary.getMonth() + "/" + salary.getYear()
                            + " của bạn đã được phê duyệt. Tổng lương: "
                            + String.format("%,.0f", salary.getTotalAmount()) + " ₫";
                    notificationService.pushSystemAsync(
                            user.getUsername(),
                            Notification.NotificationType.SYSTEM,
                            title, message,
                            "SALARY", salary.getId(),
                            tenantContext.getCurrentTenantId());
                });
            });
        } catch (Exception e) {
            log.warn("Failed to push salary approval notification for salaryId={}: {}", salary.getId(), e.getMessage());
        }
    }

    private void pushPaymentNotification(Salary salary) {
        try {
            employeeRepository.findById(salary.getEmployeeId()).ifPresent(emp -> {
                if (emp.getUserId() == null) return;
                userRepository.findById(emp.getUserId()).ifPresent(user -> {
                    String title   = "Lương đã được thanh toán";
                    String message = "Lương tháng " + salary.getMonth() + "/" + salary.getYear()
                            + " của bạn đã được thanh toán. Tổng lương: "
                            + String.format("%,.0f", salary.getTotalAmount()) + " ₫";
                    notificationService.pushSystemAsync(
                            user.getUsername(),
                            Notification.NotificationType.SYSTEM,
                            title, message,
                            "SALARY", salary.getId(),
                            tenantContext.getCurrentTenantId());
                });
            });
        } catch (Exception e) {
            log.warn("Failed to push salary payment notification for salaryId={}: {}", salary.getId(), e.getMessage());
        }
    }

    private Salary findSalary(Long id) {
        return salaryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.salary.not.found", id)));
    }

    private void logAction(ActivityAction action, Long targetId, String desc) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                action, "SALARY", targetId == null ? null : targetId.toString(), desc, null);
    }

    private SalaryDTO mapToDTO(Salary s) {
        return SalaryDTO.builder()
                .id(s.getId())
                .employeeId(s.getEmployeeId())
                .employeeName(s.getEmployeeName())
                .month(s.getMonth())
                .year(s.getYear())
                .baseWage(s.getBaseWage())
                .totalCommission(s.getTotalCommission())
                .advanceAmount(s.getAdvanceAmount())
                .totalAmount(s.getTotalAmount())
                .status(s.getStatus().name())
                .notes(s.getNotes())
                .approvedAt(s.getApprovedAt())
                .paidAt(s.getPaidAt())
                .createdBy(s.getCreatedBy())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SalaryAdjustmentDTO mapAdjToDTO(SalaryAdjustment a) {
        return SalaryAdjustmentDTO.builder()
                .id(a.getId())
                .type(a.getType())
                .amount(a.getAmount())
                .note(a.getNote())
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private static Long       toLong(Object v) { return v == null ? null : ((Number) v).longValue(); }
    private static Integer    toInt(Object v)  { return v == null ? null : ((Number) v).intValue(); }
    private static BigDecimal toBD(Object v)   { return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString()); }
    private static String     str(Object v)    { return v == null ? null : v.toString(); }
}
