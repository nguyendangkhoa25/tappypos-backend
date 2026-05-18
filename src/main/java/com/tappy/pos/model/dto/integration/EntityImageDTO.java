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
public class EntityImageDTO {
    private Long          id;
    private String        entityType;
    private Long          entityId;
    private String        driveFileId;
    private String        driveUrl;
    private String        thumbnailUrl;
    private String        label;
    private LocalDateTime uploadedAt;
}
