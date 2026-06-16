package com.tappy.pos.model.entity.stocktake;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.StocktakeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A stocktake (physical inventory count) run. Resumable until applied or cancelled.
 */
@Entity
@Table(name = "stocktake_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StocktakeSessionEntity extends TenantAwareEntity {

    @Column(name = "name")
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StocktakeStatus status = StocktakeStatus.IN_PROGRESS;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "started_by")
    private String startedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
