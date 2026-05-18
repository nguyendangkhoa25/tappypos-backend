package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.bank.BankAccountDTO;
import com.tappy.pos.model.dto.tenant.MobileShopInfoDTO;
import com.tappy.pos.model.dto.tenant.PosConfigDTO;
import com.tappy.pos.model.dto.tenant.UpdateMobileShopConfigRequest;
import com.tappy.pos.model.enums.ShopType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.service.finance.BankAccountService;
import com.tappy.pos.service.tenant.ShopInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/shop-config")
@RequiredArgsConstructor
public class MobileShopConfigController {

    private final BankAccountService bankAccountService;
    private final ShopInfoService shopInfoService;
    private final TenantContext tenantContext;

    @GetMapping
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<MobileShopInfoDTO>> getShopConfig() {
        log.info("Endpoint: GET /shop-config");
        String shopTypeCode = Optional.ofNullable(tenantContext.getCurrentTenant())
                .map(t -> t.getShopType())
                .map(ShopType::name)
                .orElse(null);
        MobileShopInfoDTO dto = shopInfoService.getMobileShopConfig(shopTypeCode);
        return ResponseEntity.ok(ApiResponse.success(dto, "OK"));
    }

    @PutMapping
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<MobileShopInfoDTO>> updateShopConfig(
            @RequestBody UpdateMobileShopConfigRequest request) {
        log.info("Endpoint: PUT /shop-config");
        String shopTypeCode = Optional.ofNullable(tenantContext.getCurrentTenant())
                .map(t -> t.getShopType())
                .map(ShopType::name)
                .orElse(null);
        MobileShopInfoDTO dto = shopInfoService.updateMobileShopConfig(request);
        // re-attach shopTypeCode (service doesn't have tenant context access)
        MobileShopInfoDTO withType = MobileShopInfoDTO.builder()
                .shopName(dto.getShopName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .shopTypeCode(shopTypeCode)
                .posMode(dto.getPosMode())
                .build();
        return ResponseEntity.ok(ApiResponse.success(withType, "Shop config updated successfully"));
    }

    @GetMapping("/pos-config")
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<PosConfigDTO>> getPosConfig() {
        log.info("Endpoint: GET /shop-config/pos-config");
        return ResponseEntity.ok(ApiResponse.success(shopInfoService.getPosConfig(), "OK"));
    }

    @PutMapping("/pos-config")
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<PosConfigDTO>> updatePosConfig(@RequestBody PosConfigDTO request) {
        log.info("Endpoint: PUT /shop-config/pos-config");
        return ResponseEntity.ok(ApiResponse.success(shopInfoService.updatePosConfig(request), "POS config updated successfully"));
    }

    @GetMapping("/banks")
    @RequiresFeature("BANK_ACCOUNT")
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getBanks() {
        log.info("Endpoint: GET /shop-config/banks");
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getAll(), "OK"));
    }

    @GetMapping("/loyalty")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoyalty() {
        log.info("Endpoint: GET /shop-config/loyalty");
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("enabled", false, "pointsPerUnit", 1, "minimumRedeemPoints", 100),
                "OK"));
    }
}
