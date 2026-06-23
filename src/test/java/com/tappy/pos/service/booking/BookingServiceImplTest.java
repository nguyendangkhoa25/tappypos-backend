package com.tappy.pos.service.booking;

import com.tappy.pos.service.audit.ActivityLogService;

import com.tappy.pos.config.AuthContext;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.BookingResourceDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRateDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRequest;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import com.tappy.pos.model.entity.booking.Booking;
import com.tappy.pos.model.entity.booking.BookingResource;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.model.entity.booking.BookingResourceRate;
import com.tappy.pos.repository.booking.BookingRepository;
import com.tappy.pos.repository.booking.BookingResourceRateRepository;
import com.tappy.pos.repository.booking.BookingResourceRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final String TENANT = "t1";

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingResourceRepository resourceRepository;
    @Mock private BookingResourceRateRepository rateRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @Mock
    private AuthContext authContext;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private BookingResource resource(BigDecimal rate) {
        BookingResource r = BookingResource.builder()
                .tenantId(TENANT).name("Bàn 1").resourceType("TABLE")
                .hourlyRate(rate).status("ACTIVE").createdBy("owner").build();
        r.setId(10L);
        return r;
    }

    @Test
    @DisplayName("Walk-in on a resource that already has a running session is rejected")
    void walkIn_busyResource_rejected() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findActiveByResource(TENANT, 10L))
                .thenReturn(Optional.of(new Booking()));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("WALK_IN");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.resource.busy");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Walk-in losing the resource-busy race (unique index) maps to a friendly 'busy', not a raw conflict")
    void walkIn_resourceBusyRace_mappedToFriendlyError() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"idx_bookings_one_active_per_resource\""));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("WALK_IN");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.resource.busy");
    }

    @Test
    @DisplayName("Concurrent same-day booking-number clash (unique index) maps to a friendly retry message")
    void create_bookingNumberRace_mappedToRetryError() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"idx_bookings_number\""));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("WALK_IN");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.conflict.retry");
    }

    @Test
    @DisplayName("Reservation overlapping an existing one on the same resource is rejected")
    void reservation_overlap_rejected() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        Booking existing = Booking.builder()
                .tenantId(TENANT).resourceId(10L).bookingType("RESERVATION").status("RESERVED")
                .scheduledDate(LocalDate.of(2026, 6, 20))
                .scheduledStartTime(LocalTime.of(18, 0))
                .scheduledEndTime(LocalTime.of(19, 0))
                .build();
        when(bookingRepository.findReservationsByResourceAndDate(eq(TENANT), eq(10L), any(), anyLong()))
                .thenReturn(List.of(existing));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        req.setScheduledDate(LocalDate.of(2026, 6, 20));
        req.setScheduledStartTime(LocalTime.of(18, 30));   // overlaps 18:00–19:00
        req.setScheduledEndTime(LocalTime.of(19, 30));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.reservation.overlap");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Late-night open-ended reservations overlap correctly (no midnight wrap)")
    void reservation_lateNightOpenEnded_overlapDetected() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        // Existing open-ended reservation at 23:45 (default end would wrap to 00:45).
        Booking existing = Booking.builder()
                .tenantId(TENANT).resourceId(10L).bookingType("RESERVATION").status("RESERVED")
                .scheduledDate(LocalDate.of(2026, 6, 20))
                .scheduledStartTime(LocalTime.of(23, 45))
                .build();
        when(bookingRepository.findReservationsByResourceAndDate(eq(TENANT), eq(10L), any(), anyLong()))
                .thenReturn(List.of(existing));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        req.setScheduledDate(LocalDate.of(2026, 6, 20));
        req.setScheduledStartTime(LocalTime.of(23, 30));   // open-ended; clamps to 24:00, overlaps 23:45+
        // no end time

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.reservation.overlap");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Non-overlapping reservations earlier the same day are accepted")
    void reservation_nonOverlapping_accepted() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        Booking existing = Booking.builder()
                .tenantId(TENANT).resourceId(10L).bookingType("RESERVATION").status("RESERVED")
                .scheduledDate(LocalDate.of(2026, 6, 20))
                .scheduledStartTime(LocalTime.of(10, 0))
                .scheduledEndTime(LocalTime.of(11, 0))
                .build();
        when(bookingRepository.findReservationsByResourceAndDate(eq(TENANT), eq(10L), any(), anyLong()))
                .thenReturn(List.of(existing));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        req.setScheduledDate(LocalDate.of(2026, 6, 20));
        req.setScheduledStartTime(LocalTime.of(11, 0));    // starts exactly when the other ends → no overlap
        req.setScheduledEndTime(LocalTime.of(12, 0));

        BookingDTO result = service.create(req);

        assertThat(result.getStatus()).isEqualTo("RESERVED");
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Checkout bills elapsed time, creates a linked POS order and completes the booking")
    void checkout_createsOrder_andCompletes() {
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Bàn 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(LocalDateTime.now().minusMinutes(90))   // 1.5h → 90,000
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        // 90 min × 60,000/h = 90,000
        assertThat(result.getTimeAmount()).isEqualByComparingTo("90000");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getLinkedOrderId()).isEqualTo(777L);
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Checkout on a booking that is not in progress is rejected")
    void checkout_notInProgress_rejected() {
        Booking reserved = Booking.builder()
                .tenantId(TENANT).resourceId(10L).status("RESERVED").bookingType("RESERVATION")
                .hourlyRate(BigDecimal.ZERO).createdBy("staff").build();
        reserved.setId(6L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(6L, TENANT)).thenReturn(Optional.of(reserved));

        assertThatThrownBy(() -> service.checkout(6L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.invalid.status");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout floors a short session at the resource's minimum charge")
    void checkout_flooredAtMinimumCharge() {
        BookingResource r = resource(new BigDecimal("60000"));
        r.setMinimumCharge(new BigDecimal("50000"));
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Bàn 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(LocalDateTime.now().minusMinutes(15))   // 15 min × 60k/h = 15,000 → floored to 50,000
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(r));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of());
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        assertThat(result.getTimeAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Checkout uses the peak rate window when the session start falls inside it")
    void checkout_appliesPeakRateWindow() {
        BookingResource r = resource(new BigDecimal("60000"));
        // Peak window covering all day → 100,000/h.
        BookingResourceRate peak = BookingResourceRate.builder()
                .tenantId(TENANT).resourceId(10L).dayKind("ALL")
                .startTime(LocalTime.of(0, 0)).endTime(LocalTime.of(23, 59))
                .rate(new BigDecimal("100000")).build();
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Sân 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(LocalDateTime.now().withHour(19).withMinute(0).minusMinutes(0).minusHours(0))
                .createdBy("staff").build();
        // 60 minutes ago, inside the all-day window.
        running.setStartedAt(LocalDateTime.now().minusMinutes(60));
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(r));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of(peak));
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        // 60 min × 100,000/h (peak) = 100,000, not 60,000.
        assertThat(result.getTimeAmount()).isEqualByComparingTo("100000");
        assertThat(result.getHourlyRate()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Checkout nets a paid deposit against the order total")
    void checkout_netsPaidDeposit() {
        BookingResource r = resource(new BigDecimal("60000"));
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Sân 1")
                .bookingType("RESERVATION").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .depositAmount(new BigDecimal("30000")).depositPaid(true)
                .startedAt(LocalDateTime.now().minusMinutes(60))   // 60,000 time charge
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(r));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(orderCaptor.capture())).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkout(5L);

        Order order = orderCaptor.getValue();
        // 60,000 charge − 30,000 paid deposit = 30,000 payable.
        assertThat(order.getTotalAmount()).isEqualByComparingTo("30000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("Recurring weekly reservation materializes N rows sharing a recurrence group id")
    void create_recurringWeekly_materializesRows() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("60000"))));
        when(bookingRepository.findReservationsByResourceAndDate(eq(TENANT), eq(10L), any(), anyLong()))
                .thenReturn(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        req.setScheduledDate(LocalDate.of(2026, 6, 20));
        req.setScheduledStartTime(LocalTime.of(18, 0));
        req.setScheduledEndTime(LocalTime.of(20, 0));
        req.setRecurrenceWeekly(true);
        req.setRecurrenceCount(4);

        BookingDTO result = service.create(req);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(4)).saveAndFlush(captor.capture());
        // All four rows share one non-null recurrence group id.
        List<Booking> saved = captor.getAllValues();
        String groupId = saved.get(0).getRecurrenceGroupId();
        assertThat(groupId).isNotNull();
        assertThat(saved).allMatch(b -> groupId.equals(b.getRecurrenceGroupId()));
        // The four weekly dates are consecutive Saturdays.
        assertThat(saved.get(3).getScheduledDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(result.getRecurrenceGroupId()).isEqualTo(groupId);
    }

    // ── Resources ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getResources maps each resource with its active booking and rate windows")
    void getResources_mapsActiveAndRates() {
        BookingResource r = resource(new BigDecimal("50000"));
        Booking active = Booking.builder()
                .tenantId(TENANT).resourceId(10L).status("IN_PROGRESS")
                .bookingNumber("BKG-9").startedAt(LocalDateTime.now().minusMinutes(30)).build();
        active.setId(99L);
        BookingResourceRate rate = BookingResourceRate.builder()
                .tenantId(TENANT).resourceId(10L).dayKind("ALL")
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(22, 0))
                .rate(new BigDecimal("70000")).sortOrder(0).build();
        rate.setId(7L);

        when(bookingRepository.findInProgress(TENANT)).thenReturn(List.of(active));
        when(rateRepository.findAllActive(TENANT)).thenReturn(List.of(rate));
        when(resourceRepository.findAllActive(TENANT)).thenReturn(List.of(r));

        List<BookingResourceDTO> result = service.getResources();

        assertThat(result).hasSize(1);
        BookingResourceDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getRates()).hasSize(1);
        assertThat(dto.getRates().get(0).getRate()).isEqualByComparingTo("70000");
        assertThat(dto.getActiveBooking()).isNotNull();
        assertThat(dto.getActiveBooking().getId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("createResource persists the resource, its rate windows, and logs the activity")
    void createResource_persistsResourceAndRates() {
        BookingResourceRateDTO rateDto = BookingResourceRateDTO.builder()
                .dayKind("WEEKEND").startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(22, 0))
                .rate(new BigDecimal("90000")).build();
        // one invalid rate (missing rate value) is skipped
        BookingResourceRateDTO invalid = BookingResourceRateDTO.builder()
                .dayKind("ALL").startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(12, 0)).build();

        BookingResourceRequest req = new BookingResourceRequest();
        req.setName("  Sân A  ");
        req.setResourceType("COURT");
        req.setHourlyRate(new BigDecimal("60000"));
        req.setMinimumCharge(new BigDecimal("30000"));
        req.setRates(List.of(rateDto, invalid));

        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(inv -> {
            BookingResource r = inv.getArgument(0);
            r.setId(11L);
            return r;
        });
        when(rateRepository.save(any(BookingResourceRate.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResourceDTO dto = service.createResource(req);

        assertThat(dto.getName()).isEqualTo("Sân A");          // trimmed
        assertThat(dto.getResourceType()).isEqualTo("COURT");
        assertThat(dto.getRates()).hasSize(1);                 // invalid one skipped
        verify(rateRepository).softDeleteByResource(TENANT, 11L);
        verify(rateRepository, times(1)).save(any(BookingResourceRate.class));
        verify(activityLogService).logAsync(eq(TENANT), any(), any(), any(), eq("BOOKING_RESOURCE"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("createResource applies default type/status when omitted")
    void createResource_appliesDefaults() {
        BookingResourceRequest req = new BookingResourceRequest();
        req.setName("Bàn");
        req.setResourceType(null);
        req.setStatus(null);
        req.setHourlyRate(null);
        req.setMinimumCharge(null);
        req.setSortOrder(null);

        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(inv -> {
            BookingResource r = inv.getArgument(0);
            r.setId(12L);
            return r;
        });

        BookingResourceDTO dto = service.createResource(req);

        assertThat(dto.getResourceType()).isEqualTo("TABLE");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getHourlyRate()).isEqualByComparingTo("0");
        assertThat(dto.getMinimumCharge()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("updateResource patches only provided fields and keeps existing windows when rates are null")
    void updateResource_partialPatch_keepsWindows() {
        BookingResource existing = resource(new BigDecimal("50000"));
        BookingResourceRate window = BookingResourceRate.builder()
                .tenantId(TENANT).resourceId(10L).dayKind("ALL")
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(20, 0))
                .rate(new BigDecimal("55000")).build();

        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of(window));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());

        BookingResourceRequest req = new BookingResourceRequest();
        req.setName("Bàn VIP");
        req.setRates(null);   // leave windows unchanged
        req.setHourlyRate(null);
        req.setMinimumCharge(null);
        req.setResourceType(null);
        req.setStatus(null);
        req.setSortOrder(null);

        BookingResourceDTO dto = service.updateResource(10L, req);

        assertThat(dto.getName()).isEqualTo("Bàn VIP");
        assertThat(dto.getRates()).hasSize(1);
        verify(rateRepository, never()).softDeleteByResource(anyString(), anyLong());
    }

    @Test
    @DisplayName("updateResource with a non-null rate list fully replaces the windows")
    void updateResource_withRates_replacesWindows() {
        BookingResource existing = resource(new BigDecimal("50000"));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rateRepository.save(any(BookingResourceRate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());

        BookingResourceRequest req = new BookingResourceRequest();
        req.setStatus("INACTIVE");
        req.setNote("đang sửa");
        req.setSortOrder(3);
        req.setRates(List.of(BookingResourceRateDTO.builder()
                .dayKind("WEEKDAY").startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .rate(new BigDecimal("40000")).sortOrder(1).build()));

        BookingResourceDTO dto = service.updateResource(10L, req);

        assertThat(dto.getStatus()).isEqualTo("INACTIVE");
        assertThat(dto.getNote()).isEqualTo("đang sửa");
        assertThat(dto.getSortOrder()).isEqualTo(3);
        assertThat(dto.getRates()).hasSize(1);
        verify(rateRepository).softDeleteByResource(TENANT, 10L);
    }

    @Test
    @DisplayName("updateResource on a missing resource throws not-found")
    void updateResource_missing_throws() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(99L, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateResource(99L, new BookingResourceRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.bookingResource.not.found");
    }

    @Test
    @DisplayName("deleteResource soft-deletes when no active session is running")
    void deleteResource_softDeletes() {
        BookingResource existing = resource(new BigDecimal("50000"));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(existing));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteResource(10L);

        assertThat(existing.isDeleted()).isTrue();
        verify(resourceRepository).save(existing);
        verify(activityLogService).logAsync(eq(TENANT), any(), any(), any(), eq("BOOKING_RESOURCE"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("deleteResource is rejected when the resource has a running session")
    void deleteResource_busy_rejected() {
        BookingResource existing = resource(new BigDecimal("50000"));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(existing));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.of(new Booking()));

        assertThatThrownBy(() -> service.deleteResource(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.resource.busy");

        verify(resourceRepository, never()).save(any());
    }

    // ── Bookings: queries ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getByDate normalizes a blank status filter to null and maps the page")
    void getByDate_blankStatus_normalizedToNull() {
        Booking b = Booking.builder().tenantId(TENANT).status("RESERVED").bookingNumber("BKG-1").build();
        b.setId(1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(bookingRepository.findByDate(eq(TENANT), eq(LocalDate.of(2026, 6, 20)), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(b)));

        Page<BookingDTO> page = service.getByDate(LocalDate.of(2026, 6, 20), "  ", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getBookingNumber()).isEqualTo("BKG-1");
    }

    @Test
    @DisplayName("getByDate passes a real status filter through")
    void getByDate_withStatus_passesThrough() {
        Pageable pageable = PageRequest.of(0, 10);
        when(bookingRepository.findByDate(eq(TENANT), any(), eq("RESERVED"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getByDate(LocalDate.of(2026, 6, 20), "RESERVED", pageable);

        verify(bookingRepository).findByDate(eq(TENANT), any(), eq("RESERVED"), eq(pageable));
    }

    @Test
    @DisplayName("getById returns the booking; elapsedMinutes is set while in progress")
    void getById_inProgress_setsElapsed() {
        Booking b = Booking.builder().tenantId(TENANT).status("IN_PROGRESS").bookingNumber("BKG-2")
                .startedAt(LocalDateTime.now().minusMinutes(20)).build();
        b.setId(3L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(3L, TENANT)).thenReturn(Optional.of(b));

        BookingDTO dto = service.getById(3L);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getElapsedMinutes()).isGreaterThanOrEqualTo(20L);
    }

    @Test
    @DisplayName("getById on a missing booking throws not-found")
    void getById_missing_throws() {
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(404L, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.booking.not.found");
    }

    // ── Bookings: create paths ─────────────────────────────────────────────────

    @Test
    @DisplayName("Walk-in create succeeds and starts an IN_PROGRESS session")
    void walkIn_create_succeeds() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(2L);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("WALK_IN");
        req.setCustomerName("Anh Ba");

        BookingDTO dto = service.create(req);

        assertThat(dto.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(dto.getBookingType()).isEqualTo("WALK_IN");
        assertThat(dto.getCustomerName()).isEqualTo("Anh Ba");
        assertThat(dto.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("Reservation without a scheduled slot is rejected")
    void reservation_missingSlot_rejected() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        // no scheduledDate / scheduledStartTime

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.reservation.requires.slot");
    }

    @Test
    @DisplayName("create on a missing resource throws not-found")
    void create_missingResource_throws() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.empty());
        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.bookingResource.not.found");
    }

    @Test
    @DisplayName("Single (non-recurring) reservation has no recurrence group id")
    void reservation_single_noGroupId() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findReservationsByResourceAndDate(eq(TENANT), eq(10L), any(), anyLong()))
                .thenReturn(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("RESERVATION");
        req.setScheduledDate(LocalDate.of(2026, 6, 20));
        req.setScheduledStartTime(LocalTime.of(18, 0));
        req.setScheduledEndTime(LocalTime.of(19, 0));

        BookingDTO dto = service.create(req);

        assertThat(dto.getStatus()).isEqualTo("RESERVED");
        assertThat(dto.getRecurrenceGroupId()).isNull();
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("An unrelated integrity violation is rethrown as-is, not mapped to a friendly message")
    void create_otherIntegrityViolation_rethrown() {
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT))
                .thenReturn(Optional.of(resource(new BigDecimal("50000"))));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("violates foreign key constraint \"fk_something_else\""));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setResourceId(10L);
        req.setBookingType("WALK_IN");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_something_else");
    }

    // ── Status transitions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("checkIn promotes a RESERVED booking to IN_PROGRESS")
    void checkIn_reserved_succeeds() {
        Booking reserved = Booking.builder().tenantId(TENANT).resourceId(10L).status("RESERVED")
                .bookingType("RESERVATION").bookingNumber("BKG-1").build();
        reserved.setId(4L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(4L, TENANT)).thenReturn(Optional.of(reserved));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO dto = service.checkIn(4L);

        assertThat(dto.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(dto.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("checkIn on a non-reserved booking is rejected")
    void checkIn_notReserved_rejected() {
        Booking running = Booking.builder().tenantId(TENANT).resourceId(10L).status("IN_PROGRESS").build();
        running.setId(4L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(4L, TENANT)).thenReturn(Optional.of(running));

        assertThatThrownBy(() -> service.checkIn(4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.invalid.status");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("checkIn is rejected when the resource is already busy")
    void checkIn_resourceBusy_rejected() {
        Booking reserved = Booking.builder().tenantId(TENANT).resourceId(10L).status("RESERVED").build();
        reserved.setId(4L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(4L, TENANT)).thenReturn(Optional.of(reserved));
        when(bookingRepository.findActiveByResource(TENANT, 10L)).thenReturn(Optional.of(new Booking()));

        assertThatThrownBy(() -> service.checkIn(4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.resource.busy");
    }

    @Test
    @DisplayName("cancel moves an active booking to CANCELLED")
    void cancel_succeeds() {
        Booking b = Booking.builder().tenantId(TENANT).resourceId(10L).status("RESERVED").bookingNumber("BKG-1").build();
        b.setId(5L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO dto = service.cancel(5L);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        verify(activityLogService).logAsync(eq(TENANT), any(), any(), any(), eq("BOOKING"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("cancel on a completed booking is rejected")
    void cancel_completed_rejected() {
        Booking b = Booking.builder().tenantId(TENANT).resourceId(10L).status("COMPLETED").build();
        b.setId(5L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancel(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.invalid.status");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("noShow marks a RESERVED booking as NO_SHOW")
    void noShow_succeeds() {
        Booking b = Booking.builder().tenantId(TENANT).resourceId(10L).status("RESERVED").bookingNumber("BKG-1").build();
        b.setId(6L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(6L, TENANT)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO dto = service.noShow(6L);

        assertThat(dto.getStatus()).isEqualTo("NO_SHOW");
    }

    @Test
    @DisplayName("noShow on a non-reserved booking is rejected")
    void noShow_notReserved_rejected() {
        Booking b = Booking.builder().tenantId(TENANT).resourceId(10L).status("IN_PROGRESS").build();
        b.setId(6L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(6L, TENANT)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.noShow(6L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.booking.invalid.status");
    }

    @Test
    @DisplayName("delete soft-deletes the booking and logs the activity")
    void delete_softDeletes() {
        Booking b = Booking.builder().tenantId(TENANT).resourceId(10L).status("RESERVED").build();
        b.setId(7L);
        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(7L, TENANT)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(7L);

        assertThat(b.isDeleted()).isTrue();
        verify(bookingRepository).save(b);
        verify(activityLogService).logAsync(eq(TENANT), any(), any(), any(), eq("BOOKING"), anyString(), anyString(), any());
    }

    // ── Rate resolution (weekend / weekday dayKind) ────────────────────────────

    @Test
    @DisplayName("Checkout applies a WEEKEND rate window only when the session starts on a weekend")
    void checkout_weekendWindow_appliesOnWeekend() {
        BookingResource r = resource(new BigDecimal("60000"));
        BookingResourceRate weekend = BookingResourceRate.builder()
                .tenantId(TENANT).resourceId(10L).dayKind("WEEKEND")
                .startTime(LocalTime.of(0, 0)).endTime(LocalTime.of(23, 59))
                .rate(new BigDecimal("120000")).build();
        // Pick a Saturday at noon, 60 minutes ago relative to a fixed started/ended span.
        LocalDateTime saturdayNoon = LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(12, 0);
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Sân 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(saturdayNoon)
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(r));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of(weekend));
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        // Weekend window matches → 120,000/h applied.
        assertThat(result.getHourlyRate()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("Checkout ignores a WEEKDAY-only window when the session starts on a weekend, falling back to flat rate")
    void checkout_weekdayWindow_skippedOnWeekend() {
        BookingResource r = resource(new BigDecimal("60000"));
        BookingResourceRate weekday = BookingResourceRate.builder()
                .tenantId(TENANT).resourceId(10L).dayKind("WEEKDAY")
                .startTime(LocalTime.of(0, 0)).endTime(LocalTime.of(23, 59))
                .rate(new BigDecimal("120000")).build();
        LocalDateTime sundayNoon = LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(12, 0);
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Sân 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(sundayNoon)
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.of(r));
        when(rateRepository.findByResource(TENANT, 10L)).thenReturn(List.of(weekday));
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        // Weekday window does not match a Sunday → flat 60,000/h.
        assertThat(result.getHourlyRate()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("Checkout falls back to the flat rate when the resource was deleted before checkout")
    void checkout_resourceMissing_usesFlatRate() {
        Booking running = Booking.builder()
                .tenantId(TENANT).bookingNumber("BKG-1").resourceId(10L).resourceName("Bàn 1")
                .bookingType("WALK_IN").status("IN_PROGRESS")
                .hourlyRate(new BigDecimal("60000"))
                .startedAt(LocalDateTime.now().minusMinutes(60))
                .createdBy("staff").build();
        running.setId(5L);

        when(bookingRepository.findByIdAndTenantIdAndDeletedFalse(5L, TENANT)).thenReturn(Optional.of(running));
        when(resourceRepository.findByIdAndTenantIdAndDeletedFalse(10L, TENANT)).thenReturn(Optional.empty());
        Order savedOrder = new Order();
        savedOrder.setId(777L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = service.checkout(5L);

        assertThat(result.getTimeAmount()).isEqualByComparingTo("60000");
        assertThat(result.getHourlyRate()).isEqualByComparingTo("60000");
    }
}
