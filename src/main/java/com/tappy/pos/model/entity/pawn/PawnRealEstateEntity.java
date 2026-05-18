package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pawn_item_real_estate")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnRealEstateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "pawn_id", nullable = false, unique = true)
    private Long pawnId;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "certificate_type")
    private String certificateType;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "address")
    private String address;

    @Column(name = "area_sqm")
    private BigDecimal areaSqm;

    @Column(name = "condition")
    private String condition;
}
