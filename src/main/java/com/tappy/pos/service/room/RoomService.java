package com.tappy.pos.service.room;

import com.tappy.pos.model.dto.room.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoomService {

    // ── QR + reception inbox ────────────────────────────────────────────────────
    /** Ensure the room has a QR token (generate + persist if absent); returns it + the guest path. */
    RoomQrDTO ensureQrToken(Long roomId);

    /** Reception inbox: guest requests, newest first; optional status filter. */
    Page<RoomRequestDTO> listRequests(String status, Pageable pageable);

    /** Count of unhandled (NEW) requests — for the inbox badge. */
    long countNewRequests();

    RoomRequestDTO updateRequestStatus(Long requestId, String status);


    // ── Rooms / board ─────────────────────────────────────────────────────────
    /** All rooms with their live in-house stay attached (the board). */
    List<RoomDTO> getBoard();

    RoomDTO createRoom(CreateRoomRequest request);

    RoomDTO updateRoom(Long id, CreateRoomRequest request);

    void deleteRoom(Long id);

    /** Housekeeping / front-desk status change (e.g. DIRTY → AVAILABLE, → OOO). */
    RoomDTO setRoomStatus(Long id, String status);

    // ── Reservations (advance bookings) ─────────────────────────────────────────
    /** Create a RESERVED stay for a future arrival (room is not occupied yet). */
    RoomStayDTO createReservation(CreateReservationRequest request);

    /** Reservations with planned arrival in [from, to] (to optional) — calendar / agenda feed. */
    List<RoomStayDTO> listReservations(java.time.LocalDate from, java.time.LocalDate to);

    /** Convert a reservation into an active stay (RESERVED → IN_HOUSE, room → OCCUPIED). */
    RoomStayDTO checkInReservation(Long stayId);

    /** Cancel a reservation (RESERVED → CANCELLED); no charge, room unaffected. */
    RoomStayDTO cancelReservation(Long stayId);

    /** Mark a reservation as a no-show (RESERVED → NO_SHOW). */
    RoomStayDTO markNoShow(Long stayId);

    // ── Stays ─────────────────────────────────────────────────────────────────
    /** Walk-in check-in: occupy a room and open a stay. */
    RoomStayDTO checkIn(CheckInRequest request);

    /** Stay + folio detail. */
    RoomStayDTO getStay(Long stayId);

    Page<RoomStayDTO> listStays(String status, Pageable pageable);

    // ── Folio ──────────────────────────────────────────────────────────────────
    RoomStayItemDTO addFolioItem(Long stayId, AddFolioItemRequest request);

    void removeFolioItem(Long stayId, Long itemId);

    // ── Checkout / cancel ───────────────────────────────────────────────────────
    /** Bill room nights/hours + folio, create a completed order, mark room DIRTY. */
    RoomStayDTO checkout(Long stayId, CheckoutRequest request);

    /** Abandon a stay with no charge; room returns to AVAILABLE. */
    RoomStayDTO cancelStay(Long stayId);
}
