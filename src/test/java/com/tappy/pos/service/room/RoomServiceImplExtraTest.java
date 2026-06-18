package com.tappy.pos.service.room;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.room.AddFolioItemRequest;
import com.tappy.pos.model.dto.room.CheckoutRequest;
import com.tappy.pos.model.dto.room.CreateRoomRequest;
import com.tappy.pos.model.dto.room.RoomDTO;
import com.tappy.pos.model.dto.room.RoomQrDTO;
import com.tappy.pos.model.dto.room.RoomRequestDTO;
import com.tappy.pos.model.dto.room.RoomStayDTO;
import com.tappy.pos.model.dto.room.RoomStayItemDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.room.RoomEntity;
import com.tappy.pos.model.entity.room.RoomRequestEntity;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.model.entity.room.RoomStayItemEntity;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoomServiceImpl — CRUD / folio / inbox coverage")
class RoomServiceImplExtraTest {

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
        when(stayRepository.save(any(RoomStayEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(777L);
            return o;
        });
        when(itemRepository.save(any(RoomStayItemEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByStayIdAndDeletedFalseOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RoomEntity room(long id, String status) {
        RoomEntity r = RoomEntity.builder()
                .roomNumber("10" + id).nightlyRate(new BigDecimal("300000"))
                .hourlyRate(new BigDecimal("80000")).overnightRate(new BigDecimal("250000"))
                .status(status).sortOrder(0).build();
        r.setId(id);
        return r;
    }

    private RoomStayEntity stay(long id, String status) {
        RoomStayEntity s = RoomStayEntity.builder()
                .stayNumber("STY-1").roomId(1L).roomNumber("101").rate(new BigDecimal("300000"))
                .units(1).billingMode("NIGHTLY").status(status).deposit(BigDecimal.ZERO)
                .checkinAt(LocalDateTime.now().minusDays(2)).build();
        s.setId(id);
        return s;
    }

    // ── getBoard ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBoard maps occupied rooms to their in-house stay and due reservations")
    void getBoard() {
        RoomEntity occupied = room(1, "OCCUPIED");
        RoomEntity free = room(2, "AVAILABLE");
        RoomStayEntity inHouse = stay(50, "IN_HOUSE");
        RoomStayEntity reservation = stay(51, "RESERVED");
        reservation.setRoomId(2L);
        reservation.setReservedCheckin(LocalDateTime.now());

        when(stayRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(eq("IN_HOUSE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inHouse)));
        when(stayRepository.findByStatusAndReservedCheckinLessThanEqualAndDeletedFalseOrderByReservedCheckinAsc(eq("RESERVED"), any()))
                .thenReturn(List.of(reservation));
        when(roomRepository.findByDeletedFalseOrderBySortOrderAscRoomNumberAsc())
                .thenReturn(List.of(occupied, free));

        List<RoomDTO> board = service.getBoard();

        assertThat(board).hasSize(2);
        assertThat(board.get(0).getActiveStay()).isNotNull();
        assertThat(board.get(1).getReservedStay()).isNotNull();
    }

    // ── createRoom / updateRoom / deleteRoom ──────────────────────────────────────

    @Test
    @DisplayName("createRoom persists with defaults")
    void createRoom() {
        when(roomRepository.existsByRoomNumberAndDeletedFalse("201")).thenReturn(false);
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomNumber(" 201 ");
        req.setNightlyRate(new BigDecimal("400000"));

        RoomDTO dto = service.createRoom(req);

        assertThat(dto.getRoomNumber()).isEqualTo("201");
        assertThat(dto.getStatus()).isEqualTo("AVAILABLE");
        assertThat(dto.getMaxOccupancy()).isEqualTo(2);
    }

    @Test
    @DisplayName("createRoom rejects a duplicate room number")
    void createRoom_duplicate() {
        when(roomRepository.existsByRoomNumberAndDeletedFalse("201")).thenReturn(true);
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomNumber("201");
        assertThatThrownBy(() -> service.createRoom(req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateRoom applies only the provided fields")
    void updateRoom() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "AVAILABLE")));
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomType("VIP");
        req.setMaxOccupancy(4);

        RoomDTO dto = service.updateRoom(1L, req);

        assertThat(dto.getRoomType()).isEqualTo("VIP");
        assertThat(dto.getMaxOccupancy()).isEqualTo(4);
        assertThat(dto.getRoomNumber()).isEqualTo("101"); // unchanged
    }

    @Test
    @DisplayName("deleteRoom soft-deletes a free room")
    void deleteRoom() {
        RoomEntity r = room(1, "AVAILABLE");
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(r));
        service.deleteRoom(1L);
        assertThat(r.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteRoom rejects an occupied room")
    void deleteRoom_occupied() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "OCCUPIED")));
        assertThatThrownBy(() -> service.deleteRoom(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("deleteRoom throws when room not found")
    void deleteRoom_notFound() {
        when(roomRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRoom(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── setRoomStatus ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setRoomStatus flips a free room to DIRTY")
    void setRoomStatus() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "AVAILABLE")));
        assertThat(service.setRoomStatus(1L, "dirty").getStatus()).isEqualTo("DIRTY");
    }

    @Test
    @DisplayName("setRoomStatus rejects an invalid status")
    void setRoomStatus_invalid() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "AVAILABLE")));
        assertThatThrownBy(() -> service.setRoomStatus(1L, "WEIRD")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("setRoomStatus refuses to set/clear OCCUPIED manually")
    void setRoomStatus_occupiedLocked() {
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "OCCUPIED")));
        assertThatThrownBy(() -> service.setRoomStatus(1L, "AVAILABLE")).isInstanceOf(BadRequestException.class);
    }

    // ── ensureQrToken ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ensureQrToken generates a token when missing")
    void ensureQrToken_generates() {
        RoomEntity r = room(1, "AVAILABLE");
        r.setQrToken(null);
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(r));

        RoomQrDTO dto = service.ensureQrToken(1L);

        assertThat(dto.getQrToken()).isNotBlank();
        assertThat(dto.getGuestPath()).contains("/qr-room/t1/");
        verify(roomRepository).save(r);
    }

    @Test
    @DisplayName("ensureQrToken keeps an existing token")
    void ensureQrToken_existing() {
        RoomEntity r = room(1, "AVAILABLE");
        r.setQrToken("existing-token");
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(r));

        RoomQrDTO dto = service.ensureQrToken(1L);

        assertThat(dto.getQrToken()).isEqualTo("existing-token");
        verify(roomRepository, never()).save(any());
    }

    // ── requests inbox ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listRequests filters by status when given")
    void listRequests_withStatus() {
        Pageable p = PageRequest.of(0, 10);
        RoomRequestEntity req = RoomRequestEntity.builder().roomId(1L).status("NEW").build();
        when(requestRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc("NEW", p))
                .thenReturn(new PageImpl<>(List.of(req)));

        Page<RoomRequestDTO> result = service.listRequests("NEW", p);

        assertThat(result.getContent()).hasSize(1);
        verify(requestRepository, never()).findByDeletedFalseOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("listRequests without status returns all")
    void listRequests_all() {
        Pageable p = PageRequest.of(0, 10);
        when(requestRepository.findByDeletedFalseOrderByCreatedAtDesc(p))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.listRequests("  ", p).getContent()).isEmpty();
    }

    @Test
    @DisplayName("countNewRequests delegates to the repository")
    void countNewRequests() {
        when(requestRepository.countByStatusAndDeletedFalse("NEW")).thenReturn(4L);
        assertThat(service.countNewRequests()).isEqualTo(4L);
    }

    @Test
    @DisplayName("updateRequestStatus to DONE stamps handler and time")
    void updateRequestStatus_done() {
        RoomRequestEntity req = RoomRequestEntity.builder().roomId(1L).status("NEW").build();
        req.setId(3L);
        when(requestRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RoomRequestDTO dto = service.updateRequestStatus(3L, "done");

        assertThat(dto.getStatus()).isEqualTo("DONE");
        assertThat(req.getHandledBy()).isEqualTo("tester");
        assertThat(req.getHandledAt()).isNotNull();
    }

    @Test
    @DisplayName("updateRequestStatus rejects an invalid status")
    void updateRequestStatus_invalid() {
        RoomRequestEntity req = RoomRequestEntity.builder().status("NEW").build();
        when(requestRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(req));
        assertThatThrownBy(() -> service.updateRequestStatus(3L, "ZZZ")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateRequestStatus throws when request not found")
    void updateRequestStatus_notFound() {
        when(requestRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRequestStatus(9L, "DONE")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── reservations list / cancel / no-show ──────────────────────────────────────

    @Test
    @DisplayName("listReservations with a date range uses the between query")
    void listReservations_range() {
        when(stayRepository.findByStatusAndReservedCheckinBetweenAndDeletedFalseOrderByReservedCheckinAsc(
                eq("RESERVED"), any(), any())).thenReturn(List.of(stay(60, "RESERVED")));
        List<RoomStayDTO> result = service.listReservations(LocalDate.now(), LocalDate.now().plusDays(7));
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("listReservations without an end date uses the open-ended query")
    void listReservations_openEnded() {
        when(stayRepository.findByStatusAndReservedCheckinGreaterThanEqualAndDeletedFalseOrderByReservedCheckinAsc(
                eq("RESERVED"), any())).thenReturn(List.of());
        assertThat(service.listReservations(null, null)).isEmpty();
    }

    @Test
    @DisplayName("cancelReservation marks a reserved stay CANCELLED")
    void cancelReservation() {
        RoomStayEntity s = stay(60, "RESERVED");
        when(stayRepository.findByIdAndDeletedFalse(60L)).thenReturn(Optional.of(s));
        RoomStayDTO dto = service.cancelReservation(60L);
        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        assertThat(s.getCheckoutAt()).isNotNull();
    }

    @Test
    @DisplayName("cancelReservation rejects a non-reserved stay")
    void cancelReservation_notReserved() {
        when(stayRepository.findByIdAndDeletedFalse(60L)).thenReturn(Optional.of(stay(60, "IN_HOUSE")));
        assertThatThrownBy(() -> service.cancelReservation(60L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("markNoShow marks a reserved stay NO_SHOW")
    void markNoShow() {
        RoomStayEntity s = stay(60, "RESERVED");
        when(stayRepository.findByIdAndDeletedFalse(60L)).thenReturn(Optional.of(s));
        assertThat(service.markNoShow(60L).getStatus()).isEqualTo("NO_SHOW");
    }

    // ── stays read ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStay returns the stay with folio")
    void getStay() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        assertThat(service.getStay(50L).getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("getStay throws when missing")
    void getStay_notFound() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStay(50L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listStays filters by status when provided")
    void listStays_withStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(stayRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc("IN_HOUSE", p))
                .thenReturn(new PageImpl<>(List.of(stay(50, "IN_HOUSE"))));
        assertThat(service.listStays("IN_HOUSE", p).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listStays without status returns all")
    void listStays_all() {
        Pageable p = PageRequest.of(0, 10);
        when(stayRepository.findByDeletedFalseOrderByCreatedAtDesc(p)).thenReturn(new PageImpl<>(List.of()));
        assertThat(service.listStays(null, p).getContent()).isEmpty();
    }

    // ── folio ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addFolioItem with a productId pulls name and price from the catalog")
    void addFolioItem_product() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        Product p = Product.builder().id(9L).name("Coca").price(new BigDecimal("15000")).build();
        when(productRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(p));
        AddFolioItemRequest req = new AddFolioItemRequest();
        req.setProductId(9L);
        req.setQuantity(2);

        RoomStayItemDTO dto = service.addFolioItem(50L, req);

        assertThat(dto.getProductName()).isEqualTo("Coca");
        assertThat(dto.getUnitPrice()).isEqualByComparingTo("15000");
        assertThat(dto.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("addFolioItem with a manual name uses the supplied price")
    void addFolioItem_manual() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        AddFolioItemRequest req = new AddFolioItemRequest();
        req.setProductName(" Bia ");
        req.setUnitPrice(new BigDecimal("20000"));
        req.setSource("QR");

        RoomStayItemDTO dto = service.addFolioItem(50L, req);

        assertThat(dto.getProductName()).isEqualTo("Bia");
        assertThat(dto.getSource()).isEqualTo("QR");
    }

    @Test
    @DisplayName("addFolioItem rejects a manual item with no name")
    void addFolioItem_noName() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        AddFolioItemRequest req = new AddFolioItemRequest();
        assertThatThrownBy(() -> service.addFolioItem(50L, req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addFolioItem rejects a stay that is not in-house")
    void addFolioItem_notInHouse() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "CHECKED_OUT")));
        assertThatThrownBy(() -> service.addFolioItem(50L, new AddFolioItemRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("removeFolioItem soft-deletes a folio item of the stay")
    void removeFolioItem() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        RoomStayItemEntity item = RoomStayItemEntity.builder().stayId(50L).unitPrice(BigDecimal.TEN).quantity(1).build();
        item.setId(5L);
        when(itemRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(item));

        service.removeFolioItem(50L, 5L);

        assertThat(item.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("removeFolioItem rejects an item belonging to a different stay")
    void removeFolioItem_wrongStay() {
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(stay(50, "IN_HOUSE")));
        RoomStayItemEntity item = RoomStayItemEntity.builder().stayId(99L).unitPrice(BigDecimal.TEN).quantity(1).build();
        item.setId(5L);
        when(itemRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.removeFolioItem(50L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── checkout with explicit units + folio ──────────────────────────────────────

    @Test
    @DisplayName("checkout with explicit units bills room + folio and creates a settlement order")
    void checkout_withUnitsAndFolio() {
        RoomStayEntity s = stay(50, "IN_HOUSE");
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(s));
        RoomStayItemEntity folio = RoomStayItemEntity.builder()
                .stayId(50L).productName("Bia").quantity(2).unitPrice(new BigDecimal("20000")).build();
        when(itemRepository.findByStayIdAndDeletedFalseOrderByCreatedAtAsc(50L)).thenReturn(List.of(folio));
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(room(1, "OCCUPIED")));

        CheckoutRequest req = new CheckoutRequest();
        req.setUnits(3);
        req.setPaymentMethod("CARD");

        RoomStayDTO dto = service.checkout(50L, req);

        assertThat(dto.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(s.getUnits()).isEqualTo(3);
        assertThat(s.getRoomCharge()).isEqualByComparingTo("900000"); // 3 * 300000
        assertThat(s.getLinkedOrderId()).isEqualTo(777L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelStay frees the room and logs")
    void cancelStay() {
        RoomStayEntity s = stay(50, "IN_HOUSE");
        when(stayRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(s));
        RoomEntity r = room(1, "OCCUPIED");
        when(roomRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(r));

        RoomStayDTO dto = service.cancelStay(50L);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        assertThat(r.getStatus()).isEqualTo("AVAILABLE");
    }
}
