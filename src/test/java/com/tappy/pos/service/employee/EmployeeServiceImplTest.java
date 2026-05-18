package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.enums.EmployeePosition;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.MessageService;
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
import java.time.LocalDate;
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
import com.tappy.pos.service.audit.ActivityLogService;

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

    @Test
    @DisplayName("toDTO: returns null username when user lookup throws exception")
    void toDTO_userLookupThrows_returnsNullUsername() {
        employee.setUserId(99L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(99L)).thenThrow(new RuntimeException("DB error"));

        EmployeeDTO dto = employeeService.getById(1L);

        assertThat(dto.getUsername()).isNull();
    }

    @Test
    @DisplayName("create: null active defaults to true")
    void create_nullActive_defaultsToTrue() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFullName("Trần Thị B");
        request.setPosition("RECEPTIONIST");
        request.setActive(null);

        Employee saved = Employee.builder().id(2L).fullName("Trần Thị B")
                .position(EmployeePosition.RECEPTIONIST).active(true).build();

        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);
        when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");

        org.mockito.ArgumentCaptor<Employee> captor = org.mockito.ArgumentCaptor.forClass(Employee.class);

        employeeService.create(request);

        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    @DisplayName("update: sets all basic fields when provided")
    void update_setsAllBasicFields() {
        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setPhone("0900000099");
        req.setEmail("test@example.com");
        req.setDepartment("Sales");
        req.setActive(false);
        req.setBaseWage(java.math.BigDecimal.valueOf(5000000));
        req.setCommissionRate(java.math.BigDecimal.valueOf(0.1));
        req.setNotes("Some notes");
        req.setAvatar("avatar.jpg");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");

        EmployeeDTO dto = employeeService.update(1L, req);

        verify(employeeRepository).save(argThat(e ->
                "0900000099".equals(e.getPhone()) &&
                "test@example.com".equals(e.getEmail()) &&
                "Sales".equals(e.getDepartment()) &&
                Boolean.FALSE.equals(e.getActive())));
        assertThat(dto.getPhone()).isEqualTo("0900000099");
    }

    @Test
    @DisplayName("update: sets all extended ID card fields when provided")
    void update_setsExtendedFields() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setIdCardNumber("123456789");
        request.setGender("MALE");
        request.setPermanentAddress("123 Đường ABC");
        request.setIdCardFrontImage("front.jpg");
        request.setIdCardBackImage("back.jpg");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");

        employeeService.update(1L, request);

        verify(employeeRepository).save(argThat(e ->
                "123456789".equals(e.getIdCardNumber()) &&
                "MALE".equals(e.getGender()) &&
                "123 Đường ABC".equals(e.getPermanentAddress())));
    }

    @Test
    @DisplayName("update: sets valid position, hireDate, dateOfBirth, idCardIssuedDate, idCardIssuedPlace")
    void update_setsDatesAndPosition() {
        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setPosition("MANAGER");
        req.setHireDate(LocalDate.of(2022, 1, 15));
        req.setDateOfBirth(LocalDate.of(1990, 5, 20));
        req.setIdCardIssuedDate(LocalDate.of(2018, 3, 10));
        req.setIdCardIssuedPlace("Hà Nội");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");

        employeeService.update(1L, req);

        verify(employeeRepository).save(argThat(e ->
                EmployeePosition.MANAGER.equals(e.getPosition()) &&
                LocalDate.of(2022, 1, 15).equals(e.getHireDate()) &&
                LocalDate.of(1990, 5, 20).equals(e.getDateOfBirth()) &&
                LocalDate.of(2018, 3, 10).equals(e.getIdCardIssuedDate()) &&
                "Hà Nội".equals(e.getIdCardIssuedPlace())));
    }

    @Test
    @DisplayName("update: links new userId when user is found successfully")
    void update_newUserId_userFound_linksSuccessfully() {
        employee.setUserId(3L);
        User user = User.builder().build();
        user.setId(9L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUserId(9L)).thenReturn(false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setUserId(9L);
        employeeService.update(1L, req);

        verify(employeeRepository).save(argThat(e -> Long.valueOf(9L).equals(e.getUserId())));
    }
}
