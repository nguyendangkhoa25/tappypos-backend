package com.knp.model.dto.tenant;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintTemplateDTO {
    private Long id;
    private String templateType;
    private String name;
    private String configJson;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
