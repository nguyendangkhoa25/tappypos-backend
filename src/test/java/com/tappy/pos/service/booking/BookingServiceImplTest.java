package com.tappy.pos.service.booking;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import com.tappy.pos.model.entity.booking.Booking;
import com.tappy.pos.model.entity.booking.BookingResource;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.booking.BookingRepository;
import com.tappy.pos.repository.booking.BookingResourceRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock private OrderRepository orderRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

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
}
