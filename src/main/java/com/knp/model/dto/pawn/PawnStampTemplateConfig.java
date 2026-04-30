package com.knp.model.dto.pawn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Configures which fields appear on pawn receipt stamps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PawnStampTemplateConfig {

    @Builder.Default private boolean showShopName = true;
    @Builder.Default private boolean showQrCode = true;
    @Builder.Default private boolean showCustomerInfo = true;
    @Builder.Default private boolean showItemDetails = true;
    @Builder.Default private boolean showWeight = true;
    @Builder.Default private boolean showItemType = true;
    @Builder.Default private boolean showPawnAmount = true;
    @Builder.Default private boolean showAmountInWords = true;
    @Builder.Default private boolean showInterestRate = false;
    @Builder.Default private boolean showDueDate = true;
    @Builder.Default private boolean showPawnDate = true;
    @Builder.Default private boolean showPawnId = true;

    /** Paper dimensions in mm */
    @Builder.Default private int paperWidth = 210;
    @Builder.Default private int paperHeight = 145;

    /** Number of copies to print side-by-side (1 or 2) */
    @Builder.Default private int copies = 2;

    public static PawnStampTemplateConfig defaults() {
        return PawnStampTemplateConfig.builder().build();
    }
}
