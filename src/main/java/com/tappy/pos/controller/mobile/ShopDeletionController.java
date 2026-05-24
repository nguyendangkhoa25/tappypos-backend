package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.DeleteShopRequest;
import com.tappy.pos.service.tenant.ShopDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DELETE /api/v1/shop-config/delete-shop
 *
 * Permanently soft-deletes the authenticated user's shop:
 * - Marks tenant as deleted (deleted_at, active = false)
 * - Unlinks all staff users (tenant_id → NULL)
 * - Writes an audit record to shop_deletion_log
 *
 * Requires: X-Tenant-ID header + JWT with USER feature.
 * The SHOP_OWNER role is enforced inside ShopDeletionService.
 */
@Slf4j
@RestController
@RequestMapping("/shop-config")
@RequiredArgsConstructor
public class ShopDeletionController {

    private final ShopDeletionService shopDeletionService;

    @DeleteMapping("/delete-shop")
    @RequiresFeature("USER")
    public ResponseEntity<ApiResponse<Void>> deleteShop(
            @RequestBody @Valid DeleteShopRequest request) {
        log.info("DELETE /shop-config/delete-shop");
        shopDeletionService.deleteShop(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cửa hàng đã được xoá thành công."));
    }
}
