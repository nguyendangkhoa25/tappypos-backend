package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeleteShopRequest {

    /** Optional reason the owner is deleting the shop (for audit trail). */
    private String reason;

    /**
     * Confirmation token — the client must send exactly "DELETE" (case-insensitive check on backend)
     * to prove the user read the warning dialog.
     */
    @NotBlank
    private String confirmToken;
}
