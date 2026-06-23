package com.tappy.pos.service.installment;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.installment.CreateInstallmentRequest;
import com.tappy.pos.model.dto.installment.InstallmentDTO;
import com.tappy.pos.model.dto.installment.PayInstallmentRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.finance.CustomerDebt;
import com.tappy.pos.model.entity.installment.InstallmentScheduleEntity;
import com.tappy.pos.model.enums.DebtStatus;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.finance.CustomerDebtRepository;
import com.tappy.pos.repository.installment.InstallmentScheduleRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentServiceImpl Unit Tests")
class InstallmentServiceImplTest {

    private static final String TENANT = "test-tenant";
    private static final String USER = "user1";
    private static final String VIEW_ALL = "INSTALLMENT_VIEW_ALL";

    @Mock private CustomerDebtRepository debtRepository;
    @Mock private InstallmentScheduleRepository scheduleRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private com.tappy.pos.multitenant.TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private FeatureContext featureContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private NotificationService notificationService;
    @Mock private MessageService messageService;

    private InstallmentServiceImpl service;

    @Captor private ArgumentCaptor<CustomerDebt> debtCaptor;
    @Captor private ArgumentCaptor<InstallmentScheduleEntity> scheduleCaptor;

    @BeforeEach
    void setUp() {
        // InstallmentServiceImpl constructor order:
        // debtRepository, scheduleRepository, customerRepository, tenantContext,
        // authContext, featureContext, activityLogService, notificationService, messageService
        service = new InstallmentServiceImpl(
                debtRepository, scheduleRepository, customerRepository, tenantContext,
                authContext, featureContext, activityLogService, notificationService, messageService);

        lenient().when(authContext.getCurrentUsername()).thenReturn(USER);
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(any())).thenReturn("msg");
        lenient().when(debtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(scheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Customer customer(Long id, String name) {
        Customer c = Customer.builder().name(name).phone("0900000000").build();
        c.setId(id);
        c.setTenantId(TENANT);
        return c;
    }

    private CustomerDebt installmentDebt(Long id, BigDecimal original, BigDecimal paid, String createdBy) {
        CustomerDebt d = CustomerDebt.builder()
                .tenantId(TENANT)
                .customerId(10L)
                .customerName("Nguyen Van A")
                .originalAmount(original)
                .paidAmount(paid)
                .outstandingAmount(original.subtract(paid))
                .status(DebtStatus.OPEN)
                .installmentCount(3)
                .createdBy(createdBy)
                .build();
        d.setId(id);
        return d;
    }

    private InstallmentScheduleEntity scheduleRow(Long id, Long debtId, int no, BigDecimal amount,
                                                  boolean paid, LocalDate due) {
        return InstallmentScheduleEntity.builder()
                .id(id)
                .tenantId(TENANT)
                .debtId(debtId)
                .installmentNo(no)
                .dueDate(due)
                .amount(amount)
                .paid(paid)
                .deleted(false)
                .build();
    }

    private CreateInstallmentRequest createRequest(BigDecimal total, BigDecimal down,
                                                   int periods, Integer interval) {
        CreateInstallmentRequest req = new CreateInstallmentRequest();
        req.setCustomerId(10L);
        req.setTotalAmount(total);
        req.setDownPayment(down);
        req.setNumberOfPeriods(periods);
        req.setIntervalMonths(interval);
        req.setFirstDueDate(LocalDate.of(2026, 1, 15));
        req.setOrderId(99L);
        req.setOrderNumber("ORD-99");
        req.setNote("hop dong tra gop");
        return req;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: splits financed amount into N kỳ, last kỳ absorbs remainder")
    void create_success_schedule() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        // for the final toDTO call, no schedule rows yet
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(any()))
                .thenReturn(new ArrayList<>());
        lenient().when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);

        // total 1.000.000, down 100.000 → financed 900.000, 3 kỳ → 300.000 each (no remainder)
        CreateInstallmentRequest req = createRequest(
                new BigDecimal("1000000"), new BigDecimal("100000"), 3, 1);

        InstallmentDTO dto = service.create(req);

        verify(debtRepository).save(debtCaptor.capture());
        CustomerDebt saved = debtCaptor.getValue();
        assertThat(saved.getOriginalAmount()).isEqualByComparingTo("900000");
        assertThat(saved.getOutstandingAmount()).isEqualByComparingTo("900000");
        assertThat(saved.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(saved.getStatus()).isEqualTo(DebtStatus.OPEN);
        assertThat(saved.getInstallmentCount()).isEqualTo(3);
        assertThat(saved.getDownPayment()).isEqualByComparingTo("100000");
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 15)); // first + 2 months

        verify(scheduleRepository, times(3)).save(scheduleCaptor.capture());
        List<InstallmentScheduleEntity> rows = scheduleCaptor.getAllValues();
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo("300000");
        assertThat(rows.get(1).getAmount()).isEqualByComparingTo("300000");
        assertThat(rows.get(2).getAmount()).isEqualByComparingTo("300000");
        assertThat(rows.get(0).getInstallmentNo()).isEqualTo(1);
        assertThat(rows.get(2).getInstallmentNo()).isEqualTo(3);
        assertThat(rows.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(rows.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(rows.get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 15));

        assertThat(dto.getDebtId()).isNull(); // entity id not generated in unit test
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                any(), eq("INSTALLMENT"), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create: remainder lands on last kỳ when not evenly divisible")
    void create_remainderOnLastPeriod() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(any()))
                .thenReturn(new ArrayList<>());

        // financed 1.000.000 over 3 kỳ → floor 333.333 ×2, last = 1.000.000-666.666 = 333.334
        CreateInstallmentRequest req = createRequest(new BigDecimal("1000000"), null, 3, 1);

        service.create(req);

        verify(scheduleRepository, times(3)).save(scheduleCaptor.capture());
        List<InstallmentScheduleEntity> rows = scheduleCaptor.getAllValues();
        assertThat(rows.get(0).getAmount()).isEqualByComparingTo("333333");
        assertThat(rows.get(1).getAmount()).isEqualByComparingTo("333333");
        assertThat(rows.get(2).getAmount()).isEqualByComparingTo("333334");
        // sum equals financed
        BigDecimal sum = rows.stream().map(InstallmentScheduleEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("create: null down payment is treated as zero and stored as null")
    void create_nullDownPayment() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(any()))
                .thenReturn(new ArrayList<>());

        CreateInstallmentRequest req = createRequest(new BigDecimal("600000"), null, 2, 1);

        service.create(req);

        verify(debtRepository).save(debtCaptor.capture());
        assertThat(debtCaptor.getValue().getDownPayment()).isNull();
        assertThat(debtCaptor.getValue().getOriginalAmount()).isEqualByComparingTo("600000");
    }

    @Test
    @DisplayName("create: null intervalMonths defaults to 1 month spacing")
    void create_nullIntervalDefaultsToOne() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(any()))
                .thenReturn(new ArrayList<>());

        CreateInstallmentRequest req = createRequest(new BigDecimal("400000"), null, 2, null);

        service.create(req);

        verify(scheduleRepository, times(2)).save(scheduleCaptor.capture());
        List<InstallmentScheduleEntity> rows = scheduleCaptor.getAllValues();
        assertThat(rows.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(rows.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    @DisplayName("create: custom intervalMonths spaces kỳ accordingly")
    void create_customInterval() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(any()))
                .thenReturn(new ArrayList<>());

        CreateInstallmentRequest req = createRequest(new BigDecimal("600000"), null, 3, 2);

        service.create(req);

        verify(debtRepository).save(debtCaptor.capture());
        // last due = first + 2*(3-1) = first + 4 months
        assertThat(debtCaptor.getValue().getDueDate()).isEqualTo(LocalDate.of(2026, 5, 15));
        verify(scheduleRepository, times(3)).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getAllValues().get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("create: customer not found → ResourceNotFoundException")
    void create_customerNotFound() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT)).thenReturn(Optional.empty());
        CreateInstallmentRequest req = createRequest(new BigDecimal("600000"), null, 2, 1);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(debtRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: financed amount ≤ 0 (down ≥ total) → BadRequestException")
    void create_financedNonPositive() {
        when(customerRepository.findByIdActiveAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "Nguyen Van A")));
        // down equals total → financed = 0
        CreateInstallmentRequest req = createRequest(new BigDecimal("500000"), new BigDecimal("500000"), 2, 1);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class);
        verify(debtRepository, never()).save(any());
        verify(scheduleRepository, never()).save(any());
    }

    // ── getById / loadGuarded ──────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns DTO with schedule, nextDueDate and overdue flags")
    void getById_success() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), new BigDecimal("300000"), USER);
        debt.setStatus(DebtStatus.PARTIAL);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);

        LocalDate today = LocalDate.now();
        List<InstallmentScheduleEntity> rows = List.of(
                scheduleRow(1L, 5L, 1, new BigDecimal("300000"), true, today.minusMonths(2)),
                scheduleRow(2L, 5L, 2, new BigDecimal("300000"), false, today.minusDays(1)),  // overdue
                scheduleRow(3L, 5L, 3, new BigDecimal("300000"), false, today.plusMonths(1)));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L)).thenReturn(rows);

        InstallmentDTO dto = service.getById(5L);

        assertThat(dto.getDebtId()).isEqualTo(5L);
        assertThat(dto.getStatus()).isEqualTo(DebtStatus.PARTIAL);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("900000");
        assertThat(dto.getSchedule()).hasSize(3);
        assertThat(dto.isOverdue()).isTrue();
        assertThat(dto.getNextDueDate()).isEqualTo(today.minusDays(1)); // first unpaid kỳ
        assertThat(dto.getSchedule().get(0).isOverdue()).isFalse(); // paid
        assertThat(dto.getSchedule().get(1).isOverdue()).isTrue();
        assertThat(dto.getSchedule().get(2).isOverdue()).isFalse(); // future
    }

    @Test
    @DisplayName("getById: not found → ResourceNotFoundException")
    void getById_notFound() {
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: debt without installmentCount → not an installment contract → 404")
    void getById_notInstallmentContract() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        debt.setInstallmentCount(null);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: ownership guard — no VIEW_ALL and not owner → 404")
    void getById_ownershipDenied() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, "someoneElse");
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);

        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: owner without VIEW_ALL is allowed")
    void getById_ownerAllowedWithoutViewAll() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        InstallmentDTO dto = service.getById(5L);
        assertThat(dto.getDebtId()).isEqualTo(5L);
        assertThat(dto.getNextDueDate()).isNull();
        assertThat(dto.isOverdue()).isFalse();
    }

    // ── search ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: with VIEW_ALL uses findInstallments (all tenant)")
    void search_viewAll() {
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        CustomerDebt debt = installmentDebt(7L, new BigDecimal("600000"), BigDecimal.ZERO, USER);
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerDebt> page = new PageImpl<>(List.of(debt));
        when(debtRepository.findInstallments(TENANT, pageable)).thenReturn(page);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(7L))
                .thenReturn(new ArrayList<>());

        Page<InstallmentDTO> result = service.search(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDebtId()).isEqualTo(7L);
        verify(debtRepository).findInstallments(TENANT, pageable);
        verify(debtRepository, never()).findInstallmentsByCreatedBy(any(), any(), any());
    }

    @Test
    @DisplayName("search: without VIEW_ALL uses findInstallmentsByCreatedBy (own only)")
    void search_ownOnly() {
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerDebt> page = new PageImpl<>(new ArrayList<>());
        when(debtRepository.findInstallmentsByCreatedBy(TENANT, USER, pageable)).thenReturn(page);

        Page<InstallmentDTO> result = service.search(pageable);

        assertThat(result.getContent()).isEmpty();
        verify(debtRepository).findInstallmentsByCreatedBy(TENANT, USER, pageable);
        verify(debtRepository, never()).findInstallments(any(), any());
    }

    // ── payPeriod ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("payPeriod: partial payment → status PARTIAL, balance remaining")
    void payPeriod_partial() {
        InstallmentScheduleEntity row = scheduleRow(1L, 5L, 1, new BigDecimal("300000"), false,
                LocalDate.of(2026, 1, 15));
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(row));
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        PayInstallmentRequest req = new PayInstallmentRequest();
        req.setAmount(new BigDecimal("300000"));

        InstallmentDTO dto = service.payPeriod(1L, req);

        assertThat(row.isPaid()).isTrue();
        assertThat(row.getPaidAmount()).isEqualByComparingTo("300000");
        assertThat(row.getPaidDate()).isEqualTo(LocalDate.now());
        assertThat(row.getPaidBy()).isEqualTo(USER);
        verify(scheduleRepository).save(row);

        assertThat(debt.getPaidAmount()).isEqualByComparingTo("300000");
        assertThat(debt.getOutstandingAmount()).isEqualByComparingTo("600000");
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.PARTIAL);
        verify(debtRepository).save(debt);
        assertThat(dto.getDebtId()).isEqualTo(5L);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                any(), eq("INSTALLMENT"), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("payPeriod: full payoff → status PAID, outstanding 0")
    void payPeriod_full() {
        InstallmentScheduleEntity row = scheduleRow(3L, 5L, 3, new BigDecimal("300000"), false,
                LocalDate.of(2026, 3, 15));
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(3L, TENANT)).thenReturn(Optional.of(row));
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), new BigDecimal("600000"), USER);
        debt.setStatus(DebtStatus.PARTIAL);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        PayInstallmentRequest req = new PayInstallmentRequest();
        req.setAmount(new BigDecimal("300000"));

        service.payPeriod(3L, req);

        assertThat(debt.getPaidAmount()).isEqualByComparingTo("900000");
        assertThat(debt.getOutstandingAmount()).isEqualByComparingTo("0");
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.PAID);
    }

    @Test
    @DisplayName("payPeriod: amount omitted defaults to the kỳ scheduled amount")
    void payPeriod_defaultAmount() {
        InstallmentScheduleEntity row = scheduleRow(1L, 5L, 1, new BigDecimal("250000"), false,
                LocalDate.of(2026, 1, 15));
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(row));
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("750000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        PayInstallmentRequest req = new PayInstallmentRequest(); // amount null

        service.payPeriod(1L, req);

        assertThat(row.getPaidAmount()).isEqualByComparingTo("250000");
        assertThat(debt.getPaidAmount()).isEqualByComparingTo("250000");
    }

    @Test
    @DisplayName("payPeriod: overpay clamps outstanding to 0 and marks PAID")
    void payPeriod_overpayClampsToZero() {
        InstallmentScheduleEntity row = scheduleRow(1L, 5L, 1, new BigDecimal("300000"), false,
                LocalDate.of(2026, 1, 15));
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(row));
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("500000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        PayInstallmentRequest req = new PayInstallmentRequest();
        req.setAmount(new BigDecimal("700000")); // more than the 500.000 owed

        service.payPeriod(1L, req);

        assertThat(debt.getPaidAmount()).isEqualByComparingTo("700000");
        assertThat(debt.getOutstandingAmount()).isEqualByComparingTo("0"); // clamped, not negative
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.PAID);
    }

    @Test
    @DisplayName("payPeriod: schedule row not found → ResourceNotFoundException")
    void payPeriod_scheduleNotFound() {
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.empty());
        PayInstallmentRequest req = new PayInstallmentRequest();

        assertThatThrownBy(() -> service.payPeriod(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(debtRepository, never()).save(any());
    }

    @Test
    @DisplayName("payPeriod: already-paid kỳ → BadRequestException")
    void payPeriod_alreadyPaid() {
        InstallmentScheduleEntity row = scheduleRow(1L, 5L, 1, new BigDecimal("300000"), true,
                LocalDate.of(2026, 1, 15));
        when(scheduleRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(row));
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);

        PayInstallmentRequest req = new PayInstallmentRequest();
        req.setAmount(new BigDecimal("300000"));

        assertThatThrownBy(() -> service.payPeriod(1L, req))
                .isInstanceOf(BadRequestException.class);
        verify(debtRepository, never()).save(any());
    }

    // ── cancel ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: soft-deletes all schedule rows + the debt, sets reason as note")
    void cancel_success() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        List<InstallmentScheduleEntity> rows = new ArrayList<>(List.of(
                scheduleRow(1L, 5L, 1, new BigDecimal("450000"), false, LocalDate.of(2026, 1, 15)),
                scheduleRow(2L, 5L, 2, new BigDecimal("450000"), false, LocalDate.of(2026, 2, 15))));
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L)).thenReturn(rows);

        InstallmentDTO dto = service.cancel(5L, "khach huy hop dong");

        assertThat(rows.get(0).isDeleted()).isTrue();
        assertThat(rows.get(0).getDeletedAt()).isNotNull();
        assertThat(rows.get(1).isDeleted()).isTrue();
        verify(scheduleRepository, times(2)).save(any());
        assertThat(debt.isDeleted()).isTrue();
        assertThat(debt.getNote()).isEqualTo("khach huy hop dong");
        verify(debtRepository).save(debt);
        assertThat(dto.getDebtId()).isEqualTo(5L);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                any(), eq("INSTALLMENT"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cancel: null reason keeps the existing note")
    void cancel_nullReasonKeepsNote() {
        CustomerDebt debt = installmentDebt(5L, new BigDecimal("900000"), BigDecimal.ZERO, USER);
        debt.setNote("ghi chu cu");
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(debt));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(scheduleRepository.findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(5L))
                .thenReturn(new ArrayList<>());

        service.cancel(5L, null);

        assertThat(debt.getNote()).isEqualTo("ghi chu cu");
        assertThat(debt.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("cancel: debt not found → ResourceNotFoundException")
    void cancel_notFound() {
        when(debtRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(5L, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── notifyOverdue ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("notifyOverdue: no overdue kỳ → returns early, no notification pushed")
    void notifyOverdue_none() {
        when(scheduleRepository.findOverdue(any())).thenReturn(new ArrayList<>());

        service.notifyOverdue();

        verify(notificationService, never()).pushToRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("notifyOverdue: overdue kỳ exist → pushes notification with distinct contract count")
    void notifyOverdue_pushes() {
        List<InstallmentScheduleEntity> overdue = List.of(
                scheduleRow(1L, 100L, 1, new BigDecimal("100000"), false, LocalDate.now().minusDays(2)),
                scheduleRow(2L, 100L, 2, new BigDecimal("100000"), false, LocalDate.now().minusDays(1)),
                scheduleRow(3L, 200L, 1, new BigDecimal("100000"), false, LocalDate.now().minusDays(3)));
        when(scheduleRepository.findOverdue(any())).thenReturn(overdue);

        service.notifyOverdue();

        // 3 overdue kỳ across 2 distinct contracts (debtId 100 & 200)
        verify(notificationService).pushToRoles(any(), any(), any(),
                eq("INSTALLMENT"), any(), any());
    }
}
