package com.tappy.pos.service.repair;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.repair.*;
import com.tappy.pos.model.entity.repair.RepairPart;
import com.tappy.pos.model.entity.repair.RepairTicket;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.RepairStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.repair.RepairTicketRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepairTicketServiceImpl implements RepairTicketService {

    private static final String FEATURE_VIEW_ALL = "REPAIR_VIEW_ALL";

    private final RepairTicketRepository repairTicketRepository;
    private final TenantContext tenantContext;
    private final FeatureContext featureContext;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;

    @Override
    public Page<RepairTicketDTO> search(String status, String keyword, Pageable pageable) {
        String tenantId = tenantContext.getCurrentTenantId();
        String normStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String normKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<RepairTicket> page = featureContext.hasFeature(FEATURE_VIEW_ALL)
                ? repairTicketRepository.search(tenantId, normStatus, normKeyword, pageable)
                : repairTicketRepository.searchByCreatedBy(tenantId, normStatus, normKeyword, currentUsername(), pageable);
        return page.map(this::mapToDTO);
    }

    @Override
    public RepairTicketDTO getById(Long id) {
        return mapToDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public RepairTicketDTO create(CreateRepairTicketRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        String username = currentUsername();

        RepairTicket ticket = RepairTicket.builder()
                .tenantId(tenantId)
                .ticketNumber(generateNumber(tenantId))
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone())
                .deviceType(request.getDeviceType())
                .brand(request.getBrand())
                .model(request.getModel())
                .serialImei(request.getSerialImei())
                .reportedFault(request.getReportedFault().trim())
                .diagnosis(request.getDiagnosis())
                .quoteAmount(nz(request.getQuoteAmount()))
                .laborAmount(nz(request.getLaborAmount()))
                .warrantyDays(request.getWarrantyDays() != null ? request.getWarrantyDays() : 0)
                .assignedTechnicianId(request.getAssignedTechnicianId())
                .assignedTechnicianName(request.getAssignedTechnicianName())
                .isWarrantyClaim(Boolean.TRUE.equals(request.getIsWarrantyClaim()))
                .note(request.getNote())
                .status(RepairStatus.RECEIVED.name())
                .receivedAt(LocalDateTime.now())
                .createdBy(username)
                .parts(new ArrayList<>())
                .build();

        applyParts(ticket, request.getParts(), tenantId);
        recomputeAmounts(ticket);

        RepairTicket saved = repairTicketRepository.save(ticket);

        activityLogService.logAsync(tenantId, username, null, ActivityAction.REPAIR_CREATED,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                messageService.getMessage("activity.repair.created", saved.getTicketNumber(), saved.getCustomerName()), null);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public RepairTicketDTO update(Long id, UpdateRepairTicketRequest request) {
        RepairTicket ticket = findOrThrow(id);
        assertEditable(ticket);
        String tenantId = ticket.getTenantId();
        String actor = currentUsername();

        if (request.getCustomerId() != null) ticket.setCustomerId(request.getCustomerId());
        if (request.getCustomerName() != null) ticket.setCustomerName(request.getCustomerName().trim());
        if (request.getCustomerPhone() != null) ticket.setCustomerPhone(request.getCustomerPhone());
        if (request.getDeviceType() != null) ticket.setDeviceType(request.getDeviceType());
        if (request.getBrand() != null) ticket.setBrand(request.getBrand());
        if (request.getModel() != null) ticket.setModel(request.getModel());
        if (request.getSerialImei() != null) ticket.setSerialImei(request.getSerialImei());
        if (request.getReportedFault() != null) ticket.setReportedFault(request.getReportedFault().trim());
        if (request.getDiagnosis() != null) ticket.setDiagnosis(request.getDiagnosis());
        if (request.getQuoteAmount() != null) ticket.setQuoteAmount(nz(request.getQuoteAmount()));
        if (request.getLaborAmount() != null) ticket.setLaborAmount(nz(request.getLaborAmount()));
        if (request.getWarrantyDays() != null) ticket.setWarrantyDays(request.getWarrantyDays());
        if (request.getAssignedTechnicianId() != null) ticket.setAssignedTechnicianId(request.getAssignedTechnicianId());
        if (request.getAssignedTechnicianName() != null) ticket.setAssignedTechnicianName(request.getAssignedTechnicianName());
        if (request.getIsWarrantyClaim() != null) ticket.setIsWarrantyClaim(request.getIsWarrantyClaim());
        if (request.getNote() != null) ticket.setNote(request.getNote());

        if (request.getParts() != null) {
            ticket.getParts().clear();
            applyParts(ticket, request.getParts(), tenantId);
        }
        recomputeAmounts(ticket);

        RepairTicket saved = repairTicketRepository.save(ticket);
        activityLogService.logAsync(tenantId, actor, null, ActivityAction.REPAIR_UPDATED,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                messageService.getMessage("activity.repair.updated", saved.getTicketNumber()), null);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public RepairTicketDTO updateStatus(Long id, UpdateRepairStatusRequest request) {
        RepairTicket ticket = findOrThrow(id);
        String actor = currentUsername();
        String target = request.getStatus() != null ? request.getStatus().trim() : null;
        if (!RepairStatus.exists(target)) {
            throw new BadRequestException(messageService.getMessage("error.repair.status.invalid"));
        }
        if (RepairStatus.TERMINAL.contains(ticket.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.repair.status.terminal"));
        }

        ticket.setStatus(target);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            ticket.setNote(request.getNote().trim());
        }
        LocalDateTime now = LocalDateTime.now();
        ActivityAction action = ActivityAction.REPAIR_STATUS_CHANGED;
        if (RepairStatus.COMPLETED.name().equals(target)) {
            ticket.setCompletedAt(now);
        } else if (RepairStatus.DELIVERED.name().equals(target)) {
            if (ticket.getCompletedAt() == null) ticket.setCompletedAt(now);
            ticket.setDeliveredAt(now);
            action = ActivityAction.REPAIR_DELIVERED;
        } else if (RepairStatus.CANCELLED.name().equals(target)) {
            action = ActivityAction.REPAIR_CANCELLED;
        }

        RepairTicket saved = repairTicketRepository.save(ticket);
        activityLogService.logAsync(saved.getTenantId(), currentUsername(), null, action,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                messageService.getMessage("activity.repair.status", saved.getTicketNumber(),
                        messageService.getMessage("repair.status." + target)), null);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public RepairTicketDTO assignTechnician(Long id, AssignTechnicianRequest request) {
        RepairTicket ticket = findOrThrow(id);
        assertEditable(ticket);
        ticket.setAssignedTechnicianId(request.getTechnicianId());
        ticket.setAssignedTechnicianName(request.getTechnicianName());
        RepairTicket saved = repairTicketRepository.save(ticket);
        activityLogService.logAsync(saved.getTenantId(), currentUsername(), null, ActivityAction.REPAIR_UPDATED,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                messageService.getMessage("activity.repair.assigned", saved.getTicketNumber(),
                        request.getTechnicianName() != null ? request.getTechnicianName() : "—"), null);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RepairTicket ticket = findOrThrow(id);
        ticket.softDelete();
        repairTicketRepository.save(ticket);
        activityLogService.logAsync(ticket.getTenantId(), currentUsername(), null, ActivityAction.REPAIR_DELETED,
                "REPAIR_TICKET", String.valueOf(ticket.getId()),
                messageService.getMessage("activity.repair.deleted", ticket.getTicketNumber()), null);
    }

    @Override
    public Map<String, Long> statusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RepairStatus s : RepairStatus.values()) counts.put(s.name(), 0L);
        for (Object[] row : repairTicketRepository.countByStatus(tenantContext.getCurrentTenantId())) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Finds the ticket within tenant scope. When the caller lacks REPAIR_VIEW_ALL,
     * a ticket created by someone else is treated as not-found (404, never 403) so
     * record existence is not leaked.
     */
    private RepairTicket findOrThrow(Long id) {
        RepairTicket ticket = repairTicketRepository
                .findByIdAndTenantIdAndDeletedFalse(id, tenantContext.getCurrentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.repair.not.found", id)));
        if (!featureContext.hasFeature(FEATURE_VIEW_ALL) && !currentUsername().equals(ticket.getCreatedBy())) {
            throw new ResourceNotFoundException(messageService.getMessage("error.repair.not.found", id));
        }
        return ticket;
    }

    private void assertEditable(RepairTicket ticket) {
        if (RepairStatus.TERMINAL.contains(ticket.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.repair.edit.terminal"));
        }
    }

    private void applyParts(RepairTicket ticket, List<RepairPartRequest> parts, String tenantId) {
        if (parts == null) return;
        for (RepairPartRequest p : parts) {
            int qty = p.getQuantity() != null && p.getQuantity() > 0 ? p.getQuantity() : 1;
            BigDecimal unit = nz(p.getUnitPrice());
            RepairPart part = RepairPart.builder()
                    .tenantId(tenantId)
                    .repairTicket(ticket)
                    .productId(p.getProductId())
                    .productName(p.getProductName().trim())
                    .quantity(qty)
                    .unitPrice(unit)
                    .lineTotal(unit.multiply(BigDecimal.valueOf(qty)))
                    .build();
            ticket.getParts().add(part);
        }
    }

    private void recomputeAmounts(RepairTicket ticket) {
        BigDecimal partsAmount = ticket.getParts().stream()
                .map(RepairPart::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ticket.setPartsAmount(partsAmount);
        ticket.setTotalAmount(partsAmount.add(nz(ticket.getLaborAmount())));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String generateNumber(String tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = repairTicketRepository.countTodayByTenantId(tenantId, LocalDate.now());
        return String.format("SC-%s-%03d", dateStr, seq);
    }

    private RepairTicketDTO mapToDTO(RepairTicket t) {
        List<RepairPartDTO> parts = t.getParts() == null ? List.of()
                : t.getParts().stream().map(this::mapPart).collect(Collectors.toList());
        return RepairTicketDTO.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .customerId(t.getCustomerId())
                .customerName(t.getCustomerName())
                .customerPhone(t.getCustomerPhone())
                .deviceType(t.getDeviceType())
                .brand(t.getBrand())
                .model(t.getModel())
                .serialImei(t.getSerialImei())
                .reportedFault(t.getReportedFault())
                .diagnosis(t.getDiagnosis())
                .quoteAmount(t.getQuoteAmount())
                .partsAmount(t.getPartsAmount())
                .laborAmount(t.getLaborAmount())
                .totalAmount(t.getTotalAmount())
                .warrantyDays(t.getWarrantyDays())
                .assignedTechnicianId(t.getAssignedTechnicianId())
                .assignedTechnicianName(t.getAssignedTechnicianName())
                .status(t.getStatus())
                .isWarrantyClaim(t.getIsWarrantyClaim())
                .note(t.getNote())
                .receivedAt(t.getReceivedAt())
                .completedAt(t.getCompletedAt())
                .deliveredAt(t.getDeliveredAt())
                .linkedOrderId(t.getLinkedOrderId())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .parts(parts)
                .build();
    }

    private RepairPartDTO mapPart(RepairPart p) {
        return RepairPartDTO.builder()
                .id(p.getId())
                .productId(p.getProductId())
                .productName(p.getProductName())
                .quantity(p.getQuantity())
                .unitPrice(p.getUnitPrice())
                .lineTotal(p.getLineTotal())
                .build();
    }
}
