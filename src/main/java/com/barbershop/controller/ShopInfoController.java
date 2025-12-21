package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.ShopInfoDTO;
import com.barbershop.service.ShopInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ShopInfoController - REST API endpoints for shop information management
 * Database routing is handled automatically by TenantContext and RoutingDataSource
 */
@RestController
@RequestMapping("/shop-info")
@RequiredArgsConstructor
@Slf4j
public class ShopInfoController {

    private final ShopInfoService shopInfoService;

    /**
     * GET /api/shop-info
     * Get current shop information for the tenant
     * If shop info doesn't exist, creates default shop info
     *
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
     *
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
     *
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
     *
     * Examples:
     * - PUT /api/shop-info
     *   {
     *     "shopName": "Premium Barber Shop",
     *     "address": "123 Main Street, City",
     *     "companyName": "Premium Barber Co., Ltd",
     *     "defaultTaxRate": 10,
     *     "eInvoiceUsername": "user@example.com",
     *     "eInvoicePassword": "encrypted_password",
     *     "eInvoiceKey": "api_key_here",
     *     "phone": "0123456789",
     *     "email": "info@barbershop.com",
     *     "taxId": "1234567890",
     *     "website": "www.barbershop.com"
     *   }
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
     *
     * Returns:
     * - double: Default tax rate (0.0-100.0)
     *
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
}

