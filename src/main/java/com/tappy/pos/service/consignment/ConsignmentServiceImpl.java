package com.tappy.pos.service.consignment;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.consignment.*;
import com.tappy.pos.model.entity.consignment.Consignment;
import com.tappy.pos.model.entity.consignment.ConsignmentItem;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.ConsignmentStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.consignment.ConsignmentItemRepository;
import com.tappy.pos.repository.consignment.ConsignmentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsignmentServiceImpl implements ConsignmentService {

    private static final String VIEW_ALL = "CONSIGNMENT_VIEW_ALL";
    private static final DateTimeFormatter NUM_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentItemRepository consignmentItemRepository;
    private final AuthContext authContext;
    private final FeatureContext featureContext;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    @Override
    @Transactional
    public ConsignmentDTO create(ConsignmentRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        String actor = authContext.getCurrentUsername();

        Consignment consignment = Consignment.builder()
                .tenantId(tenantId)
                .publisherId(request.getPublisherId())
                .publisherName(request.getPublisherName())
                .placementNumber("KG-TMP")
                .placementDate(request.getPlacementDate())
                .status(ConsignmentStatus.ACTIVE)
                .note(request.getNote())
                .createdBy(actor)
                .build();

        for (ConsignmentItemRequest ir : request.getItems()) {
            ConsignmentItem item = ConsignmentItem.builder()
                    .tenantId(tenantId)
                    .consignment(consignment)
                    .productId(ir.getProductId())
                    .productName(ir.getProductName())
                    .quantityPlaced(ir.getQuantityPlaced())
                    .unitPrice(ir.getUnitPrice())
                    .build();
            consignment.getItems().add(item);
        }

        Consignment saved = consignmentRepository.save(consignment);
        // Stamp a human-friendly placement number now the id is known (guaranteed unique).
        saved.setPlacementNumber("KG-" + request.getPlacementDate().format(NUM_FMT)
                + "-" + String.format("%05d", saved.getId()));
        saved = consignmentRepository.save(saved);

        activityLogService.logAsync(tenantId, actor, null,
                ActivityAction.CONSIGNMENT_CREATED, "CONSIGNMENT", saved.getPlacementNumber(),
                "activity.consignment.created", null, saved.getPlacementNumber(), saved.getPublisherName());

        return toDTO(saved, true);
    }

    @Override
    @Transactional
    public ConsignmentDTO update(Long id, ConsignmentRequest request) {
        Consignment consignment = findOwned(id);
        if (consignment.getStatus() != ConsignmentStatus.ACTIVE) {
            throw new com.tappy.pos.exception.BadRequestException(
                    messageService.getMessage("error.consignment.not.editable"));
        }
        String tenantId = tenantContext.getCurrentTenantId();

        consignment.setPublisherId(request.getPublisherId());
        consignment.setPublisherName(request.getPublisherName());
        consignment.setPlacementDate(request.getPlacementDate());
        consignment.setNote(request.getNote());
        consignment.setUpdatedBy(authContext.getCurrentUsername());

        // Replace line items (orphanRemoval deletes the old rows).
        consignment.getItems().clear();
        for (ConsignmentItemRequest ir : request.getItems()) {
            ConsignmentItem item = ConsignmentItem.builder()
                    .tenantId(tenantId)
                    .consignment(consignment)
                    .productId(ir.getProductId())
                    .productName(ir.getProductName())
                    .quantityPlaced(ir.getQuantityPlaced())
                    .unitPrice(ir.getUnitPrice())
                    .build();
            consignment.getItems().add(item);
        }

        Consignment saved = consignmentRepository.save(consignment);
        activityLogService.logAsync(tenantId, authContext.getCurrentUsername(), null,
                ActivityAction.CONSIGNMENT_UPDATED, "CONSIGNMENT", saved.getPlacementNumber(),
                "activity.consignment.updated", null, saved.getPlacementNumber());
        return toDTO(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignmentDTO getById(Long id) {
        return toDTO(findOwned(id), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsignmentDTO> search(ConsignmentStatus status, Pageable pageable) {
        String createdBy = featureContext.hasFeature(VIEW_ALL) ? null : authContext.getCurrentUsername();
        return consignmentRepository
                .search(tenantContext.getCurrentTenantId(), status != null ? status.name() : null, createdBy, pageable)
                .map(c -> toDTO(c, false));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Consignment consignment = findOwned(id);
        consignment.softDelete();
        consignmentRepository.save(consignment);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CONSIGNMENT_CANCELLED, "CONSIGNMENT", consignment.getPlacementNumber(),
                "activity.consignment.cancelled", null, consignment.getPlacementNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignmentSettlementDTO getSettlement(Long id, LocalDate from, LocalDate to) {
        Consignment consignment = findOwned(id);
        return buildSettlement(consignment, from, to);
    }

    @Override
    @Transactional
    public ConsignmentDTO settle(Long id, LocalDate from, LocalDate to) {
        Consignment consignment = findOwned(id);
        ConsignmentSettlementDTO settlement = buildSettlement(consignment, from, to);

        consignment.setStatus(ConsignmentStatus.SETTLED);
        consignment.setSettledFrom(from);
        consignment.setSettledTo(to);
        consignment.setSettledDate(LocalDateTime.now());
        consignment.setSettledAmount(settlement.getTotalAmountDue());
        consignment.setUpdatedBy(authContext.getCurrentUsername());
        Consignment saved = consignmentRepository.save(consignment);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CONSIGNMENT_SETTLED, "CONSIGNMENT", saved.getPlacementNumber(),
                "activity.consignment.settled", null,
                saved.getPlacementNumber(), String.valueOf(settlement.getTotalAmountDue()));
        return toDTO(saved, true);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Loads a consignment in the current tenant, enforcing ownership when the caller lacks VIEW_ALL. */
    private Consignment findOwned(Long id) {
        Consignment consignment = consignmentRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .filter(c -> c.getTenantId().equals(tenantContext.getCurrentTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.consignment.not.found")));
        // Throw 404 (not 403) so a non-owner can't tell the record exists.
        if (!featureContext.hasFeature(VIEW_ALL)
                && consignment.getCreatedBy() != null
                && !consignment.getCreatedBy().equals(authContext.getCurrentUsername())) {
            throw new ResourceNotFoundException(messageService.getMessage("error.consignment.not.found"));
        }
        return consignment;
    }

    private ConsignmentSettlementDTO buildSettlement(Consignment consignment, LocalDate from, LocalDate to) {
        List<ConsignmentItem> items = consignment.getItems();
        List<Long> productIds = items.stream()
                .map(ConsignmentItem::getProductId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, Integer> soldByProduct = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : consignmentItemRepository.sumSoldByProductIds(
                    tenantContext.getCurrentTenantId(), productIds, from, to)) {
                Long pid = ((Number) row[0]).longValue();
                int qty = ((Number) row[1]).intValue();
                soldByProduct.put(pid, qty);
            }
        }

        List<ConsignmentSettlementLineDTO> lines = new ArrayList<>();
        int totalSold = 0;
        BigDecimal totalDue = BigDecimal.ZERO;
        for (ConsignmentItem item : items) {
            int sold = item.getProductId() != null ? soldByProduct.getOrDefault(item.getProductId(), 0) : 0;
            BigDecimal due = item.getUnitPrice().multiply(BigDecimal.valueOf(sold));
            totalSold += sold;
            totalDue = totalDue.add(due);
            lines.add(ConsignmentSettlementLineDTO.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantityPlaced(item.getQuantityPlaced())
                    .quantitySold(sold)
                    .unitPrice(item.getUnitPrice())
                    .amountDue(due)
                    .build());
        }

        return ConsignmentSettlementDTO.builder()
                .consignmentId(consignment.getId())
                .placementNumber(consignment.getPlacementNumber())
                .publisherName(consignment.getPublisherName())
                .from(from)
                .to(to)
                .totalQuantitySold(totalSold)
                .totalAmountDue(totalDue)
                .lines(lines)
                .build();
    }

    private ConsignmentDTO toDTO(Consignment c, boolean includeItems) {
        List<ConsignmentItemDTO> itemDtos = null;
        int totalPlaced = 0;
        for (ConsignmentItem item : c.getItems()) {
            totalPlaced += item.getQuantityPlaced() != null ? item.getQuantityPlaced() : 0;
        }
        if (includeItems) {
            itemDtos = c.getItems().stream()
                    .map(i -> ConsignmentItemDTO.builder()
                            .id(i.getId())
                            .productId(i.getProductId())
                            .productName(i.getProductName())
                            .quantityPlaced(i.getQuantityPlaced())
                            .unitPrice(i.getUnitPrice())
                            .build())
                    .toList();
        }
        return ConsignmentDTO.builder()
                .id(c.getId())
                .publisherId(c.getPublisherId())
                .publisherName(c.getPublisherName())
                .placementNumber(c.getPlacementNumber())
                .placementDate(c.getPlacementDate())
                .status(c.getStatus())
                .statusDisplayName(c.getStatus().getDisplayName())
                .note(c.getNote())
                .settledFrom(c.getSettledFrom())
                .settledTo(c.getSettledTo())
                .settledDate(c.getSettledDate())
                .settledAmount(c.getSettledAmount())
                .totalQuantityPlaced(totalPlaced)
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}
