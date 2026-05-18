package com.tappy.pos.model.dto.tenant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for POST /orders/receipt/preview.
 * Carries the pre-checkout cart state so the server can render
 * a receipt HTML without a persisted order.
 */
@Data
public class ReceiptPreviewRequest {

    private List<PreviewItem> items;

    private BigDecimal totalDiscount;
    private BigDecimal total;

    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal changeAmount;

    private String customerName;
    private String tableLabel;

    @Data
    public static class PreviewItem {
        private String productName;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BigDecimal taxRate;
    }
}
