package com.tappy.pos.model.dto.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateVariantsRequest {

    @NotNull
    @NotEmpty
    private List<Long> variantTypeIds;

    /** If null, the parent product's SKU is used as the base. */
    private String baseSku;
}
