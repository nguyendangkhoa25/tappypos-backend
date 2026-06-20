package com.tappy.pos.model.dto.order;

import lombok.Data;

import java.util.List;

/**
 * Split a running table tab into several child checks (tách bill).
 *
 * <ul>
 *   <li>{@code mode = "ITEM"} — distribute the order's items into {@link #groups}; each group
 *       becomes one child check. Any item quantity left unassigned stays on the original order.</li>
 *   <li>{@code mode = "EVEN"} — divide the order total into {@link #splitCount} equal child checks;
 *       the original order is voided and the food history stays on it.</li>
 * </ul>
 */
@Data
public class SplitBillRequest {

    /** "ITEM" (split by dish) or "EVEN" (split the total evenly). Defaults to ITEM. */
    private String mode;

    /** EVEN mode only — number of equal checks to produce (2–20). */
    private Integer splitCount;

    /** ITEM mode only — one entry per child check. */
    private List<SplitGroup> groups;

    @Data
    public static class SplitGroup {
        /** Dishes (and quantities) that go onto this child check. */
        private List<SplitItem> items;
    }

    @Data
    public static class SplitItem {
        private Long itemId;
        private Integer quantity;
    }
}
