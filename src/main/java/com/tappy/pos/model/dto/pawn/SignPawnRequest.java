package com.tappy.pos.model.dto.pawn;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Borrower's drawn signature for the digital pawn contract (§4d).
 * The client signature pad emits a PNG data URL ("data:image/png;base64,...."); either the
 * full data URL or the raw base64 payload is accepted. Kept small (the service caps the size).
 */
@Data
public class SignPawnRequest {

    @NotBlank(message = "{error.pawn.signatureRequired}")
    private String signature;
}
