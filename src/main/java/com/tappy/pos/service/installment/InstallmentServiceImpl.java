package com.tappy.pos.service.installment;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.installment.CreateInstallmentRequest;
import com.tappy.pos.model.dto.installment.InstallmentDTO;
import com.tappy.pos.model.dto.installment.InstallmentScheduleDTO;
import com.tappy.pos.model.dto.installment.PayInstallmentRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.finance.CustomerDebt;
import com.tappy.pos.model.entity.installment.InstallmentScheduleEntity;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.DebtStatus;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.finance.CustomerDebtRepository;
import com.tappy.pos.repository.installment.InstallmentScheduleRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trả-góp (installment). A contract is a {@code CustomerDebt} with installmentCount set, plus an
 * {@code installment_schedule} child per kỳ. Interest-free. Gated by INSTALLMENT; INSTALLMENT_VIEW_ALL
 * scopes list/detail (own-vs-all, mirrors ORDER_VIEW_ALL). VEHICLE_SHOP_SHOP_TYPE_PLAN §4e.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InstallmentServiceImpl implements InstallmentService {

    private static final String VIEW_ALL = "INSTALLMENT_VIEW_ALL";

    private final CustomerDebtRepository debtRepository;
    private final InstallmentScheduleRepository scheduleRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final FeatureContext featureContext;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final MessageService messageService;

    @Override
    public InstallmentDTO create(CreateInstallmentRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        String actor = authContext.getCurrentUsername();
        Customer customer = customerRepository.findByIdActiveAndTenantId(request.getCustomerId(), tid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.customer.not.found")));

        BigDecimal down = request.getDownPayment() != null ? request.getDownPayment() : BigDecimal.ZERO;
        BigDecimal financed = request.getTotalAmount().subtract(down);
        if (financed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(messageService.getMessage("error.installment.financedNonPositive"));
        }
        int n = request.getNumberOfPeriods();
        int interval = request.getIntervalMonths() != null && request.getIntervalMonths() > 0
                ? request.getIntervalMonths() : 1;

        // Split financed amount into N whole-VND kỳ; the last kỳ absorbs the rounding remainder.
        BigDecimal perPeriod = financed.divide(BigDecimal.valueOf(n), 0, RoundingMode.FLOOR);
        LocalDate lastDue = request.getFirstDueDate().plusMonths((long) interval * (n - 1));

        CustomerDebt debt = CustomerDebt.builder()
                .tenantId(tid)
                .customerId(customer.getId())
                .customerName(customer.getName())
                .orderId(request.getOrderId())
                .orderNumber(request.getOrderNumber())
                .originalAmount(financed)
                .paidAmount(BigDecimal.ZERO)
                .outstandingAmount(financed)
                .dueDate(lastDue)
                .status(DebtStatus.OPEN)
                .note(request.getNote())
                .installmentCount(n)
                .downPayment(down.compareTo(BigDecimal.ZERO) > 0 ? down : null)
                .createdBy(actor)
                .build();
        CustomerDebt savedDebt = debtRepository.save(debt);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            BigDecimal amount = (i < n) ? perPeriod : financed.subtract(allocated);
            allocated = allocated.add(amount);
            InstallmentScheduleEntity row = InstallmentScheduleEntity.builder()
                    .tenantId(tid)
                    .debtId(savedDebt.getId())
                    .orderId(request.getOrderId())
                    .installmentNo(i)
                    .dueDate(request.getFirstDueDate().plusMonths((long) interval * (i - 1)))
                    .amount(amount)
                    .paid(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .deleted(false)
                    .build();
            scheduleRepository.save(row);
        }

        activityLogService.logAsync(tid, actor, null,
                ActivityAction.INSTALLMENT_CREATED, "INSTALLMENT", String.valueOf(savedDebt.getId()),
                "activity.installment.created", null,
                customer.getName(), String.valueOf(financed), n);
        return toDTO(savedDebt);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentDTO getById(Long debtId) {
        return toDTO(loadGuarded(debtId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstallmentDTO> search(Pageable pageable) {
        String tid = tenantContext.getCurrentTenantId();
        Page<CustomerDebt> page = featureContext.hasFeature(VIEW_ALL)
                ? debtRepository.findInstallments(tid, pageable)
                : debtRepository.findInstallmentsByCreatedBy(tid, authContext.getCurrentUsername(), pageable);
        return page.map(this::toDTO);
    }

    @Override
    public InstallmentDTO payPeriod(Long scheduleId, PayInstallmentRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        InstallmentScheduleEntity row = scheduleRepository.findByIdAndTenantIdAndDeletedFalse(scheduleId, tid)
                .orElseThrow(ResourceNotFoundException::new);
        CustomerDebt debt = loadGuarded(row.getDebtId());
        if (row.isPaid()) {
            throw new BadRequestException(messageService.getMessage("error.installment.alreadyPaid"));
        }
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : row.getAmount();
        LocalDate today = LocalDate.now();

        row.setPaid(true);
        row.setPaidAmount(amount);
        row.setPaidDate(today);
        row.setPaidBy(authContext.getCurrentUsername());
        row.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(row);

        BigDecimal newPaid = debt.getPaidAmount().add(amount);
        BigDecimal newOutstanding = debt.getOriginalAmount().subtract(newPaid);
        if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) newOutstanding = BigDecimal.ZERO;
        debt.setPaidAmount(newPaid);
        debt.setOutstandingAmount(newOutstanding);
        debt.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) <= 0 ? DebtStatus.PAID : DebtStatus.PARTIAL);
        debtRepository.save(debt);

        activityLogService.logAsync(tid, authContext.getCurrentUsername(), null,
                ActivityAction.INSTALLMENT_PAYMENT, "INSTALLMENT", String.valueOf(debt.getId()),
                "activity.installment.payment", null,
                row.getInstallmentNo(), debt.getCustomerName(), String.valueOf(amount));
        return toDTO(debt);
    }

    @Override
    public InstallmentDTO cancel(Long debtId, String reason) {
        CustomerDebt debt = loadGuarded(debtId);
        for (InstallmentScheduleEntity row : scheduleRepository
                .findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(debtId)) {
            row.setDeleted(true);
            row.setDeletedAt(LocalDateTime.now());
            scheduleRepository.save(row);
        }
        debt.softDelete();
        debt.setNote(reason != null ? reason : debt.getNote());
        debtRepository.save(debt);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.INSTALLMENT_CANCELLED, "INSTALLMENT", String.valueOf(debtId),
                "activity.installment.cancelled", null, debtId);
        return toDTO(debt);
    }

    @Override
    @Transactional
    public void notifyOverdue() {
        String tid = tenantContext.getCurrentTenantId();
        List<InstallmentScheduleEntity> overdue = scheduleRepository.findOverdue(LocalDate.now());
        if (overdue.isEmpty()) return;
        long contracts = overdue.stream().map(InstallmentScheduleEntity::getDebtId).distinct().count();
        notificationService.pushToRoles(Notification.NotificationType.INFO,
                LocalizedText.of("notification.installment.overdue.title", contracts),
                LocalizedText.of("notification.installment.overdue.message", overdue.size(), contracts),
                "INSTALLMENT", null, List.of(RoleEnum.SHOP_OWNER.getCode()));
        log.info("Installment overdue notification sent for tenant {}: {} kỳ across {} contract(s)",
                tid, overdue.size(), contracts);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private CustomerDebt loadGuarded(Long debtId) {
        String tid = tenantContext.getCurrentTenantId();
        CustomerDebt debt = debtRepository.findByIdAndTenantIdAndDeletedFalse(debtId, tid)
                .orElseThrow(ResourceNotFoundException::new);
        if (debt.getInstallmentCount() == null) {
            throw new ResourceNotFoundException(); // not an installment contract
        }
        // Ownership guard (mirrors ORDER_VIEW_ALL): 404, not 403, to avoid leaking existence.
        if (!featureContext.hasFeature(VIEW_ALL)
                && !authContext.getCurrentUsername().equals(debt.getCreatedBy())) {
            throw new ResourceNotFoundException();
        }
        return debt;
    }

    private InstallmentDTO toDTO(CustomerDebt debt) {
        List<InstallmentScheduleEntity> rows =
                scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(debt.getId());
        LocalDate today = LocalDate.now();
        List<InstallmentScheduleDTO> schedule = new ArrayList<>();
        LocalDate nextDue = null;
        boolean overdue = false;
        for (InstallmentScheduleEntity r : rows) {
            boolean rowOverdue = !r.isPaid() && r.getDueDate().isBefore(today);
            if (rowOverdue) overdue = true;
            if (!r.isPaid() && nextDue == null) nextDue = r.getDueDate();
            schedule.add(InstallmentScheduleDTO.builder()
                    .id(r.getId())
                    .installmentNo(r.getInstallmentNo())
                    .dueDate(r.getDueDate())
                    .amount(r.getAmount())
                    .paid(r.isPaid())
                    .paidAmount(r.getPaidAmount())
                    .paidDate(r.getPaidDate())
                    .overdue(rowOverdue)
                    .build());
        }
        return InstallmentDTO.builder()
                .debtId(debt.getId())
                .customerId(debt.getCustomerId())
                .customerName(debt.getCustomerName())
                .orderId(debt.getOrderId())
                .orderNumber(debt.getOrderNumber())
                .totalAmount(debt.getOriginalAmount())
                .downPayment(debt.getDownPayment())
                .paidAmount(debt.getPaidAmount())
                .outstandingAmount(debt.getOutstandingAmount())
                .installmentCount(debt.getInstallmentCount())
                .status(debt.getStatus())
                .nextDueDate(nextDue)
                .overdue(overdue)
                .note(debt.getNote())
                .createdBy(debt.getCreatedBy())
                .createdAt(debt.getCreatedAt())
                .schedule(schedule)
                .build();
    }
}
