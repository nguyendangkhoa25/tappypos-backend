package com.tappy.pos.service.employee;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.*;
import com.tappy.pos.model.entity.auth.User;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryService Unit Tests")
class SalaryServiceImplTest {

    @Mock private SalaryRepository           salaryRepository;
    @Mock private SalaryAdjustmentRepository adjustmentRepository;
    @Mock private SalaryAdvanceRepository    advanceRepository;
    @Mock private OrderItemRepository        orderItemRepository;
    @Mock private EmployeeRepository         employeeRepository;
    @Mock private UserRepository             userRepository;
    @Mock private MessageService             messageService;
    @Mock private TenantContext              tenantContext;
    @Mock private FeatureContext             featureContext;
    @Mock private ActivityLogService         activityLogService;
    @Mock private NotificationService        notificationService;

    @InjectMocks
    private SalaryServiceImpl salaryService;

    private Employee employee;
    private Salary   draftSalary;

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        employee = Employee.builder()
                .id(1L)
                .fullName("Nguyễn Văn An")
                .baseWage(BigDecimal.valueOf(5_000_000))
                .build();

        draftSalary = Salary.builder()
                .id(1L)
                .employeeId(1L)
                .employeeName("Nguyễn Văn An")
                .month(5)
                .year(2026)
                .baseWage(BigDecimal.valueOf(5_000_000))
                .totalCommission(BigDecimal.ZERO)
                .advanceAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(5_000_000))
                .status(SalaryStatus.DRAFT)
                .createdBy("testuser")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── generatePayroll ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generatePayroll: month < 1 → BadRequestException")
    void generatePayroll_invalidMonth_throws() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(0);
        req.setYear(2026);
        when(messageService.getMessage("error.salary.month.invalid")).thenReturn("Invalid month");

        assertThatThrownBy(() -> salaryService.generatePayroll(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("generatePayroll: month > 12 → BadRequestException")
    void generatePayroll_monthAbove12_throws() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(13);
        req.setYear(2026);
        when(messageService.getMessage("error.salary.month.invalid")).thenReturn("Invalid month");

        assertThatThrownBy(() -> salaryService.generatePayroll(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("generatePayroll: year < 2000 → BadRequestException")
    void generatePayroll_invalidYear_throws() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(1999);
        when(messageService.getMessage("error.salary.year.invalid")).thenReturn("Invalid year");

        assertThatThrownBy(() -> salaryService.generatePayroll(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("generatePayroll: success — creates salary for one active employee")
    void generatePayroll_success_createsOneSalary() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(2026);

        when(employeeRepository.findAllActive()).thenReturn(List.of(employee));
        when(salaryRepository.existsByEmployeeIdAndMonthAndYear(1L, 5, 2026)).thenReturn(false);
        when(orderItemRepository.sumPendingCommissionByEmployeeAndMonth(1L, 5, 2026))
                .thenReturn(BigDecimal.valueOf(200_000));
        when(advanceRepository.sumPendingByEmployeeAndMonth(1L, 5, 2026)).thenReturn(BigDecimal.ZERO);
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        List<SalaryDTO> result = salaryService.generatePayroll(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo(1L);
        verify(salaryRepository).save(any(Salary.class));
        verify(orderItemRepository).linkItemsToSalary(eq(1L), eq(1L), eq(5), eq(2026));
        verify(activityLogService).logAsync(eq("tenant1"), eq("testuser"), isNull(),
                eq(ActivityAction.SALARY_GENERATED), eq("SALARY"), isNull(), anyString(), isNull());
    }

    @Test
    @DisplayName("generatePayroll: null commission → treated as zero")
    void generatePayroll_nullCommission_treatedAsZero() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(2026);

        when(employeeRepository.findAllActive()).thenReturn(List.of(employee));
        when(salaryRepository.existsByEmployeeIdAndMonthAndYear(1L, 5, 2026)).thenReturn(false);
        when(orderItemRepository.sumPendingCommissionByEmployeeAndMonth(1L, 5, 2026)).thenReturn(null);
        when(advanceRepository.sumPendingByEmployeeAndMonth(1L, 5, 2026)).thenReturn(null);
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        List<SalaryDTO> result = salaryService.generatePayroll(req);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("generatePayroll: salary already exists for employee — skips that employee")
    void generatePayroll_skipsExistingSalary() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(2026);

        when(employeeRepository.findAllActive()).thenReturn(List.of(employee));
        when(salaryRepository.existsByEmployeeIdAndMonthAndYear(1L, 5, 2026)).thenReturn(true);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        List<SalaryDTO> result = salaryService.generatePayroll(req);

        assertThat(result).isEmpty();
        verify(salaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("generatePayroll: with advance > 0 — links advances to salary")
    void generatePayroll_withAdvance_linksAdvances() {
        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(2026);

        when(employeeRepository.findAllActive()).thenReturn(List.of(employee));
        when(salaryRepository.existsByEmployeeIdAndMonthAndYear(1L, 5, 2026)).thenReturn(false);
        when(orderItemRepository.sumPendingCommissionByEmployeeAndMonth(1L, 5, 2026)).thenReturn(null);
        when(advanceRepository.sumPendingByEmployeeAndMonth(1L, 5, 2026))
                .thenReturn(BigDecimal.valueOf(500_000));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        salaryService.generatePayroll(req);

        verify(advanceRepository).linkAdvancesToSalary(eq(1L), eq(1L), eq(5), eq(2026));
    }

    // ── getSalaries ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSalaries: SALARY_VIEW_ALL feature — returns all salaries via findAllFiltered")
    void getSalaries_viewAll_returnsAll() {
        Page<Salary> page = new PageImpl<>(List.of(draftSalary));
        when(featureContext.hasFeature("SALARY_VIEW_ALL")).thenReturn(true);
        when(salaryRepository.findAllFiltered(isNull(), eq(2026), eq(5), any(PageRequest.class)))
                .thenReturn(page);

        Page<SalaryDTO> result = salaryService.getSalaries(null, 2026, 5, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository, never()).findByUsernameTenantScoped(anyString());
    }

    @Test
    @DisplayName("getSalaries: statusStr DRAFT parsed — passes DRAFT to repository")
    void getSalaries_withStatus_parsesStatus() {
        Page<Salary> page = new PageImpl<>(List.of(draftSalary));
        when(featureContext.hasFeature("SALARY_VIEW_ALL")).thenReturn(true);
        when(salaryRepository.findAllFiltered(eq(SalaryStatus.DRAFT), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        Page<SalaryDTO> result = salaryService.getSalaries("DRAFT", null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("getSalaries: no SALARY_VIEW_ALL — returns employee's own salaries")
    void getSalaries_noViewAll_returnsOwn() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        Employee emp = Employee.builder().id(1L).build();
        Page<Salary> page = new PageImpl<>(List.of(draftSalary));

        when(featureContext.hasFeature("SALARY_VIEW_ALL")).thenReturn(false);
        when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(emp));
        when(salaryRepository.findByEmployeeIdFiltered(eq(1L), isNull(), isNull(), isNull(),
                any(PageRequest.class))).thenReturn(page);

        Page<SalaryDTO> result = salaryService.getSalaries(null, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(salaryRepository, never()).findAllFiltered(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getSalaries: no view-all and user not found → ResourceNotFoundException")
    void getSalaries_noViewAll_userNotFound_throws() {
        when(featureContext.hasFeature("SALARY_VIEW_ALL")).thenReturn(false);
        when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.empty());
        when(messageService.getMessage("employee.not.found")).thenReturn("Not found");

        assertThatThrownBy(() -> salaryService.getSalaries(null, null, null, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getSalaries: no view-all and employee not found → ResourceNotFoundException")
    void getSalaries_noViewAll_employeeNotFound_throws() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);

        when(featureContext.hasFeature("SALARY_VIEW_ALL")).thenReturn(false);
        when(userRepository.findByUsernameTenantScoped("testuser")).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(messageService.getMessage("employee.not.found")).thenReturn("Not found");

        assertThatThrownBy(() -> salaryService.getSalaries(null, null, null, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSalaryDetail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSalaryDetail: success — returns DTO with commission items and adjustments")
    void getSalaryDetail_success() {
        SalaryAdjustment adj = SalaryAdjustment.builder()
                .id(1L).type("BONUS").amount(BigDecimal.valueOf(100_000)).salaryId(1L).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(orderItemRepository.findCommissionItemsBySalaryId(1L)).thenReturn(List.of());
        when(adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(adj));

        SalaryDTO result = salaryService.getSalaryDetail(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAdjustments()).hasSize(1);
        assertThat(result.getCommissionItems()).isEmpty();
    }

    @Test
    @DisplayName("getSalaryDetail: salary not found → ResourceNotFoundException")
    void getSalaryDetail_notFound_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> salaryService.getSalaryDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── approve ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve: DRAFT → APPROVED, no notification sent")
    void approve_success_noNotification() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        ApproveSalaryRequest req = new ApproveSalaryRequest();
        req.setSendNotification(false);

        SalaryDTO result = salaryService.approve(1L, req);

        assertThat(result).isNotNull();
        verify(salaryRepository).save(draftSalary);
        verify(notificationService, never()).pushSystemAsync(any(), any(), any(), any(), any(), any(), any());
        verify(activityLogService).logAsync(eq("tenant1"), eq("testuser"), isNull(),
                eq(ActivityAction.SALARY_APPROVED), eq("SALARY"), eq("1"), anyString(), isNull());
    }

    @Test
    @DisplayName("approve: null request — treated as no notification")
    void approve_nullRequest_noNotification() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        SalaryDTO result = salaryService.approve(1L, null);

        assertThat(result).isNotNull();
        verify(notificationService, never()).pushSystemAsync(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("approve: with sendNotification=true — pushes notification to employee's user")
    void approve_withNotification_pushesToUser() {
        employee.setUserId(10L);
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("employee_user");

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        ApproveSalaryRequest req = new ApproveSalaryRequest();
        req.setSendNotification(true);

        salaryService.approve(1L, req);

        verify(notificationService).pushSystemAsync(eq("employee_user"),
                eq(Notification.NotificationType.SYSTEM),
                anyString(), anyString(), eq("SALARY"), eq(1L), eq("tenant1"));
    }

    @Test
    @DisplayName("approve: salary not DRAFT → BadRequestException")
    void approve_notDraft_throws() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(messageService.getMessage("error.salary.cannot.approve")).thenReturn("Cannot approve");

        assertThatThrownBy(() -> salaryService.approve(1L, null))
                .isInstanceOf(BadRequestException.class);
    }

    // ── markPaid ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markPaid: APPROVED → PAID, marks commission items")
    void markPaid_success() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(salaryRepository.save(any(Salary.class))).thenReturn(approved);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        PaySalaryRequest req = new PaySalaryRequest();
        req.setSendNotification(false);

        SalaryDTO result = salaryService.markPaid(1L, req);

        assertThat(result).isNotNull();
        verify(orderItemRepository).markSalaryCalculated(1L);
        verify(activityLogService).logAsync(eq("tenant1"), eq("testuser"), isNull(),
                eq(ActivityAction.SALARY_PAID), eq("SALARY"), eq("1"), anyString(), isNull());
    }

    @Test
    @DisplayName("markPaid: salary not APPROVED → BadRequestException")
    void markPaid_notApproved_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(messageService.getMessage("error.salary.cannot.pay")).thenReturn("Cannot pay");

        assertThatThrownBy(() -> salaryService.markPaid(1L, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("markPaid: with notification — pushes to employee's user")
    void markPaid_withNotification_pushesToUser() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        employee.setUserId(10L);
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("employee_user");

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(salaryRepository.save(any(Salary.class))).thenReturn(approved);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        PaySalaryRequest req = new PaySalaryRequest();
        req.setSendNotification(true);

        salaryService.markPaid(1L, req);

        verify(notificationService).pushSystemAsync(eq("employee_user"),
                eq(Notification.NotificationType.SYSTEM),
                anyString(), anyString(), eq("SALARY"), eq(1L), eq("tenant1"));
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: DRAFT salary — unlinks everything and deletes")
    void delete_success() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        salaryService.delete(1L);

        verify(orderItemRepository).unlinkFromSalary(1L);
        verify(advanceRepository).unlinkFromSalary(1L);
        verify(salaryRepository).delete(draftSalary);
    }

    @Test
    @DisplayName("delete: DRAFT salary with adjustments — deletes each adjustment")
    void delete_withAdjustments_deletesAll() {
        SalaryAdjustment adj = SalaryAdjustment.builder()
                .id(5L).salaryId(1L).type("BONUS").amount(BigDecimal.valueOf(100_000)).build();
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(adj));

        salaryService.delete(1L);

        verify(adjustmentRepository).delete(adj);
        verify(salaryRepository).delete(draftSalary);
    }

    @Test
    @DisplayName("delete: not DRAFT → BadRequestException")
    void delete_notDraft_throws() {
        Salary paid = buildSalary(SalaryStatus.PAID);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(paid));
        when(messageService.getMessage("error.salary.cannot.delete")).thenReturn("Cannot delete");

        assertThatThrownBy(() -> salaryService.delete(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("delete: salary not found → ResourceNotFoundException")
    void delete_notFound_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> salaryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addAdjustment ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addAdjustment: BONUS — saves adjustment and recalculates total")
    void addAdjustment_bonus_success() {
        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("BONUS");
        req.setAmount(BigDecimal.valueOf(200_000));
        req.setNote("KPI thưởng");

        SalaryAdjustment saved = SalaryAdjustment.builder()
                .id(1L).type("BONUS").amount(BigDecimal.valueOf(200_000)).salaryId(1L).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(adjustmentRepository.save(any(SalaryAdjustment.class))).thenReturn(saved);
        when(adjustmentRepository.sumByType(1L, "BONUS")).thenReturn(BigDecimal.valueOf(200_000));
        when(adjustmentRepository.sumByType(1L, "DEDUCTION")).thenReturn(null);
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);

        SalaryAdjustmentDTO result = salaryService.addAdjustment(1L, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("BONUS");
        verify(adjustmentRepository).save(any(SalaryAdjustment.class));
        verify(salaryRepository).save(draftSalary);
    }

    @Test
    @DisplayName("addAdjustment: DEDUCTION — saves correctly")
    void addAdjustment_deduction_success() {
        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("DEDUCTION");
        req.setAmount(BigDecimal.valueOf(100_000));

        SalaryAdjustment saved = SalaryAdjustment.builder()
                .id(2L).type("DEDUCTION").amount(BigDecimal.valueOf(100_000)).salaryId(1L).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(adjustmentRepository.save(any(SalaryAdjustment.class))).thenReturn(saved);
        when(adjustmentRepository.sumByType(1L, "BONUS")).thenReturn(null);
        when(adjustmentRepository.sumByType(1L, "DEDUCTION")).thenReturn(BigDecimal.valueOf(100_000));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);

        SalaryAdjustmentDTO result = salaryService.addAdjustment(1L, req);

        assertThat(result.getType()).isEqualTo("DEDUCTION");
    }

    @Test
    @DisplayName("addAdjustment: salary not DRAFT → BadRequestException")
    void addAdjustment_notDraft_throws() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(messageService.getMessage("error.salary.cannot.adjust")).thenReturn("Cannot adjust");

        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("BONUS");
        req.setAmount(BigDecimal.valueOf(100_000));

        assertThatThrownBy(() -> salaryService.addAdjustment(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addAdjustment: null amount → BadRequestException")
    void addAdjustment_nullAmount_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(messageService.getMessage("error.adjustment.amount.invalid")).thenReturn("Invalid amount");

        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("BONUS");
        req.setAmount(null);

        assertThatThrownBy(() -> salaryService.addAdjustment(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addAdjustment: zero amount → BadRequestException")
    void addAdjustment_zeroAmount_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(messageService.getMessage("error.adjustment.amount.invalid")).thenReturn("Invalid amount");

        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("BONUS");
        req.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> salaryService.addAdjustment(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addAdjustment: invalid type → BadRequestException")
    void addAdjustment_invalidType_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(messageService.getMessage("error.adjustment.type.invalid")).thenReturn("Invalid type");

        SalaryAdjustmentRequest req = new SalaryAdjustmentRequest();
        req.setType("INVALID");
        req.setAmount(BigDecimal.valueOf(100_000));

        assertThatThrownBy(() -> salaryService.addAdjustment(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── removeAdjustment ───────────────────────────────────────────────────────

    @Test
    @DisplayName("removeAdjustment: success — deletes adjustment and recalculates total")
    void removeAdjustment_success() {
        SalaryAdjustment adj = SalaryAdjustment.builder()
                .id(5L).salaryId(1L).type("BONUS").amount(BigDecimal.valueOf(100_000)).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(adjustmentRepository.findByIdAndSalaryId(5L, 1L)).thenReturn(Optional.of(adj));
        when(adjustmentRepository.sumByType(1L, "BONUS")).thenReturn(null);
        when(adjustmentRepository.sumByType(1L, "DEDUCTION")).thenReturn(null);
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);

        salaryService.removeAdjustment(1L, 5L);

        verify(adjustmentRepository).deleteById(5L);
        verify(salaryRepository).save(draftSalary);
    }

    @Test
    @DisplayName("removeAdjustment: salary not DRAFT → BadRequestException")
    void removeAdjustment_notDraft_throws() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(messageService.getMessage("error.salary.cannot.adjust")).thenReturn("Cannot adjust");

        assertThatThrownBy(() -> salaryService.removeAdjustment(1L, 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("removeAdjustment: adjustment not found → ResourceNotFoundException")
    void removeAdjustment_adjNotFound_throws() {
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(adjustmentRepository.findByIdAndSalaryId(99L, 1L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> salaryService.removeAdjustment(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSalaryDetail with commission items ──────────────────────────────────

    @Test
    @DisplayName("getSalaryDetail: maps commission row columns via helper converters")
    void getSalaryDetail_withCommissionRows_mappedCorrectly() {
        Object[] row = new Object[]{10L, "ORD-001", "Sản phẩm A", 2, new java.math.BigDecimal("100000"),
                new java.math.BigDecimal("0.05"), new java.math.BigDecimal("5000"), java.time.LocalDateTime.now()};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row);

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(orderItemRepository.findCommissionItemsBySalaryId(1L)).thenReturn(rows);
        when(adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        SalaryDTO result = salaryService.getSalaryDetail(1L);

        assertThat(result.getCommissionItems()).hasSize(1);
        assertThat(result.getCommissionItems().get(0).getOrderNumber()).isEqualTo("ORD-001");
        assertThat(result.getCommissionItems().get(0).getProductName()).isEqualTo("Sản phẩm A");
        assertThat(result.getCommissionItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("getSalaryDetail: commission row with null completedAt — maps to null")
    void getSalaryDetail_nullCompletedAt_mapsToNull() {
        Object[] row = new Object[]{5L, "ORD-002", "Sản phẩm B", 1,
                new java.math.BigDecimal("50000"), new java.math.BigDecimal("0.10"),
                new java.math.BigDecimal("5000"), null};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row);

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(orderItemRepository.findCommissionItemsBySalaryId(1L)).thenReturn(rows);
        when(adjustmentRepository.findBySalaryIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        SalaryDTO result = salaryService.getSalaryDetail(1L);

        assertThat(result.getCommissionItems().get(0).getCompletedAt()).isNull();
    }

    // ── generatePayroll with null baseWage ─────────────────────────────────────

    @Test
    @DisplayName("generatePayroll: employee with null baseWage treated as zero")
    void generatePayroll_nullBaseWage_treatedAsZero() {
        Employee empNullWage = Employee.builder().id(2L).fullName("Trần Thị B").baseWage(null).build();

        GenerateSalaryRequest req = new GenerateSalaryRequest();
        req.setMonth(5);
        req.setYear(2026);

        Salary zeroBySalary = Salary.builder()
                .id(2L).employeeId(2L).employeeName("Trần Thị B")
                .month(5).year(2026)
                .baseWage(BigDecimal.ZERO).totalCommission(BigDecimal.ZERO)
                .advanceAmount(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .status(SalaryStatus.DRAFT).build();

        when(employeeRepository.findAllActive()).thenReturn(List.of(empNullWage));
        when(salaryRepository.existsByEmployeeIdAndMonthAndYear(2L, 5, 2026)).thenReturn(false);
        when(orderItemRepository.sumPendingCommissionByEmployeeAndMonth(2L, 5, 2026)).thenReturn(null);
        when(advanceRepository.sumPendingByEmployeeAndMonth(2L, 5, 2026)).thenReturn(null);
        when(salaryRepository.save(any(Salary.class))).thenReturn(zeroBySalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        List<SalaryDTO> result = salaryService.generatePayroll(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseWage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── approve notification with userId == null ───────────────────────────────

    @Test
    @DisplayName("approve: notification enabled but employee has no userId — notification skipped")
    void approve_withNotification_employeeNoUserId_skipsNotification() {
        Employee empNoUser = Employee.builder().id(1L).fullName("Nguyễn Văn An").userId(null).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(draftSalary));
        when(salaryRepository.save(any(Salary.class))).thenReturn(draftSalary);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(empNoUser));

        ApproveSalaryRequest req = new ApproveSalaryRequest();
        req.setSendNotification(true);

        salaryService.approve(1L, req);

        verify(notificationService, never()).pushSystemAsync(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("markPaid: with null request — treated as no notification")
    void markPaid_nullRequest_noNotification() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(salaryRepository.save(any(Salary.class))).thenReturn(approved);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        SalaryDTO result = salaryService.markPaid(1L, null);

        assertThat(result).isNotNull();
        verify(notificationService, never()).pushSystemAsync(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("markPaid: notification enabled but employee has no userId — notification skipped")
    void markPaid_withNotification_employeeNoUserId_skipsNotification() {
        Salary approved = buildSalary(SalaryStatus.APPROVED);
        Employee empNoUser = Employee.builder().id(1L).fullName("Nguyễn Văn An").userId(null).build();

        when(salaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(approved));
        when(salaryRepository.save(any(Salary.class))).thenReturn(approved);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(empNoUser));

        PaySalaryRequest req = new PaySalaryRequest();
        req.setSendNotification(true);

        salaryService.markPaid(1L, req);

        verify(notificationService, never()).pushSystemAsync(any(), any(), any(), any(), any(), any(), any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Salary buildSalary(SalaryStatus status) {
        return Salary.builder()
                .id(1L)
                .employeeId(1L)
                .employeeName("Nguyễn Văn An")
                .month(5)
                .year(2026)
                .baseWage(BigDecimal.valueOf(5_000_000))
                .totalCommission(BigDecimal.ZERO)
                .advanceAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(5_000_000))
                .status(status)
                .build();
    }
}
