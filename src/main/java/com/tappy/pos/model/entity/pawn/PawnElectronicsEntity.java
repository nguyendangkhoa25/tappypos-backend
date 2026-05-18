package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pawn_item_electronics")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnElectronicsEntity {
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

    @Column(name = "imei")
    private String imei;

    @Column(name = "storage")
    private String storage;

    @Column(name = "color")
    private String color;

    @Column(name = "condition")
    private String condition;
}
