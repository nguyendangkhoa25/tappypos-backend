package com.tappy.pos.model.entity.integration;

import com.tappy.pos.model.converter.EncryptedStringConverter;
import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_integrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShopIntegration extends TenantAwareEntity {

    @Column(name = "integration_type", nullable = false, length = 50)
    private String integrationType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntegrationStatus status = IntegrationStatus.DISCONNECTED;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
}
