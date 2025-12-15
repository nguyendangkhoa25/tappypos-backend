package com.barbershop.model.dto;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceRequest {
    private String status;
    private BigDecimal tax;
    private String notes;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncInvoiceRequest {
    private String externalSystemUrl;
    private String apiKey;
}

