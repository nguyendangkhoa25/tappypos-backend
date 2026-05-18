package com.tappy.pos.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.*;

/**
 * Configures how the POS receipt HTML is rendered.
 * Serialised as JSON and stored in the print_templates table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = ReceiptTemplateConfig.ReceiptTemplateConfigBuilder.class)
public class ReceiptTemplateConfig {

    @JsonPOJOBuilder(withPrefix = "")
    public static class ReceiptTemplateConfigBuilder {}

    /** Extra line printed directly below the shop name. */
    @Builder.Default
    private String headerText = "";

    /** Lines printed at the very bottom of the receipt. Use \n for line breaks. */
    @Builder.Default
    private String footerText = "Cảm ơn quý khách!\nHẹn gặp lại!";

    @Builder.Default private boolean showAddress = true;
    @Builder.Default private boolean showTaxId = true;
    @Builder.Default private boolean showOrderNumber = true;
    @Builder.Default private boolean showDateTime = true;
    @Builder.Default private boolean showCustomer = true;
    @Builder.Default private boolean showTaxBreakdown = true;
    @Builder.Default private boolean showCashDetails = true;

    /** 58mm or 80mm */
    @Builder.Default
    private String paperWidth = "80mm";

    /** Auto-print and close window after receipt loads. */
    @Builder.Default
    private boolean autoClose = true;

    /** Print VietQR code of the default bank account with the order total embedded. */
    @Builder.Default
    private boolean showVietQr = false;

    /** Print the table label (e.g. "Bàn 3") on the receipt — useful for F&B shops. */
    @Builder.Default
    private boolean showTable = false;

    public static ReceiptTemplateConfig defaults() {
        return ReceiptTemplateConfig.builder().build();
    }
}
