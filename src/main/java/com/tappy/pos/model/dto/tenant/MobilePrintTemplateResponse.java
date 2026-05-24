package com.tappy.pos.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response shape for the mobile print-template endpoints.
 * Fields are named to match the mobile's {@code PrintTemplate} TypeScript type:
 *   id (string), name, type, isDefault, config (JSON object), updatedAt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobilePrintTemplateResponse {

    /** Template id as a string — matches mobile TypeScript type. */
    private String id;

    private String name;

    /** Template type, e.g. "POS_RECEIPT". Maps from {@code PrintTemplateDTO.templateType}. */
    private String type;

    @JsonProperty("isDefault")
    private boolean isDefault;

    /** Parsed config JSON object — mobile expects an object, not a raw string. */
    private JsonNode config;

    private String updatedAt;
}
