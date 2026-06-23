package com.tappy.pos.service.repair;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.repair.AssignTechnicianRequest;
import com.tappy.pos.model.dto.repair.CreateRepairTicketRequest;
import com.tappy.pos.model.dto.repair.RepairPartRequest;
import com.tappy.pos.model.dto.repair.RepairTicketDTO;
import com.tappy.pos.model.dto.repair.UpdateRepairStatusRequest;
import com.tappy.pos.model.dto.repair.UpdateRepairTicketRequest;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.repair.RepairPart;
import com.tappy.pos.model.entity.repair.RepairTicket;
import com.tappy.pos.model.enums.RepairStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.repair.RepairTicketRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepairTicketServiceImpl Unit Tests")
class RepairTicketServiceImplTest {

    private static final String TENANT = "test-tenant";
    private static final String USER = "user1";

    @Mock private RepairTicketRepository repairTicketRepository;
    @Mock private TenantContext tenantContext;
    @Mock private FeatureContext featureContext;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;
    @Mock private OrderRepository orderRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private RepairTicketServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(anyString())).thenReturn("msg");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        lenient().when(featureContext.hasFeature("REPAIR_VIEW_ALL")).thenReturn(true);

        // SecurityContextHolder provides currentUsername()
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(USER, "pw");
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private RepairTicket ticket(Long id, String status, String createdBy) {
        RepairTicket t = RepairTicket.builder()
                .ticketNumber("SC-20260101-001")
                .customerName("Nguyen Van A")
                .customerPhone("0900000000")
                .reportedFault("Broken screen")
                .status(status)
                .createdBy(createdBy)
                .warrantyDays(0)
                .laborAmount(BigDecimal.ZERO)
                .quoteAmount(BigDecimal.ZERO)
                .parts(new ArrayList<>())
                .build();
        t.setId(id);
        t.setTenantId(TENANT);
        return t;
    }

    private RepairPartRequest partRequest(String name, Integer qty, BigDecimal unit) {
        RepairPartRequest p = new RepairPartRequest();
        p.setProductName(name);
        p.setQuantity(qty);
        p.setUnitPrice(unit);
        p.setProductId(7L);
        return p;
    }

    private void stubSaveEcho() {
        when(repairTicketRepository.save(any(RepairTicket.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    // ── search ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: with REPAIR_VIEW_ALL uses search() and trims status/keyword")
    void search_viewAll() {
        Page<RepairTicket> page = new PageImpl<>(List.of(ticket(1L, "RECEIVED", USER)));
        when(repairTicketRepository.search(eq(TENANT), eq("RECEIVED"), eq("abc"), any(Pageable.class)))
                .thenReturn(page);

        Page<RepairTicketDTO> result = service.search("  RECEIVED  ", "  abc  ", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(repairTicketRepository).search(eq(TENANT), eq("RECEIVED"), eq("abc"), any(Pageable.class));
    }

    @Test
    @DisplayName("search: without REPAIR_VIEW_ALL uses searchByCreatedBy and blank inputs normalize to null")
    void search_ownOnly_blankInputs() {
        when(featureContext.hasFeature("REPAIR_VIEW_ALL")).thenReturn(false);
        Page<RepairTicket> page = new PageImpl<>(List.of(ticket(1L, "RECEIVED", USER)));
        when(repairTicketRepository.searchByCreatedBy(eq(TENANT), eq(null), eq(null), eq(USER), any(Pageable.class)))
                .thenReturn(page);

        Page<RepairTicketDTO> result = service.search("   ", "", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(repairTicketRepository).searchByCreatedBy(eq(TENANT), eq(null), eq(null), eq(USER), any(Pageable.class));
    }

    @Test
    @DisplayName("search: null status/keyword normalize to null")
    void search_nullInputs() {
        Page<RepairTicket> page = new PageImpl<>(List.of());
        when(repairTicketRepository.search(eq(TENANT), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        Page<RepairTicketDTO> result = service.search(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ── getById ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns DTO when found")
    void getById_found() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));

        RepairTicketDTO dto = service.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStatus()).isEqualTo("RECEIVED");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when missing")
    void getById_notFound() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(99L, TENANT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: without REPAIR_VIEW_ALL and not owner is treated as not-found")
    void getById_notOwner_notFound() {
        when(featureContext.hasFeature("REPAIR_VIEW_ALL")).thenReturn(false);
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", "someone-else")));

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: without REPAIR_VIEW_ALL but owner succeeds")
    void getById_ownerWithoutViewAll() {
        when(featureContext.hasFeature("REPAIR_VIEW_ALL")).thenReturn(false);
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));

        RepairTicketDTO dto = service.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
    }

    // ── create ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: builds ticket, applies parts, computes amounts and logs")
    void create_success() {
        when(repairTicketRepository.countTodayByTenantId(eq(TENANT), any()))
                .thenReturn(5L);
        stubSaveEcho();

        CreateRepairTicketRequest req = new CreateRepairTicketRequest();
        req.setCustomerId(3L);
        req.setCustomerName("  Tran B  ");
        req.setCustomerPhone("0911222333");
        req.setDeviceType("Phone");
        req.setBrand("Apple");
        req.setModel("iPhone 12");
        req.setSerialImei("IMEI123");
        req.setReportedFault("  no power  ");
        req.setDiagnosis("battery");
        req.setQuoteAmount(new BigDecimal("100000"));
        req.setLaborAmount(new BigDecimal("50000"));
        req.setWarrantyDays(30);
        req.setAssignedTechnicianId(8L);
        req.setAssignedTechnicianName("Tech");
        req.setIsWarrantyClaim(Boolean.TRUE);
        req.setNote("note");
        req.setParts(List.of(partRequest("Battery", 2, new BigDecimal("20000"))));

        RepairTicketDTO dto = service.create(req);

        assertThat(dto.getCustomerName()).isEqualTo("Tran B");
        assertThat(dto.getReportedFault()).isEqualTo("no power");
        assertThat(dto.getStatus()).isEqualTo(RepairStatus.RECEIVED.name());
        assertThat(dto.getTicketNumber()).startsWith("SC-");
        // parts: 2 * 20000 = 40000 ; total = parts + labor = 40000 + 50000 = 90000
        assertThat(dto.getPartsAmount()).isEqualByComparingTo("40000");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("90000");
        assertThat(dto.getParts()).hasSize(1);
        assertThat(dto.getIsWarrantyClaim()).isTrue();
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null), any(), eq("REPAIR_TICKET"),
                anyString(), anyString(), eq(null), any(), any());
    }

    @Test
    @DisplayName("create: null optional amounts/warrantyDays/parts default sensibly")
    void create_nullDefaults() {
        when(repairTicketRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(1L);
        stubSaveEcho();

        CreateRepairTicketRequest req = new CreateRepairTicketRequest();
        req.setCustomerName("Cust");
        req.setReportedFault("fault");
        // quoteAmount, laborAmount, warrantyDays, isWarrantyClaim, parts all null

        RepairTicketDTO dto = service.create(req);

        assertThat(dto.getQuoteAmount()).isEqualByComparingTo("0");
        assertThat(dto.getLaborAmount()).isEqualByComparingTo("0");
        assertThat(dto.getWarrantyDays()).isZero();
        assertThat(dto.getIsWarrantyClaim()).isFalse();
        assertThat(dto.getPartsAmount()).isEqualByComparingTo("0");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("0");
        assertThat(dto.getParts()).isEmpty();
    }

    @Test
    @DisplayName("create: part with null quantity defaults to 1")
    void create_partNullQuantity() {
        when(repairTicketRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(0L);
        stubSaveEcho();

        CreateRepairTicketRequest req = new CreateRepairTicketRequest();
        req.setCustomerName("Cust");
        req.setReportedFault("fault");
        req.setParts(List.of(partRequest("Screen", null, new BigDecimal("30000"))));

        RepairTicketDTO dto = service.create(req);

        assertThat(dto.getParts()).hasSize(1);
        assertThat(dto.getParts().get(0).getQuantity()).isEqualTo(1);
        assertThat(dto.getPartsAmount()).isEqualByComparingTo("30000");
    }

    // ── update ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: applies all non-null fields and replaces parts")
    void update_allFields() {
        RepairTicket existing = ticket(1L, "DIAGNOSING", USER);
        existing.getParts().add(RepairPart.builder()
                .productName("Old").quantity(1).unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN).build());
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        UpdateRepairTicketRequest req = new UpdateRepairTicketRequest();
        req.setCustomerId(9L);
        req.setCustomerName("  New Name  ");
        req.setCustomerPhone("0999");
        req.setDeviceType("Laptop");
        req.setBrand("Dell");
        req.setModel("XPS");
        req.setSerialImei("SER");
        req.setReportedFault("  new fault  ");
        req.setDiagnosis("diag");
        req.setQuoteAmount(new BigDecimal("11111"));
        req.setLaborAmount(new BigDecimal("22222"));
        req.setWarrantyDays(15);
        req.setAssignedTechnicianId(4L);
        req.setAssignedTechnicianName("Tech2");
        req.setIsWarrantyClaim(Boolean.TRUE);
        req.setNote("newnote");
        req.setParts(List.of(partRequest("New Part", 3, new BigDecimal("10000"))));

        RepairTicketDTO dto = service.update(1L, req);

        assertThat(dto.getCustomerName()).isEqualTo("New Name");
        assertThat(dto.getReportedFault()).isEqualTo("new fault");
        assertThat(dto.getDeviceType()).isEqualTo("Laptop");
        assertThat(dto.getWarrantyDays()).isEqualTo(15);
        // parts replaced: 3 * 10000 = 30000 ; total = 30000 + 22222 = 52222
        assertThat(dto.getParts()).hasSize(1);
        assertThat(dto.getPartsAmount()).isEqualByComparingTo("30000");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("52222");
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null), any(), eq("REPAIR_TICKET"),
                anyString(), anyString(), eq(null), any());
    }

    @Test
    @DisplayName("update: with all null fields keeps existing values (no parts touched)")
    void update_noFields() {
        RepairTicket existing = ticket(1L, "RECEIVED", USER);
        existing.setCustomerName("Keep");
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        RepairTicketDTO dto = service.update(1L, new UpdateRepairTicketRequest());

        assertThat(dto.getCustomerName()).isEqualTo("Keep");
    }

    @Test
    @DisplayName("update: terminal ticket rejected")
    void update_terminal() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "DELIVERED", USER)));

        assertThatThrownBy(() -> service.update(1L, new UpdateRepairTicketRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(repairTicketRepository, never()).save(any());
    }

    // ── updateStatus ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: invalid status throws BadRequest")
    void updateStatus_invalid() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("NOPE");

        assertThatThrownBy(() -> service.updateStatus(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateStatus: null status throws BadRequest")
    void updateStatus_nullStatus() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));

        assertThatThrownBy(() -> service.updateStatus(1L, new UpdateRepairStatusRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateStatus: ticket already terminal throws BadRequest")
    void updateStatus_alreadyTerminal() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "CANCELLED", USER)));
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("REPAIRING");

        assertThatThrownBy(() -> service.updateStatus(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateStatus: transition to REPAIRING with note set")
    void updateStatus_repairingWithNote() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "QUOTED", USER)));
        stubSaveEcho();
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("  REPAIRING  ");
        req.setNote("  in progress  ");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("REPAIRING");
        assertThat(dto.getNote()).isEqualTo("in progress");
        assertThat(dto.getLinkedOrderId()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: blank note is ignored")
    void updateStatus_blankNoteIgnored() {
        RepairTicket existing = ticket(1L, "QUOTED", USER);
        existing.setNote("original");
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("REPAIRING");
        req.setNote("   ");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getNote()).isEqualTo("original");
    }

    @Test
    @DisplayName("updateStatus: COMPLETED with nothing to bill sets completedAt, no order")
    void updateStatus_completedNoBilling() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "REPAIRING", USER)));
        stubSaveEcho();
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("COMPLETED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getCompletedAt()).isNotNull();
        assertThat(dto.getLinkedOrderId()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: COMPLETED with billable amounts creates a linked order")
    void updateStatus_completedBillable() {
        RepairTicket existing = ticket(1L, "REPAIRING", USER);
        existing.setLaborAmount(new BigDecimal("50000"));
        existing.setAssignedTechnicianId(8L);
        existing.setAssignedTechnicianName("Tech");
        existing.getParts().add(RepairPart.builder()
                .productName("Screen").productId(2L).quantity(2)
                .unitPrice(new BigDecimal("30000")).lineTotal(new BigDecimal("60000")).build());
        // updateStatus does NOT recompute amounts; createCompletionOrder gates on totalAmount > 0
        existing.setTotalAmount(new BigDecimal("110000"));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        Employee emp = Employee.builder().commissionRate(new BigDecimal("10")).build();
        emp.setId(8L);
        when(employeeRepository.findById(8L)).thenReturn(Optional.of(emp));

        Order savedOrder = new Order();
        savedOrder.setId(777L);
        savedOrder.setOrderNumber("ORD-1");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("COMPLETED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getLinkedOrderId()).isEqualTo(777L);
        verify(orderRepository).save(any(Order.class));
        verify(employeeRepository).findById(8L);
    }

    @Test
    @DisplayName("updateStatus: DELIVERED sets completedAt+deliveredAt and creates order")
    void updateStatus_delivered() {
        RepairTicket existing = ticket(1L, "COMPLETED", USER);
        existing.setLaborAmount(new BigDecimal("40000"));
        // updateStatus does NOT recompute amounts; createCompletionOrder gates on totalAmount > 0
        existing.setTotalAmount(new BigDecimal("40000"));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();
        Order savedOrder = new Order();
        savedOrder.setId(555L);
        savedOrder.setOrderNumber("ORD-2");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("DELIVERED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("DELIVERED");
        assertThat(dto.getDeliveredAt()).isNotNull();
        assertThat(dto.getCompletedAt()).isNotNull();
        assertThat(dto.getLinkedOrderId()).isEqualTo(555L);
    }

    @Test
    @DisplayName("updateStatus: DELIVERED when already completed keeps original completedAt")
    void updateStatus_deliveredKeepsCompletedAt() {
        RepairTicket existing = ticket(1L, "COMPLETED", USER);
        LocalDateTime original = LocalDateTime.now().minusDays(2);
        existing.setCompletedAt(original);
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("DELIVERED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getCompletedAt()).isEqualTo(original);
        assertThat(dto.getDeliveredAt()).isNotNull();
        // total is zero → no billable order
        assertThat(dto.getLinkedOrderId()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: CANCELLED uses cancelled action, no order")
    void updateStatus_cancelled() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));
        stubSaveEcho();
        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("CANCELLED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: billable but already linked order is idempotent (no new order)")
    void updateStatus_alreadyLinked() {
        RepairTicket existing = ticket(1L, "COMPLETED", USER);
        existing.setLaborAmount(new BigDecimal("40000"));
        existing.setLinkedOrderId(123L);
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("DELIVERED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getLinkedOrderId()).isEqualTo(123L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: billable but order has no chargeable lines returns null orderId")
    void updateStatus_billableButTotalDrivenByQuoteOnly() {
        // totalAmount > 0 (driven by parts present at recompute? here total comes from labor=0,
        // parts with non-positive price) → createCompletionOrder returns null (items empty)
        RepairTicket existing = ticket(1L, "REPAIRING", USER);
        existing.setTotalAmount(new BigDecimal("5000")); // force billable entry
        existing.getParts().add(RepairPart.builder()
                .productName("Free").quantity(1)
                .unitPrice(BigDecimal.ZERO).lineTotal(BigDecimal.ZERO).build());
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("COMPLETED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        // recomputeAmounts is NOT called in updateStatus, so totalAmount stays 5000 → enters
        // createCompletionOrder, but the only part is zero-priced and labor=0 → items empty → null
        assertThat(dto.getLinkedOrderId()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: billable order with technician not resolving to employee skips commission")
    void updateStatus_technicianNotFound() {
        RepairTicket existing = ticket(1L, "REPAIRING", USER);
        existing.setLaborAmount(new BigDecimal("50000"));
        existing.setAssignedTechnicianId(8L);
        // updateStatus does NOT recompute amounts; createCompletionOrder gates on totalAmount > 0
        existing.setTotalAmount(new BigDecimal("50000"));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();
        when(employeeRepository.findById(8L)).thenReturn(Optional.empty());
        Order savedOrder = new Order();
        savedOrder.setId(999L);
        savedOrder.setOrderNumber("ORD-9");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("COMPLETED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getLinkedOrderId()).isEqualTo(999L);
        verify(employeeRepository).findById(8L);
    }

    @Test
    @DisplayName("updateStatus: employee with null commissionRate defaults to zero")
    void updateStatus_employeeNullCommission() {
        RepairTicket existing = ticket(1L, "REPAIRING", USER);
        existing.setLaborAmount(new BigDecimal("50000"));
        existing.setAssignedTechnicianId(8L);
        existing.setAssignedTechnicianName("Tech");
        // updateStatus does NOT recompute amounts; createCompletionOrder gates on totalAmount > 0
        existing.setTotalAmount(new BigDecimal("50000"));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();
        Employee emp = Employee.builder().build(); // null commissionRate
        emp.setId(8L);
        when(employeeRepository.findById(8L)).thenReturn(Optional.of(emp));
        Order savedOrder = new Order();
        savedOrder.setId(1000L);
        savedOrder.setOrderNumber("ORD-10");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        UpdateRepairStatusRequest req = new UpdateRepairStatusRequest();
        req.setStatus("COMPLETED");

        RepairTicketDTO dto = service.updateStatus(1L, req);

        assertThat(dto.getLinkedOrderId()).isEqualTo(1000L);
    }

    // ── assignTechnician ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTechnician: sets technician and logs")
    void assignTechnician_success() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));
        stubSaveEcho();
        AssignTechnicianRequest req = new AssignTechnicianRequest();
        req.setTechnicianId(12L);
        req.setTechnicianName("Mr Fix");

        RepairTicketDTO dto = service.assignTechnician(1L, req);

        assertThat(dto.getAssignedTechnicianId()).isEqualTo(12L);
        assertThat(dto.getAssignedTechnicianName()).isEqualTo("Mr Fix");
        // logAsync passes two trailing varargs: ticket number + technician name
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null), any(), eq("REPAIR_TICKET"),
                anyString(), anyString(), eq(null), any(), any());
    }

    @Test
    @DisplayName("assignTechnician: null technician name uses dash in log")
    void assignTechnician_nullName() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "RECEIVED", USER)));
        stubSaveEcho();
        AssignTechnicianRequest req = new AssignTechnicianRequest();
        req.setTechnicianId(12L);

        RepairTicketDTO dto = service.assignTechnician(1L, req);

        assertThat(dto.getAssignedTechnicianId()).isEqualTo(12L);
        assertThat(dto.getAssignedTechnicianName()).isNull();
    }

    @Test
    @DisplayName("assignTechnician: terminal ticket rejected")
    void assignTechnician_terminal() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(ticket(1L, "DELIVERED", USER)));

        assertThatThrownBy(() -> service.assignTechnician(1L, new AssignTechnicianRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes ticket and logs")
    void delete_success() {
        RepairTicket existing = ticket(1L, "RECEIVED", USER);
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(existing));
        stubSaveEcho();

        service.delete(1L);

        assertThat(existing.isDeleted()).isTrue();
        verify(repairTicketRepository).save(existing);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null), any(), eq("REPAIR_TICKET"),
                anyString(), anyString(), eq(null), any());
    }

    @Test
    @DisplayName("delete: missing ticket throws not-found")
    void delete_notFound() {
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(2L, TENANT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── statusCounts ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("statusCounts: seeds all statuses with zero then overlays repo counts")
    void statusCounts_success() {
        List<Object[]> rows = List.of(
                new Object[]{"RECEIVED", 3L},
                new Object[]{"COMPLETED", 1}
        );
        when(repairTicketRepository.countByStatus(TENANT)).thenReturn(rows);

        Map<String, Long> counts = service.statusCounts();

        assertThat(counts).containsKeys("RECEIVED", "DIAGNOSING", "QUOTED", "REPAIRING",
                "COMPLETED", "DELIVERED", "CANCELLED");
        assertThat(counts.get("RECEIVED")).isEqualTo(3L);
        assertThat(counts.get("COMPLETED")).isEqualTo(1L);
        assertThat(counts.get("DELIVERED")).isZero();
    }

    // ── warrantyLookup ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("warrantyLookup: keeps only still-valid warranty tickets (VIEW_ALL → no createdBy scope)")
    void warrantyLookup_validKept() {
        RepairTicket valid = ticket(1L, "DELIVERED", USER);
        valid.setWarrantyDays(30);
        valid.setDeliveredAt(LocalDateTime.now().minusDays(1));
        RepairTicket expired = ticket(2L, "DELIVERED", USER);
        expired.setWarrantyDays(5);
        expired.setDeliveredAt(LocalDateTime.now().minusDays(100));
        when(repairTicketRepository.searchDeliveredUnderWarranty(TENANT, "abc", null))
                .thenReturn(List.of(valid, expired));

        List<RepairTicketDTO> result = service.warrantyLookup("  abc  ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getUnderWarranty()).isTrue();
    }

    @Test
    @DisplayName("warrantyLookup: without VIEW_ALL scopes to current user, null keyword")
    void warrantyLookup_ownScope() {
        when(featureContext.hasFeature("REPAIR_VIEW_ALL")).thenReturn(false);
        when(repairTicketRepository.searchDeliveredUnderWarranty(TENANT, null, USER))
                .thenReturn(List.of());

        List<RepairTicketDTO> result = service.warrantyLookup(null);

        assertThat(result).isEmpty();
        verify(repairTicketRepository).searchDeliveredUnderWarranty(TENANT, null, USER);
    }

    // ── createWarrantyClaim ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createWarrantyClaim: builds zero-cost claim from a valid original")
    void createWarrantyClaim_success() {
        RepairTicket original = ticket(1L, "DELIVERED", USER);
        original.setWarrantyDays(30);
        original.setDeliveredAt(LocalDateTime.now().minusDays(1));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(original));
        when(repairTicketRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(2L);
        stubSaveEcho();

        RepairTicketDTO dto = service.createWarrantyClaim(1L);

        assertThat(dto.getIsWarrantyClaim()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(RepairStatus.RECEIVED.name());
        assertThat(dto.getLaborAmount()).isEqualByComparingTo("0");
        assertThat(dto.getWarrantyDays()).isZero();
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("0");
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), eq(null), any(), eq("REPAIR_TICKET"),
                anyString(), anyString(), eq(null), any(), any());
    }

    @Test
    @DisplayName("createWarrantyClaim: expired warranty rejected")
    void createWarrantyClaim_expired() {
        RepairTicket original = ticket(1L, "DELIVERED", USER);
        original.setWarrantyDays(5);
        original.setDeliveredAt(LocalDateTime.now().minusDays(100));
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.createWarrantyClaim(1L))
                .isInstanceOf(BadRequestException.class);
        verify(repairTicketRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWarrantyClaim: never-delivered original rejected")
    void createWarrantyClaim_noWarranty() {
        RepairTicket original = ticket(1L, "COMPLETED", USER);
        // no deliveredAt, warrantyDays 0 → warrantyExpiry null
        when(repairTicketRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.createWarrantyClaim(1L))
                .isInstanceOf(BadRequestException.class);
    }
}
