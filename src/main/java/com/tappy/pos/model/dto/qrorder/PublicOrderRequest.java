package com.tappy.pos.model.dto.qrorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Customer-submitted order from the QR page. Prices are NOT trusted — the server re-prices from the catalog. */
@Data
public class PublicOrderRequest {

    @NotEmpty
    @Size(max = 100, message = "Quá nhiều món trong một đơn")
    @Valid
    private List<Line> items;

    @Size(max = 100)
    private String customerName;

    @Size(max = 500)
    private String note;

    @Data
    public static class Line {
        /** A standalone product line. Exactly one of {@code productId} / {@code comboId} must be set. */
        private Long productId;

        /** A combo (fixed bundle) line. Mutually exclusive with {@code productId}. */
        private Long comboId;

        @NotNull
        @Positive
        @jakarta.validation.constraints.Max(value = 99, message = "Số lượng tối đa 99")
        private Integer quantity;

        @Size(max = 255)
        private String notes;

        /** Chosen modifier option ids (F&B add-ons). Server re-prices and validates min/max/required. */
        @Size(max = 30, message = "Quá nhiều tùy chọn cho một món")
        private List<Long> modifierOptionIds;
    }
}
