package com.tappy.pos.model.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationStatusDTO {
    private String        integrationType;  // "GOOGLE_DRIVE"
    private String        status;           // CONNECTED | DISCONNECTED | NOT_CONFIGURED
    private String        email;            // connected account email (null if not connected)
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
}
