package com.knp.model.enums;

import lombok.Getter;

@Getter
public enum ShopConfigKey {

    // GENERAL
    DEFAULT_TAX_RATE("default_tax_rate", "GENERAL", false),

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

    // PAWN
    PAWN_INTEREST_RATE("pawn_interest_rate", "PAWN", false),
    PAWN_INTEREST_TYPE("pawn_interest_type", "PAWN", false),
    PAWN_DUE_DATE("pawn_due_date", "PAWN", false),
    PAWN_EXCLUDE_VISIBLE_ITEM("pawn_exclude_visible_item", "PAWN", false),

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
