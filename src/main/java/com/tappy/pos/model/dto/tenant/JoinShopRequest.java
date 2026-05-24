package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /invitations/join. */
@Data
public class JoinShopRequest {

    /** The invitation code entered by the user. */
    @NotBlank
    private String code;
}
