package com.tappy.pos.service.table;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.CreateTableReservationRequest;
import com.tappy.pos.model.dto.table.SetTableStatusRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.TableReservationDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.entity.table.TableReservation;
import com.tappy.pos.model.enums.TableStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.repository.table.TableReservationRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableServiceImpl Unit Tests")
class TableServiceImplTest {

    @Mock private TableRepository tableRepository;
    @Mock private TableReservationRepository tableReservationRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private TableServiceImpl service;

    private static final String TENANT = "shop-1";

    private ShopTable table(Long id, TableStatus status, Long currentOrderId) {
        ShopTable t = ShopTable.builder()
                .tenantId(TENANT)
                .tableNumber("B1")
                .capacity(4)
                .location("Tầng 1")
                .displayOrder(1)
                .status(status)
                .currentOrderId(currentOrderId)
                .build();
        t.setId(id);
        return t;
    }

    @Test
    @DisplayName("getTables: maps active tables; resolves order info for occupied tables")
    void getTables() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        ShopTable occupied = table(1L, TableStatus.OCCUPIED, 200L);
        when(tableRepository.findAllActive(TENANT)).thenReturn(List.of(occupied));
        Order order = Order.builder().orderNumber("ORD-9").totalAmount(new BigDecimal("250000")).build();
        order.setId(200L);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        List<TableDTO> result = service.getTables();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentOrderNumber()).isEqualTo("ORD-9");
        assertThat(result.get(0).getElapsedMinutes()).isGreaterThanOrEqualTo(14L);
    }

    @Test
    @DisplayName("createTable: builds an AVAILABLE table")
    void createTable() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(tableRepository.save(any(ShopTable.class))).thenAnswer(inv -> {
            ShopTable t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });
        CreateTableRequest req = new CreateTableRequest();
        req.setTableNumber("  B2  ");
        req.setCapacity(6);

        TableDTO dto = service.createTable(req);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getTableNumber()).isEqualTo("B2");
        assertThat(dto.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    @DisplayName("updateTable: applies provided fields")
    void updateTable() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table(1L, TableStatus.AVAILABLE, null)));
        when(tableRepository.save(any(ShopTable.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateTableRequest req = new UpdateTableRequest();
        req.setTableNumber("B9");
        req.setCapacity(8);

        TableDTO dto = service.updateTable(1L, req);

        assertThat(dto.getTableNumber()).isEqualTo("B9");
        assertThat(dto.getCapacity()).isEqualTo(8);
    }

    @Test
    @DisplayName("updateTable: not found → ResourceNotFoundException")
    void updateTable_notFound() {
        when(tableRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTable(99L, new UpdateTableRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTable: AVAILABLE table is soft-deleted")
    void deleteTable() {
        ShopTable t = table(1L, TableStatus.AVAILABLE, null);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        service.deleteTable(1L);

        verify(tableRepository).save(t);
    }

    @Test
    @DisplayName("deleteTable: non-AVAILABLE table cannot be deleted")
    void deleteTable_notEmpty() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table(1L, TableStatus.OCCUPIED, 200L)));
        when(messageService.getMessage("error.table.delete.not.empty")).thenReturn("không thể xóa");

        assertThatThrownBy(() -> service.deleteTable(1L)).isInstanceOf(BadRequestException.class);
        verify(tableRepository, never()).save(any());
    }

    @Test
    @DisplayName("occupyTable: marks OCCUPIED with the order id")
    void occupyTable() {
        ShopTable t = table(1L, TableStatus.AVAILABLE, null);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        service.occupyTable(1L, 300L);

        assertThat(t.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(t.getCurrentOrderId()).isEqualTo(300L);
        verify(tableRepository).save(t);
    }

    @Test
    @DisplayName("releaseTable: clears status and order id")
    void releaseTable() {
        ShopTable t = table(1L, TableStatus.OCCUPIED, 300L);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        service.releaseTable(1L);

        assertThat(t.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(t.getCurrentOrderId()).isNull();
    }

    @Test
    @DisplayName("setStatus: RESERVED requires a reservation name")
    void setStatus_reserved() {
        ShopTable t = table(1L, TableStatus.AVAILABLE, null);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tableRepository.save(any(ShopTable.class))).thenAnswer(inv -> inv.getArgument(0));
        SetTableStatusRequest req = new SetTableStatusRequest();
        req.setStatus(TableStatus.RESERVED);
        req.setReservedFor("  Anh B  ");

        TableDTO dto = service.setStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo(TableStatus.RESERVED);
        assertThat(dto.getReservedFor()).isEqualTo("Anh B");
    }

    @Test
    @DisplayName("setStatus: RESERVED without name → BadRequestException")
    void setStatus_reservedNoName() {
        ShopTable t = table(1L, TableStatus.AVAILABLE, null);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));
        when(messageService.getMessage("error.table.reservation.name.required")).thenReturn("cần tên");
        SetTableStatusRequest req = new SetTableStatusRequest();
        req.setStatus(TableStatus.RESERVED);

        assertThatThrownBy(() -> service.setStatus(1L, req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("setStatus: cannot set OCCUPIED directly")
    void setStatus_occupiedRejected() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table(1L, TableStatus.AVAILABLE, null)));
        when(messageService.getMessage("error.table.cannot.set.occupied")).thenReturn("không thể");
        SetTableStatusRequest req = new SetTableStatusRequest();
        req.setStatus(TableStatus.OCCUPIED);

        assertThatThrownBy(() -> service.setStatus(1L, req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("setStatus: an OCCUPIED table cannot change status")
    void setStatus_fromOccupied() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table(1L, TableStatus.OCCUPIED, 200L)));
        when(messageService.getMessage("error.table.occupied.cannot.change")).thenReturn("đang có khách");
        SetTableStatusRequest req = new SetTableStatusRequest();
        req.setStatus(TableStatus.AVAILABLE);

        assertThatThrownBy(() -> service.setStatus(1L, req)).isInstanceOf(BadRequestException.class);
    }

    // ── Reservations ──────────────────────────────────────────────────────────

    @BeforeEach
    void authSetUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
    }

    @AfterEach
    void authTearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createReservation: snapshots the table label and defaults party size")
    void createReservation_ok() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table(1L, TableStatus.AVAILABLE, null)));
        when(tableReservationRepository.save(any(TableReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTableReservationRequest req = new CreateTableReservationRequest();
        req.setTableId(1L);
        req.setReservedAt(LocalDateTime.now().plusHours(3));
        req.setCustomerName("Anh Ba");

        TableReservationDTO dto = service.createReservation(req);

        assertThat(dto.getTableLabel()).isEqualTo("B1");
        assertThat(dto.getPartySize()).isEqualTo(2);
        assertThat(dto.getStatus()).isEqualTo("RESERVED");
        verify(activityLogService).logAsync(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("seatReservation: marks SEATED and flags a free table RESERVED")
    void seatReservation_flagsTable() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        TableReservation r = TableReservation.builder()
                .tenantId(TENANT).tableId(1L).tableLabel("B1").reservedAt(LocalDateTime.now().plusHours(1))
                .partySize(4).status("RESERVED").createdBy("owner").build();
        r.setId(9L);
        ShopTable freeTable = table(1L, TableStatus.AVAILABLE, null);
        when(tableReservationRepository.findById(9L)).thenReturn(Optional.of(r));
        when(tableReservationRepository.save(any(TableReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(freeTable));

        TableReservationDTO dto = service.seatReservation(9L);

        assertThat(dto.getStatus()).isEqualTo("SEATED");
        assertThat(freeTable.getStatus()).isEqualTo(TableStatus.RESERVED);
        verify(tableRepository).save(freeTable);
    }

    @Test
    @DisplayName("cancelReservation: refuses a reservation that is not active")
    void cancelReservation_notActive() {
        TableReservation r = TableReservation.builder()
                .tenantId(TENANT).tableId(1L).tableLabel("B1").reservedAt(LocalDateTime.now())
                .status("SEATED").createdBy("owner").build();
        r.setId(9L);
        when(tableReservationRepository.findById(9L)).thenReturn(Optional.of(r));
        when(messageService.getMessage("error.table.reservation.not.active")).thenReturn("hết hiệu lực");

        assertThatThrownBy(() -> service.cancelReservation(9L)).isInstanceOf(BadRequestException.class);
        verify(tableReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("listReservations: queries the day window and maps results")
    void listReservations_ok() {
        TableReservation r = TableReservation.builder()
                .tenantId(TENANT).tableId(1L).tableLabel("B1").reservedAt(LocalDateTime.now())
                .partySize(2).status("RESERVED").createdBy("owner").build();
        r.setId(9L);
        when(tableReservationRepository.findInRange(any(), any())).thenReturn(List.of(r));

        List<TableReservationDTO> list = service.listReservations(LocalDate.now(), LocalDate.now());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTableLabel()).isEqualTo("B1");
    }
}
