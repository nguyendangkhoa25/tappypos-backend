package com.tappy.pos.service.goldprice;

import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.goldprice.PriceBoardResponse;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.tenant.GoldPrice;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.repository.product.CategoryRepository;
import com.tappy.pos.repository.tenant.GoldPriceRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.tenant.ShopConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoldPriceServiceImpl implements GoldPriceService {

    private final GoldPriceRepository goldPriceRepository;
    private final CategoryRepository categoryRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;
    private final MessageService messageService;
    private final TenantContext tenantContext;

    @Override
    public List<GoldPriceDTO> getAllPrices() {
        log.info("Request: Get all gold prices");
        List<GoldPrice> prices = goldPriceRepository.findAllActive();
        Map<Long, Category> catMap = buildCategoryMap();
        return prices.stream().map(p -> toDTO(p, catMap)).toList();
    }

    @Override
    @Transactional
    public GoldPriceDTO createPrice(GoldPriceDTO dto) {
        log.info("Request: Create gold price categoryId={}", dto.getCategoryId());
        if (dto.getCategoryId() == null) {
            throw new BadRequestException("Vui lòng chọn loại vàng");
        }
        Category cat = categoryRepository.findByIdAndDeletedFalse(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId()));
        if (cat.getParent() == null) {
            throw new BadRequestException("Chỉ có thể đặt giá cho loại vàng con (610, 9999, 925…), không phải danh mục gốc");
        }
        goldPriceRepository.findByCategoryIdAndDeletedFalse(dto.getCategoryId()).ifPresent(existing -> {
            throw new BadRequestException("Loại vàng " + cat.getName() + " đã có cấu hình giá");
        });

        String catName    = cat.getName();
        String parentName = cat.getParent().getName();
        String currentUser = getCurrentUsername();

        GoldPrice price = GoldPrice.builder()
                .categoryId(dto.getCategoryId())
                .code(catName)
                .label(parentName + " " + catName)
                .buy(dto.getBuy()   != null ? dto.getBuy()   : BigDecimal.ZERO)
                .sell(dto.getSell() != null ? dto.getSell()  : BigDecimal.ZERO)
                .pawn(dto.getPawn() != null ? dto.getPawn()  : BigDecimal.ZERO)
                .vendorPrice(dto.getVendorPrice())
                .displayOrder(dto.getDisplayOrder() > 0 ? dto.getDisplayOrder() : 10)
                .note(dto.getNote())
                .showInBoard(dto.isShowInBoard())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        price.setTenantId(tenantContext.getCurrentTenantId());
        GoldPrice saved = goldPriceRepository.save(price);
        log.info("Gold price created id={} for category '{}' by {}", saved.getId(), catName, currentUser);

        Map<Long, Category> catMap = buildCategoryMap();
        return toDTO(saved, catMap);
    }

    @Override
    @Transactional
    public GoldPriceDTO updatePrice(Long id, GoldPriceDTO dto) {
        log.info("Request: Update gold price id={}", id);
        GoldPrice price = goldPriceRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Gold price not found: " + id));

        if (dto.getBuy()         != null) price.setBuy(dto.getBuy());
        if (dto.getSell()        != null) price.setSell(dto.getSell());
        if (dto.getPawn()        != null) price.setPawn(dto.getPawn());
        price.setVendorPrice(dto.getVendorPrice());
        price.setDisplayOrder(dto.getDisplayOrder());
        price.setShowInBoard(dto.isShowInBoard());
        if (dto.getNote() != null) price.setNote(dto.getNote());

        String currentUser = getCurrentUsername();
        price.setUpdatedBy(currentUser);
        GoldPrice saved = goldPriceRepository.save(price);
        log.info("Gold price {} updated by {}", id, currentUser);

        Map<Long, Category> catMap = buildCategoryMap();
        return toDTO(saved, catMap);
    }

    @Override
    @Transactional
    public void deletePrice(Long id) {
        log.info("Request: Delete gold price id={}", id);
        GoldPrice price = goldPriceRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Gold price not found: " + id));
        price.softDelete();
        goldPriceRepository.save(price);
        log.info("Gold price {} soft-deleted by {}", id, getCurrentUsername());
    }

    @Override
    public GoldPriceDTO getPriceForCategory(Long categoryId) {
        log.info("Request: Get gold price for categoryId={}", categoryId);
        GoldPrice price = goldPriceRepository.findByCategoryIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No price configured for category: " + categoryId));
        Map<Long, Category> catMap = buildCategoryMap();
        return toDTO(price, catMap);
    }

    @Override
    public PriceBoardResponse getPriceBoard(String code) {
        log.info("Request: Get price board code={}", code);
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Shop info not configured"));

        String configuredCode = shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE);
        if (configuredCode != null && !configuredCode.isBlank() && !configuredCode.equals(code)) {
            throw new BadRequestException(messageService.getMessage("error.goldprice.invalidBoardCode"));
        }

        Map<Long, Category> catMap = buildCategoryMap();
        List<GoldPriceDTO> prices = goldPriceRepository.findAllVisibleInBoard()
                .stream().map(p -> toDTO(p, catMap)).toList();

        return PriceBoardResponse.builder()
                .shopName(shopInfo.getShopName())
                .shopAddress(shopInfo.getAddress())
                .prices(prices)
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Load all active categories with parents in one query and index by ID. */
    private Map<Long, Category> buildCategoryMap() {
        return categoryRepository.findAllActiveWithParent()
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c));
    }

    private GoldPriceDTO toDTO(GoldPrice g, Map<Long, Category> catMap) {
        String catName    = g.getCode();
        String parentName = null;
        Long   categoryId = g.getCategoryId();

        if (categoryId != null) {
            Category cat = catMap.get(categoryId);
            if (cat != null) {
                catName = cat.getName();
                if (cat.getParent() != null) {
                    Category parent = catMap.get(cat.getParent().getId());
                    if (parent != null) parentName = parent.getName();
                }
            }
        }

        return GoldPriceDTO.builder()
                .id(g.getId())
                .categoryId(categoryId)
                .categoryName(catName)
                .parentCategoryName(parentName)
                .code(g.getCode())
                .label(g.getLabel())
                .buy(g.getBuy())
                .sell(g.getSell())
                .pawn(g.getPawn())
                .vendorPrice(g.getVendorPrice())
                .displayOrder(g.getDisplayOrder())
                .note(g.getNote())
                .showInBoard(Boolean.TRUE.equals(g.getShowInBoard()))
                .createdBy(g.getCreatedBy())
                .updatedBy(g.getUpdatedBy())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
