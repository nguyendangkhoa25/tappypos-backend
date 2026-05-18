package com.tappy.pos.model.dto.variant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SaveVariantTypeRequest {

    @NotBlank(message = "Variant type name is required")
    private String name;

    private String description;

    /** Null = applies to all product types */
    private Long productTypeId;

    private Integer sortOrder;

    /** Full option list — replaces existing options on update */
    private List<String> options;
}
