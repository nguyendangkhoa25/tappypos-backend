package com.tappy.pos.model.dto.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest {

    private String reason;

    /** Username of the person cancelling the order. Populated from JWT on the backend if blank. */
    private String cancelledBy;
}
