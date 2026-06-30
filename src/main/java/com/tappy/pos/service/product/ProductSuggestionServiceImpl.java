package com.tappy.pos.service.product;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.product.ProductSuggestionDTO;
import com.tappy.pos.model.dto.product.ProductSuggestionRequest;
import com.tappy.pos.model.entity.product.ProductSuggestion;
import com.tappy.pos.repository.product.ProductSuggestionRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSuggestionServiceImpl implements ProductSuggestionService {

    private final ProductSuggestionRepository productSuggestionRepository;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSuggestionDTO> search(String name, String shopType, String productType, int page, int size) {
        String n = (name == null) ? "" : name.trim();
        String st = (shopType == null) ? "" : shopType.trim();
        String pt = (productType == null) ? "" : productType.trim();
        // Native query already has ORDER BY — pass an UNSORTED PageRequest.
        PageRequest pageable = PageRequest.of(page, size);
        Page<ProductSuggestion> entities = productSuggestionRepository.search(n, st, pt, pageable);
        return entities.map(this::toDTO);
    }

    @Override
    @Transactional
    public ProductSuggestionDTO create(ProductSuggestionRequest request) {
        ProductSuggestion entity = ProductSuggestion.builder()
                .name(request.getName())
                .emoji(request.getEmoji())
                .defaultPrice(request.getDefaultPrice())
                .unit(request.getUnit())
                .productTypeCode(request.getProductTypeCode())
                .dynamicPrice(request.getDynamicPrice())
                .shopTypes(request.getShopTypes())
                .displayOrder(request.getDisplayOrder())
                .categoryName(request.getCategoryName())
                .nameEn(request.getNameEn())
                .durationMinutes(request.getDurationMinutes())
                .build();
        try {
            ProductSuggestion saved = productSuggestionRepository.save(entity);
            return toDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException(messageService.getMessage("error.product.suggestion.name_exists"));
        }
    }

    @Override
    @Transactional
    public ProductSuggestionDTO update(Long id, ProductSuggestionRequest request) {
        ProductSuggestion entity = productSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.suggestion.not_found")));
        entity.setName(request.getName());
        entity.setEmoji(request.getEmoji());
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setUnit(request.getUnit());
        entity.setProductTypeCode(request.getProductTypeCode());
        entity.setDynamicPrice(request.getDynamicPrice());
        entity.setShopTypes(request.getShopTypes());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setCategoryName(request.getCategoryName());
        entity.setNameEn(request.getNameEn());
        entity.setDurationMinutes(request.getDurationMinutes());
        try {
            ProductSuggestion saved = productSuggestionRepository.save(entity);
            return toDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException(messageService.getMessage("error.product.suggestion.name_exists"));
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productSuggestionRepository.existsById(id)) {
            throw new ResourceNotFoundException(messageService.getMessage("error.product.suggestion.not_found"));
        }
        productSuggestionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getProductTypeCodes() {
        return productSuggestionRepository.findDistinctProductTypeCodes();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private ProductSuggestionDTO toDTO(ProductSuggestion entity) {
        return ProductSuggestionDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .emoji(entity.getEmoji())
                .defaultPrice(entity.getDefaultPrice())
                .unit(entity.getUnit())
                .productTypeCode(entity.getProductTypeCode())
                .dynamicPrice(entity.getDynamicPrice())
                .shopTypes(entity.getShopTypes())
                .displayOrder(entity.getDisplayOrder())
                .categoryName(entity.getCategoryName())
                .nameEn(entity.getNameEn())
                .durationMinutes(entity.getDurationMinutes())
                .build();
    }
}
