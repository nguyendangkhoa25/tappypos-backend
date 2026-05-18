package com.tappy.pos.model.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeGroupDTO {

    private Long id;
    private String code;
    private String name;
    private Integer displayOrder;
    private List<AttributeDefinitionDTO> attributes;
}

