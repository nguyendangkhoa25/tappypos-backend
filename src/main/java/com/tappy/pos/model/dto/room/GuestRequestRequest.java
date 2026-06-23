package com.tappy.pos.model.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Guest → reception request from the in-room QR page. */
@Data
public class GuestRequestRequest {
    /** SERVICE | CLEANING | SUPPLIES | CHECKOUT | OTHER */
    @NotBlank
    private String requestType;

    @Size(max = 500)
    private String message;
}
