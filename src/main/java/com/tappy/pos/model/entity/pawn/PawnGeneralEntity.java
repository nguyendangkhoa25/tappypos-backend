package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pawn_item_general")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class PawnGeneralEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "pawn_id", nullable = false, unique = true)
    private Long pawnId;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "condition")
    private String condition;
}
