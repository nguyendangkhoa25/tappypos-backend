package com.tappy.pos.service.booking;

import com.tappy.pos.service.audit.ActivityLogService;

import com.tappy.pos.config.AuthContext;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.booking.BookingDTO;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
}
