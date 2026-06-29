package com.tappy.pos.model.dto.tenant;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for PUT /shop-config/print-templates/{id} (mobile).
 * Only name and config can be changed after creation (type is immutable).
 */
@Data
public class MobileUpdatePrintTemplateRequest {

    @NotBlank
    private String name;

    /** Updated configuration object. */
    private JsonNode config;
}
