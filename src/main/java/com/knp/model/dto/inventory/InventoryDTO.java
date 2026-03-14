package com.knp.model.dto.inventory;

import com.knp.model.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long quantityInStock;
    private Long reorderLevel;
    private Long reorderQuantity;
    private BigDecimal unitCost;
    private String warehouseLocation;
    private String zone;
    private String aisle;
    private String shelf;
    private String bin;
    /** Human-readable shelf label: "Khu A > Hàng 3 > Kệ B > Ô 01" */
    private String shelfLabel;
    private LocalDateTime lastRestockDate;
    private LocalDate expiryDate;
    private String batchNumber;
    private String notes;
    private String status;
    private String inventoryType;
    private Boolean lowStock;
    private Boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static String buildShelfLabel(Inventory entity) {
        List<String> parts = new ArrayList<>();
        if (entity.getZone() != null && !entity.getZone().isBlank()) parts.add("Khu " + entity.getZone().trim());
        if (entity.getAisle() != null && !entity.getAisle().isBlank()) parts.add("Hàng " + entity.getAisle().trim());
        if (entity.getShelf() != null && !entity.getShelf().isBlank()) parts.add("Kệ " + entity.getShelf().trim());
        if (entity.getBin() != null && !entity.getBin().isBlank()) parts.add("Ô " + entity.getBin().trim());
        return parts.isEmpty() ? entity.getWarehouseLocation() : String.join(" > ", parts);
    }

    public static InventoryDTO fromEntity(Inventory entity) {
        return InventoryDTO.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getName() : null)
                .productSku(entity.getProduct() != null ? entity.getProduct().getSku() : null)
                .quantityInStock(entity.getQuantityInStock())
                .reorderLevel(entity.getReorderLevel())
                .reorderQuantity(entity.getReorderQuantity())
                .unitCost(entity.getUnitCost())
                .warehouseLocation(entity.getWarehouseLocation())
                .zone(entity.getZone())
                .aisle(entity.getAisle())
                .shelf(entity.getShelf())
                .bin(entity.getBin())
                .shelfLabel(buildShelfLabel(entity))
                .lastRestockDate(entity.getLastRestockDate())
                .expiryDate(entity.getExpiryDate())
                .batchNumber(entity.getBatchNumber())
                .notes(entity.getNotes())
                .status(entity.getStatus().toString())
                .inventoryType(entity.getInventoryType().toString())
                .lowStock(entity.isLowStock())
                .expired(entity.isExpired())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

