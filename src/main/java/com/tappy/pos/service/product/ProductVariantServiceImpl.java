package com.tappy.pos.service.product;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.product.GenerateVariantsRequest;
import com.tappy.pos.model.dto.product.ProductVariantDTO;
import com.tappy.pos.model.dto.product.SaveProductVariantRequest;
import com.tappy.pos.model.entity.inventory.Inventory;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.model.entity.product.VariantType;
import com.tappy.pos.model.entity.product.VariantTypeOption;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.repository.product.VariantTypeRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final VariantTypeRepository variantTypeRepository;
    private final InventoryRepository inventoryRepository;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantDTO> getVariants(Long productId) {
        requireProduct(productId);
        return productVariantRepository.findByProductIdAndDeletedAtIsNull(productId)
                .stream()
                .map(v -> mapToDTO(v, v.getProduct().getPrice()))
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantDTO createVariant(Long productId, SaveProductVariantRequest req) {
        Product product = requireProduct(productId);
        checkSkuUnique(req.getSku());
        boolean isFirstVariant = !productVariantRepository.existsByProductIdAndDeletedAtIsNull(productId);
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .tenantId(product.getTenantId())
                .sku(req.getSku())
                .barcode(req.getBarcode())
                .variantOptions(req.getVariantOptions() != null ? req.getVariantOptions() : Map.of())
                .priceOverride(req.getPriceOverride())
                .costOverride(req.getCostOverride())
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        ProductVariant saved = productVariantRepository.save(variant);
        createVariantInventory(saved, product);
        if (isFirstVariant) {
            zeroOutProductLevelInventory(productId);
        }
        log.info("Created variant {} for product {}", saved.getId(), productId);
        return mapToDTO(saved, product.getPrice());
    }

    @Override
    public ProductVariantDTO updateVariant(Long productId, Long variantId, SaveProductVariantRequest req) {
        Product product = requireProduct(productId);
        ProductVariant variant = productVariantRepository
                .findByIdAndProductIdAndDeletedAtIsNull(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found", variantId)));

        // Only enforce uniqueness if SKU actually changed
        if (!variant.getSku().equals(req.getSku())) {
            checkSkuUnique(req.getSku());
        }

        variant.setSku(req.getSku());
        variant.setBarcode(req.getBarcode());
        variant.setVariantOptions(req.getVariantOptions() != null ? req.getVariantOptions() : Map.of());
        variant.setPriceOverride(req.getPriceOverride());
        variant.setCostOverride(req.getCostOverride());

        ProductVariant saved = productVariantRepository.save(variant);
        log.info("Updated variant {} for product {}", variantId, productId);
        return mapToDTO(saved, product.getPrice());
    }

    @Override
    public void deleteVariant(Long productId, Long variantId) {
        requireProduct(productId);
        ProductVariant variant = productVariantRepository
                .findByIdAndProductIdAndDeletedAtIsNull(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found", variantId)));
        inventoryRepository.findByProductIdAndVariantId(productId, variantId).ifPresent(inv -> {
            if (inv.getQuantityInStock() != null && inv.getQuantityInStock() > 0) {
                throw new BadRequestException(
                        messageService.getMessage("error.variant.delete.has.stock", inv.getQuantityInStock()));
            }
        });
        variant.setDeletedAt(LocalDateTime.now());
        variant.setDeleted(true);
        productVariantRepository.save(variant);
        log.info("Deleted variant {} for product {}", variantId, productId);
    }

    @Override
    public List<ProductVariantDTO> generateVariants(Long productId, GenerateVariantsRequest req) {
        Product product = requireProduct(productId);
        String baseSkuStr = (req.getBaseSku() != null && !req.getBaseSku().isBlank())
                ? req.getBaseSku().trim()
                : product.getSku();

        boolean isFirstVariantBatch = !productVariantRepository.existsByProductIdAndDeletedAtIsNull(productId);

        // Load variant types with options (ordered)
        List<VariantType> variantTypes = req.getVariantTypeIds().stream()
                .map(id -> variantTypeRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Variant type not found: " + id)))
                .collect(Collectors.toList());

        // Build list of option-lists per type for cartesian product
        List<Map.Entry<String, List<String>>> typesWithValues = variantTypes.stream()
                .map(vt -> Map.entry(
                        vt.getName(),
                        vt.getOptions().stream()
                                .map(VariantTypeOption::getValue)
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        // Compute cartesian product of all option combinations
        List<Map<String, String>> combinations = cartesianProduct(typesWithValues);

        List<ProductVariantDTO> created = new ArrayList<>();
        for (Map<String, String> combo : combinations) {
            // Build SKU suffix from option values
            String suffix = combo.values().stream()
                    .map(v -> v.toUpperCase().replace(" ", ""))
                    .collect(Collectors.joining("-"));
            String sku = baseSkuStr + "-" + suffix;

            // Skip if SKU already exists
            if (productVariantRepository.existsBySkuAndDeletedAtIsNull(sku)) {
                log.debug("Skipping existing variant SKU: {}", sku);
                continue;
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .tenantId(product.getTenantId())
                    .sku(sku)
                    .variantOptions(new LinkedHashMap<>(combo))
                    .status(ProductVariant.VariantStatus.ACTIVE)
                    .build();
            ProductVariant saved = productVariantRepository.save(variant);
            createVariantInventory(saved, product);
            created.add(mapToDTO(saved, product.getPrice()));
            log.debug("Generated variant {} (SKU: {})", saved.getId(), sku);
        }

        if (isFirstVariantBatch && !created.isEmpty()) {
            zeroOutProductLevelInventory(productId);
        }

        log.info("Generated {} variants for product {}", created.size(), productId);
        return created;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void createVariantInventory(ProductVariant variant, Product product) {
        if (inventoryRepository.findByProductIdAndVariantId(product.getId(), variant.getId()).isPresent()) {
            return; // Already exists (e.g. retry after partial failure)
        }
        Inventory inv = Inventory.builder()
                .tenantId(product.getTenantId())
                .product(product)
                .variant(variant)
                .quantityInStock(0L)
                .reorderLevel(10L)
                .reorderQuantity(50L)
                .unitCost(product.getCostPrice() != null ? product.getCostPrice() : java.math.BigDecimal.ZERO)
                .warehouseLocation("Kho chính")
                .status(Inventory.InventoryStatus.ACTIVE)
                .inventoryType(Inventory.InventoryType.RETAIL)
                .build();
        inventoryRepository.save(inv);
        log.debug("Created inventory record for variant {} (product {})", variant.getId(), product.getId());
    }

    private void zeroOutProductLevelInventory(Long productId) {
        inventoryRepository.findProductLevelInventory(productId).ifPresent(inv -> {
            if (inv.getQuantityInStock() > 0) {
                log.info("Zeroing product-level stock ({} units) for product {} — variants now track stock",
                        inv.getQuantityInStock(), productId);
                inv.setQuantityInStock(0L);
                inventoryRepository.save(inv);
            }
        });
    }

    private Product requireProduct(Long productId) {
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found", productId)));
    }

    private void checkSkuUnique(String sku) {
        if (productVariantRepository.existsBySkuAndDeletedAtIsNull(sku)) {
            throw new DuplicateResourceException(
                    messageService.getMessage("error.product.sku.duplicate", sku));
        }
    }

    private ProductVariantDTO mapToDTO(ProductVariant v, java.math.BigDecimal parentPrice) {
        Long qty = inventoryRepository.findByProductIdAndVariantId(v.getProduct().getId(), v.getId())
                .map(inv -> inv.getQuantityInStock())
                .orElse(null);
        return ProductVariantDTO.builder()
                .id(v.getId())
                .productId(v.getProduct().getId())
                .sku(v.getSku())
                .barcode(v.getBarcode())
                .variantOptions(v.getVariantOptions())
                .priceOverride(v.getPriceOverride())
                .price(v.getPriceOverride() != null ? v.getPriceOverride() : parentPrice)
                .costOverride(v.getCostOverride())
                .status(v.getStatus().name())
                .quantityInStock(qty)
                .build();
    }

    /**
     * Computes the cartesian product of a list of (typeName → option-values) pairs.
     * Returns a list of option-maps, one per unique combination.
     */
    private List<Map<String, String>> cartesianProduct(List<Map.Entry<String, List<String>>> types) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());

        for (Map.Entry<String, List<String>> entry : types) {
            String typeName = entry.getKey();
            List<String> values = entry.getValue();
            List<Map<String, String>> expanded = new ArrayList<>();
            for (Map<String, String> existing : result) {
                for (String val : values) {
                    Map<String, String> combo = new LinkedHashMap<>(existing);
                    combo.put(typeName, val);
                    expanded.add(combo);
                }
            }
            result = expanded;
        }
        return result;
    }
}
