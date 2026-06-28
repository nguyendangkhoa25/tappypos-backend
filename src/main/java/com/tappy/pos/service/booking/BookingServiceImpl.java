package com.tappy.pos.service.booking;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.BookingResourceDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRateDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRequest;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import com.tappy.pos.model.entity.booking.Booking;
import com.tappy.pos.model.entity.booking.BookingResource;
import com.tappy.pos.model.entity.booking.BookingResourceRate;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.repository.booking.BookingRepository;
import com.tappy.pos.repository.booking.BookingResourceRateRepository;
import com.tappy.pos.repository.booking.BookingResourceRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final int MAX_RECURRENCE_OCCURRENCES = 52;   // cap weekly materialization at ~1 year

    private final BookingRepository bookingRepository;
    private final BookingResourceRepository resourceRepository;
    private final BookingResourceRateRepository rateRepository;
    private final OrderRepository orderRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final AuthContext authContext;

    // ── Resources ────────────────────────────────────────────────────────────

    @Override
    public List<BookingResourceDTO> getResources() {
        String tenantId = tenantContext.getCurrentTenantId();
        Map<Long, Booking> activeByResource = bookingRepository.findInProgress(tenantId).stream()
                .collect(Collectors.toMap(Booking::getResourceId, b -> b, (a, b) -> a));
        Map<Long, List<BookingResourceRate>> ratesByResource = rateRepository.findAllActive(tenantId).stream()
                .collect(Collectors.groupingBy(BookingResourceRate::getResourceId));
        return resourceRepository.findAllActive(tenantId).stream()
                .map(r -> mapResource(r, activeByResource.get(r.getId()),
                        ratesByResource.getOrDefault(r.getId(), List.of())))
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
                .minimumCharge(request.getMinimumCharge() != null ? request.getMinimumCharge() : BigDecimal.ZERO)
                .status(orDefault(request.getStatus(), "ACTIVE"))
                .note(request.getNote())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdBy(currentUsername())
                .build();
        BookingResource saved = resourceRepository.save(resource);
        List<BookingResourceRate> rates = replaceRates(saved, request.getRates());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_RESOURCE_CREATED, "BOOKING_RESOURCE", String.valueOf(saved.getId()),
                "activity.booking.resource.created", null);
        return mapResource(saved, null, rates);
    }

    @Override
    @Transactional
    public BookingResourceDTO updateResource(Long id, BookingResourceRequest request) {
        BookingResource resource = findResourceOrThrow(id);
        if (request.getName() != null) resource.setName(request.getName().trim());
        if (request.getResourceType() != null) resource.setResourceType(request.getResourceType());
        if (request.getHourlyRate() != null) resource.setHourlyRate(request.getHourlyRate());
        if (request.getMinimumCharge() != null) resource.setMinimumCharge(request.getMinimumCharge());
        if (request.getStatus() != null) resource.setStatus(request.getStatus());
        if (request.getNote() != null) resource.setNote(request.getNote());
        if (request.getSortOrder() != null) resource.setSortOrder(request.getSortOrder());
        BookingResource saved = resourceRepository.save(resource);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_RESOURCE_UPDATED, "BOOKING_RESOURCE", String.valueOf(saved.getId()),
                "activity.booking.resource.updated", null);
        // null = leave windows unchanged; non-null = full replace.
        List<BookingResourceRate> rates = (request.getRates() != null)
                ? replaceRates(saved, request.getRates())
                : rateRepository.findByResource(saved.getTenantId(), saved.getId());
        return mapResource(saved, bookingRepository.findActiveByResource(saved.getTenantId(), saved.getId()).orElse(null), rates);
    }

    /** Replace all rate windows of a resource with the given list; returns the persisted windows. */
    private List<BookingResourceRate> replaceRates(BookingResource resource, List<BookingResourceRateDTO> rateDtos) {
        rateRepository.softDeleteByResource(resource.getTenantId(), resource.getId());
        if (rateDtos == null || rateDtos.isEmpty()) return List.of();
        List<BookingResourceRate> saved = new ArrayList<>();
        int order = 0;
        for (BookingResourceRateDTO dto : rateDtos) {
            if (dto.getStartTime() == null || dto.getEndTime() == null || dto.getRate() == null) continue;
            BookingResourceRate rate = BookingResourceRate.builder()
                    .tenantId(resource.getTenantId())
                    .resourceId(resource.getId())
                    .dayKind(orDefault(dto.getDayKind(), "ALL"))
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .rate(dto.getRate())
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++)
                    .createdBy(currentUsername())
                    .build();
            saved.add(rateRepository.save(rate));
        }
        return saved;
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
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_RESOURCE_DELETED, "BOOKING_RESOURCE", String.valueOf(id),
                "activity.booking.resource.deleted", null);
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

        BigDecimal deposit = request.getDepositAmount() != null ? request.getDepositAmount() : BigDecimal.ZERO;
        boolean depositPaid = Boolean.TRUE.equals(request.getDepositPaid());

        if (walkIn) {
            // Only one running session per resource.
            bookingRepository.findActiveByResource(tenantId, resource.getId()).ifPresent(b -> {
                throw new BadRequestException(messageService.getMessage("error.booking.resource.busy"));
            });
            Booking booking = baseBuilder(tenantId, resource, request, deposit, depositPaid)
                    .bookingType("WALK_IN").status("IN_PROGRESS").startedAt(LocalDateTime.now())
                    .build();
            // The pre-check can't prevent a race between two concurrent creates — the DB unique
            // index is the real guard; translate its violation into the friendly "busy" message.
            Booking saved = persistDetectingConflicts(booking);
            log.info("Booking created: {} resource={} type=WALK_IN", saved.getBookingNumber(), resource.getName());
            activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                    ActivityAction.BOOKING_CREATED, "BOOKING", String.valueOf(saved.getId()),
                    "activity.booking.created", null);
            return mapBooking(saved);
        }

        // RESERVATION (possibly recurring weekly — "sân cố định").
        if (request.getScheduledDate() == null || request.getScheduledStartTime() == null) {
            throw new BadRequestException(messageService.getMessage("error.booking.reservation.requires.slot"));
        }

        boolean recurring = Boolean.TRUE.equals(request.getRecurrenceWeekly());
        int occurrences = recurring
                ? Math.min(Math.max(request.getRecurrenceCount() != null ? request.getRecurrenceCount() : 1, 1), MAX_RECURRENCE_OCCURRENCES)
                : 1;
        String recurrenceGroupId = (recurring && occurrences > 1) ? UUID.randomUUID().toString() : null;

        Booking first = null;
        for (int i = 0; i < occurrences; i++) {
            LocalDate slotDate = request.getScheduledDate().plusWeeks(i);
            // Overlap is checked per materialized slot; a conflict on any week aborts the whole series.
            assertNoReservationOverlap(tenantId, resource.getId(), slotDate,
                    request.getScheduledStartTime(), request.getScheduledEndTime(), -1L);
            Booking booking = baseBuilder(tenantId, resource, request, deposit, depositPaid)
                    .bookingType("RESERVATION").status("RESERVED")
                    .scheduledDate(slotDate)
                    .scheduledStartTime(request.getScheduledStartTime())
                    .scheduledEndTime(request.getScheduledEndTime())
                    .recurrenceGroupId(recurrenceGroupId)
                    .build();
            Booking saved = persistDetectingConflicts(booking);
            if (first == null) first = saved;
        }
        log.info("Reservation created: {} resource={} occurrences={}", first.getBookingNumber(), resource.getName(), occurrences);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_CREATED, "BOOKING", String.valueOf(first.getId()),
                "activity.booking.created", null);
        return mapBooking(first);
    }

    /** Shared builder for a new booking — common identity, customer, rate and deposit fields. */
    private Booking.BookingBuilder<?, ?> baseBuilder(String tenantId, BookingResource resource,
                                                     CreateBookingRequest request,
                                                     BigDecimal deposit, boolean depositPaid) {
        return Booking.builder()
                .tenantId(tenantId)
                .bookingNumber(generateNumber(tenantId))
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .hourlyRate(resource.getHourlyRate())
                .depositAmount(deposit)
                .depositPaid(depositPaid)
                .note(request.getNote())
                .createdBy(currentUsername());
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
        Booking saved = persistDetectingConflicts(booking);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_CHECKIN, "BOOKING", String.valueOf(saved.getId()),
                "activity.booking.checkin", null);
        return mapBooking(saved);
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

        // Pick the rate by the session's start window (giá giờ vàng), falling back to the flat rate.
        BookingResource resource = resourceRepository
                .findByIdAndTenantIdAndDeletedFalse(booking.getResourceId(), booking.getTenantId())
                .orElse(null);
        BigDecimal effectiveRate = resolveHourlyRate(booking, resource);
        BigDecimal amount = effectiveRate
                .multiply(BigDecimal.valueOf(minutes))
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.HALF_UP);

        // Floor at the resource's minimum charge (giờ tối thiểu).
        BigDecimal minimum = resource != null && resource.getMinimumCharge() != null
                ? resource.getMinimumCharge() : BigDecimal.ZERO;
        if (amount.compareTo(minimum) < 0) {
            amount = minimum;
        }

        booking.setEndedAt(endedAt);
        booking.setDurationMinutes((int) minutes);
        booking.setHourlyRate(effectiveRate);
        booking.setTimeAmount(amount);
        booking.setStatus("COMPLETED");

        Long orderId = createTimeChargeOrder(booking, amount);
        booking.setLinkedOrderId(orderId);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking checked out: {} minutes={} amount={} order={}", saved.getBookingNumber(), minutes, amount, orderId);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_CHECKOUT, "BOOKING", String.valueOf(saved.getId()),
                "activity.booking.checkout", null);
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
        Booking saved = bookingRepository.save(booking);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_CANCELLED, "BOOKING", String.valueOf(saved.getId()),
                "activity.booking.cancelled", null);
        return mapBooking(saved);
    }

    @Override
    @Transactional
    public BookingDTO noShow(Long id) {
        Booking booking = findOrThrow(id);
        if (!"RESERVED".equals(booking.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.booking.invalid.status", booking.getStatus()));
        }
        booking.setStatus("NO_SHOW");
        Booking saved = bookingRepository.save(booking);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_NO_SHOW, "BOOKING", String.valueOf(saved.getId()),
                "activity.booking.no_show", null);
        return mapBooking(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Booking booking = findOrThrow(id);
        booking.softDelete();
        bookingRepository.save(booking);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BOOKING_DELETED, "BOOKING", String.valueOf(id),
                "activity.booking.deleted", null);
    }

    // ── Order creation on checkout ───────────────────────────────────────────

    /**
     * Creates a PENDING POS order with the time charge as a single product-less
     * line. Staff can then add drinks/food via the normal order endpoints and
     * collect payment through POS. Returns the new order id.
     */
    private Long createTimeChargeOrder(Booking booking, BigDecimal amount) {
        String tenantId = booking.getTenantId();
        // Đặt cọc đã thu được trừ vào hoá đơn (deposit netted against the time charge).
        BigDecimal depositApplied = BigDecimal.ZERO;
        if (booking.isDepositPaid() && booking.getDepositAmount() != null
                && booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            depositApplied = booking.getDepositAmount().min(amount);
        }
        BigDecimal payable = amount.subtract(depositApplied);

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(payable);
        order.setDiscountAmount(depositApplied);
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

    private void assertNoReservationOverlap(String tenantId, Long resourceId, LocalDate date,
                                            LocalTime startTime, LocalTime endTime, long excludeId) {
        int[] slot = slotSeconds(startTime, endTime);
        for (Booking existing : bookingRepository.findReservationsByResourceAndDate(
                tenantId, resourceId, date, excludeId)) {
            int[] ex = slotSeconds(existing.getScheduledStartTime(), existing.getScheduledEndTime());
            boolean overlaps = slot[0] < ex[1] && ex[0] < slot[1];   // half-open [start, end) intervals
            if (overlaps) {
                throw new BadRequestException(messageService.getMessage("error.booking.reservation.overlap"));
            }
        }
    }

    /**
     * The hourly rate to bill: the rate of the rate window (giá giờ vàng) whose [start, end)
     * contains the session's start time-of-day and whose dayKind matches the day, else the
     * resource's flat hourlyRate (which was copied onto the booking at create time).
     */
    private BigDecimal resolveHourlyRate(Booking booking, BookingResource resource) {
        BigDecimal flat = booking.getHourlyRate() != null ? booking.getHourlyRate() : BigDecimal.ZERO;
        if (resource == null || booking.getStartedAt() == null) return flat;
        List<BookingResourceRate> windows = rateRepository.findByResource(booking.getTenantId(), resource.getId());
        if (windows.isEmpty()) return flat;

        LocalTime tod = booking.getStartedAt().toLocalTime();
        DayOfWeek dow = booking.getStartedAt().getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        int sec = tod.toSecondOfDay();
        for (BookingResourceRate w : windows) {
            if (!dayKindMatches(w.getDayKind(), weekend)) continue;
            int[] win = slotSeconds(w.getStartTime(), w.getEndTime());
            if (sec >= win[0] && sec < win[1]) {
                return w.getRate();
            }
        }
        return flat;
    }

    private static boolean dayKindMatches(String dayKind, boolean weekend) {
        if (dayKind == null || "ALL".equalsIgnoreCase(dayKind)) return true;
        if ("WEEKEND".equalsIgnoreCase(dayKind)) return weekend;
        if ("WEEKDAY".equalsIgnoreCase(dayKind)) return !weekend;
        return true;
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

    private BookingResourceDTO mapResource(BookingResource r, Booking active, List<BookingResourceRate> rates) {
        return BookingResourceDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .resourceType(r.getResourceType())
                .hourlyRate(r.getHourlyRate())
                .minimumCharge(r.getMinimumCharge())
                .status(r.getStatus())
                .note(r.getNote())
                .sortOrder(r.getSortOrder())
                .rates(rates == null ? List.of() : rates.stream().map(this::mapRate).collect(Collectors.toList()))
                .activeBooking(active != null ? mapBooking(active) : null)
                .build();
    }

    private BookingResourceRateDTO mapRate(BookingResourceRate r) {
        return BookingResourceRateDTO.builder()
                .id(r.getId())
                .dayKind(r.getDayKind())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .rate(r.getRate())
                .sortOrder(r.getSortOrder())
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
                .depositAmount(b.getDepositAmount())
                .depositPaid(b.isDepositPaid())
                .recurrenceGroupId(b.getRecurrenceGroupId())
                .status(b.getStatus())
                .note(b.getNote())
                .linkedOrderId(b.getLinkedOrderId())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .elapsedMinutes(elapsed)
                .build();
    }
}
