package com.tappy.pos.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
