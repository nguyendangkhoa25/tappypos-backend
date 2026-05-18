package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pawn_item_watch")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnWatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "pawn_id", nullable = false, unique = true)
    private Long pawnId;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "material")
    private String material;

    @Column(name = "condition")
    private String condition;
}
