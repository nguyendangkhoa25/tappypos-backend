package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Returned to the shop owner after generating an invitation code. */
@Data
@AllArgsConstructor
public class InvitationCodeResponse {

    /** The 6-character code to share with the invitee. */
    private String code;

    /** Role that will be assigned on join. */
    private String roleName;

    /** Features that will be assigned on join. */
    private List<String> features;

    /** ISO-8601 expiry time (5 minutes after generation). */
    private String expiresAt;

    /** Remaining seconds (always ~300 immediately after generation). */
    private long secondsRemaining;
}
