package com.tappy.pos.model.entity.integration;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "entity_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EntityImage extends TenantAwareEntity {

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "drive_file_id", nullable = false, length = 200)
    private String driveFileId;

    @Column(name = "drive_url", columnDefinition = "TEXT")
    private String driveUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
