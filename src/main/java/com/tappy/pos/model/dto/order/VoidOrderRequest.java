package com.tappy.pos.model.dto.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VoidOrderRequest {

    private String reason;

    /** Username of the person voiding the order. Falls back to JWT principal if blank. */
    private String voidedBy;
}
