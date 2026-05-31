package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pawn_item_jewelry")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnJewelryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "pawn_id", nullable = false, unique = true)
    private Long pawnId;

    @Column(name = "total_weight")
    private java.math.BigDecimal totalWeight;

    @Column(name = "gem_weight")
    private java.math.BigDecimal gemWeight;

    @Column(name = "gold_weight")
    private java.math.BigDecimal goldWeight;

    @Column(name = "purity")
    private String purity;

    @Column(name = "metal_type")
    private String metalType;

    @Column(name = "hallmark")
    private String hallmark;
}
