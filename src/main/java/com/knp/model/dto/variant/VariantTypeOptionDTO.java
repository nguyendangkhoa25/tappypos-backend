package com.knp.model.dto.variant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VariantTypeOptionDTO {
    private Long id;
    private String value;
    private Integer sortOrder;
}
