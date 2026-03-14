package com.knp.model.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotNull(message = "Product type ID is required")
    private Long productTypeId;

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 100, message = "SKU must be between 2 and 100 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Cost price must be greater than or equal to 0")
    private BigDecimal costPrice = BigDecimal.ZERO;

    private String status = "ACTIVE";

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    private Long vendorId;

    private Set<Long> categoryIds;

    @NotNull(message = "Attributes are required")
    private Map<String, Object> attributes;
}

