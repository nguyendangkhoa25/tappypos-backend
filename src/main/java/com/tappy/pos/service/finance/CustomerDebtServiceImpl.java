package com.tappy.pos.service.finance;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.finance.CreateDebtRequest;
import com.tappy.pos.model.dto.finance.CustomerDebtDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtStatementDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtSummaryDTO;
import com.tappy.pos.model.dto.finance.DebtPaymentDTO;
import com.tappy.pos.model.dto.finance.RecordDebtPaymentRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.finance.CustomerDebt;
import com.tappy.pos.model.entity.finance.DebtPayment;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.DebtStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.finance.CustomerDebtRepository;
import com.tappy.pos.repository.finance.DebtPaymentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerDebtServiceImpl implements CustomerDebtService {

    private final CustomerDebtRepository debtRepository;
    private final DebtPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDebtSummaryDTO> getBalances() {
        String tid = tenantContext.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        List<Object[]> rows = debtRepository.findOutstandingBalancesByCustomer(tid);
        List<CustomerDebtSummaryDTO> result = new ArrayList<>();
        for (Object[] r : rows) {
            LocalDate earliestDue = r[4] != null ? ((java.sql.Date) r[4]).toLocalDate() : null;
            result.add(CustomerDebtSummaryDTO.builder()
                    .customerId(((Number) r[0]).longValue())
                    .customerName((String) r[1])
                    .totalOutstanding((BigDecimal) r[2])
                    .debtCount(((Number) r[3]).intValue())
                    .earliestDueDate(earliestDue)
                    .overdue(earliestDue != null && earliestDue.isBefore(today))
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDebtStatementDTO getCustomerStatement(Long customerId) {
        String tid = tenantContext.getCurrentTenantId();
        List<CustomerDebt> debts =
                debtRepository.findByCustomerIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(customerId, tid);
        List<DebtPayment> payments =
                paymentRepository.findByCustomerIdAndTenantIdOrderByPaidAtDesc(customerId, tid);
        BigDecimal outstanding = nullSafe(debtRepository.sumOutstandingByCustomer(tid, customerId));
        String customerName = debts.isEmpty()
                ? customerRepository.findByIdActiveAndTenantId(customerId, tid).map(Customer::getName).orElse(null)
                : debts.get(debts.size() - 1).getCustomerName();
        return CustomerDebtStatementDTO.builder()
                .customerId(customerId)
                .customerName(customerName)
                .totalOutstanding(outstanding)
                .debts(debts.stream().map(this::toDTO).toList())
                .payments(payments.stream().map(this::toPaymentDTO).toList())
                .build();
    }

    @Override
    @Transactional
    public CustomerDebtDTO createDebt(CreateDebtRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        Customer customer = customerRepository.findByIdActiveAndTenantId(request.getCustomerId(), tid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.customer.not.found")));
        CustomerDebt debt = CustomerDebt.builder()
                .tenantId(tid)
                .customerId(customer.getId())
                .customerName(customer.getName())
                .orderId(request.getOrderId())
                .orderNumber(request.getOrderNumber())
                .originalAmount(request.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .outstandingAmount(request.getAmount())
                .dueDate(request.getDueDate())
                .status(DebtStatus.OPEN)
                .note(request.getNote())
                .createdBy(authContext.getCurrentUsername())
                .build();
        CustomerDebtDTO saved = toDTO(debtRepository.save(debt));
        activityLogService.logAsync(tid, authContext.getCurrentUsername(), null,
                ActivityAction.DEBT_CREATED, "CUSTOMER_DEBT", String.valueOf(saved.getId()),
                "activity.debt.created", null, customer.getName(), request.getAmount());
        return saved;
    }

    @Override
    @Transactional
    public DebtPaymentDTO recordPayment(RecordDebtPaymentRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        BigDecimal amount = request.getAmount();
        BigDecimal outstanding = nullSafe(debtRepository.sumOutstandingByCustomer(tid, request.getCustomerId()));
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(messageService.getMessage("error.debt.no_outstanding"));
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new BadRequestException(messageService.getMessage("error.debt.payment_exceeds_outstanding"));
        }
        // Allocate oldest-first across the customer's open debts.
        List<CustomerDebt> openDebts = debtRepository
                .findByCustomerIdAndTenantIdAndStatusNotAndDeletedFalseOrderByCreatedAtAsc(
                        request.getCustomerId(), tid, DebtStatus.PAID);
        BigDecimal remaining = amount;
        for (CustomerDebt debt : openDebts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal applied = remaining.min(debt.getOutstandingAmount());
            debt.setPaidAmount(debt.getPaidAmount().add(applied));
            debt.setOutstandingAmount(debt.getOutstandingAmount().subtract(applied));
            debt.setStatus(debt.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? DebtStatus.PAID : DebtStatus.PARTIAL);
            debtRepository.save(debt);
            remaining = remaining.subtract(applied);
        }
        DebtPayment payment = DebtPayment.builder()
                .tenantId(tid)
                .customerId(request.getCustomerId())
                .debtId(null) // customer-level repayment allocated across debts
                .amount(amount)
                .method(request.getMethod() != null ? request.getMethod() : "CASH")
                .note(request.getNote())
                .paidAt(LocalDateTime.now())
                .createdBy(authContext.getCurrentUsername())
                .build();
        DebtPaymentDTO saved = toPaymentDTO(paymentRepository.save(payment));
        activityLogService.logAsync(tid, authContext.getCurrentUsername(), null,
                ActivityAction.DEBT_PAYMENT, "CUSTOMER_DEBT", String.valueOf(request.getCustomerId()),
                "activity.debt.payment", null, amount);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstanding() {
        return nullSafe(debtRepository.sumTotalOutstanding(tenantContext.getCurrentTenantId()));
    }

    @Override
    @Transactional
    public void deleteDebt(Long id) {
        String tid = tenantContext.getCurrentTenantId();
        CustomerDebt debt = debtRepository
                .findByIdAndTenantIdAndDeletedFalse(id, tid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.debt.not_found")));
        debt.softDelete();
        debtRepository.save(debt);
        activityLogService.logAsync(tid, authContext.getCurrentUsername(), null,
                ActivityAction.DEBT_DELETED, "CUSTOMER_DEBT", String.valueOf(id),
                "activity.debt.deleted", null, debt.getCustomerName());
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private CustomerDebtDTO toDTO(CustomerDebt d) {
        return CustomerDebtDTO.builder()
                .id(d.getId())
                .customerId(d.getCustomerId())
                .customerName(d.getCustomerName())
                .orderId(d.getOrderId())
                .orderNumber(d.getOrderNumber())
                .originalAmount(d.getOriginalAmount())
                .paidAmount(d.getPaidAmount())
                .outstandingAmount(d.getOutstandingAmount())
                .dueDate(d.getDueDate())
                .status(d.getStatus())
                .note(d.getNote())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private DebtPaymentDTO toPaymentDTO(DebtPayment p) {
        return DebtPaymentDTO.builder()
                .id(p.getId())
                .customerId(p.getCustomerId())
                .debtId(p.getDebtId())
                .amount(p.getAmount())
                .method(p.getMethod())
                .note(p.getNote())
                .paidAt(p.getPaidAt())
                .createdBy(p.getCreatedBy())
                .build();
    }
}
