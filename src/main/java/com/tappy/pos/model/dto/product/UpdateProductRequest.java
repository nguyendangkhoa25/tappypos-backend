package com.tappy.pos.model.dto.product;

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
public class UpdateProductRequest {

    // Accepted but not applied — SKU is immutable after creation
    private String sku;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    private String barcode;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Cost price must be greater than or equal to 0")
    private BigDecimal costPrice;

    @DecimalMin(value = "0", message = "Commission rate must be >= 0")
    private BigDecimal commissionRate;

    @Min(value = 0, message = "Duration must be >= 0")
    private Integer durationMinutes;

    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    /** Optional alternate sell unit (bán sỉ), e.g. "bao". */
    @Size(max = 20, message = "Alt unit must not exceed 20 characters")
    private String altUnit;

    /** Base units per alternate unit (e.g. 50 → 1 bao = 50 kg). */
    private BigDecimal altUnitFactor;

    /** Price per alternate unit; null → price × factor. */
    private BigDecimal altUnitPrice;

    /** Wholesale price (per base unit) for WHOLESALE-type customers; null = no tier. */
    private BigDecimal wholesalePrice;

    /** FINISHED (default) | INGREDIENT | BOTH — Phase 3 two-stage inventory. */
    private String productKind;

    private Long vendorId;

    @Size(max = 100, message = "Shelf location must not exceed 100 characters")
    private String shelfLocation;

    private Set<Long> categoryIds;

    private Map<String, Object> attributes;
}

