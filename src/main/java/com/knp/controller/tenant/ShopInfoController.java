package com.knp.controller.tenant;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.tenant.PublicShopInfoDTO;
import com.knp.model.dto.tenant.ShopInfoDTO;
import com.knp.service.tenant.ShopInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.knp.annotation.RequiresFeature;

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
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicShopInfoDTO>> getPublicShopInfo() {
        log.info("Endpoint: GET /shop-info/public - Get public shop information");
        PublicShopInfoDTO publicShopInfo = shopInfoService.getPublicShopInfo();
        log.info("Response: Public shop info retrieved - id: {}, shopName: {}",
                publicShopInfo.getId(), publicShopInfo.getShopName());
        return ResponseEntity.ok(ApiResponse.success(publicShopInfo, "Public shop information retrieved successfully"));
    }
}

