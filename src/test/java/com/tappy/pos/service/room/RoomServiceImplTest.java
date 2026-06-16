package com.tappy.pos.service.room;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.room.CheckInRequest;
import com.tappy.pos.model.dto.room.CheckoutRequest;
import com.tappy.pos.model.dto.room.CreateReservationRequest;
import com.tappy.pos.model.dto.room.RoomStayDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.room.RoomEntity;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomRequestRepository;
import com.tappy.pos.repository.room.RoomStayItemRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceImplTest {

    @Mock RoomRepository roomRepository;
    @Mock RoomStayRepository stayRepository;
    @Mock RoomStayItemRepository itemRepository;
    @Mock RoomRequestRepository requestRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock TenantContext tenantContext;
    @Mock MessageService messageService;
    @Mock ActivityLogService activityLogService;

    @InjectMocks RoomServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, Collections.emptyList()));
        when(tenantContext.getCurrentTenantId()).thenReturn("t1");
        when(messageService.getMessage(anyString())).thenReturn("msg");
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        // echo-save: return the entity passed in (ids set by builders/tests)
        when(stayRepository.save(any(RoomStayEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByStayIdAndDeletedFalseOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RoomEntity room(String status) {
        RoomEntity r = RoomEntity.builder()
                .roomNumber("101").nightlyRate(new BigDecimal("300000")).hourlyRate(new BigDecimal("80000"))
                .overnightRate(new BigDecimal("250000")).status(status).build();
        r.setId(1L);
        return r;
    }

    // ── Check-in ─────────────────────────────────────────────────────────────

    @Test
    void checkIn_occupiesRoom_andUsesNightlyRate() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("AVAILABLE")));
        CheckInRequest req = new CheckInRequest();
        req.setRoomId(1L);
        req.setGuestName("Khách A");
        req.setBillingMode("NIGHTLY");

        RoomStayDTO dto = service.checkIn(req);

        assertThat(dto.getStatus()).isEqualTo("IN_HOUSE");
        assertThat(dto.getRate()).isEqualByComparingTo("300000");
        assertThat(dto.getCheckinAt()).isNotNull();

        ArgumentCaptor<RoomEntity> roomCap = ArgumentCaptor.forClass(RoomEntity.class);
        verify(roomRepository).save(roomCap.capture());
        assertThat(roomCap.getValue().getStatus()).isEqualTo("OCCUPIED");
        verify(activityLogService).logAsync(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkIn_hourlyMode_usesHourlyRate() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("AVAILABLE")));
        CheckInRequest req = new CheckInRequest();
        req.setRoomId(1L);
        req.setBillingMode("HOURLY");

        RoomStayDTO dto = service.checkIn(req);
        assertThat(dto.getRate()).isEqualByComparingTo("80000");
    }

    @Test
    void checkIn_rejectsOccupiedRoom() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("OCCUPIED")));
        CheckInRequest req = new CheckInRequest();
        req.setRoomId(1L);

        assertThatThrownBy(() -> service.checkIn(req)).isInstanceOf(BadRequestException.class);
        verify(stayRepository, never()).save(any());
    }

    // ── Checkout ─────────────────────────────────────────────────────────────

    @Test
    void checkout_marksCheckedOut_buildsOrder_andRoomDirty() {
        RoomStayEntity stay = RoomStayEntity.builder()
                .roomId(1L).roomNumber("101").billingMode("NIGHTLY").rate(new BigDecimal("300000"))
                .checkinAt(LocalDateTime.now().minusDays(1)).status("IN_HOUSE").deposit(BigDecimal.ZERO).build();
        stay.setId(10L);
        when(stayRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(stay));
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("OCCUPIED")));

        CheckoutRequest req = new CheckoutRequest();
        req.setUnits(1);
        req.setPaymentMethod("CASH");

        RoomStayDTO dto = service.checkout(10L, req);

        assertThat(dto.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(dto.getRoomCharge()).isEqualByComparingTo("300000");

        // A settlement order is created, tagged source=ROOM and linked back to the stay.
        ArgumentCaptor<Order> orderCap = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCap.capture());
        Order order = orderCap.getValue();
        assertThat(order.getSource()).isEqualTo("ROOM");
        assertThat(order.getRoomStayId()).isEqualTo(10L);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("300000");

        // Room is flagged for cleaning after checkout.
        ArgumentCaptor<RoomEntity> roomCap = ArgumentCaptor.forClass(RoomEntity.class);
        verify(roomRepository).save(roomCap.capture());
        assertThat(roomCap.getValue().getStatus()).isEqualTo("DIRTY");
    }

    @Test
    void checkout_rejectsStayNotInHouse() {
        RoomStayEntity stay = RoomStayEntity.builder().status("CHECKED_OUT").build();
        stay.setId(10L);
        when(stayRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(stay));

        assertThatThrownBy(() -> service.checkout(10L, new CheckoutRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    // ── Reservations ───────────────────────────────────────────────────────────

    @Test
    void createReservation_rejectsOverlap() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("AVAILABLE")));
        RoomStayEntity existing = RoomStayEntity.builder()
                .roomId(1L).status("RESERVED")
                .reservedCheckin(LocalDateTime.parse("2026-06-17T14:00:00"))
                .expectedCheckout(LocalDateTime.parse("2026-06-19T12:00:00")).build();
        when(stayRepository.findByRoomIdAndStatusInAndDeletedFalse(eq(1L), any())).thenReturn(List.of(existing));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setRoomId(1L);
        req.setReservedCheckin(LocalDateTime.parse("2026-06-18T14:00:00"));   // overlaps 17–19
        req.setExpectedCheckout(LocalDateTime.parse("2026-06-20T12:00:00"));

        assertThatThrownBy(() -> service.createReservation(req)).isInstanceOf(BadRequestException.class);
        verify(stayRepository, never()).save(any());
    }

    @Test
    void createReservation_allowsNonOverlapping() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("AVAILABLE")));
        RoomStayEntity existing = RoomStayEntity.builder()
                .roomId(1L).status("RESERVED")
                .reservedCheckin(LocalDateTime.parse("2026-06-17T14:00:00"))
                .expectedCheckout(LocalDateTime.parse("2026-06-19T12:00:00")).build();
        when(stayRepository.findByRoomIdAndStatusInAndDeletedFalse(eq(1L), any())).thenReturn(List.of(existing));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setRoomId(1L);
        req.setReservedCheckin(LocalDateTime.parse("2026-06-20T14:00:00"));   // after existing
        req.setExpectedCheckout(LocalDateTime.parse("2026-06-21T12:00:00"));

        RoomStayDTO dto = service.createReservation(req);
        assertThat(dto.getStatus()).isEqualTo("RESERVED");
        verify(stayRepository).save(any());
    }

    @Test
    void createReservation_rejectsOverlapWithCurrentOccupancy() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("OCCUPIED")));
        // Room is occupied (IN_HOUSE) checked in yesterday, leaving tomorrow.
        RoomStayEntity occupied = RoomStayEntity.builder()
                .roomId(1L).status("IN_HOUSE")
                .checkinAt(LocalDateTime.now().minusDays(1))
                .expectedCheckout(LocalDateTime.now().plusDays(1)).build();
        when(stayRepository.findByRoomIdAndStatusInAndDeletedFalse(eq(1L), any())).thenReturn(List.of(occupied));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setRoomId(1L);
        req.setReservedCheckin(LocalDateTime.now());                 // overlaps the current stay
        req.setExpectedCheckout(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> service.createReservation(req)).isInstanceOf(BadRequestException.class);
        verify(stayRepository, never()).save(any());
    }

    @Test
    void checkInReservation_convertsReservedToInHouse_andOccupiesRoom() {
        RoomStayEntity stay = RoomStayEntity.builder()
                .roomId(1L).roomNumber("101").status("RESERVED").rate(new BigDecimal("300000"))
                .billingMode("NIGHTLY").deposit(BigDecimal.ZERO).build();
        stay.setId(7L);
        when(stayRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(stay));
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room("AVAILABLE")));

        RoomStayDTO dto = service.checkInReservation(7L);

        assertThat(dto.getStatus()).isEqualTo("IN_HOUSE");
        assertThat(dto.getCheckinAt()).isNotNull();
        ArgumentCaptor<RoomEntity> roomCap = ArgumentCaptor.forClass(RoomEntity.class);
        verify(roomRepository).save(roomCap.capture());
        assertThat(roomCap.getValue().getStatus()).isEqualTo("OCCUPIED");
    }

    @Test
    void checkInReservation_rejectsNonReservedStay() {
        RoomStayEntity stay = RoomStayEntity.builder().status("IN_HOUSE").build();
        stay.setId(7L);
        when(stayRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(stay));

        assertThatThrownBy(() -> service.checkInReservation(7L)).isInstanceOf(BadRequestException.class);
    }
}
