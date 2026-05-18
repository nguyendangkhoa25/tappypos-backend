package com.tappy.pos.model.dto.order;

import com.tappy.pos.model.entity.order.CartItemEntity;
import com.tappy.pos.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cart Item Response DTO
 * Returned to frontend when displaying cart items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productTypeCode;
    private Double itemTaxRate; // tax rate as percentage (0–100)
    private String sku;
    private String barcode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal basePrice;
    private BigDecimal unitCost;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private String discountReason;
    private BigDecimal lineSubtotal;
    private BigDecimal lineTotal;
    private BigDecimal tax;
    private BigDecimal lineGrandTotal;
    private Map<String, String> variants;
    private CartItemEntity.ItemType itemType;
    private String metadata;
    private String notes;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private java.math.BigDecimal commissionRate;
    private java.math.BigDecimal commissionAmount;
}

