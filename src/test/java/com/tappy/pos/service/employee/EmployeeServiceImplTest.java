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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl Unit Tests")
class EmployeeServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;
    @Mock private R2StorageService r2StorageService;
    @Mock private R2CleanupService r2CleanupService;

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

    // ── create: position null path ────────────────────────────────────────────

    @Test
    @DisplayName("create: null position stays null on saved employee")
    void create_nullPosition() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("No Position");
        req.setPhone("0900000123");
        req.setPosition(null);
        when(employeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EmployeeDTO dto = employeeService.create(req);

        assertThat(dto.getPosition()).isNull();
    }

    // ── uploadAvatar ──────────────────────────────────────────────────────────

    private MultipartFile validImageFile() throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return new MockMultipartFile("file", "a.png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("uploadAvatar: rejects unsupported content type")
    void uploadAvatar_invalidContentType() {
        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> employeeService.uploadAvatar(1L, file))
                .isInstanceOf(BadRequestException.class);
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("uploadAvatar: null content type rejected")
    void uploadAvatar_nullContentType() {
        MultipartFile file = new MockMultipartFile("file", "a", null, new byte[]{1});

        assertThatThrownBy(() -> employeeService.uploadAvatar(1L, file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("uploadAvatar: employee not found throws ResourceNotFoundException")
    void uploadAvatar_notFound() throws IOException {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        MultipartFile file = validImageFile();

        assertThatThrownBy(() -> employeeService.uploadAvatar(99L, file))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("uploadAvatar: compresses, stores, saves, cleans old key")
    void uploadAvatar_success() throws IOException {
        employee.setAvatarUrl("https://cdn/old.jpg");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(r2StorageService.keyFromUrl("https://cdn/old.jpg")).thenReturn("employees/shop1/1.jpg");
        when(r2StorageService.upload(anyString(), any(byte[].class), eq("image/jpeg")))
                .thenReturn("https://cdn/employees/shop1/1.jpg");

        EmployeeDTO dto = employeeService.uploadAvatar(1L, validImageFile());

        assertThat(dto.getAvatarUrl()).startsWith("https://cdn/employees/shop1/1.jpg?v=");
        verify(r2CleanupService).deleteAsync("employees/shop1/1.jpg");
        verify(r2StorageService).upload(eq("employees/shop1/1.jpg"), any(byte[].class), eq("image/jpeg"));
    }

    @Test
    @DisplayName("uploadAvatar: blank upload url stored as null avatar")
    void uploadAvatar_blankUrl() throws IOException {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(r2StorageService.upload(anyString(), any(byte[].class), eq("image/jpeg"))).thenReturn("");

        EmployeeDTO dto = employeeService.uploadAvatar(1L, validImageFile());

        assertThat(dto.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("uploadAvatar: corrupt image bytes throw BadRequestException")
    void uploadAvatar_processFailure() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        MultipartFile bad = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{0, 1, 2, 3});

        assertThatThrownBy(() -> employeeService.uploadAvatar(1L, bad))
                .isInstanceOf(BadRequestException.class);
    }

    // ── deleteAvatar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAvatar: clears url, saves, cleans old key")
    void deleteAvatar_success() {
        employee.setAvatarUrl("https://cdn/old.jpg");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
        when(r2StorageService.keyFromUrl("https://cdn/old.jpg")).thenReturn("employees/shop1/1.jpg");

        EmployeeDTO dto = employeeService.deleteAvatar(1L);

        assertThat(dto.getAvatarUrl()).isNull();
        verify(r2CleanupService).deleteAsync("employees/shop1/1.jpg");
    }

    @Test
    @DisplayName("deleteAvatar: employee not found throws ResourceNotFoundException")
    void deleteAvatar_notFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteAvatar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAnalytics ──────────────────────────────────────────────────────────

    private void stubAnalyticsCommon() {
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59);
        when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(new BigDecimal("1000"));
        when(orderItemRepository.sumTeamCommissionByDateRange(any(), any())).thenReturn(new BigDecimal("100"));
        when(orderRepository.countActiveEmployees(any(), any())).thenReturn(2L);
        when(orderRepository.getEmployeeRevenueRankingByDateRange(any(), any(), anyInt()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"Emp A", "11", 3L, new BigDecimal("600")}));
        when(orderItemRepository.getEmployeeCommissionRankingByDateRange(any(), any(), anyInt()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"21", "Emp A", new BigDecimal("60"), 3L, new BigDecimal("600")}));
    }

    @Test
    @DisplayName("getAnalytics: day granularity merges revenue + commission trend by label")
    void getAnalytics_dayGranularity() {
        stubAnalyticsCommon();
        when(orderRepository.getEmployeeRevenueTrendByDay(any(), any()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"2025-01-01", new BigDecimal("600")}));
        when(orderItemRepository.getTeamCommissionTrendByDay(any(), any()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"2025-01-01", new BigDecimal("60")}));

        Map<String, Object> result = employeeService.getAnalytics(
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59), "day", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat((double) summary.get("totalRevenue")).isEqualTo(1000.0);
        assertThat((double) summary.get("totalCommission")).isEqualTo(100.0);
        assertThat(summary.get("activeEmployeeCount")).isEqualTo(2L);
        assertThat((double) summary.get("avgRevenuePerEmployee")).isEqualTo(500.0);

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> trend =
                (java.util.List<Map<String, Object>>) result.get("trend");
        assertThat(trend).hasSize(1);
        assertThat(trend.getFirst().get("label")).isEqualTo("2025-01-01");
        assertThat((double) trend.getFirst().get("revenue")).isEqualTo(600.0);
        assertThat((double) trend.getFirst().get("commission")).isEqualTo(60.0);
    }

    @Test
    @DisplayName("getAnalytics: week granularity uses week trend queries")
    void getAnalytics_weekGranularity() {
        stubAnalyticsCommon();
        when(orderRepository.getEmployeeRevenueTrendByWeek(any(), any()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"2025-W01", new BigDecimal("700")}));
        when(orderItemRepository.getTeamCommissionTrendByWeek(any(), any()))
                .thenReturn(java.util.List.of());

        Map<String, Object> result = employeeService.getAnalytics(
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59), "week", 5);

        verify(orderRepository).getEmployeeRevenueTrendByWeek(any(), any());
        verify(orderItemRepository).getTeamCommissionTrendByWeek(any(), any());
        assertThat(result).containsKey("rankingRevenue");
    }

    @Test
    @DisplayName("getAnalytics: month granularity uses month trend queries")
    void getAnalytics_monthGranularity() {
        stubAnalyticsCommon();
        when(orderRepository.getEmployeeRevenueTrendByMonth(any(), any()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"2025-01", new BigDecimal("800")}));
        when(orderItemRepository.getTeamCommissionTrendByMonth(any(), any()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"2025-01", new BigDecimal("80")}));

        employeeService.getAnalytics(
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59), "month", 5);

        verify(orderRepository).getEmployeeRevenueTrendByMonth(any(), any());
        verify(orderItemRepository).getTeamCommissionTrendByMonth(any(), any());
    }

    @Test
    @DisplayName("getAnalytics: null revenue/commission default to zero; zero active -> zero avg")
    void getAnalytics_nullsAndZeroActive() {
        when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(null);
        when(orderItemRepository.sumTeamCommissionByDateRange(any(), any())).thenReturn(null);
        when(orderRepository.countActiveEmployees(any(), any())).thenReturn(0L);
        when(orderRepository.getEmployeeRevenueRankingByDateRange(any(), any(), anyInt()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{null, null, null, null}));
        when(orderItemRepository.getEmployeeCommissionRankingByDateRange(any(), any(), anyInt()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{null, null, null, null, null}));
        when(orderRepository.getEmployeeRevenueTrendByDay(any(), any())).thenReturn(java.util.List.<Object[]>of(
                new Object[]{null, null}));
        when(orderItemRepository.getTeamCommissionTrendByDay(any(), any())).thenReturn(java.util.List.of());

        Map<String, Object> result = employeeService.getAnalytics(
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59), "day", 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat((double) summary.get("totalRevenue")).isZero();
        assertThat((double) summary.get("avgRevenuePerEmployee")).isZero();

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> rankRevenue =
                (java.util.List<Map<String, Object>>) result.get("rankingRevenue");
        assertThat(rankRevenue.getFirst().get("employeeName")).isEqualTo("");
        assertThat(rankRevenue.getFirst().get("userId")).isNull();
    }
}
