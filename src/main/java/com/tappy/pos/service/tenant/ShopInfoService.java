package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.pawn.PawnSetting;
import com.tappy.pos.model.dto.tenant.MobileShopInfoDTO;
import com.tappy.pos.model.dto.tenant.PosConfigDTO;
import com.tappy.pos.model.dto.tenant.PublicShopInfoDTO;
import com.tappy.pos.model.dto.tenant.ShopInfoDTO;
import com.tappy.pos.model.dto.tenant.UpdateMobileShopConfigRequest;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShopInfoService {

    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;
    private final ObjectMapper objectMapper;

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
        if (dto.getTaxAutoApply() != null)     shopConfigService.set(ShopConfigKey.TAX_AUTO_APPLY, dto.getTaxAutoApply());
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
        if (dto.getTaxRateByProductType() != null) {
            try {
                shopConfigService.set(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE,
                        objectMapper.writeValueAsString(dto.getTaxRateByProductType()));
            } catch (Exception e) {
                log.warn("Failed to serialize taxRateByProductType: {}", e.getMessage());
            }
        }
        if (dto.getPawnInterestRate() != null) shopConfigService.set(ShopConfigKey.PAWN_INTEREST_RATE, dto.getPawnInterestRate());
        if (dto.getPawnInterestType() != null) shopConfigService.set(ShopConfigKey.PAWN_INTEREST_TYPE, dto.getPawnInterestType());
        if (dto.getPawnDueDate() != null)      shopConfigService.set(ShopConfigKey.PAWN_DUE_DATE, dto.getPawnDueDate());
        if (dto.getExcludeVisibleItem() != null) shopConfigService.set(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, dto.getExcludeVisibleItem());
        if (dto.getPawnCategoryConfig() != null)  shopConfigService.set(ShopConfigKey.PAWN_CATEGORY_CONFIG, dto.getPawnCategoryConfig());
        shopConfigService.set(ShopConfigKey.PAWN_DENOMINATIONS, dto.getPawnDenominations());
        // priceBoardCode may be explicitly cleared — always write
        shopConfigService.set(ShopConfigKey.PRICE_BOARD_CODE, dto.getPriceBoardCode());
        // shopLocations may be explicitly cleared — always write
        shopConfigService.set(ShopConfigKey.SHOP_LOCATIONS, dto.getShopLocations());

        log.info("Shop info updated successfully - id: {}, shopName: {}", saved.getId(), saved.getShopName());
        return mapToDTO(saved);
    }

    public MobileShopInfoDTO getMobileShopConfig(String shopTypeCode) {
        ShopInfo shopInfo = findOrCreate();
        return MobileShopInfoDTO.builder()
                .shopName(shopInfo.getShopName())
                .address(shopInfo.getAddress())
                .phone(shopInfo.getPhone())
                .description(shopInfo.getDescription())
                .logoUrl(shopInfo.getLogoUrl())
                .shopTypeCode(shopTypeCode)
                .posMode(shopConfigService.getString(ShopConfigKey.POS_MODE, "STANDARD"))
                .build();
    }

    public MobileShopInfoDTO updateMobileShopConfig(UpdateMobileShopConfigRequest req) {
        ShopInfo shopInfo = findOrCreate();
        if (req.getShopName() != null && !req.getShopName().isBlank())
            shopInfo.setShopName(req.getShopName());
        if (req.getAddress() != null)     shopInfo.setAddress(req.getAddress());
        if (req.getPhone() != null)       shopInfo.setPhone(req.getPhone());
        if (req.getDescription() != null) shopInfo.setDescription(req.getDescription());
        shopInfoRepository.save(shopInfo);
        String shopTypeCode = null; // caller passes tenantContext separately if needed
        return getMobileShopConfig(shopTypeCode);
    }

    public PosConfigDTO getPosConfig() {
        return PosConfigDTO.builder()
                .posMode(shopConfigService.getString(ShopConfigKey.POS_MODE, "STANDARD"))
                .autoPrint(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false))
                .vatEnabled(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false))
                .cashDenominations(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS))
                .quickPhrases(parseQuickPhrases())
                .build();
    }

    public PosConfigDTO updatePosConfig(PosConfigDTO dto) {
        if (dto.getPosMode() != null)       shopConfigService.set(ShopConfigKey.POS_MODE, dto.getPosMode());
        if (dto.getAutoPrint() != null)     shopConfigService.set(ShopConfigKey.AUTO_PRINT, dto.getAutoPrint());
        if (dto.getVatEnabled() != null)    shopConfigService.set(ShopConfigKey.VAT_ENABLED, dto.getVatEnabled());
        if (dto.getCashDenominations() != null) shopConfigService.set(ShopConfigKey.CASH_DENOMINATIONS, dto.getCashDenominations());
        if (dto.getQuickPhrases() != null)  shopConfigService.set(ShopConfigKey.QUICK_PHRASES, String.join("||", dto.getQuickPhrases()));
        return getPosConfig();
    }

    private List<String> parseQuickPhrases() {
        String raw = shopConfigService.getString(ShopConfigKey.QUICK_PHRASES);
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
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
        if (pawnSetting.getAcceptedTypes() != null) shopConfigService.set(ShopConfigKey.PAWN_ACCEPTED_TYPES, pawnSetting.getAcceptedTypes());
        return pawnSetting;
    }

    public PawnSetting getPawnSetting() {
        return PawnSetting.builder()
                .interestRate(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE) != null
                        ? shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)
                        : BigDecimal.ZERO)
                .interestType(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30))
                .dueDate(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30))
                .acceptedTypes(shopConfigService.getString(ShopConfigKey.PAWN_ACCEPTED_TYPES))
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

    public Map<String, Double> parseTaxRateByProductType() {
        String raw = shopConfigService.getString(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE);
        if (raw == null || raw.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse taxRateByProductType config: {}", e.getMessage());
            return new HashMap<>();
        }
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
                .taxAutoApply(shopConfigService.getBoolean(ShopConfigKey.TAX_AUTO_APPLY, true))
                .taxRateByProductType(parseTaxRateByProductType())
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
                .pawnCategoryConfig(shopConfigService.getString(ShopConfigKey.PAWN_CATEGORY_CONFIG))
                .pawnDenominations(shopConfigService.getString(ShopConfigKey.PAWN_DENOMINATIONS))
                .priceBoardCode(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE))
                .shopLocations(shopConfigService.getString(ShopConfigKey.SHOP_LOCATIONS))
                .createdAt(shopInfo.getCreatedAt())
                .updatedAt(shopInfo.getUpdatedAt())
                .build();
    }
}
