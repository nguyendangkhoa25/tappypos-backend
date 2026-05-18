package com.tappy.pos.model.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SavePrintTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    private String configJson;
}
