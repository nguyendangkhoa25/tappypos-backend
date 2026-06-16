package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

/** What the guest's QR page sees when resolving a room token. */
@Data
@Builder
public class PublicRoomDTO {
    private String shopName;
    private Long roomId;
    private String roomNumber;
    /** True when a stay is checked in — only then can the guest charge items to the folio. */
    private boolean hasActiveStay;
    private Long stayId;
}
