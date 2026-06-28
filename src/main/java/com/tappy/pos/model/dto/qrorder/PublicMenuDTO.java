package com.tappy.pos.model.dto.qrorder;

import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Read-only menu served to an unauthenticated QR customer. Exposes only display fields. */
@Data
@Builder
public class PublicMenuDTO {
    private String shopName;
    private List<MenuCategory> categories;
    /** Fixed combo bundles, shown as their own section. Empty/null when the shop has none. */
    private List<MenuCombo> combos;

    @Data
    @Builder
    public static class MenuCategory {
        private Long id;
        private String name;
        private List<MenuItem> items;
    }

    /** A fixed bundle sold at {@code price}; {@code retailTotal} is the à-la-carte sum (for "savings"). */
    @Data
    @Builder
    public static class MenuCombo {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal retailTotal;
        private List<ComboComponent> components;
    }

    @Data
    @Builder
    public static class ComboComponent {
        private String name;
        private Integer quantity;
    }

    @Data
    @Builder
    public static class MenuItem {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String unit;
        private String imageUrl;
        /** F&B add-on groups (size, sugar, ice, topping). Null/empty when the product has none. */
        private List<ModifierGroupDTO> modifierGroups;
    }
}
