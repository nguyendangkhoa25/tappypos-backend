package com.tappy.pos.model.dto.pawn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Configures the pawn receipt stamp layout.
 * <p>
 * variant = "default" → prints shop name + row labels; for blank paper.<br>
 * variant = "custom"  → prints data only; for pre-printed paper that already
 *                       carries the shop name and field labels.
 * <p>
 * The layout fields (column widths, spacer heights, line heights) only apply
 * to the "custom" variant and let each shop's pre-printed form be matched
 * without code changes — adjust them via PrintTemplatePage during onboarding.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PawnStampTemplateConfig {

    /** "default" (blank paper) or "custom" (pre-printed paper). */
    @Builder.Default private String variant = "default";

    // ── Layout measurements for the "custom" variant ──────────────────────────
    // All mm values match the pre-printed form's physical dimensions.

    /** Width of the left (customer stub) column in mm. */
    @Builder.Default private int leftColWidth = 63;

    /** Left padding of the left column in px. */
    @Builder.Default private int leftColPaddingLeft = 5;

    /** Line height of the left column in em × 10 (e.g. 15 = 1.5em). */
    @Builder.Default private int leftColLineHeight = 15;

    /** Line height of the right column in em × 10 (e.g. 18 = 1.8em). */
    @Builder.Default private int rightColLineHeight = 18;

    /** Height of the top spacer in the left column in mm (shop header area). */
    @Builder.Default private int topSpacerHeight = 31;

    /** Height of the item-name spacer row in mm. */
    @Builder.Default private int itemSpacerHeight = 8;

    /** Height of the QR/barcode section in the right column in mm. */
    @Builder.Default private int qrSectionHeight = 38;

    /** Spacer above the QR image within the QR section in mm. */
    @Builder.Default private int qrTopSpacerHeight = 20;

    public static PawnStampTemplateConfig defaults() {
        return PawnStampTemplateConfig.builder().build();
    }

    public static PawnStampTemplateConfig custom() {
        return PawnStampTemplateConfig.builder().variant("custom").build();
    }
}
