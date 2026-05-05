package com.knp.service.employee;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.employee.CreateEmployeeRequest;
import com.knp.model.dto.employee.EmployeeDTO;
import com.knp.model.dto.employee.UpdateEmployeeRequest;
import com.knp.model.entity.employee.Employee;
import com.knp.model.entity.auth.User;
import com.knp.model.enums.EmployeePosition;
import com.knp.multitenant.TenantContext;
import com.knp.repository.employee.EmployeeRepository;
import com.knp.repository.auth.UserRepository;
import com.knp.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import com.knp.service.audit.ActivityLogService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl Unit Tests")
class EmployeeServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

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

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns paged employee DTOs")
    void getAll_returnsPaged() {
        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 10), 1);
        when(employeeRepository.findAllWithSearch("", PageRequest.of(0, 10))).thenReturn(page);

        Page<EmployeeDTO> result = employeeService.getAll("", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Nguyen Van A");
    }

    // ── getAllActive ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllActive: returns list of all active employee DTOs")
    void getAllActive_returnsList() {
        when(employeeRepository.findAllActive()).thenReturn(List.of(employee));

        List<EmployeeDTO> result = employeeService.getAllActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).isEqualTo("0901234567");
    }

    // ── getByUserId ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByUserId: returns DTO for employee linked to userId")
    void getByUserId_success() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));

        EmployeeDTO dto = employeeService.getByUserId(5L);

        assertThat(dto.getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    @DisplayName("getByUserId: throws ResourceNotFoundException when no employee linked")
    void getByUserId_notFound() {
        when(employeeRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getByUserId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getById edge cases ────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: throws when employee is soft-deleted")
    void getById_softDeleted() {
        employee.softDelete();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update edge cases ─────────────────────────────────────────────────────

    @Test
    @DisplayName("update: throws when employee not found")
    void update_notFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(99L, new UpdateEmployeeRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: skips user re-link when same userId provided")
    void update_sameUserId_noChange() {
        employee.setUserId(5L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setUserId(5L);

        employeeService.update(1L, req);

        verify(employeeRepository, never()).existsByUserId(anyLong());
    }

    @Test
    @DisplayName("update: throws when new linked userId user does not exist")
    void update_newUserId_userNotFound() {
        employee.setUserId(3L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUserId(8L)).thenReturn(false);
        when(userRepository.findById(8L)).thenReturn(Optional.empty());

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setUserId(8L);

        assertThatThrownBy(() -> employeeService.update(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: applies invalid position update throws BadRequestException")
    void update_invalidPosition() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setPosition("INVALID_ROLE");

        assertThatThrownBy(() -> employeeService.update(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── create edge cases ─────────────────────────────────────────────────────

    @Test
    @DisplayName("create: throws ResourceNotFoundException when linked userId user does not exist")
    void create_withLinkedUser_userNotFound() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Test User");
        req.setPhone("0900000000");
        req.setPosition("RECEPTIONIST");
        req.setUserId(99L);

        when(employeeRepository.existsByUserId(99L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── toDTO: linked user lookup coverage ───────────────────────────────────

    @Test
    @DisplayName("getById: maps username from linked user")
    void getById_withLinkedUser_mapsUsername() {
        employee.setUserId(5L);
        User user = User.builder().username("linked_user").build();
        user.setId(5L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        EmployeeDTO dto = employeeService.getById(1L);

        assertThat(dto.getUserId()).isEqualTo(5L);
        assertThat(dto.getUsername()).isEqualTo("linked_user");
    }

    @Test
    @DisplayName("getById: returns null username when linked user not found")
    void getById_withLinkedUser_userMissing() {
        employee.setUserId(5L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        EmployeeDTO dto = employeeService.getById(1L);

        assertThat(dto.getUserId()).isEqualTo(5L);
        assertThat(dto.getUsername()).isNull();
    }
}
