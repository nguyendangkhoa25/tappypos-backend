package com.tappy.pos.service.room;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.room.*;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.room.RoomEntity;
import com.tappy.pos.model.entity.room.RoomRequestEntity;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.model.entity.room.RoomStayItemEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomRequestRepository;
import com.tappy.pos.repository.room.RoomStayItemRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> BILLING_MODES = Set.of("NIGHTLY", "HOURLY", "OVERNIGHT");
    private static final Set<String> ROOM_STATUSES = Set.of("AVAILABLE", "OCCUPIED", "RESERVED", "DIRTY", "OOO");
    private static final Set<String> ROOM_REQUEST_STATUSES = Set.of("NEW", "IN_PROGRESS", "DONE", "CANCELLED");

    private final RoomRepository roomRepository;
    private final RoomStayRepository stayRepository;
    private final RoomStayItemRepository itemRepository;
    private final RoomRequestRepository requestRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;

    // ── Rooms / board ──────────────────────────────────────────────────────────

    @Override
    public List<RoomDTO> getBoard() {
        return roomRepository.findByDeletedFalseOrderBySortOrderAscRoomNumberAsc().stream()
                .map(room -> {
                    RoomStayEntity stay = "OCCUPIED".equals(room.getStatus())
                            ? stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(room.getId(), "IN_HOUSE").orElse(null)
                            : null;
                    return mapRoom(room, stay != null ? mapStay(stay, false) : null);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomDTO createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByRoomNumberAndDeletedFalse(request.getRoomNumber().trim())) {
            throw new BadRequestException(messageService.getMessage("error.room.number.exists", request.getRoomNumber()));
        }
        RoomEntity room = RoomEntity.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .roomNumber(request.getRoomNumber().trim())
                .roomType(request.getRoomType())
                .floor(request.getFloor())
                .nightlyRate(orZero(request.getNightlyRate()))
                .hourlyRate(request.getHourlyRate())
                .overnightRate(request.getOvernightRate())
                .maxOccupancy(request.getMaxOccupancy() != null ? request.getMaxOccupancy() : 2)
                .status("AVAILABLE")
                .note(request.getNote())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return mapRoom(roomRepository.save(room), null);
    }

    @Override
    @Transactional
    public RoomDTO updateRoom(Long id, CreateRoomRequest request) {
        RoomEntity room = findRoomOrThrow(id);
        if (request.getRoomNumber() != null) room.setRoomNumber(request.getRoomNumber().trim());
        if (request.getRoomType() != null) room.setRoomType(request.getRoomType());
        if (request.getFloor() != null) room.setFloor(request.getFloor());
        if (request.getNightlyRate() != null) room.setNightlyRate(request.getNightlyRate());
        if (request.getHourlyRate() != null) room.setHourlyRate(request.getHourlyRate());
        if (request.getOvernightRate() != null) room.setOvernightRate(request.getOvernightRate());
        if (request.getMaxOccupancy() != null) room.setMaxOccupancy(request.getMaxOccupancy());
        if (request.getNote() != null) room.setNote(request.getNote());
        if (request.getSortOrder() != null) room.setSortOrder(request.getSortOrder());
        return mapRoom(roomRepository.save(room), null);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        RoomEntity room = findRoomOrThrow(id);
        if ("OCCUPIED".equals(room.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.room.occupied"));
        }
        room.softDelete();
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public RoomDTO setRoomStatus(Long id, String status) {
        RoomEntity room = findRoomOrThrow(id);
        String next = status != null ? status.toUpperCase() : "";
        if (!ROOM_STATUSES.contains(next)) {
            throw new BadRequestException(messageService.getMessage("error.room.status.invalid", status));
        }
        // Housekeeping only flips a free room between AVAILABLE / DIRTY / OOO / RESERVED.
        // Occupancy (OCCUPIED) is owned by the stay lifecycle, never by a manual status set.
        if ("OCCUPIED".equals(room.getStatus()) || "OCCUPIED".equals(next)) {
            throw new BadRequestException(messageService.getMessage("error.room.status.occupied.locked"));
        }
        room.setStatus(next);
        return mapRoom(roomRepository.save(room), null);
    }

    // ── QR + reception inbox ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoomQrDTO ensureQrToken(Long roomId) {
        RoomEntity room = findRoomOrThrow(roomId);
        if (room.getQrToken() == null || room.getQrToken().isBlank()) {
            room.setQrToken(java.util.UUID.randomUUID().toString());
            room = roomRepository.save(room);
        }
        return RoomQrDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .qrToken(room.getQrToken())
                .guestPath("/qr-room/" + tenantContext.getCurrentTenantId() + "/" + room.getQrToken())
                .build();
    }

    @Override
    public Page<RoomRequestDTO> listRequests(String status, Pageable pageable) {
        Page<RoomRequestEntity> page = (status != null && !status.isBlank())
                ? requestRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status, pageable)
                : requestRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable);
        return page.map(this::mapRequest);
    }

    @Override
    public long countNewRequests() {
        return requestRepository.countByStatusAndDeletedFalse("NEW");
    }

    @Override
    @Transactional
    public RoomRequestDTO updateRequestStatus(Long requestId, String status) {
        RoomRequestEntity req = requestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.room.request.not.found", requestId)));
        String next = status != null ? status.toUpperCase() : "";
        if (!ROOM_REQUEST_STATUSES.contains(next)) {
            throw new BadRequestException(messageService.getMessage("error.room.request.status.invalid", status));
        }
        req.setStatus(next);
        if ("DONE".equals(next) || "IN_PROGRESS".equals(next)) {
            req.setHandledBy(currentUsername());
            if ("DONE".equals(next)) req.setHandledAt(LocalDateTime.now());
        }
        return mapRequest(requestRepository.save(req));
    }

    // ── Stays ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoomStayDTO checkIn(CheckInRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        RoomEntity room = findRoomOrThrow(request.getRoomId());
        if (!"AVAILABLE".equals(room.getStatus()) && !"RESERVED".equals(room.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.room.not.available", room.getRoomNumber()));
        }

        String billingMode = normalizeBillingMode(request.getBillingMode());
        BigDecimal rate = request.getRate() != null ? request.getRate() : rateForMode(room, billingMode);

        RoomStayEntity stay = RoomStayEntity.builder()
                .tenantId(tenantId)
                .stayNumber(generateStayNumber())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .guestName(request.getGuestName())
                .guestPhone(request.getGuestPhone())
                .guestIdNumber(request.getGuestIdNumber())
                .customerId(request.getCustomerId())
                .adults(request.getAdults() != null ? request.getAdults() : 1)
                .billingMode(billingMode)
                .rate(rate)
                .checkinAt(LocalDateTime.now())
                .expectedCheckout(request.getExpectedCheckout())
                .deposit(orZero(request.getDeposit()))
                .status("IN_HOUSE")
                .note(request.getNote())
                .createdBy(currentUsername())
                .build();
        RoomStayEntity saved = stayRepository.save(stay);

        room.setStatus("OCCUPIED");
        roomRepository.save(room);

        activityLogService.logAsync(tenantId, currentUsername(), null, ActivityAction.ROOM_CHECKIN,
                "ROOM_STAY", String.valueOf(saved.getId()),
                messageService.getMessage("activity.room.checkin", room.getRoomNumber()), null);
        log.info("Room check-in: stay={} room={} mode={}", saved.getStayNumber(), room.getRoomNumber(), billingMode);
        return mapStay(saved, true);
    }

    @Override
    public RoomStayDTO getStay(Long stayId) {
        return mapStay(findStayOrThrow(stayId), true);
    }

    @Override
    public Page<RoomStayDTO> listStays(String status, Pageable pageable) {
        Page<RoomStayEntity> page = (status != null && !status.isBlank())
                ? stayRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status, pageable)
                : stayRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable);
        return page.map(s -> mapStay(s, false));
    }

    // ── Folio ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoomStayItemDTO addFolioItem(Long stayId, AddFolioItemRequest request) {
        RoomStayEntity stay = findStayOrThrow(stayId);
        assertInHouse(stay);

        String name;
        BigDecimal unitPrice;
        Long productId = request.getProductId();
        if (productId != null) {
            Product product = productRepository.findByIdAndDeletedFalse(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.product.not.found", productId)));
            name = product.getName();
            unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : orZero(product.getPrice());
        } else {
            if (request.getProductName() == null || request.getProductName().isBlank()) {
                throw new BadRequestException(messageService.getMessage("error.room.folio.item.required"));
            }
            name = request.getProductName().trim();
            unitPrice = orZero(request.getUnitPrice());
        }

        RoomStayItemEntity item = RoomStayItemEntity.builder()
                .tenantId(stay.getTenantId())
                .stayId(stay.getId())
                .productId(productId)
                .productName(name)
                .quantity(request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1)
                .unitPrice(unitPrice)
                .source("QR".equalsIgnoreCase(request.getSource()) ? "QR" : "STAFF")
                .note(request.getNote())
                .createdBy(currentUsername())
                .build();
        return mapItem(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeFolioItem(Long stayId, Long itemId) {
        RoomStayEntity stay = findStayOrThrow(stayId);
        assertInHouse(stay);
        RoomStayItemEntity item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.room.folio.item.not.found", itemId)));
        if (!item.getStayId().equals(stay.getId())) {
            throw new ResourceNotFoundException(messageService.getMessage("error.room.folio.item.not.found", itemId));
        }
        item.softDelete();
        itemRepository.save(item);
    }

    // ── Checkout / cancel ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoomStayDTO checkout(Long stayId, CheckoutRequest request) {
        RoomStayEntity stay = findStayOrThrow(stayId);
        assertInHouse(stay);

        LocalDateTime checkoutAt = LocalDateTime.now();
        int units = request != null && request.getUnits() != null && request.getUnits() > 0
                ? request.getUnits()
                : computeUnits(stay, checkoutAt);
        BigDecimal roomCharge = stay.getRate().multiply(BigDecimal.valueOf(units));

        List<RoomStayItemEntity> folio = itemRepository.findByStayIdAndDeletedFalseOrderByCreatedAtAsc(stay.getId());
        BigDecimal itemsTotal = folio.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = roomCharge.add(itemsTotal);

        Long orderId = createSettlementOrder(stay, roomCharge, folio, grandTotal,
                request != null ? request.getPaymentMethod() : null);

        stay.setCheckoutAt(checkoutAt);
        stay.setUnits(units);
        stay.setRoomCharge(roomCharge);
        stay.setStatus("CHECKED_OUT");
        stay.setLinkedOrderId(orderId);
        if (request != null && request.getNote() != null) stay.setNote(request.getNote());
        RoomStayEntity saved = stayRepository.save(stay);

        // Room needs cleaning before it can be re-let.
        roomRepository.findByIdAndDeletedFalse(stay.getRoomId()).ifPresent(room -> {
            room.setStatus("DIRTY");
            roomRepository.save(room);
        });

        activityLogService.logAsync(stay.getTenantId(), currentUsername(), null, ActivityAction.ROOM_CHECKOUT,
                "ROOM_STAY", String.valueOf(saved.getId()),
                messageService.getMessage("activity.room.checkout", stay.getRoomNumber(), grandTotal.toPlainString()), null);
        log.info("Room checkout: stay={} units={} roomCharge={} grandTotal={} order={}",
                saved.getStayNumber(), units, roomCharge, grandTotal, orderId);
        return mapStay(saved, true);
    }

    @Override
    @Transactional
    public RoomStayDTO cancelStay(Long stayId) {
        RoomStayEntity stay = findStayOrThrow(stayId);
        assertInHouse(stay);
        stay.setStatus("CANCELLED");
        stay.setCheckoutAt(LocalDateTime.now());
        RoomStayEntity saved = stayRepository.save(stay);

        roomRepository.findByIdAndDeletedFalse(stay.getRoomId()).ifPresent(room -> {
            room.setStatus("AVAILABLE");
            roomRepository.save(room);
        });

        activityLogService.logAsync(stay.getTenantId(), currentUsername(), null, ActivityAction.ROOM_STAY_CANCELLED,
                "ROOM_STAY", String.valueOf(saved.getId()),
                messageService.getMessage("activity.room.cancelled", stay.getRoomNumber()), null);
        return mapStay(saved, true);
    }

    // ── Order creation on checkout ───────────────────────────────────────────────

    /**
     * Builds a COMPLETED POS order settling the stay: one room-charge line plus a line per
     * folio item. Links back to the stay via {@code roomStayId} and is tagged source=ROOM so it
     * never collides with normal POS orders. Mirrors {@code BookingServiceImpl.createTimeChargeOrder}.
     */
    private Long createSettlementOrder(RoomStayEntity stay, BigDecimal roomCharge,
                                       List<RoomStayItemEntity> folio, BigDecimal grandTotal,
                                       String paymentMethod) {
        String tenantId = stay.getTenantId();
        String actor = currentUsername();

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setTotalAmount(grandTotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxPercentage(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setSource("ROOM");
        order.setRoomStayId(stay.getId());
        order.setPaymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "CASH");
        order.setAmountPaid(grandTotal);
        order.setCreatedBy(actor);
        order.setCompletedBy(actor);
        order.setCompletedAt(LocalDateTime.now());
        order.setTableLabel(messageService.getMessage("room.label", stay.getRoomNumber()));
        order.setNotes(stay.getNote());

        List<OrderItem> items = new ArrayList<>();
        items.add(buildOrderItem(tenantId, order, null,
                messageService.getMessage("room.charge.label", stay.getRoomNumber()), 1, roomCharge));
        for (RoomStayItemEntity f : folio) {
            items.add(buildOrderItem(tenantId, order, f.getProductId(), f.getProductName(), f.getQuantity(), f.getUnitPrice()));
        }
        order.setOrderItems(items);

        return orderRepository.save(order).getId();
    }

    private OrderItem buildOrderItem(String tenantId, Order order, Long productId,
                                     String name, Integer quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.setTenantId(tenantId);
        item.setOrder(order);
        item.setProductId(productId);
        item.setProductName(name);
        item.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        item.setUnitPrice(unitPrice);
        item.setUnitCost(BigDecimal.ZERO);
        item.setItemType(OrderItem.ItemType.STANDARD);
        item.setStatus(OrderItem.ItemStatus.PENDING);
        return item;
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(ORDER_DATE_FMT) + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    private String generateStayNumber() {
        return "STY-" + LocalDate.now().format(ORDER_DATE_FMT) + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Nights/hours billed when not overridden at checkout. */
    private int computeUnits(RoomStayEntity stay, LocalDateTime checkoutAt) {
        switch (stay.getBillingMode()) {
            case "HOURLY": {
                long minutes = ChronoUnit.MINUTES.between(stay.getCheckinAt(), checkoutAt);
                return (int) Math.max(1, (minutes + 59) / 60);   // round up to the next hour
            }
            case "OVERNIGHT":
                return 1;
            case "NIGHTLY":
            default: {
                long nights = ChronoUnit.DAYS.between(stay.getCheckinAt().toLocalDate(), checkoutAt.toLocalDate());
                return (int) Math.max(1, nights);                // same-day checkout still bills 1 night
            }
        }
    }

    private BigDecimal rateForMode(RoomEntity room, String mode) {
        BigDecimal rate = switch (mode) {
            case "HOURLY" -> room.getHourlyRate();
            case "OVERNIGHT" -> room.getOvernightRate();
            default -> room.getNightlyRate();
        };
        return orZero(rate);
    }

    private String normalizeBillingMode(String mode) {
        if (mode == null || mode.isBlank()) return "NIGHTLY";
        String upper = mode.toUpperCase();
        if (!BILLING_MODES.contains(upper)) {
            throw new BadRequestException(messageService.getMessage("error.room.billing.mode.invalid", mode));
        }
        return upper;
    }

    private void assertInHouse(RoomStayEntity stay) {
        if (!"IN_HOUSE".equals(stay.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.room.stay.not.active", stay.getStatus()));
        }
    }

    private RoomEntity findRoomOrThrow(Long id) {
        return roomRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.room.not.found", id)));
    }

    private RoomStayEntity findStayOrThrow(Long id) {
        return stayRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.room.stay.not.found", id)));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private RoomDTO mapRoom(RoomEntity r, RoomStayDTO activeStay) {
        return RoomDTO.builder()
                .id(r.getId())
                .roomNumber(r.getRoomNumber())
                .roomType(r.getRoomType())
                .floor(r.getFloor())
                .nightlyRate(r.getNightlyRate())
                .hourlyRate(r.getHourlyRate())
                .overnightRate(r.getOvernightRate())
                .maxOccupancy(r.getMaxOccupancy())
                .status(r.getStatus())
                .qrToken(r.getQrToken())
                .note(r.getNote())
                .sortOrder(r.getSortOrder())
                .activeStay(activeStay)
                .build();
    }

    private RoomStayDTO mapStay(RoomStayEntity s, boolean withFolio) {
        RoomStayDTO.RoomStayDTOBuilder b = RoomStayDTO.builder()
                .id(s.getId())
                .stayNumber(s.getStayNumber())
                .roomId(s.getRoomId())
                .roomNumber(s.getRoomNumber())
                .guestName(s.getGuestName())
                .guestPhone(s.getGuestPhone())
                .guestIdNumber(s.getGuestIdNumber())
                .customerId(s.getCustomerId())
                .adults(s.getAdults())
                .billingMode(s.getBillingMode())
                .rate(s.getRate())
                .checkinAt(s.getCheckinAt())
                .expectedCheckout(s.getExpectedCheckout())
                .checkoutAt(s.getCheckoutAt())
                .deposit(s.getDeposit())
                .status(s.getStatus())
                .linkedOrderId(s.getLinkedOrderId())
                .note(s.getNote());

        // Room charge to date: live estimate while in-house, final stored value once checked out.
        boolean inHouse = "IN_HOUSE".equals(s.getStatus());
        int units = inHouse ? computeUnits(s, LocalDateTime.now()) : s.getUnits();
        BigDecimal roomCharge = inHouse
                ? s.getRate().multiply(BigDecimal.valueOf(units))
                : orZero(s.getRoomCharge());
        b.units(units).roomCharge(roomCharge);

        if (withFolio) {
            List<RoomStayItemEntity> folio = itemRepository.findByStayIdAndDeletedFalseOrderByCreatedAtAsc(s.getId());
            List<RoomStayItemDTO> items = folio.stream().map(this::mapItem).collect(Collectors.toList());
            BigDecimal itemsTotal = items.stream().map(RoomStayItemDTO::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandTotal = roomCharge.add(itemsTotal);
            b.items(items)
                    .itemsTotal(itemsTotal)
                    .grandTotal(grandTotal)
                    .balanceDue(grandTotal.subtract(orZero(s.getDeposit())));
        }
        return b.build();
    }

    private RoomRequestDTO mapRequest(RoomRequestEntity r) {
        return RoomRequestDTO.builder()
                .id(r.getId())
                .roomId(r.getRoomId())
                .roomNumber(r.getRoomNumber())
                .stayId(r.getStayId())
                .requestType(r.getRequestType())
                .message(r.getMessage())
                .status(r.getStatus())
                .handledBy(r.getHandledBy())
                .handledAt(r.getHandledAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private RoomStayItemDTO mapItem(RoomStayItemEntity i) {
        return RoomStayItemDTO.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .source(i.getSource())
                .note(i.getNote())
                .build();
    }
}
