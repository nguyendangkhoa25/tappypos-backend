package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.CreateAdvanceRequest;
import com.tappy.pos.model.dto.employee.SalaryAdvanceDTO;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.employee.SalaryAdvance;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.employee.SalaryAdvanceRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAdvanceService Unit Tests")
class SalaryAdvanceServiceImplTest {

    @Mock private SalaryAdvanceRepository advanceRepository;
    @Mock private EmployeeRepository      employeeRepository;
    @Mock private MessageService          messageService;
    @Mock private TenantContext           tenantContext;
    @Mock private ActivityLogService      activityLogService;

    @InjectMocks
    private SalaryAdvanceServiceImpl salaryAdvanceService;

    private Employee employee;
    private SalaryAdvance advance;

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
                .build();

        advance = SalaryAdvance.builder()
                .id(1L)
                .employeeId(1L)
                .employeeName("Nguyễn Văn An")
                .amount(BigDecimal.valueOf(500_000))
                .advanceDate(LocalDate.now())
                .deducted(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createAdvance ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAdvance: success — returns DTO with correct fields")
    void createAdvance_success() {
        CreateAdvanceRequest req = new CreateAdvanceRequest();
        req.setEmployeeId(1L);
        req.setAmount(BigDecimal.valueOf(500_000));
        req.setAdvanceDate(LocalDate.now());
        req.setNote("Ứng lương tháng 5");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(advanceRepository.save(any(SalaryAdvance.class))).thenReturn(advance);

        SalaryAdvanceDTO result = salaryAdvanceService.createAdvance(req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        verify(advanceRepository).save(any(SalaryAdvance.class));
        verify(activityLogService).logAsync(eq("tenant1"), eq("testuser"), isNull(),
                eq(ActivityAction.SALARY_ADVANCE_CREATED), eq("SALARY_ADVANCE"),
                anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("createAdvance: null amount → BadRequestException")
    void createAdvance_nullAmount_throws() {
        CreateAdvanceRequest req = new CreateAdvanceRequest();
        req.setEmployeeId(1L);
        req.setAmount(null);
        req.setAdvanceDate(LocalDate.now());

        when(messageService.getMessage("error.advance.amount.invalid")).thenReturn("Amount invalid");

        assertThatThrownBy(() -> salaryAdvanceService.createAdvance(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createAdvance: zero amount → BadRequestException")
    void createAdvance_zeroAmount_throws() {
        CreateAdvanceRequest req = new CreateAdvanceRequest();
        req.setEmployeeId(1L);
        req.setAmount(BigDecimal.ZERO);
        req.setAdvanceDate(LocalDate.now());

        when(messageService.getMessage("error.advance.amount.invalid")).thenReturn("Amount invalid");

        assertThatThrownBy(() -> salaryAdvanceService.createAdvance(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createAdvance: null advanceDate → BadRequestException")
    void createAdvance_nullDate_throws() {
        CreateAdvanceRequest req = new CreateAdvanceRequest();
        req.setEmployeeId(1L);
        req.setAmount(BigDecimal.valueOf(500_000));
        req.setAdvanceDate(null);

        when(messageService.getMessage("error.advance.date.required")).thenReturn("Date required");

        assertThatThrownBy(() -> salaryAdvanceService.createAdvance(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createAdvance: employee not found → ResourceNotFoundException")
    void createAdvance_employeeNotFound_throws() {
        CreateAdvanceRequest req = new CreateAdvanceRequest();
        req.setEmployeeId(99L);
        req.setAmount(BigDecimal.valueOf(500_000));
        req.setAdvanceDate(LocalDate.now());

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("employee.not.found")).thenReturn("Not found");

        assertThatThrownBy(() -> salaryAdvanceService.createAdvance(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAdvances ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAdvances: returns page of DTOs")
    void getAdvances_success() {
        Page<SalaryAdvance> page = new PageImpl<>(List.of(advance));
        when(advanceRepository.findFiltered(eq(1L), any(PageRequest.class))).thenReturn(page);

        Page<SalaryAdvanceDTO> result = salaryAdvanceService.getAdvances(1L, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getAdvances: no employeeId filter — returns all page")
    void getAdvances_noFilter_success() {
        Page<SalaryAdvance> page = new PageImpl<>(List.of(advance));
        when(advanceRepository.findFiltered(isNull(), any(PageRequest.class))).thenReturn(page);

        Page<SalaryAdvanceDTO> result = salaryAdvanceService.getAdvances(null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── deleteAdvance ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAdvance: success — deletes non-deducted advance")
    void deleteAdvance_success() {
        when(advanceRepository.findById(1L)).thenReturn(Optional.of(advance));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");

        salaryAdvanceService.deleteAdvance(1L);

        verify(advanceRepository).delete(advance);
        verify(activityLogService).logAsync(eq("tenant1"), eq("testuser"), isNull(),
                eq(ActivityAction.SALARY_ADVANCE_DELETED), eq("SALARY_ADVANCE"),
                anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("deleteAdvance: not found → ResourceNotFoundException")
    void deleteAdvance_notFound_throws() {
        when(advanceRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> salaryAdvanceService.deleteAdvance(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteAdvance: already deducted → BadRequestException")
    void deleteAdvance_alreadyDeducted_throws() {
        advance = SalaryAdvance.builder()
                .id(1L)
                .employeeId(1L)
                .employeeName("Nguyễn Văn An")
                .amount(BigDecimal.valueOf(500_000))
                .deducted(true)
                .build();
        when(advanceRepository.findById(1L)).thenReturn(Optional.of(advance));
        when(messageService.getMessage("error.advance.already.deducted")).thenReturn("Already deducted");

        assertThatThrownBy(() -> salaryAdvanceService.deleteAdvance(1L))
                .isInstanceOf(BadRequestException.class);
    }
}
