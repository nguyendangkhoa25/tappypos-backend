package com.knp.service;

import com.knp.model.dto.ShopInfoDTO;
import com.knp.model.entity.ShopInfo;
import com.knp.repository.ShopInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShopInfoService {

    private final ShopInfoRepository shopInfoRepository;

    /**
     * Get or create shop info for current tenant
     * If no shop info exists, create an empty one (database will apply defaults)
     *
     * @return ShopInfoDTO
     */
    public ShopInfoDTO getShopInfo() {
        log.info("Request: Get shop info");

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("Shop info not found, creating new shop info with database defaults");
                    ShopInfo newShopInfo = new ShopInfo();
                    return shopInfoRepository.save(newShopInfo);
                });

        log.info("Retrieved shop info - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return mapToDTO(shopInfo);
    }

    /**
     * Update shop info
     *
     * @param shopInfoDTO Updated shop info
     * @return ShopInfoDTO
     */
    public ShopInfoDTO updateShopInfo(ShopInfoDTO shopInfoDTO) {
        log.info("Request: Update shop info");

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("Shop info not found, creating new shop info");
                    return new ShopInfo();
                });

        // Update modifiable fields
        if (shopInfoDTO.getShopName() != null && !shopInfoDTO.getShopName().trim().isEmpty()) {
            log.debug("Updating shop name - old: {}, new: {}", shopInfo.getShopName(), shopInfoDTO.getShopName());
            shopInfo.setShopName(shopInfoDTO.getShopName());
        }

        if (shopInfoDTO.getAddress() != null) {
            log.debug("Updating address");
            shopInfo.setAddress(shopInfoDTO.getAddress());
        }

        if (shopInfoDTO.getCompanyName() != null) {
            log.debug("Updating company name");
            shopInfo.setCompanyName(shopInfoDTO.getCompanyName());
        }

        if (shopInfoDTO.getDefaultTaxRate() != null) {
            log.debug("Updating default tax rate - old: {}, new: {}",
                    shopInfo.getDefaultTaxRate(), shopInfoDTO.getDefaultTaxRate());
            shopInfo.setDefaultTaxRate(shopInfoDTO.getDefaultTaxRate());
        }

        if (shopInfoDTO.getEInvoiceUsername() != null) {
            log.debug("Updating e-invoice username");
            shopInfo.setEInvoiceUsername(shopInfoDTO.getEInvoiceUsername());
        }

        if (shopInfoDTO.getEInvoicePassword() != null) {
            log.debug("Updating e-invoice password");
            shopInfo.setEInvoicePassword(shopInfoDTO.getEInvoicePassword());
        }

        if (shopInfoDTO.getEInvoiceKey() != null) {
            log.debug("Updating e-invoice key");
            shopInfo.setEInvoiceKey(shopInfoDTO.getEInvoiceKey());
        }

        if (shopInfoDTO.getPhone() != null) {
            log.debug("Updating phone");
            shopInfo.setPhone(shopInfoDTO.getPhone());
        }

        if (shopInfoDTO.getEmail() != null) {
            log.debug("Updating email");
            shopInfo.setEmail(shopInfoDTO.getEmail());
        }

        if (shopInfoDTO.getSupplierTaxCode() != null) {
            log.debug("Updating supplier tax code");
            shopInfo.setSupplierTaxCode(shopInfoDTO.getSupplierTaxCode());
        }

        if (shopInfoDTO.getWebsite() != null) {
            log.debug("Updating website");
            shopInfo.setWebsite(shopInfoDTO.getWebsite());
        }

        // cashDenominations may be explicitly cleared (empty string) — allow null to clear
        shopInfo.setCashDenominations(shopInfoDTO.getCashDenominations());

        if (shopInfoDTO.getPosMode() != null) {
            shopInfo.setPosMode(shopInfoDTO.getPosMode());
        }

        if (shopInfoDTO.getInvoiceVendor() != null) {
            shopInfo.setInvoiceVendor(shopInfoDTO.getInvoiceVendor());
        }

        if (shopInfoDTO.getTemplateCode() != null) {
            log.debug("Updating template code");
            shopInfo.setTemplateCode(shopInfoDTO.getTemplateCode());
        }

        if (shopInfoDTO.getInvoiceSeries() != null) {
            log.debug("Updating invoice series");
            shopInfo.setInvoiceSeries(shopInfoDTO.getInvoiceSeries());
        }

        if (shopInfoDTO.getInvoiceSystem() != null) {
            log.debug("Updating invoice system - old: {}, new: {}",
                    shopInfo.getInvoiceSystem(), shopInfoDTO.getInvoiceSystem());
            shopInfo.setInvoiceSystem(shopInfoDTO.getInvoiceSystem());
        }

        ShopInfo updatedShopInfo = shopInfoRepository.save(shopInfo);
        log.info("Shop info updated successfully - id: {}, shopName: {}",
                updatedShopInfo.getId(), updatedShopInfo.getShopName());
        return mapToDTO(updatedShopInfo);
    }

    /**
    /**
     * Get default tax rate
     * @return Default tax rate
     */
    public Double getDefaultTaxRate() {
        log.info("Request: Get default tax rate");
        Double taxRate = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .map(ShopInfo::getDefaultTaxRate)
                .orElse(0.0);
        log.info("Default tax rate: {}", taxRate);
        return taxRate;
    }

    /**
     * Get public shop info without sensitive E-Invoice credentials
     * Safe for public API exposure
     * @return PublicShopInfoDTO without invoice credentials
     */
    public com.knp.model.dto.PublicShopInfoDTO getPublicShopInfo() {
        log.info("Request: Get public shop info");

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("Shop info not found, creating new shop info with database defaults");
                    ShopInfo newShopInfo = new ShopInfo();
                    return shopInfoRepository.save(newShopInfo);
                });

        log.info("Retrieved public shop info - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return mapToPublicDTO(shopInfo);
    }

    /**
     * Map ShopInfo entity to DTO
     *
     * @param shopInfo ShopInfo entity
     * @return ShopInfoDTO
     */
    private ShopInfoDTO mapToDTO(ShopInfo shopInfo) {
        log.debug("Converting ShopInfo to DTO - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return ShopInfoDTO.builder()
                .id(shopInfo.getId())
                .shopName(shopInfo.getShopName())
                .address(shopInfo.getAddress())
                .companyName(shopInfo.getCompanyName())
                .defaultTaxRate(shopInfo.getDefaultTaxRate())
                .eInvoiceUsername(shopInfo.getEInvoiceUsername())
                //.eInvoicePassword(shopInfo.getEInvoicePassword())
                //.eInvoiceKey(shopInfo.getEInvoiceKey())
                .phone(shopInfo.getPhone())
                .email(shopInfo.getEmail())
                .supplierTaxCode(shopInfo.getSupplierTaxCode())
                .website(shopInfo.getWebsite())
                .cashDenominations(shopInfo.getCashDenominations())
                .invoiceVendor(shopInfo.getInvoiceVendor())
                .templateCode(shopInfo.getTemplateCode())
                .invoiceSeries(shopInfo.getInvoiceSeries())
                .invoiceSystem(shopInfo.getInvoiceSystem())
                .posMode(shopInfo.getPosMode() != null ? shopInfo.getPosMode() : "STANDARD")
                .createdAt(shopInfo.getCreatedAt())
                .updatedAt(shopInfo.getUpdatedAt())
                .build();
    }

    /**
     * Map ShopInfo entity to Public DTO (without sensitive E-Invoice credentials)
     * Safe for public API exposure
     * @param shopInfo ShopInfo entity
     * @return PublicShopInfoDTO without invoice credentials
     */
    public com.knp.model.dto.PublicShopInfoDTO mapToPublicDTO(ShopInfo shopInfo) {
        log.debug("Converting ShopInfo to Public DTO - id: {}, shopName: {}", shopInfo.getId(), shopInfo.getShopName());
        return com.knp.model.dto.PublicShopInfoDTO.builder()
                .id(shopInfo.getId())
                .shopName(shopInfo.getShopName())
                .address(shopInfo.getAddress())
                .companyName(shopInfo.getCompanyName())
                .defaultTaxRate(shopInfo.getDefaultTaxRate())
                .phone(shopInfo.getPhone())
                .email(shopInfo.getEmail())
                .supplierTaxCode(shopInfo.getSupplierTaxCode())
                .website(shopInfo.getWebsite())
                .cashDenominations(shopInfo.getCashDenominations())
                .posMode(shopInfo.getPosMode() != null ? shopInfo.getPosMode() : "STANDARD")
                .createdAt(shopInfo.getCreatedAt())
                .updatedAt(shopInfo.getUpdatedAt())
                .build();
    }
}

