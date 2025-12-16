package com.barbershop.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {
    private Long orderId;
    private BigDecimal tax;
    private String notes;
}
