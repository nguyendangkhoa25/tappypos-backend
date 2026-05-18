package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pawn_item_vehicle")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnVehicleEntity {
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

    @Column(name = "year")
    private Integer year;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "engine_number")
    private String engineNumber;

    @Column(name = "chassis_number")
    private String chassisNumber;

    @Column(name = "color")
    private String color;

    @Column(name = "condition")
    private String condition;
}
