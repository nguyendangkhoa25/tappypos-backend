package com.knp.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class ReceiptTemplateConfig {

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

    public static ReceiptTemplateConfig defaults() {
        return ReceiptTemplateConfig.builder().build();
    }
}
