package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.CommissionReportDTO;
import com.tappy.pos.model.dto.employee.MyCommissionDTO;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionServiceImpl Unit Tests")
class CommissionServiceImplTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private CommissionServiceImpl service;

    private User user(Long id) {
        User u = User.builder().username("thoA").build();
        u.setId(id);
        return u;
    }

    private Employee employee(Long id) {
        Employee e = Employee.builder().fullName("Thợ A").build();
        e.setId(id);
        return e;
    }

    @Test
    @DisplayName("getMyCommission: aggregates total, count and detail items")
    void getMyCommission_success() {
        when(userRepository.findByUsernameTenantScoped("thoA")).thenReturn(Optional.of(user(7L)));
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.of(employee(99L)));
        when(orderItemRepository.sumAllCommissionByEmployeeAndMonth(99L, 6, 2026)).thenReturn(new BigDecimal("150000"));
        when(orderItemRepository.countCommissionItemsByEmployeeAndMonth(99L, 6, 2026)).thenReturn(3L);
        when(orderItemRepository.findCommissionDetailByEmployeeAndMonth(99L, 6, 2026))
                .thenReturn(List.<Object[]>of(new Object[]{
                        1L, "ORD-1", "Cắt tóc", 1, new BigDecimal("100000"),
                        new BigDecimal("10"), new BigDecimal("10000"), Timestamp.valueOf(LocalDateTime.now())}));

        MyCommissionDTO dto = service.getMyCommission("thoA", 6, 2026);

        assertThat(dto.getEmployeeId()).isEqualTo(99L);
        assertThat(dto.getEmployeeName()).isEqualTo("Thợ A");
        assertThat(dto.getTotalCommission()).isEqualByComparingTo("150000");
        assertThat(dto.getItemCount()).isEqualTo(3L);
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getMyCommission: null totals default to zero")
    void getMyCommission_nullTotals() {
        when(userRepository.findByUsernameTenantScoped("thoA")).thenReturn(Optional.of(user(7L)));
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.of(employee(99L)));
        when(orderItemRepository.sumAllCommissionByEmployeeAndMonth(99L, 6, 2026)).thenReturn(null);
        when(orderItemRepository.countCommissionItemsByEmployeeAndMonth(99L, 6, 2026)).thenReturn(null);
        when(orderItemRepository.findCommissionDetailByEmployeeAndMonth(99L, 6, 2026)).thenReturn(List.of());

        MyCommissionDTO dto = service.getMyCommission("thoA", 6, 2026);

        assertThat(dto.getTotalCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getItemCount()).isZero();
    }

    @Test
    @DisplayName("getMyCommission: user not found → ResourceNotFoundException")
    void getMyCommission_userNotFound() {
        when(userRepository.findByUsernameTenantScoped("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyCommission("ghost", 6, 2026))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMyCommission: employee not linked → ResourceNotFoundException")
    void getMyCommission_employeeNotLinked() {
        when(userRepository.findByUsernameTenantScoped("thoA")).thenReturn(Optional.of(user(7L)));
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyCommission("thoA", 6, 2026))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMyCommission: invalid month → BadRequestException")
    void getMyCommission_invalidMonth() {
        when(messageService.getMessage("error.salary.month.invalid")).thenReturn("tháng không hợp lệ");

        assertThatThrownBy(() -> service.getMyCommission("thoA", 13, 2026))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getCommissionReport: aggregates per-employee totals and grand total")
    void getCommissionReport_success() {
        when(orderItemRepository.findAllEmployeesCommissionSummaryByMonth(6, 2026))
                .thenReturn(List.<Object[]>of(
                        new Object[]{99L, "Thợ A", new BigDecimal("100000"), 2L},
                        new Object[]{100L, "Thợ B", new BigDecimal("50000"), 1L}));

        CommissionReportDTO report = service.getCommissionReport(6, 2026);

        assertThat(report.getEmployees()).hasSize(2);
        assertThat(report.getTotalCommission()).isEqualByComparingTo("150000");
        assertThat(report.getTotalItemCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getCommissionReport: invalid year → BadRequestException")
    void getCommissionReport_invalidYear() {
        when(messageService.getMessage("error.salary.year.invalid")).thenReturn("năm không hợp lệ");

        assertThatThrownBy(() -> service.getCommissionReport(6, 1999))
                .isInstanceOf(BadRequestException.class);
    }
}
