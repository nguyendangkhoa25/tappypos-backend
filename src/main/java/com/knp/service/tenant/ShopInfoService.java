package com.knp.service.tenant;

import com.knp.model.dto.pawn.PawnSetting;
import com.knp.model.dto.tenant.PublicShopInfoDTO;
import com.knp.model.dto.tenant.ShopInfoDTO;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.tenant.ShopInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShopInfoService {

    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;

    public ShopInfoDTO getShopInfo() {
        log.info("Request: Get shop info");
        ShopInfo shopInfo = findOrCreate();
        log.info("Retrieved shop info - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return mapToDTO(shopInfo);
    }

    public ShopInfoDTO updateShopInfo(ShopInfoDTO dto) {
        log.info("Request: Update shop info");
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(ShopInfo::new);

        if (dto.getShopName() != null && !dto.getShopName().trim().isEmpty())
            shopInfo.setShopName(dto.getShopName());
        if (dto.getAddress() != null)       shopInfo.setAddress(dto.getAddress());
        if (dto.getCompanyName() != null)   shopInfo.setCompanyName(dto.getCompanyName());
        if (dto.getPhone() != null)         shopInfo.setPhone(dto.getPhone());
        if (dto.getEmail() != null)         shopInfo.setEmail(dto.getEmail());
        if (dto.getSupplierTaxCode() != null) shopInfo.setSupplierTaxCode(dto.getSupplierTaxCode());
        if (dto.getWebsite() != null)       shopInfo.setWebsite(dto.getWebsite());

        ShopInfo saved = shopInfoRepository.save(shopInfo);

        // Config fields — write to shop_config (null clears the value)
        if (dto.getDefaultTaxRate() != null)   shopConfigService.set(ShopConfigKey.DEFAULT_TAX_RATE, dto.getDefaultTaxRate());
        if (dto.getEInvoiceUsername() != null) shopConfigService.set(ShopConfigKey.EINVOICE_USERNAME, dto.getEInvoiceUsername());
        if (dto.getEInvoicePassword() != null) shopConfigService.set(ShopConfigKey.EINVOICE_PASSWORD, dto.getEInvoicePassword());
        if (dto.getEInvoiceKey() != null)      shopConfigService.set(ShopConfigKey.EINVOICE_KEY, dto.getEInvoiceKey());
        if (dto.getInvoiceVendor() != null)    shopConfigService.set(ShopConfigKey.INVOICE_VENDOR, dto.getInvoiceVendor());
        if (dto.getTemplateCode() != null)     shopConfigService.set(ShopConfigKey.EINVOICE_TEMPLATE_CODE, dto.getTemplateCode());
        if (dto.getInvoiceSeries() != null)    shopConfigService.set(ShopConfigKey.EINVOICE_SERIES, dto.getInvoiceSeries());
        if (dto.getInvoiceSystem() != null)    shopConfigService.set(ShopConfigKey.INVOICE_SYSTEM, dto.getInvoiceSystem());
        // cashDenominations may be explicitly cleared to empty string
        shopConfigService.set(ShopConfigKey.CASH_DENOMINATIONS, dto.getCashDenominations());
        if (dto.getPosMode() != null)          shopConfigService.set(ShopConfigKey.POS_MODE, dto.getPosMode());
        if (dto.getPawnInterestRate() != null) shopConfigService.set(ShopConfigKey.PAWN_INTEREST_RATE, dto.getPawnInterestRate());
        if (dto.getPawnInterestType() != null) shopConfigService.set(ShopConfigKey.PAWN_INTEREST_TYPE, dto.getPawnInterestType());
        if (dto.getPawnDueDate() != null)      shopConfigService.set(ShopConfigKey.PAWN_DUE_DATE, dto.getPawnDueDate());
        if (dto.getExcludeVisibleItem() != null) shopConfigService.set(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, dto.getExcludeVisibleItem());
        // priceBoardCode may be explicitly cleared — always write
        shopConfigService.set(ShopConfigKey.PRICE_BOARD_CODE, dto.getPriceBoardCode());

        log.info("Shop info updated successfully - id: {}, shopName: {}", saved.getId(), saved.getShopName());
        return mapToDTO(saved);
    }

    public Double getDefaultTaxRate() {
        return shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 0.0);
    }

    public boolean getExcludeVisibleItemFlag() {
        return shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false);
    }

    public PawnSetting updatePawnSetting(PawnSetting pawnSetting) {
        if (pawnSetting.getInterestRate() != null) shopConfigService.set(ShopConfigKey.PAWN_INTEREST_RATE, pawnSetting.getInterestRate());
        if (pawnSetting.getInterestType() > 0)     shopConfigService.set(ShopConfigKey.PAWN_INTEREST_TYPE, pawnSetting.getInterestType());
        if (pawnSetting.getDueDate() > 0)          shopConfigService.set(ShopConfigKey.PAWN_DUE_DATE, pawnSetting.getDueDate());
        return pawnSetting;
    }

    public PawnSetting getPawnSetting() {
        return PawnSetting.builder()
                .interestRate(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE) != null
                        ? shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)
                        : BigDecimal.ZERO)
                .interestType(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30))
                .dueDate(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30))
                .build();
    }

    public PublicShopInfoDTO getPublicShopInfo() {
        log.info("Request: Get public shop info");
        ShopInfo shopInfo = findOrCreate();
        return mapToPublicDTO(shopInfo);
    }

    public PublicShopInfoDTO mapToPublicDTO(ShopInfo shopInfo) {
        return PublicShopInfoDTO.builder()
                .id(shopInfo.getId())
                .shopName(shopInfo.getShopName())
                .address(shopInfo.getAddress())
                .companyName(shopInfo.getCompanyName())
                .defaultTaxRate(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 0.0))
                .phone(shopInfo.getPhone())
                .email(shopInfo.getEmail())
                .supplierTaxCode(shopInfo.getSupplierTaxCode())
                .website(shopInfo.getWebsite())
                .cashDenominations(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS))
                .posMode(shopConfigService.getString(ShopConfigKey.POS_MODE, "STANDARD"))
                .createdAt(shopInfo.getCreatedAt())
                .updatedAt(shopInfo.getUpdatedAt())
                .build();
    }

    private ShopInfo findOrCreate() {
        return shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("Shop info not found, creating with defaults");
                    return shopInfoRepository.save(new ShopInfo());
                });
    }

    private ShopInfoDTO mapToDTO(ShopInfo shopInfo) {
        return ShopInfoDTO.builder()
                .id(shopInfo.getId())
                .shopName(shopInfo.getShopName())
                .address(shopInfo.getAddress())
                .companyName(shopInfo.getCompanyName())
                .phone(shopInfo.getPhone())
                .email(shopInfo.getEmail())
                .supplierTaxCode(shopInfo.getSupplierTaxCode())
                .website(shopInfo.getWebsite())
                .defaultTaxRate(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 0.0))
                .eInvoiceUsername(shopConfigService.getString(ShopConfigKey.EINVOICE_USERNAME))
                // passwords are not sent to frontend
                .invoiceVendor(shopConfigService.getString(ShopConfigKey.INVOICE_VENDOR))
                .templateCode(shopConfigService.getString(ShopConfigKey.EINVOICE_TEMPLATE_CODE))
                .invoiceSeries(shopConfigService.getString(ShopConfigKey.EINVOICE_SERIES))
                .invoiceSystem(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM))
                .cashDenominations(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS))
                .posMode(shopConfigService.getString(ShopConfigKey.POS_MODE, "STANDARD"))
                .pawnInterestRate(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE))
                .pawnInterestType(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30))
                .pawnDueDate(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30))
                .excludeVisibleItem(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false))
                .priceBoardCode(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE))
                .createdAt(shopInfo.getCreatedAt())
                .updatedAt(shopInfo.getUpdatedAt())
                .build();
    }
}
