package com.tappy.pos.model.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Check a guest into a room (walk-in). */
@Data
public class CheckInRequest {
    @NotNull
    private Long roomId;
    private String guestName;
    private String guestPhone;
    private String guestIdNumber;
    private Long customerId;
    private Integer adults;
    /** NIGHTLY | HOURLY | OVERNIGHT — defaults to NIGHTLY. */
    private String billingMode;
    /** Optional rate override; otherwise taken from the room's rate for the billing mode. */
    private BigDecimal rate;
    private LocalDateTime expectedCheckout;
    private BigDecimal deposit;
    private String note;
}
