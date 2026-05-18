package com.tappy.pos.model.enums;

import lombok.Getter;

@Getter
public enum ShopConfigKey {

    // GENERAL
    DEFAULT_TAX_RATE("default_tax_rate", "GENERAL", false),
    TAX_AUTO_APPLY("tax_auto_apply", "GENERAL", false),
    TAX_RATE_BY_PRODUCT_TYPE("tax_rate_by_product_type", "GENERAL", false),
    SHOP_LOCATIONS("shop_locations", "GENERAL", false),

    // EINVOICE
    EINVOICE_USERNAME("einvoice_username", "EINVOICE", false),
    EINVOICE_PASSWORD("einvoice_password", "EINVOICE", true),
    EINVOICE_KEY("einvoice_key", "EINVOICE", true),
    INVOICE_VENDOR("invoice_vendor", "EINVOICE", false),
    EINVOICE_TEMPLATE_CODE("einvoice_template_code", "EINVOICE", false),
    EINVOICE_SERIES("einvoice_series", "EINVOICE", false),
    INVOICE_SYSTEM("invoice_system", "EINVOICE", false),

    // POS
    CASH_DENOMINATIONS("cash_denominations", "POS", false),
    POS_MODE("pos_mode", "POS", false),
    AUTO_PRINT("auto_print", "POS", false),
    VAT_ENABLED("vat_enabled", "POS", false),
    QUICK_PHRASES("quick_phrases", "POS", false),

    // PAWN
    PAWN_INTEREST_RATE("pawn_interest_rate", "PAWN", false),
    PAWN_INTEREST_TYPE("pawn_interest_type", "PAWN", false),
    PAWN_CALC_MODE("pawn_calc_mode", "PAWN", false),
    PAWN_DUE_DATE("pawn_due_date", "PAWN", false),
    PAWN_EXCLUDE_VISIBLE_ITEM("pawn_exclude_visible_item", "PAWN", false),
    PAWN_CATEGORY_CONFIG("pawn_category_config", "PAWN", false),
    PAWN_DENOMINATIONS("pawn_denominations", "PAWN", false),
    PAWN_ACCEPTED_TYPES("pawn_accepted_types", "PAWN", false),

    // DASHBOARD
    DASHBOARD_WIDGETS("dashboard_widgets", "DASHBOARD", false),

    // NAVIGATION
    NAV_CONFIG("nav_config", "GENERAL", false),

    // PRICING
    PRICE_BOARD_CODE("price_board_code", "PRICING", false);

    private final String key;
    private final String group;
    private final boolean encrypted;

    ShopConfigKey(String key, String group, boolean encrypted) {
        this.key = key;
        this.group = group;
        this.encrypted = encrypted;
    }

}
