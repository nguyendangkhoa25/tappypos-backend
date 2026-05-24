package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveZaloMessageTemplateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100)
    private String name;

    /** The Zalo ZNS template ID from the Zalo OA Developer portal. */
    @NotBlank(message = "templateId is required")
    @Size(max = 100)
    private String templateId;
}
