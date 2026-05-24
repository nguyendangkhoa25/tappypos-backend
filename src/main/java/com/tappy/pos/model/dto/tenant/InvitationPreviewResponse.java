package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Returned to the invitee when they look up a code before confirming. */
@Data
@AllArgsConstructor
public class InvitationPreviewResponse {

    /** Human-readable shop name. */
    private String shopName;

    /** Shop type code, e.g. "NAIL_SHOP" — mobile uses it for emoji/label display. */
    private String shopType;

    /** Role that will be assigned, e.g. "SERVICE_STAFF". */
    private String roleName;

    /** ISO-8601 expiry timestamp. */
    private String expiresAt;

    /** Remaining seconds — mobile uses this to drive its countdown. */
    private long secondsRemaining;
}
