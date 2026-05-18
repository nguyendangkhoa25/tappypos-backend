package com.tappy.pos.model.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTypeDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
}

