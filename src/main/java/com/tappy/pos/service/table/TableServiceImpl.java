package com.tappy.pos.service.table;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.SetTableStatusRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.enums.TableStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.table.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;

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
