package com.knp.model.dto.variant;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VariantTypeDTO {
    private Long id;
    private String name;
    private String description;
    private Long productTypeId;
    private String productTypeName;
    private Integer sortOrder;
    private List<VariantTypeOptionDTO> options;
}
