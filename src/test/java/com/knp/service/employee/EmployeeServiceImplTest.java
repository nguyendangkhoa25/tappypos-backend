package com.knp.service.employee;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.employee.CreateEmployeeRequest;
import com.knp.model.dto.employee.EmployeeDTO;
import com.knp.model.dto.employee.UpdateEmployeeRequest;
import com.knp.model.entity.employee.Employee;
import com.knp.model.entity.auth.User;
import com.knp.model.enums.EmployeePosition;
import com.knp.repository.employee.EmployeeRepository;
import com.knp.repository.auth.UserRepository;
import com.knp.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl Unit Tests")
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .position(EmployeePosition.RECEPTIONIST)
                .active(true)
                .build();
        lenient().when(messageService.getMessage(anyString())).thenReturn("error message");
        lenient().when(messageService.getMessage(anyString(), any(Locale.class))).thenReturn("error message");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("error message");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns DTO for existing employee")
    void getById_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDTO dto = employeeService.getById(1L);

        assertThat(dto.getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    @DisplayName("getById: throws when not found")
    void getById_notFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves employee successfully without linked user")
    void create_noLinkedUser() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Tran Thi B");
        req.setPhone("0912345678");
        req.setPosition("RECEPTIONIST");

        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeDTO dto = employeeService.create(req);

        assertThat(dto.getFullName()).isEqualTo("Tran Thi B");
        verify(employeeRepository).save(any());
    }

    @Test
    @DisplayName("create: saves employee with linked user")
    void create_withLinkedUser() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Le Van C");
        req.setPhone("0923456789");
        req.setPosition("MANAGER");
        req.setUserId(5L);

        User user = User.builder().username("levanc").build();
        user.setId(5L);
        when(employeeRepository.existsByUserId(5L)).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeDTO dto = employeeService.create(req);

        assertThat(dto.getUserId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create: throws when user already linked to another employee")
    void create_userAlreadyLinked() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Test");
        req.setPhone("0900000000");
        req.setPosition("RECEPTIONIST");
        req.setUserId(5L);

        when(employeeRepository.existsByUserId(5L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: throws for invalid position string")
    void create_invalidPosition() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Test");
        req.setPhone("0900000001");
        req.setPosition("INVALID_ROLE");

        assertThatThrownBy(() -> employeeService.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: patches only provided fields")
    void update_partialUpdate() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setFullName("Updated Name");

        EmployeeDTO dto = employeeService.update(1L, req);

        assertThat(dto.getFullName()).isEqualTo("Updated Name");
        assertThat(dto.getPhone()).isEqualTo("0901234567"); // unchanged
    }

    @Test
    @DisplayName("update: throws when re-linking to already-linked user")
    void update_userAlreadyLinked() {
        employee.setUserId(3L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUserId(7L)).thenReturn(true);

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setUserId(7L);

        assertThatThrownBy(() -> employeeService.update(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes employee")
    void delete_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenReturn(employee);

        employeeService.delete(1L);

        assertThat(employee.isDeleted()).isTrue();
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("delete: throws when not found")
    void delete_notFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
