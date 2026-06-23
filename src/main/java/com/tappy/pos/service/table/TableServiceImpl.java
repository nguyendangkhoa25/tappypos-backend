package com.tappy.pos.service.table;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.CreateTableReservationRequest;
import com.tappy.pos.model.dto.table.SetTableStatusRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.TableReservationDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.entity.table.TableReservation;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.TableStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.repository.table.TableReservationRepository;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

    private final TableRepository tableRepository;
    private final TableReservationRepository tableReservationRepository;
    private final OrderRepository orderRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<TableDTO> getTables() {
        String tenantId = tenantContext.getCurrentTenantId();
        return tableRepository.findAllActive(tenantId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public TableDTO createTable(CreateTableRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        ShopTable table = ShopTable.builder()
                .tenantId(tenantId)
                .tableNumber(request.getTableNumber().strip())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 4)
                .location(request.getLocation())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .status(TableStatus.AVAILABLE)
                .build();
        return toDTO(tableRepository.save(table));
    }

    @Override
    @Transactional
    public TableDTO updateTable(Long id, UpdateTableRequest request) {
        ShopTable table = findActiveById(id);
        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()) {
            table.setTableNumber(request.getTableNumber().strip());
        }
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getLocation() != null) {
            table.setLocation(request.getLocation().isBlank() ? null : request.getLocation());
        }
        if (request.getDisplayOrder() != null) {
            table.setDisplayOrder(request.getDisplayOrder());
        }
        return toDTO(tableRepository.save(table));
    }

    @Override
    @Transactional
    public void deleteTable(Long id) {
        ShopTable table = findActiveById(id);
        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new BadRequestException(messageService.getMessage("error.table.delete.not.empty"));
        }
        table.softDelete();
        tableRepository.save(table);
    }

    @Override
    @Transactional
    public void occupyTable(Long tableId, Long orderId) {
        ShopTable table = findActiveById(tableId);
        table.setStatus(TableStatus.OCCUPIED);
        table.setCurrentOrderId(orderId);
        tableRepository.save(table);
        log.debug("Table {} marked OCCUPIED for order {}", tableId, orderId);
    }

    @Override
    @Transactional
    public void releaseTable(Long tableId) {
        ShopTable table = findActiveById(tableId);
        table.setStatus(TableStatus.AVAILABLE);
        table.setCurrentOrderId(null);
        tableRepository.save(table);
        log.debug("Table {} released", tableId);
    }

    @Override
    @Transactional
    public TableDTO setStatus(Long tableId, SetTableStatusRequest request) {
        ShopTable table = findActiveById(tableId);
        TableStatus newStatus = request.getStatus();

        if (newStatus == TableStatus.OCCUPIED) {
            throw new BadRequestException(messageService.getMessage("error.table.cannot.set.occupied"));
        }
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BadRequestException(messageService.getMessage("error.table.occupied.cannot.change"));
        }
        if (newStatus == TableStatus.RESERVED) {
            if (request.getReservedFor() == null || request.getReservedFor().isBlank()) {
                throw new BadRequestException(messageService.getMessage("error.table.reservation.name.required"));
            }
            table.setReservedFor(request.getReservedFor().strip());
            table.setReservedTime(request.getReservedTime() != null ? request.getReservedTime().strip() : null);
        } else {
            // Clear reservation info when transitioning away from RESERVED
            table.setReservedFor(null);
            table.setReservedTime(null);
        }
        table.setStatus(newStatus);
        return toDTO(tableRepository.save(table));
    }

    // ── Advance reservation calendar (đặt bàn trước) ───────────────────────────

    @Override
    @Transactional
    public TableReservationDTO createReservation(CreateTableReservationRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        ShopTable table = findActiveById(request.getTableId());
        int party = (request.getPartySize() != null && request.getPartySize() > 0) ? request.getPartySize() : 2;
        TableReservation r = TableReservation.builder()
                .tenantId(tenantId)
                .tableId(table.getId())
                .tableLabel(table.getTableNumber())
                .reservedAt(request.getReservedAt())
                .partySize(party)
                .customerId(request.getCustomerId())
                .customerName(blankToNull(request.getCustomerName()))
                .customerPhone(blankToNull(request.getCustomerPhone()))
                .status("RESERVED")
                .note(blankToNull(request.getNote()))
                .createdBy(currentUsername())
                .build();
        TableReservation saved = tableReservationRepository.save(r);
        activityLogService.logAsync(tenantId, currentUsername(), null,
                ActivityAction.TABLE_RESERVED, "TABLE_RESERVATION", String.valueOf(saved.getId()),
                "activity.table.reserved", null, table.getTableNumber(), request.getReservedAt().format(HHMM));
        return toResDTO(saved);
    }

    @Override
    public List<TableReservationDTO> listReservations(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now();
        LocalDate t = to != null ? to : f;
        LocalDateTime fromDt = f.atStartOfDay();
        LocalDateTime toDt = t.plusDays(1).atStartOfDay();
        return tableReservationRepository.findInRange(fromDt, toDt)
                .stream().map(this::toResDTO).toList();
    }

    @Override
    @Transactional
    public TableReservationDTO seatReservation(Long reservationId) {
        TableReservation r = findReservation(reservationId);
        requireReserved(r);
        r.setStatus("SEATED");
        TableReservation saved = tableReservationRepository.save(r);
        // Flag the table RESERVED so it surfaces on the grid — only if it is currently free.
        tableRepository.findById(r.getTableId())
                .filter(tb -> !tb.isDeleted())
                .ifPresent(tb -> {
                    if (tb.getStatus() == TableStatus.AVAILABLE) {
                        tb.setStatus(TableStatus.RESERVED);
                        tb.setReservedFor(r.getCustomerName());
                        tb.setReservedTime(r.getReservedAt().format(HHMM));
                        tableRepository.save(tb);
                    }
                });
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), currentUsername(), null,
                ActivityAction.TABLE_RESERVATION_SEATED, "TABLE_RESERVATION", String.valueOf(saved.getId()),
                "activity.table.reservation.seated", null, r.getTableLabel());
        return toResDTO(saved);
    }

    @Override
    @Transactional
    public TableReservationDTO cancelReservation(Long reservationId) {
        TableReservation r = findReservation(reservationId);
        requireReserved(r);
        r.setStatus("CANCELLED");
        TableReservation saved = tableReservationRepository.save(r);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), currentUsername(), null,
                ActivityAction.TABLE_RESERVATION_CANCELLED, "TABLE_RESERVATION", String.valueOf(saved.getId()),
                "activity.table.reservation.cancelled", null, r.getTableLabel());
        return toResDTO(saved);
    }

    @Override
    @Transactional
    public TableReservationDTO markReservationNoShow(Long reservationId) {
        TableReservation r = findReservation(reservationId);
        requireReserved(r);
        r.setStatus("NO_SHOW");
        TableReservation saved = tableReservationRepository.save(r);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), currentUsername(), null,
                ActivityAction.TABLE_RESERVATION_NO_SHOW, "TABLE_RESERVATION", String.valueOf(saved.getId()),
                "activity.table.reservation.no.show", null, r.getTableLabel());
        return toResDTO(saved);
    }

    private TableReservation findReservation(Long id) {
        return tableReservationRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.table.reservation.not.found")));
    }

    private void requireReserved(TableReservation r) {
        if (!"RESERVED".equals(r.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.table.reservation.not.active"));
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }

    private TableReservationDTO toResDTO(TableReservation r) {
        return TableReservationDTO.builder()
                .id(r.getId())
                .tableId(r.getTableId())
                .tableLabel(r.getTableLabel())
                .reservedAt(r.getReservedAt())
                .partySize(r.getPartySize())
                .customerId(r.getCustomerId())
                .customerName(r.getCustomerName())
                .customerPhone(r.getCustomerPhone())
                .status(r.getStatus())
                .note(r.getNote())
                .createdBy(r.getCreatedBy())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ShopTable findActiveById(Long id) {
        return tableRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.table.not.found")));
    }

    private TableDTO toDTO(ShopTable t) {
        String orderNumber = null;
        Long elapsedMinutes = null;
        java.math.BigDecimal orderTotal = null;
        if (t.getCurrentOrderId() != null) {
            Optional<Order> order = orderRepository.findById(t.getCurrentOrderId());
            if (order.isPresent()) {
                Order o = order.get();
                orderNumber = o.getOrderNumber();
                orderTotal = o.getTotalAmount();
                if (o.getCreatedAt() != null) {
                    elapsedMinutes = Duration.between(o.getCreatedAt(), LocalDateTime.now()).toMinutes();
                }
            }
        }
        return TableDTO.builder()
                .id(t.getId())
                .tableNumber(t.getTableNumber())
                .capacity(t.getCapacity())
                .status(t.getStatus())
                .currentOrderId(t.getCurrentOrderId())
                .currentOrderNumber(orderNumber)
                .currentOrderTotal(orderTotal)
                .location(t.getLocation())
                .displayOrder(t.getDisplayOrder())
                .elapsedMinutes(elapsedMinutes)
                .reservedFor(t.getReservedFor())
                .reservedTime(t.getReservedTime())
                .qrToken(t.getQrToken())
                .build();
    }
}
