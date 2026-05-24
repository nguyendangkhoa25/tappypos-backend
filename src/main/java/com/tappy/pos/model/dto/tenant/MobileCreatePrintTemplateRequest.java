package com.tappy.pos.model.dto.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /shop-config/print-templates (mobile).
 * The mobile sends {@code config} as a JSON object (not a raw string),
 * and specifies the template {@code type} directly in the body.
 */
@Data
public class MobileCreatePrintTemplateRequest {

    @NotBlank
    private String name;

    /** Template type, e.g. "POS_RECEIPT", "PAWN_STAMP". */
    @NotBlank
    private String type;

    /** Configuration object (mobile sends as a JSON object, we serialise to string). */
    private JsonNode config;

    private boolean isDefault;
}
