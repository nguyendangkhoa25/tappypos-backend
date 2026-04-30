package com.knp.service.goldprice;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.goldprice.GoldPriceDTO;
import com.knp.model.dto.goldprice.PriceBoardResponse;
import com.knp.model.entity.tenant.GoldPrice;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.tenant.GoldPriceRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.service.MessageService;
import com.knp.service.tenant.ShopConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoldPriceServiceImpl implements GoldPriceService {

    private final GoldPriceRepository goldPriceRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;
    private final MessageService messageService;

    @Override
    public List<GoldPriceDTO> getAllPrices() {
        log.info("Request: Get all gold prices");
        return goldPriceRepository.findAllActive().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public GoldPriceDTO updatePrice(Long id, GoldPriceDTO dto) {
        log.info("Request: Update gold price id={}", id);
        GoldPrice price = goldPriceRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Gold price not found: " + id));

        if (dto.getLabel() != null) price.setLabel(dto.getLabel());
        if (dto.getBuy() != null) price.setBuy(dto.getBuy());
        if (dto.getSell() != null) price.setSell(dto.getSell());
        if (dto.getPawn() != null) price.setPawn(dto.getPawn());
        price.setDisplayOrder(dto.getDisplayOrder());
        price.setShowInBoard(dto.isShowInBoard());
        if (dto.getNote() != null) price.setNote(dto.getNote());

        String currentUser = getCurrentUsername();
        price.setUpdatedBy(currentUser);

        GoldPrice saved = goldPriceRepository.save(price);
        log.info("Gold price {} updated by {}", id, currentUser);
        return toDTO(saved);
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

        List<GoldPriceDTO> prices = goldPriceRepository.findAllVisibleInBoard()
                .stream().map(this::toDTO).toList();

        return PriceBoardResponse.builder()
                .shopName(shopInfo.getShopName())
                .shopAddress(shopInfo.getAddress())
                .prices(prices)
                .build();
    }

    private GoldPriceDTO toDTO(GoldPrice g) {
        return GoldPriceDTO.builder()
                .id(g.getId())
                .code(g.getCode())
                .label(g.getLabel())
                .buy(g.getBuy())
                .sell(g.getSell())
                .pawn(g.getPawn())
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
