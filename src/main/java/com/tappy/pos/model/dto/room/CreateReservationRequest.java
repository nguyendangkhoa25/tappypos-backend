package com.tappy.pos.model.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Create an advance reservation (a stay in RESERVED status, not yet checked in). */
@Data
public class CreateReservationRequest {
    @NotNull
    private Long roomId;
    @NotNull
    private LocalDateTime reservedCheckin;
    private LocalDateTime expectedCheckout;

    private String guestName;
    private String guestPhone;
    private String guestIdNumber;
    private Long customerId;
    private Integer adults;
    /** NIGHTLY | HOURLY | OVERNIGHT — defaults to NIGHTLY. */
    private String billingMode;
    private BigDecimal rate;
    private BigDecimal deposit;
    private String note;
}
