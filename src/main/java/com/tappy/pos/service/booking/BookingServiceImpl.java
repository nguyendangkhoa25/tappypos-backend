package com.tappy.pos.service.booking;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.BookingResourceDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRequest;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import com.tappy.pos.model.entity.booking.Booking;
import com.tappy.pos.model.entity.booking.BookingResource;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.booking.BookingRepository;
import com.tappy.pos.repository.booking.BookingResourceRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal(60);
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int DEFAULT_SLOT_SECONDS = 60 * 60;   // open-ended reservation defaults to 1h

    private final BookingRepository bookingRepository;
    private final BookingResourceRepository resourceRepository;
    private final OrderRepository orderRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;

    // ── Resources ────────────────────────────────────────────────────────────

    @Override
    public List<BookingResourceDTO> getResources() {
        String tenantId = tenantContext.getCurrentTenantId();
        Map<Long, Booking> activeByResource = bookingRepository.findInProgress(tenantId).stream()
                .collect(Collectors.toMap(Booking::getResourceId, b -> b, (a, b) -> a));
        return resourceRepository.findAllActive(tenantId).stream()
                .map(r -> mapResource(r, activeByResource.get(r.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResourceDTO createResource(BookingResourceRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        BookingResource resource = BookingResource.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .resourceType(orDefault(request.getResourceType(), "TABLE"))
                .hourlyRate(request.getHourlyRate() != null ? request.getHourlyRate() : BigDecimal.ZERO)
                .status(orDefault(request.getStatus(), "ACTIVE"))
                .note(request.getNote())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdBy(currentUsername())
                .build();
        return mapResource(resourceRepository.save(resource), null);
    }

    @Override
    @Transactional
    public BookingResourceDTO updateResource(Long id, BookingResourceRequest request) {
        BookingResource resource = findResourceOrThrow(id);
        if (request.getName() != null) resource.setName(request.getName().trim());
        if (request.getResourceType() != null) resource.setResourceType(request.getResourceType());
        if (request.getHourlyRate() != null) resource.setHourlyRate(request.getHourlyRate());
        if (request.getStatus() != null) resource.setStatus(request.getStatus());
        if (request.getNote() != null) resource.setNote(request.getNote());
        if (request.getSortOrder() != null) resource.setSortOrder(request.getSortOrder());
        BookingResource saved = resourceRepository.save(resource);
        return mapResource(saved, bookingRepository.findActiveByResource(saved.getTenantId(), saved.getId()).orElse(null));
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        BookingResource resource = findResourceOrThrow(id);
        bookingRepository.findActiveByResource(resource.getTenantId(), id).ifPresent(b -> {
            throw new BadRequestException(messageService.getMessage("error.booking.resource.busy"));
        });
        resource.softDelete();
        resourceRepository.save(resource);
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    @Override
    public Page<BookingDTO> getByDate(LocalDate date, String status, Pageable pageable) {
        String tenantId = tenantContext.getCurrentTenantId();
        String statusFilter = (status != null && !status.isBlank()) ? status : null;
        return bookingRepository.findByDate(tenantId, date, statusFilter, pageable).map(this::mapBooking);
    }

    @Override
    public BookingDTO getById(Long id) {
        return mapBooking(findOrThrow(id));
    }

    @Override
    @Transactional
    public BookingDTO create(CreateBookingRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        BookingResource resource = findResourceOrThrow(request.getResourceId());

        boolean walkIn = !"RESERVATION".equalsIgnoreCase(request.getBookingType());

        Booking.BookingBuilder<?, ?> builder = Booking.builder()
                .tenantId(tenantId)
                .bookingNumber(generateNumber(tenantId))
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .hourlyRate(resource.getHourlyRate())
                .note(request.getNote())
                .createdBy(currentUsername());

        if (walkIn) {
            // Only one running session per resource.
            bookingRepository.findActiveByResource(tenantId, resource.getId()).ifPresent(b -> {
                throw new BadRequestException(messageService.getMessage("error.booking.resource.busy"));
            });
            builder.bookingType("WALK_IN").status("IN_PROGRESS").startedAt(LocalDateTime.now());
        } else {
            if (request.getScheduledDate() == null || request.getScheduledStartTime() == null) {
                throw new BadRequestException(messageService.getMessage("error.booking.reservation.requires.slot"));
            }
            assertNoReservationOverlap(tenantId, resource.getId(), request, -1L);
            builder.bookingType("RESERVATION").status("RESERVED")
                    .scheduledDate(request.getScheduledDate())
                    .scheduledStartTime(request.getScheduledStartTime())
                    .scheduledEndTime(request.getScheduledEndTime());
        }

        // The findActiveByResource / overlap pre-checks above can't prevent a race between
        // two concurrent creates — the DB unique indexes are the real guard. Translate their
        // violations into the same friendly messages instead of a generic 409/500.
        Booking saved = persistDetectingConflicts(builder.build());
        log.info("Booking created: {} resource={} type={}", saved.getBookingNumber(), resource.getName(), saved.getBookingType());
        return mapBooking(saved);
    }

    @Override
    @Transactional
    public BookingDTO checkIn(Long id) {
        Booking booking = findOrThrow(id);
        if (!"RESERVED".equals(booking.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.booking.invalid.status", booking.getStatus()));
        }
        bookingRepository.findActiveByResource(booking.getTenantId(), booking.getResourceId()).ifPresent(b -> {
            throw new BadRequestException(messageService.getMessage("error.booking.resource.busy"));
        });
        booking.setStatus("IN_PROGRESS");
        booking.setStartedAt(LocalDateTime.now());
        // Concurrent check-in (or a walk-in) on the same resource races past the pre-check;
        // the one-active-per-resource index catches it → friendly "busy" instead of a 409/500.
        return mapBooking(persistDetectingConflicts(booking));
    }

    @Override
    @Transactional
    public BookingDTO checkout(Long id) {
        Booking booking = findOrThrow(id);
        if (!"IN_PROGRESS".equals(booking.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.booking.invalid.status", booking.getStatus()));
        }
        LocalDateTime endedAt = LocalDateTime.now();
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(booking.getStartedAt(), endedAt));
        BigDecimal amount = booking.getHourlyRate()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.HALF_UP);

        booking.setEndedAt(endedAt);
        booking.setDurationMinutes((int) minutes);
        booking.setTimeAmount(amount);
        booking.setStatus("COMPLETED");

        Long orderId = createTimeChargeOrder(booking, amount);
        booking.setLinkedOrderId(orderId);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking checked out: {} minutes={} amount={} order={}", saved.getBookingNumber(), minutes, amount, orderId);
        return mapBooking(saved);
    }

    @Override
    @Transactional
    public BookingDTO cancel(Long id) {
        Booking booking = findOrThrow(id);
        if ("COMPLETED".equals(booking.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.booking.invalid.status", booking.getStatus()));
        }
        booking.setStatus("CANCELLED");
        return mapBooking(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDTO noShow(Long id) {
        Booking booking = findOrThrow(id);
        if (!"RESERVED".equals(booking.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.booking.invalid.status", booking.getStatus()));
        }
        booking.setStatus("NO_SHOW");
        return mapBooking(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Booking booking = findOrThrow(id);
        booking.softDelete();
        bookingRepository.save(booking);
    }

    // ── Order creation on checkout ───────────────────────────────────────────

    /**
     * Creates a PENDING POS order with the time charge as a single product-less
     * line. Staff can then add drinks/food via the normal order endpoints and
     * collect payment through POS. Returns the new order id.
     */
    private Long createTimeChargeOrder(Booking booking, BigDecimal amount) {
        String tenantId = booking.getTenantId();
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(amount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxPercentage(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setSource("BOOKING");
        order.setCreatedBy(currentUsername());
        order.setTableLabel(booking.getResourceName());
        order.setNotes(booking.getNote());

        OrderItem item = new OrderItem();
        item.setTenantId(tenantId);
        item.setOrder(order);
        item.setProductId(null);
        item.setProductName(messageService.getMessage("booking.timeCharge.label") + " - " + booking.getResourceName());
        item.setQuantity(1);
        item.setUnitPrice(amount);
        item.setUnitCost(BigDecimal.ZERO);
        item.setItemType(OrderItem.ItemType.STANDARD);
        item.setStatus(OrderItem.ItemStatus.PENDING);
        order.setOrderItems(new java.util.ArrayList<>(List.of(item)));

        return orderRepository.save(order).getId();
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(ORDER_DATE_FMT) + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertNoReservationOverlap(String tenantId, Long resourceId, CreateBookingRequest req, long excludeId) {
        int[] slot = slotSeconds(req.getScheduledStartTime(), req.getScheduledEndTime());
        for (Booking existing : bookingRepository.findReservationsByResourceAndDate(
                tenantId, resourceId, req.getScheduledDate(), excludeId)) {
            int[] ex = slotSeconds(existing.getScheduledStartTime(), existing.getScheduledEndTime());
            boolean overlaps = slot[0] < ex[1] && ex[0] < slot[1];   // half-open [start, end) intervals
            if (overlaps) {
                throw new BadRequestException(messageService.getMessage("error.booking.reservation.overlap"));
            }
        }
    }

    /**
     * The reservation's half-open occupancy window as [startSec, endSec) within the day.
     * An open-ended slot defaults to a 1-hour duration; the end is computed in seconds-of-day
     * and clamped to end-of-day so it never wraps past midnight (e.g. a 23:30 start becomes
     * [84600, 86400), not [84600, 1800)) — the old LocalTime.plusHours(1) wrap silently broke
     * overlap detection for late-night slots. A degenerate explicit end (≤ start) is treated as
     * running to end-of-day so overlap detection stays conservative rather than missing conflicts.
     */
    private static int[] slotSeconds(LocalTime start, LocalTime end) {
        int startSec = start.toSecondOfDay();
        int endSec = (end != null) ? end.toSecondOfDay() : Math.min(startSec + DEFAULT_SLOT_SECONDS, SECONDS_PER_DAY);
        if (endSec <= startSec) {
            endSec = SECONDS_PER_DAY;
        }
        return new int[]{startSec, endSec};
    }

    private BookingResource findResourceOrThrow(Long id) {
        return resourceRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantContext.getCurrentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.bookingResource.not.found", id)));
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantContext.getCurrentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.booking.not.found", id)));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String generateNumber(String tenantId) {
        String dateStr = LocalDate.now().format(ORDER_DATE_FMT);
        long seq = bookingRepository.countTodayByTenantId(tenantId, LocalDate.now());
        return String.format("BKG-%s-%03d", dateStr, seq);
    }

    /**
     * Saves a booking and flushes immediately so any unique-index violation surfaces here
     * (rather than at commit, after this method returns) and can be turned into a friendly,
     * specific message. Two races the in-Java pre-checks can't close:
     *  - idx_bookings_one_active_per_resource → two staff started the same table at once.
     *  - idx_bookings_number → two creates picked the same daily number; transient, retryable.
     * Any other integrity violation is rethrown for the generic handler.
     */
    private Booking persistDetectingConflicts(Booking booking) {
        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            if (mentionsConstraint(ex, "idx_bookings_one_active_per_resource")) {
                throw new BadRequestException(messageService.getMessage("error.booking.resource.busy"));
            }
            if (mentionsConstraint(ex, "idx_bookings_number")) {
                throw new BadRequestException(messageService.getMessage("error.booking.conflict.retry"));
            }
            throw ex;
        }
    }

    /** True when the Postgres error names the given unique index/constraint. */
    private boolean mentionsConstraint(DataIntegrityViolationException ex, String name) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause != null ? cause.getMessage() : ex.getMessage();
        return msg != null && msg.contains(name);
    }

    private static String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private BookingResourceDTO mapResource(BookingResource r, Booking active) {
        return BookingResourceDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .resourceType(r.getResourceType())
                .hourlyRate(r.getHourlyRate())
                .status(r.getStatus())
                .note(r.getNote())
                .sortOrder(r.getSortOrder())
                .activeBooking(active != null ? mapBooking(active) : null)
                .build();
    }

    private BookingDTO mapBooking(Booking b) {
        Long elapsed = null;
        if ("IN_PROGRESS".equals(b.getStatus()) && b.getStartedAt() != null) {
            elapsed = Math.max(0, ChronoUnit.MINUTES.between(b.getStartedAt(), LocalDateTime.now()));
        }
        return BookingDTO.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .resourceId(b.getResourceId())
                .resourceName(b.getResourceName())
                .customerId(b.getCustomerId())
                .customerName(b.getCustomerName())
                .customerPhone(b.getCustomerPhone())
                .bookingType(b.getBookingType())
                .scheduledDate(b.getScheduledDate())
                .scheduledStartTime(b.getScheduledStartTime())
                .scheduledEndTime(b.getScheduledEndTime())
                .startedAt(b.getStartedAt())
                .endedAt(b.getEndedAt())
                .durationMinutes(b.getDurationMinutes())
                .hourlyRate(b.getHourlyRate())
                .timeAmount(b.getTimeAmount())
                .status(b.getStatus())
                .note(b.getNote())
                .linkedOrderId(b.getLinkedOrderId())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .elapsedMinutes(elapsed)
                .build();
    }
}
