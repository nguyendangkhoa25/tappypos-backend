package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /shop-config/invitations.
 * The shop owner specifies the role and (optionally) a custom feature list.
 * If features is null or empty the role's default features are used by the service.
 */
@Data
public class GenerateInvitationRequest {

    /** Role to assign to the invitee when they join, e.g. "SERVICE_STAFF". */
    @NotBlank
    private String roleName;

    /**
     * Optional explicit feature list. When omitted the service falls back to
     * the role's currently configured features for this tenant.
     */
    private List<String> features;
}
