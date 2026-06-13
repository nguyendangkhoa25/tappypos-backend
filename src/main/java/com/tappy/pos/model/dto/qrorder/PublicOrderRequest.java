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
        @NotNull
        private Long productId;

        @NotNull
        @Positive
        @jakarta.validation.constraints.Max(value = 99, message = "Số lượng tối đa 99")
        private Integer quantity;

        @Size(max = 255)
        private String notes;
    }
}
