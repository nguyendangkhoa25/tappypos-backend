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

    @Data
    @Builder
    public static class MenuCategory {
        private Long id;
        private String name;
        private List<MenuItem> items;
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
