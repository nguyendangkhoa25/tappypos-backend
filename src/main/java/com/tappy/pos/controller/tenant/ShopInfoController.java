package com.tappy.pos.controller.tenant;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.PublicShopInfoDTO;
import com.tappy.pos.model.dto.tenant.ShopInfoDTO;
import com.tappy.pos.service.tenant.ShopConfigService;
import com.tappy.pos.service.tenant.ShopInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tappy.pos.annotation.RequiresFeature;

import java.util.List;

/**
 * ShopInfoController - REST API endpoints for shop information management
 * Tenant isolation is enforced by TenantContext, Hibernate filter, and PostgreSQL RLS
 */
@RestController
@RequestMapping("/shop-info")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("SHOP_INFO")
public class ShopInfoController {

    private final ShopInfoService shopInfoService;
    private final ShopConfigService shopConfigService;

    /**
     * GET /api/shop-info
     * Get current shop information for the tenant
     * If shop info doesn't exist, creates default shop info
     * <p>
     * Returns:
     * - id: Shop info ID
     * - shopName: Shop name
     * - address: Shop address
     * - companyName: Company name
     * - defaultTaxRate: Default tax rate in percentage (0-100)
     * - eInvoiceUsername: E-Invoice username
     * - eInvoicePassword: E-Invoice password (encrypted in response)
     * - eInvoiceKey: E-Invoice API key (encrypted in response)
     * - phone: Shop phone number
     * - email: Shop email address
     * - taxId: Company tax ID / registration number
     * - website: Shop website URL
     * - createdAt: Creation timestamp
     * - updatedAt: Last update timestamp
     * <p>
     * Examples:
     * - GET /api/shop-info
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ShopInfoDTO>> getShopInfo() {
        log.info("Endpoint: GET /shop-info - Get shop information");
        ShopInfoDTO shopInfo = shopInfoService.getShopInfo();
        log.info("Response: Shop info retrieved - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return ResponseEntity.ok(ApiResponse.success(shopInfo, "Shop information retrieved successfully"));
    }

    /**
     * PUT /api/shop-info
     * Update shop information
     * Shop owner can modify shop details and default tax rate
     * <p>
     * Request Body (all fields optional):
     * - shopName: Shop name (required for update)
     * - address: Shop address
     * - companyName: Company name
     * - defaultTaxRate: Default tax rate in percentage (0-100)
     * - eInvoiceUsername: E-Invoice username
     * - eInvoicePassword: E-Invoice password
     * - eInvoiceKey: E-Invoice API key
     * - phone: Shop phone number
     * - email: Shop email address
     * - taxId: Company tax ID / registration number
     * - website: Shop website URL
     * <p>
     * Examples:
     * - PUT /api/shop-info
     * {
     * "shopName": "Premium Barber Shop",
     * "address": "123 Main Street, City",
     * "companyName": "Premium Barber Co., Ltd",
     * "defaultTaxRate": 10,
     * "eInvoiceUsername": "user@example.com",
     * "eInvoicePassword": "encrypted_password",
     * "eInvoiceKey": "api_key_here",
     * "phone": "0123456789",
     * "email": "info@barbershop.com",
     * "taxId": "1234567890",
     * "website": "www.barbershop.com"
     * }
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ShopInfoDTO>> updateShopInfo(
            @RequestBody ShopInfoDTO shopInfoDTO) {
        log.info("Endpoint: PUT /shop-info - Update shop information");
        ShopInfoDTO updatedShopInfo = shopInfoService.updateShopInfo(shopInfoDTO);
        log.info("Response: Shop info updated - id: {}, shopName: {}",
                updatedShopInfo.getId(), updatedShopInfo.getShopName());
        return ResponseEntity.ok(ApiResponse.success(updatedShopInfo, "Shop information updated successfully"));
    }

    /**
     * GET /api/shop-info/default-tax-rate
     * Get the default tax rate for the shop
     * Useful for setting default tax when creating products/services
     * <p>
     * Returns:
     * - double: Default tax rate (0.0-100.0)
     * <p>
     * Examples:
     * - GET /api/shop-info/default-tax-rate
     */
    @GetMapping("/default-tax-rate")
    public ResponseEntity<ApiResponse<Double>> getDefaultTaxRate() {
        log.info("Endpoint: GET /shop-info/default-tax-rate - Get default tax rate");
        Double taxRate = shopInfoService.getDefaultTaxRate();
        log.info("Response: Default tax rate: {}", taxRate);
        return ResponseEntity.ok(ApiResponse.success(taxRate, "Default tax rate retrieved successfully"));
    }

    /**
     * GET /api/shop-info/public
     * Get public shop information without sensitive E-Invoice credentials
     * Safe for public API exposure (e.g., customer-facing pages)
     * <p>
     * Returns:
     * - id: Shop info ID
     * - shopName: Shop name
     * - address: Shop address
     * - companyName: Company name
     * - defaultTaxRate: Default tax rate in percentage (0-100)
     * - phone: Shop phone number
     * - email: Shop email address
     * - taxId: Company tax ID / registration number
     * - website: Shop website URL
     * - createdAt: Creation timestamp
     * - updatedAt: Last update timestamp
     * <p>
     * NOTE: E-Invoice credentials (eInvoiceUsername, eInvoicePassword, eInvoiceKey) are NOT included
     * <p>
     * Examples:
     * - GET /api/shop-info/public
     */
    @GetMapping("/dashboard-widgets")
    public ResponseEntity<ApiResponse<List<String>>> getDashboardWidgets() {
        log.info("Endpoint: GET /shop-info/dashboard-widgets");
        List<String> widgets = shopConfigService.getDashboardWidgets();
        return ResponseEntity.ok(ApiResponse.success(widgets, "Dashboard widgets retrieved successfully"));
    }

    @PutMapping("/dashboard-widgets")
    public ResponseEntity<ApiResponse<List<String>>> updateDashboardWidgets(@RequestBody List<String> widgetIds) {
        log.info("Endpoint: PUT /shop-info/dashboard-widgets - count: {}", widgetIds == null ? 0 : widgetIds.size());
        shopConfigService.setDashboardWidgets(widgetIds);
        return ResponseEntity.ok(ApiResponse.success(widgetIds, "Dashboard widgets updated successfully"));
    }

    @GetMapping("/nav-config")
    public ResponseEntity<ApiResponse<List<String>>> getNavConfig() {
        log.info("Endpoint: GET /shop-info/nav-config");
        List<String> items = shopConfigService.getNavConfig();
        return ResponseEntity.ok(ApiResponse.success(items, "Nav config retrieved successfully"));
    }

    @PutMapping("/nav-config")
    public ResponseEntity<ApiResponse<List<String>>> updateNavConfig(@RequestBody List<String> items) {
        log.info("Endpoint: PUT /shop-info/nav-config - count: {}", items == null ? 0 : items.size());
        shopConfigService.setNavConfig(items);
        return ResponseEntity.ok(ApiResponse.success(items, "Nav config updated successfully"));
    }

    @RequiresFeature("DASHBOARD")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicShopInfoDTO>> getPublicShopInfo() {
        log.info("Endpoint: GET /shop-info/public - Get public shop information");
        PublicShopInfoDTO publicShopInfo = shopInfoService.getPublicShopInfo();
        log.info("Response: Public shop info retrieved - id: {}, shopName: {}",
                publicShopInfo.getId(), publicShopInfo.getShopName());
        return ResponseEntity.ok(ApiResponse.success(publicShopInfo, "Public shop information retrieved successfully"));
    }
}

