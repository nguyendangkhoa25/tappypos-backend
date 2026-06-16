package com.tappy.pos.service.room;

import com.tappy.pos.model.dto.room.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoomService {

    // ── Rooms / board ─────────────────────────────────────────────────────────
    /** All rooms with their live in-house stay attached (the board). */
    List<RoomDTO> getBoard();

    RoomDTO createRoom(CreateRoomRequest request);

    RoomDTO updateRoom(Long id, CreateRoomRequest request);

    void deleteRoom(Long id);

    /** Housekeeping / front-desk status change (e.g. DIRTY → AVAILABLE, → OOO). */
    RoomDTO setRoomStatus(Long id, String status);

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
