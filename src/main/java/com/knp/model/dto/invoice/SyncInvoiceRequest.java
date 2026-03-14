package com.knp.model.dto.invoice;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncInvoiceRequest {
    private String externalSystemUrl;
    private String apiKey;
}
