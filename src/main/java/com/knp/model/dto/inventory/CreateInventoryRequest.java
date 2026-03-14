package com.knp.model.dto.inventory;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity in stock is required")
    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    private Long quantityInStock;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level must be greater than or equal to 0")
    private Long reorderLevel;

    @NotNull(message = "Reorder quantity is required")
    @Min(value = 1, message = "Reorder quantity must be greater than 0")
    private Long reorderQuantity;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0", message = "Unit cost must be greater than or equal to 0")
    private BigDecimal unitCost;

    @NotBlank(message = "Warehouse location is required")
    @Size(min = 2, max = 255, message = "Warehouse location must be between 2 and 255 characters")
    private String warehouseLocation;

    @Size(max = 50)
    private String zone;

    @Size(max = 20)
    private String aisle;

    @Size(max = 20)
    private String shelf;

    @Size(max = 20)
    private String bin;

    private LocalDate expiryDate;

    private String batchNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private String status = "ACTIVE"; // ACTIVE, INACTIVE, DISCONTINUED

    private String inventoryType = "RETAIL"; // RETAIL, WHOLESALE, WAREHOUSE
}

