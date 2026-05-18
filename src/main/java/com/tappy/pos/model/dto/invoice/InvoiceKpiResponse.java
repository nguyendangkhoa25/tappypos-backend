package com.tappy.pos.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceKpiResponse {
    private long totalInvoiceCount;
    private BigDecimal totalInvoiceAmount;
}
