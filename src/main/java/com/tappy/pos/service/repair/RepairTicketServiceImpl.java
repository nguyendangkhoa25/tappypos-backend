package com.tappy.pos.service.repair;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.util.MessageArgs;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.repair.*;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.repair.RepairPart;
import com.tappy.pos.model.entity.repair.RepairTicket;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.RepairStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.order.OrderRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

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
                "activity.repair.created", null, saved.getTicketNumber(), saved.getCustomerName());
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
                "activity.repair.updated", null, saved.getTicketNumber());
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

        // §4a — on completion (or first delivery), turn the repair into a POS sale so it
        // flows into REVENUE and the technician's COMMISSION. Idempotent via linkedOrderId.
        boolean billable = RepairStatus.COMPLETED.name().equals(target)
                || RepairStatus.DELIVERED.name().equals(target);
        if (billable && ticket.getLinkedOrderId() == null) {
            Long orderId = createCompletionOrder(ticket, actor);
            if (orderId != null) ticket.setLinkedOrderId(orderId);
        }

        RepairTicket saved = repairTicketRepository.save(ticket);
        // Per-status message key so the status word renders in the reader's locale at read time,
        // instead of freezing the writer's locale into description_args.
        activityLogService.logAsync(saved.getTenantId(), currentUsername(), null, action,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                "activity.repair.status." + target.toLowerCase(), null, saved.getTicketNumber());
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
                "activity.repair.assigned", null, saved.getTicketNumber(),
                request.getTechnicianName() != null ? request.getTechnicianName() : "—");
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
                "activity.repair.deleted", null, ticket.getTicketNumber());
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

    @Override
    public List<RepairTicketDTO> warrantyLookup(String keyword) {
        String tenantId = tenantContext.getCurrentTenantId();
        String normKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String createdByScope = featureContext.hasFeature(FEATURE_VIEW_ALL) ? null : currentUsername();
        LocalDateTime now = LocalDateTime.now();
        return repairTicketRepository.searchDeliveredUnderWarranty(tenantId, normKeyword, createdByScope).stream()
                .filter(t -> warrantyExpiry(t) != null && warrantyExpiry(t).isAfter(now))
                .limit(50)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RepairTicketDTO createWarrantyClaim(Long originalTicketId) {
        RepairTicket original = findOrThrow(originalTicketId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = warrantyExpiry(original);
        if (expiry == null || !expiry.isAfter(now)) {
            throw new BadRequestException(messageService.getMessage("error.repair.warranty.invalid"));
        }
        // findOrThrow already scoped `original` to the current tenant; source the new ticket's
        // tenant from TenantContext directly (consistent with create()).
        String tenantId = tenantContext.getCurrentTenantId();
        String username = currentUsername();

        RepairTicket claim = RepairTicket.builder()
                .tenantId(tenantId)
                .ticketNumber(generateNumber(tenantId))
                .customerId(original.getCustomerId())
                .customerName(original.getCustomerName())
                .customerPhone(original.getCustomerPhone())
                .deviceType(original.getDeviceType())
                .brand(original.getBrand())
                .model(original.getModel())
                .serialImei(original.getSerialImei())
                .reportedFault(messageService.getMessage("repair.warranty.fault.prefix", original.getTicketNumber()))
                .reportedFaultKey("repair.warranty.fault.prefix")
                .reportedFaultArgs(MessageArgs.toJson(original.getTicketNumber()))
                .laborAmount(BigDecimal.ZERO)
                .warrantyDays(0)
                .isWarrantyClaim(true)
                .status(RepairStatus.RECEIVED.name())
                .receivedAt(now)
                .note(messageService.getMessage("repair.warranty.note", original.getTicketNumber()))
                .noteKey("repair.warranty.note")
                .noteArgs(MessageArgs.toJson(original.getTicketNumber()))
                .createdBy(username)
                .parts(new ArrayList<>())
                .build();
        recomputeAmounts(claim);

        RepairTicket saved = repairTicketRepository.save(claim);
        activityLogService.logAsync(tenantId, username, null, ActivityAction.REPAIR_CREATED,
                "REPAIR_TICKET", String.valueOf(saved.getId()),
                "activity.repair.warranty", null, saved.getTicketNumber(), original.getTicketNumber());
        return mapToDTO(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Repair-warranty expiry (deliveredAt + warrantyDays), or null when not applicable. */
    private static LocalDateTime warrantyExpiry(RepairTicket t) {
        if (t.getDeliveredAt() == null || t.getWarrantyDays() == null || t.getWarrantyDays() <= 0) return null;
        return t.getDeliveredAt().plusDays(t.getWarrantyDays());
    }

    /**
     * §4a — builds and persists a COMPLETED SELL order from a finished repair ticket:
     * each charged part is a standard line, labor is a productless line carrying the
     * assigned technician (with their commission rate resolved) so COMMISSION + REVENUE
     * flow exactly as a normal POS sale. Returns the new order id, or null when there is
     * nothing to charge (e.g. a free warranty repair).
     */
    private Long createCompletionOrder(RepairTicket ticket, String actor) {
        BigDecimal total = nz(ticket.getTotalAmount());
        if (total.signum() <= 0) return null;
        String tenantId = ticket.getTenantId();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(Order.OrderType.SELL);
        order.setPaymentMethod("CASH");
        order.setCreatedBy(actor);
        // i18n: store key + args so the note renders in the reader's locale (not frozen at write time).
        order.setNotesKey("repair.order.note");
        order.setNotesArgs(MessageArgs.toJson(ticket.getTicketNumber()));
        order.complete(actor); // → COMPLETED + completedAt + completedBy

        List<OrderItem> items = new ArrayList<>();

        // Parts → standard lines (only positively-priced lines; unitPrice is @Positive).
        for (RepairPart p : ticket.getParts()) {
            BigDecimal unit = nz(p.getUnitPrice());
            if (unit.signum() <= 0) continue;
            int qty = p.getQuantity() != null && p.getQuantity() > 0 ? p.getQuantity() : 1;
            OrderItem oi = new OrderItem();
            oi.setTenantId(tenantId);
            oi.setOrder(order);
            oi.setProductId(p.getProductId());
            oi.setProductName(p.getProductName());
            oi.setQuantity(qty);
            oi.setUnitPrice(unit);
            oi.setAmount(unit.multiply(BigDecimal.valueOf(qty)));
            oi.setStatus(OrderItem.ItemStatus.COMPLETED);
            oi.setCompletedAt(now);
            items.add(oi);
        }

        // Labor → productless line credited to the assigned technician for commission.
        BigDecimal labor = nz(ticket.getLaborAmount());
        if (labor.signum() > 0) {
            OrderItem laborItem = new OrderItem();
            laborItem.setTenantId(tenantId);
            laborItem.setOrder(order);
            laborItem.setProductName(messageService.getMessage("repair.labor.line", ticket.getTicketNumber()));
            laborItem.setQuantity(1);
            laborItem.setUnitPrice(labor);
            laborItem.setAmount(labor);
            laborItem.setStatus(OrderItem.ItemStatus.COMPLETED);
            laborItem.setCompletedAt(now);
            // Credit the assigned technician only when they resolve to an in-tenant employee.
            // employeeRepository.findById runs under the tenant's RLS context, so a stale or
            // cross-tenant technician id simply yields no attribution (no foreign name/commission).
            if (ticket.getAssignedTechnicianId() != null) {
                employeeRepository.findById(ticket.getAssignedTechnicianId()).ifPresent(emp -> {
                    laborItem.setAssignedEmployeeId(emp.getId());
                    laborItem.setAssignedEmployeeName(ticket.getAssignedTechnicianName());
                    BigDecimal rate = emp.getCommissionRate() != null ? emp.getCommissionRate() : BigDecimal.ZERO;
                    laborItem.setCommissionRate(rate);
                    laborItem.setCommissionAmount(labor.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                });
            }
            items.add(laborItem);
        }

        if (items.isEmpty()) return null;

        order.setOrderItems(items);
        order.setTotalAmount(total);
        order.setSellAmount(total);
        order.setAmountPaid(total);
        BigDecimal commissionTotal = items.stream()
                .map(i -> nz(i.getCommissionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setCommissionAmount(commissionTotal);

        Order saved = orderRepository.save(order);
        activityLogService.logAsync(tenantId, actor, null, ActivityAction.ORDER_CREATED,
                "ORDER", saved.getOrderNumber(),
                "activity.repair.order", null, ticket.getTicketNumber(), saved.getOrderNumber());
        return saved.getId();
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "ORD-" + datePart + "-" + seq;
    }


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

    /**
     * Render a fault/note field: prefer the i18n key (in the reader's locale) for system-generated
     * text; fall back to the literal value for user-authored text and pre-V006 rows.
     */
    private String render(String key, String argsJson, String literal) {
        if (key == null || key.isBlank()) {
            return literal;
        }
        try {
            return messageService.getMessage(key, MessageArgs.fromJson(argsJson));
        } catch (Exception e) {
            log.warn("RepairTicket: failed to render key={}: {}", key, e.getMessage());
            return literal != null ? literal : key;
        }
    }

    private RepairTicketDTO mapToDTO(RepairTicket t) {
        List<RepairPartDTO> parts = t.getParts() == null ? List.of()
                : t.getParts().stream().map(this::mapPart).collect(Collectors.toList());
        LocalDateTime warrantyExpiresAt = warrantyExpiry(t);
        boolean underWarranty = warrantyExpiresAt != null && warrantyExpiresAt.isAfter(LocalDateTime.now());
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
                .reportedFault(render(t.getReportedFaultKey(), t.getReportedFaultArgs(), t.getReportedFault()))
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
                .note(render(t.getNoteKey(), t.getNoteArgs(), t.getNote()))
                .receivedAt(t.getReceivedAt())
                .completedAt(t.getCompletedAt())
                .deliveredAt(t.getDeliveredAt())
                .linkedOrderId(t.getLinkedOrderId())
                .warrantyExpiresAt(warrantyExpiresAt)
                .underWarranty(underWarranty)
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
