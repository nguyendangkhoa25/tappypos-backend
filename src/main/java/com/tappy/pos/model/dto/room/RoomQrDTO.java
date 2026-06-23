package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

/** A room's QR token + the guest-facing path the token maps to. */
@Data
@Builder
public class RoomQrDTO {
    private Long roomId;
    private String roomNumber;
    private String qrToken;
    /** Relative guest path; the client prefixes its own origin (e.g. /qr-room/<tenant>/<token>). */
    private String guestPath;
}
