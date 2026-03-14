package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.promotion.ApplyPromotionResponse;
import com.knp.model.dto.promotion.PromotionDTO;
import com.knp.model.dto.promotion.SavePromotionRequest;
import com.knp.model.entity.Promotion;
import com.knp.model.enums.DiscountType;
import com.knp.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public Page<PromotionDTO> getAll(Pageable pageable) {
        return promotionRepository.findAllActive(pageable).map(this::mapToDTO);
    }

    public PromotionDTO getById(Long id) {
        return promotionRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + id));
    }

    @Transactional
    public PromotionDTO create(SavePromotionRequest req) {
        if (promotionRepository.findByCode(req.getCode().toUpperCase().trim()).isPresent()) {
            throw new BadRequestException("Promotion code already exists: " + req.getCode());
        }
        Promotion promo = Promotion.builder()
                .name(req.getName())
                .code(req.getCode().toUpperCase().trim())
                .type(req.getType())
                .value(req.getValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .usageLimit(req.getUsageLimit())
                .usedCount(0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .description(req.getDescription())
                .build();
        return mapToDTO(promotionRepository.save(promo));
    }

    @Transactional
    public PromotionDTO update(Long id, SavePromotionRequest req) {
        Promotion promo = promotionRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + id));

        String newCode = req.getCode().toUpperCase().trim();
        if (!newCode.equals(promo.getCode())) {
            promotionRepository.findByCode(newCode).ifPresent(existing -> {
                throw new BadRequestException("Promotion code already exists: " + newCode);
            });
            promo.setCode(newCode);
        }

        promo.setName(req.getName());
        promo.setType(req.getType());
        promo.setValue(req.getValue());
        promo.setMinOrderAmount(req.getMinOrderAmount());
        promo.setMaxDiscountAmount(req.getMaxDiscountAmount());
        promo.setStartDate(req.getStartDate());
        promo.setEndDate(req.getEndDate());
        promo.setUsageLimit(req.getUsageLimit());
        if (req.getIsActive() != null) promo.setIsActive(req.getIsActive());
        promo.setDescription(req.getDescription());

        return mapToDTO(promotionRepository.save(promo));
    }

    @Transactional
    public void delete(Long id) {
        Promotion promo = promotionRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + id));
        promo.softDelete();
        promotionRepository.save(promo);
    }

    /**
     * Validate and calculate discount for a promotion code against an order subtotal.
     * Does NOT increment usedCount — that happens at checkout.
     */
    public ApplyPromotionResponse validatePromotion(String code, BigDecimal orderSubtotal) {
        Promotion promo = promotionRepository.findValidPromotion(code.toUpperCase().trim(), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired promotion code: " + code));

        if (promo.getMinOrderAmount() != null
                && orderSubtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Order must be at least " + promo.getMinOrderAmount() + " to use this promotion");
        }

        BigDecimal discount = calculateDiscount(promo, orderSubtotal);
        return ApplyPromotionResponse.builder()
                .code(promo.getCode())
                .name(promo.getName())
                .discountAmount(discount)
                .message("Promotion applied: " + promo.getName())
                .build();
    }

    /**
     * Apply promotion at checkout — validates, calculates discount, increments usedCount.
     * Returns the discount amount. Throws BadRequestException if invalid.
     */
    @Transactional
    public BigDecimal applyAtCheckout(String code, BigDecimal orderSubtotal) {
        Promotion promo = promotionRepository.findValidPromotion(code.toUpperCase().trim(), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired promotion code: " + code));

        if (promo.getMinOrderAmount() != null
                && orderSubtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Order must be at least " + promo.getMinOrderAmount() + " to use this promotion");
        }

        BigDecimal discount = calculateDiscount(promo, orderSubtotal);
        promo.setUsedCount(promo.getUsedCount() + 1);
        promotionRepository.save(promo);
        log.info("Applied promotion {} to order (discount={})", code, discount);
        return discount;
    }

    private BigDecimal calculateDiscount(Promotion promo, BigDecimal subtotal) {
        BigDecimal discount;
        if (promo.getType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(promo.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (promo.getMaxDiscountAmount() != null) {
                discount = discount.min(promo.getMaxDiscountAmount());
            }
        } else {
            discount = promo.getValue().setScale(2, RoundingMode.HALF_UP);
        }
        return discount.min(subtotal);
    }

    private PromotionDTO mapToDTO(Promotion p) {
        return PromotionDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .type(p.getType())
                .value(p.getValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .isActive(p.getIsActive())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
