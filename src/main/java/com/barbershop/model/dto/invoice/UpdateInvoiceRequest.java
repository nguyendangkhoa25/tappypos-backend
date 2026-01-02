package com.barbershop.model.dto.invoice;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceRequest {
    private String status;
    private String paymentType;
    private String notes;
}


