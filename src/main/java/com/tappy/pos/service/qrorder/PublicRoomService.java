package com.tappy.pos.service.qrorder;

import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.room.GuestRequestRequest;
import com.tappy.pos.model.dto.room.PublicRoomDTO;
import com.tappy.pos.model.dto.room.PublicRoomOrderResult;
import com.tappy.pos.model.dto.room.RoomRequestDTO;

/**
 * Unauthenticated in-room guest API (ROOM feature). A guest scans the room QR and either
 * charges minibar items to the open stay's folio or sends a request to reception.
 */
public interface PublicRoomService {

    PublicRoomDTO resolveRoom(String qrToken);

    PublicMenuDTO getMenu();

    /** Charge ordered items to the active stay's folio (source=QR); notifies reception. */
    PublicRoomOrderResult submitOrder(String qrToken, PublicOrderRequest request);

    /** Record a guest request (towel, cleaning, …) for the reception inbox; notifies reception. */
    RoomRequestDTO submitRequest(String qrToken, GuestRequestRequest request);
}
