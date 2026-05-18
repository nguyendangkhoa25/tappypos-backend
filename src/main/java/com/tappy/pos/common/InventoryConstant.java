package com.tappy.pos.common;

public class InventoryConstant {

    // Inventory Endpoints
    public static final String INVENTORY_API_ENDPOINT = "/api/v1/inventory";
    public static final String INVENTORY_GET_BY_ID = INVENTORY_API_ENDPOINT + "/{id}";
    public static final String INVENTORY_GET_BY_PRODUCT = INVENTORY_API_ENDPOINT + "/product/{productId}";
    public static final String INVENTORY_ALERTS_LOW_STOCK = INVENTORY_API_ENDPOINT + "/alerts/low-stock";
    public static final String INVENTORY_ALERTS_EXPIRED = INVENTORY_API_ENDPOINT + "/alerts/expired";
    public static final String INVENTORY_ALERTS_EXPIRING_SOON = INVENTORY_API_ENDPOINT + "/alerts/expiring-soon";
    public static final String INVENTORY_SEARCH = INVENTORY_API_ENDPOINT + "/search";
    public static final String INVENTORY_WAREHOUSE = INVENTORY_API_ENDPOINT + "/warehouse";
    public static final String INVENTORY_TYPE = INVENTORY_API_ENDPOINT + "/type";
    public static final String INVENTORY_VALUE_TOTAL = INVENTORY_API_ENDPOINT + "/value/total";
    public static final String INVENTORY_UPDATE_QUANTITY = INVENTORY_API_ENDPOINT + "/{id}/quantity";
    public static final String INVENTORY_ADD_STOCK = INVENTORY_API_ENDPOINT + "/{id}/add-stock";
    public static final String INVENTORY_REMOVE_STOCK = INVENTORY_API_ENDPOINT + "/{id}/remove-stock";

    // Inventory Statuses
    public static final String INVENTORY_STATUS_ACTIVE = "ACTIVE";
    public static final String INVENTORY_STATUS_INACTIVE = "INACTIVE";
    public static final String INVENTORY_STATUS_DISCONTINUED = "DISCONTINUED";

    // Inventory Types
    public static final String INVENTORY_TYPE_RETAIL = "RETAIL";
    public static final String INVENTORY_TYPE_WHOLESALE = "WHOLESALE";
    public static final String INVENTORY_TYPE_WAREHOUSE = "WAREHOUSE";

    // Default Values
    public static final Long DEFAULT_REORDER_LEVEL = 10L;
    public static final Long DEFAULT_REORDER_QUANTITY = 50L;
    public static final int EXPIRING_SOON_DAYS = 30;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_PAGE_NUMBER = 0;
}

