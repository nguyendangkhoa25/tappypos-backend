package com.barbershop.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Long id;
    private Long orderId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal tax;
    private String status;
    private String externalInvoiceId;
    private LocalDateTime externalSyncAt;
    private String notes;
    private LocalDateTime createdAt;
}

