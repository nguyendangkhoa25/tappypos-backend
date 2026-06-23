package com.tappy.pos.service.product;

import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.entity.inventory.Inventory;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.multitenant.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.tappy.pos.model.dto.product.*;
import com.tappy.pos.model.entity.product.*;
import com.tappy.pos.model.entity.vendor.*;
import com.tappy.pos.repository.product.*;
import com.tappy.pos.repository.vendor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.storage.R2StorageService;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.client.GoogleBooksClient;
import com.tappy.pos.client.OpenFoodFactsClient;
import com.tappy.pos.util.BarcodeValidator;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeGroupRepository attributeGroupRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;
    private final ProductCatalogRepository productCatalogRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final GoogleBooksClient googleBooksClient;
    private final ProductCatalogService productCatalogService;
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantService productVariantService;
    private final com.tappy.pos.repository.modifier.ProductModifierGroupRepository productModifierGroupRepository;
    private final R2StorageService r2StorageService;
    private final R2CleanupService r2CleanupService;
    private final com.tappy.pos.repository.order.OrderItemRepository orderItemRepository;

    @Override
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("Creating product: {}, type: {}", request.getName(), request.getProductTypeId());

        // Check if product with SKU already exists
        if (productRepository.findBySkuAndDeletedFalse(request.getSku()).isPresent()) {
            String errorMsg = messageService.getMessage("error.product.sku.duplicate", request.getSku());
            log.error("SKU already exists: {}", request.getSku());
            throw new DuplicateResourceException(errorMsg);
        }

        // Get product type
        ProductType productType = productTypeRepository.findById(request.getProductTypeId())
                .orElseThrow(() -> {
                    String errorMsg = messageService.getMessage("error.product.type.not.found", request.getProductTypeId());
                    log.error("Product type not found: {}", request.getProductTypeId());
                    return new ResourceNotFoundException(errorMsg);
                });

        Vendor vendor = null;
        if (request.getVendorId() != null) {
            vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", request.getVendorId())));
        }

        // Create product
        Product product = Product.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .productType(productType)
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .costPrice(request.getCostPrice() != null ? request.getCostPrice() : java.math.BigDecimal.ZERO)
                .commissionRate(request.getCommissionRate())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0)
                .unit(request.getUnit())
                .altUnit(request.getAltUnit())
                .altUnitFactor(request.getAltUnitFactor())
                .altUnitPrice(request.getAltUnitPrice())
                .wholesalePrice(request.getWholesalePrice())
                .shelfLocation(request.getShelfLocation())
                .sourcePawnId(request.getSourcePawnId())
                .inventoryMode(com.tappy.pos.model.enums.InventoryMode.derive(productType.getCode(), request.getSourcePawnId()))
                .productKind(request.getProductKind() != null
                        ? com.tappy.pos.model.enums.ProductKind.valueOf(request.getProductKind())
                        : com.tappy.pos.model.enums.ProductKind.FINISHED)
                .vendor(vendor)
                .status(Product.ProductStatus.valueOf(request.getStatus()))
                .deleted(false)
                .build();

        // Add categories if provided
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findByIdAndDeletedFalse(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", categoryId)));
                product.getCategories().add(category);
            }
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product saved with id: {}", savedProduct.getId());

        // Set attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getAttributes().entrySet()) {
                Object value = entry.getValue();
                
                // Skip null values
                if (value == null) {
                    log.debug("Skipping null attribute: {}", entry.getKey());
                    continue;
                }
                
                log.debug("Processing attribute: {} with value: '{}' (type: {})", 
                    entry.getKey(), value, value.getClass().getSimpleName());
                
                AttributeDefinition attrDef = attributeDefinitionRepository
                        .findByCodeAndProductTypeId(entry.getKey(), request.getProductTypeId())
                        .orElse(null);
                if (attrDef == null) {
                    log.warn("No attribute definition for code '{}' on type {}, skipping", entry.getKey(), request.getProductTypeId());
                    continue;
                }

                log.debug("Found attribute definition: {}, data type: {}", entry.getKey(), attrDef.getDataType());

                ProductAttributeValue attrValue = ProductAttributeValue.builder()
                        .product(savedProduct)
                        .attribute(attrDef)
                        .tenantId(tenantContext.getCurrentTenantId())
                        .deleted(false)
                        .build();
                
                try {
                    attrValue.setValue(value);
                    
                    Object savedValue = switch(attrDef.getDataType()) {
                        case NUMBER -> attrValue.getValueNumber();
                        case BOOLEAN -> attrValue.getValueBoolean();
                        case DATE -> attrValue.getValueDate();
                        default -> attrValue.getValueString();
                    };
                    
                    log.debug("Set attribute value: {} = {} (stored as type: {})", entry.getKey(), value, savedValue);
                    productAttributeValueRepository.save(attrValue);
                    log.info("Saved attribute: {} with value: {}", entry.getKey(), savedValue);
                } catch (Exception e) {
                    log.error("Error setting attribute {} with value {}: {}", entry.getKey(), value, e.getMessage(), e);
                    throw e;
                }
            }
            log.info("Product attributes saved for product id: {}", savedProduct.getId());
        }

        // UNIQUE products are single physical items — auto-create inventory = 1 at creation.
        // TRACKED products require manual stock entry; NO_INVENTORY skips inventory entirely.
        if (savedProduct.getInventoryMode() == com.tappy.pos.model.enums.InventoryMode.UNIQUE) {
            String location = "Quầy";
            if (request.getAttributes() != null) {
                Object counterCode = request.getAttributes().get("counter_code");
                if (counterCode instanceof String s && !s.isBlank()) {
                    location = s;
                }
            }
            Inventory inventory = Inventory.builder()
                    .tenantId(tenantContext.getCurrentTenantId())
                    .product(savedProduct)
                    .quantityInStock(1L)
                    .reorderLevel(0L)
                    .reorderQuantity(1L)
                    .unitCost(savedProduct.getCostPrice())
                    .warehouseLocation(location)
                    .deleted(false)
                    .build();
            inventoryRepository.save(inventory);
            log.info("Auto-created 1-unit inventory for UNIQUE product {}", savedProduct.getId());
        } else if (savedProduct.getInventoryMode() == com.tappy.pos.model.enums.InventoryMode.TRACKED
                && request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            Inventory inventory = Inventory.builder()
                    .tenantId(tenantContext.getCurrentTenantId())
                    .product(savedProduct)
                    .quantityInStock((long) request.getInitialQuantity())
                    .reorderLevel(0L)
                    .reorderQuantity(1L)
                    .unitCost(savedProduct.getCostPrice())
                    .warehouseLocation("Quầy")
                    .deleted(false)
                    .build();
            inventoryRepository.save(inventory);
            log.info("Created inventory qty={} for TRACKED product {}", request.getInitialQuantity(), savedProduct.getId());
        }

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", savedProduct.getId().toString(),
                "activity.product.created", null, savedProduct.getName());

        return mapToDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.info("Getting product by id: {}", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    String errorMsg = messageService.getMessage("error.product.not.found", id);
                    log.error("Product not found: {}", id);
                    return new ResourceNotFoundException(errorMsg);
                });
        return mapToDTO(product);
    }

    @Override
    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product: {}", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    String errorMsg = messageService.getMessage("error.product.not.found", id);
                    log.error("Product not found: {}", id);
                    return new ResourceNotFoundException(errorMsg);
                });

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBarcode(request.getBarcode());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice() != null ? request.getCostPrice() : java.math.BigDecimal.ZERO);
        // commissionRate: null = inherit employee default; explicit value (incl. 0) = override
        product.setCommissionRate(request.getCommissionRate());
        if (request.getDurationMinutes() != null) {
            product.setDurationMinutes(request.getDurationMinutes());
        }
        product.setUnit(request.getUnit());
        product.setAltUnit(request.getAltUnit());
        product.setAltUnitFactor(request.getAltUnitFactor());
        product.setAltUnitPrice(request.getAltUnitPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setShelfLocation(request.getShelfLocation());
        product.setStatus(Product.ProductStatus.valueOf(request.getStatus()));
        if (request.getProductKind() != null && !request.getProductKind().isBlank()) {
            product.setProductKind(com.tappy.pos.model.enums.ProductKind.valueOf(request.getProductKind()));
        }

        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", request.getVendorId())));
            product.setVendor(vendor);
        } else {
            product.setVendor(null);
        }

        // Update categories
        if (request.getCategoryIds() != null) {
            product.getCategories().clear();
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findByIdAndDeletedFalse(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", categoryId)));
                product.getCategories().add(category);
            }
        }

        // Update attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getAttributes().entrySet()) {
                Object value = entry.getValue();
                
                // Skip null values
                if (value == null) {
                    log.debug("Skipping null attribute: {}", entry.getKey());
                    continue;
                }
                
                log.debug("Updating attribute: {} with value: '{}' (type: {})", 
                    entry.getKey(), value, value.getClass().getSimpleName());
                
                AttributeDefinition attrDef = attributeDefinitionRepository
                        .findByCodeAndProductTypeId(entry.getKey(), product.getProductType().getId())
                        .orElse(null);
                if (attrDef == null) {
                    log.warn("No attribute definition for code '{}' on type {}, skipping", entry.getKey(), product.getProductType().getId());
                    continue;
                }

                log.debug("Found attribute definition: {}, data type: {}", entry.getKey(), attrDef.getDataType());

                ProductAttributeValue attrValue = productAttributeValueRepository
                        .findByProductIdAndAttributeId(product.getId(), attrDef.getId())
                        .orElse(ProductAttributeValue.builder()
                                .product(product)
                                .attribute(attrDef)
                                .tenantId(tenantContext.getCurrentTenantId())
                                .deleted(false)
                                .build());
                
                try {
                    attrValue.setValue(value);
                    
                    Object savedValue = switch(attrDef.getDataType()) {
                        case NUMBER -> attrValue.getValueNumber();
                        case BOOLEAN -> attrValue.getValueBoolean();
                        case DATE -> attrValue.getValueDate();
                        default -> attrValue.getValueString();
                    };
                    
                    log.debug("Set attribute value: {} = {} (stored as type: {})", entry.getKey(), value, savedValue);
                    productAttributeValueRepository.save(attrValue);
                    log.info("Updated attribute: {} with value: {}", entry.getKey(), savedValue);
                } catch (Exception e) {
                    log.error("Error updating attribute {} with value {}: {}", entry.getKey(), value, e.getMessage(), e);
                    throw e;
                }
            }
            log.info("Product attributes updated for product id: {}", id);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: {}", id);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_UPDATED, "PRODUCT", updated.getId().toString(),
                "activity.product.updated", null, updated.getName());

        return mapToDTO(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    String errorMsg = messageService.getMessage("error.product.not.found", id);
                    log.error("Product not found: {}", id);
                    return new ResourceNotFoundException(errorMsg);
                });
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String productName = product.getName();
        String oldImageUrl = product.getImageUrl();
        product.softDelete();
        productRepository.save(product);
        log.info("Product deleted: {}", id);

        // Fire-and-forget: clean up R2 image if present
        r2CleanupService.deleteAsync(r2StorageService.keyFromUrl(oldImageUrl));

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_DELETED, "PRODUCT", id.toString(),
                "activity.product.deleted", null, productName);
    }

    @Override
    public ProductDTO uploadImage(Long id, MultipartFile file) {
        // Validate MIME type via MessageService (supports i18n)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/jpeg")
                && !contentType.startsWith("image/png")
                && !contentType.startsWith("image/webp"))) {
            throw new IllegalArgumentException(
                    messageService.getMessage("error.product.image.invalid.type"));
        }

        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found", id)));

        String oldImageKey = r2StorageService.keyFromUrl(product.getImageUrl());

        // Resize to max 1024px, output as JPEG at 85% quality (corrects EXIF orientation)
        byte[] compressed;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(1024, 1024)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            compressed = out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(
                    messageService.getMessage("error.product.image.process.failed"), e);
        }

        // Upload new image first — only delete old after the new one is safely stored
        String key = "products/" + tenantContext.getCurrentTenantId() + "/" + id + ".jpg";
        String url = r2StorageService.upload(key, compressed, "image/jpeg");

        product.setImageUrl(url.isBlank() ? null : url);
        productRepository.save(product);

        // Fire-and-forget: remove old R2 object now that DB is committed
        r2CleanupService.deleteAsync(oldImageKey);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_IMAGE_UPDATED, "PRODUCT", id.toString(),
                "activity.product.image.updated", null, product.getName());

        log.info("Product image uploaded — productId: {}, key: {}", id, key);
        return mapToDTO(product);
    }

    @Override
    public void deleteImage(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found", id)));

        String oldImageKey = r2StorageService.keyFromUrl(product.getImageUrl());
        product.setImageUrl(null);
        productRepository.save(product);

        // Fire-and-forget: remove from R2 after DB is committed
        r2CleanupService.deleteAsync(oldImageKey);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_IMAGE_DELETED, "PRODUCT", id.toString(),
                "activity.product.image.deleted", null, product.getName());

        log.info("Product image deleted — productId: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(String status, Long categoryId, Long productTypeId, boolean pawnOriginOnly, Pageable pageable) {
        log.info("Getting all products with status: {}, categoryId: {}, productTypeId: {}, pawnOriginOnly: {}", status, categoryId, productTypeId, pawnOriginOnly);
        Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status.toUpperCase());
        Page<Product> products;
        if (productTypeId != null) {
            // Type filter: ignores categoryId (pawn sell screen uses type chips, not tenant categories)
            products = pawnOriginOnly
                    ? productRepository.findByDeletedFalseAndStatusAndProductTypeIdAndSourcePawnIdIsNotNullOrderByCreatedAtDesc(productStatus, productTypeId, pageable)
                    : productRepository.findByDeletedFalseAndStatusAndProductTypeIdOrderByCreatedAtDesc(productStatus, productTypeId, pageable);
        } else if (pawnOriginOnly) {
            products = categoryId != null
                    ? productRepository.findByStatusAndCategoryIdAndSourcePawnIdIsNotNull(productStatus, categoryId, pageable)
                    : productRepository.findByDeletedFalseAndStatusAndSourcePawnIdIsNotNullOrderByCreatedAtDesc(productStatus, pageable);
        } else {
            products = categoryId != null
                    ? productRepository.findByStatusAndCategoryId(productStatus, categoryId, pageable)
                    : productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(productStatus, pageable);
        }

        List<Long> productIds = products.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        Set<Long> productIdsWithVariants = productIds.isEmpty()
                ? Collections.emptySet()
                : productVariantRepository.findProductIdsWithActiveVariants(productIds);
        Set<Long> productIdsWithModifiers = productIds.isEmpty()
                ? Collections.emptySet()
                : productModifierGroupRepository.findProductIdsWithModifiers(productIds);

        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId()), productIdsWithModifiers.contains(p.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByType(Long productTypeId, Pageable pageable) {
        log.info("Getting products by type: {}", productTypeId);
        Page<Product> products = productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(productTypeId, pageable);
        List<Long> productIds = products.getContent().stream().map(Product::getId).collect(Collectors.toList());
        Set<Long> productIdsWithVariants = productIds.isEmpty()
                ? Collections.emptySet()
                : productVariantRepository.findProductIdsWithActiveVariants(productIds);
        Set<Long> productIdsWithModifiers = productIds.isEmpty()
                ? Collections.emptySet()
                : productModifierGroupRepository.findProductIdsWithModifiers(productIds);
        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId()), productIdsWithModifiers.contains(p.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String searchTerm, Pageable pageable) {
        log.info("Searching products with term: {}", searchTerm);
        Page<Product> products = productRepository.searchByKeyword(searchTerm.toLowerCase(), pageable);
        List<Long> productIds = products.getContent().stream().map(Product::getId).collect(Collectors.toList());
        Set<Long> productIdsWithVariants = productIds.isEmpty()
                ? Collections.emptySet()
                : productVariantRepository.findProductIdsWithActiveVariants(productIds);
        Set<Long> productIdsWithModifiers = productIds.isEmpty()
                ? Collections.emptySet()
                : productModifierGroupRepository.findProductIdsWithModifiers(productIds);
        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId()), productIdsWithModifiers.contains(p.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductTypeDTO> getAllProductTypes() {
        log.info("Getting all product types");
        return productTypeRepository.findAll().stream()
                .map(pt -> ProductTypeDTO.builder()
                        .id(pt.getId())
                        .code(pt.getCode())
                        .name(pt.getName())
                        .description(pt.getDescription())
                        .defaultInventoryMode(pt.getDefaultInventoryMode().name())
                        .defaultUnit(pt.getDefaultUnit() != null ? pt.getDefaultUnit() : "piece")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTypeWithAttributesDTO getProductTypeWithAttributes(Long productTypeId) {
        log.info("Getting product type with attributes: {}", productTypeId);
        ProductType productType = productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> {
                    String errorMsg = messageService.getMessage("error.product.type.not.found", productTypeId);
                    log.error("Product type not found: {}", productTypeId);
                    return new ResourceNotFoundException(errorMsg);
                });

        List<AttributeGroup> groups = attributeGroupRepository
                .findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(productTypeId);

        List<AttributeGroupDTO> groupDTOs = groups.stream()
                .map(group -> {
                    List<AttributeDefinition> attrs = attributeDefinitionRepository
                            .findByAttributeGroupIdAndDeletedFalse(group.getId());
                    List<AttributeDefinitionDTO> attrDTOs = attrs.stream()
                            .map(this::mapAttributeToDTO)
                            .collect(Collectors.toList());
                    return AttributeGroupDTO.builder()
                            .id(group.getId())
                            .code(group.getCode())
                            .name(group.getName())
                            .displayOrder(group.getDisplayOrder())
                            .attributes(attrDTOs)
                            .build();
                })
                .collect(Collectors.toList());

        return ProductTypeWithAttributesDTO.builder()
                .id(productType.getId())
                .code(productType.getCode())
                .name(productType.getName())
                .description(productType.getDescription())
                .attributeGroups(groupDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSku(String name, String typeCode) {
        log.info("Generating SKU for name: {}, typeCode: {}", name, typeCode);
        String prefix = buildSkuPrefix(name, typeCode);
        List<String> existing = productRepository.findSkusByPrefix(prefix + "-");
        int nextSeq = findNextSequence(existing);
        String sku = String.format("%s-%03d", prefix, nextSeq);
        log.info("Generated SKU: {}", sku);
        return sku;
    }

    private String buildSkuPrefix(String name, String typeCode) {
        String type4 = typeCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (type4.length() > 4) type4 = type4.substring(0, 4);
        String abbrev = buildNameAbbrev(name);
        return type4 + "-" + abbrev;
    }

    private String buildNameAbbrev(String name) {
        String clean = name.replaceAll("[^A-Za-z0-9\\s]", "").trim().toUpperCase();
        String[] words = clean.split("\\s+");
        String abbrev;
        if (words.length == 0 || clean.isEmpty()) {
            abbrev = "PROD";
        } else if (words.length == 1) {
            abbrev = words[0].length() >= 6 ? words[0].substring(0, 6) : words[0];
        } else {
            String part1 = words[0].length() >= 3 ? words[0].substring(0, 3) : words[0];
            String part2 = words[1].length() >= 3 ? words[1].substring(0, 3) : words[1];
            abbrev = part1 + part2;
        }
        return abbrev.length() > 6 ? abbrev.substring(0, 6) : abbrev;
    }

    private int findNextSequence(List<String> existingSkus) {
        int max = 0;
        for (String sku : existingSkus) {
            String[] parts = sku.split("-");
            try {
                int seq = Integer.parseInt(parts[parts.length - 1]);
                if (seq > max) max = seq;
            } catch (NumberFormatException ignored) {}
        }
        return max + 1;
    }

    private ProductDTO mapToDTOWithVariantFlag(Product product, boolean hasVariants, boolean hasModifiers) {
        Map<String, Object> attributes = new HashMap<>();
        for (ProductAttributeValue attrValue : product.getAttributeValues()) {
            attributes.put(attrValue.getAttribute().getCode(), attrValue.getValue());
        }

        Set<Long> categoryIds = product.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Set<String> categoryNames = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        boolean tracksStock = product.getInventoryMode() != com.tappy.pos.model.enums.InventoryMode.NO_INVENTORY;
        Long stockQuantity = null;
        Boolean inStock = null;
        if (tracksStock) {
            Optional<Inventory> inv = inventoryRepository.findByProductId(product.getId());
            if (inv.isPresent()) {
                stockQuantity = inv.get().getQuantityInStock();
                inStock = stockQuantity > 0;
            } else {
                inStock = false;
            }
        }

        return ProductDTO.builder()
                .id(product.getId())
                .productTypeId(product.getProductType().getId())
                .productTypeName(product.getProductType().getName())
                .productTypeCode(product.getProductType().getCode())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .commissionRate(product.getCommissionRate())
                .durationMinutes(product.getDurationMinutes())
                .unit(product.getUnit())
                .altUnit(product.getAltUnit())
                .altUnitFactor(product.getAltUnitFactor())
                .altUnitPrice(product.getAltUnitPrice())
                .wholesalePrice(product.getWholesalePrice())
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .vendorName(product.getVendor() != null ? product.getVendor().getName() : null)
                .shelfLocation(product.getShelfLocation())
                .status(product.getStatus().toString())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .attributes(attributes)
                .hasVariants(hasVariants)
                .hasModifiers(hasModifiers)
                .stockQuantity(stockQuantity)
                .inStock(inStock)
                .imageUrl(product.getImageUrl())
                .sourcePawnId(product.getSourcePawnId())
                .inventoryMode(product.getInventoryMode().name())
                .productKind(product.getProductKind() != null ? product.getProductKind().name() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductDTO mapToDTO(Product product) {
        Map<String, Object> attributes = new HashMap<>();
        for (ProductAttributeValue attrValue : product.getAttributeValues()) {
            attributes.put(attrValue.getAttribute().getCode(), attrValue.getValue());
        }

        Set<Long> categoryIds = product.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Set<String> categoryNames = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        boolean hasVariants = productVariantRepository.existsByProductIdAndDeletedAtIsNull(product.getId());
        boolean hasModifiers = productModifierGroupRepository.existsByProductId(product.getId());

        boolean tracksStock = product.getInventoryMode() != com.tappy.pos.model.enums.InventoryMode.NO_INVENTORY;
        Long stockQuantity = null;
        Boolean inStock = null;
        if (tracksStock) {
            Optional<Inventory> inv = inventoryRepository.findByProductId(product.getId());
            if (inv.isPresent()) {
                stockQuantity = inv.get().getQuantityInStock();
                inStock = stockQuantity > 0;
            } else {
                inStock = false;
            }
        }

        return ProductDTO.builder()
                .id(product.getId())
                .productTypeId(product.getProductType().getId())
                .productTypeName(product.getProductType().getName())
                .productTypeCode(product.getProductType().getCode())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .commissionRate(product.getCommissionRate())
                .durationMinutes(product.getDurationMinutes())
                .unit(product.getUnit())
                .altUnit(product.getAltUnit())
                .altUnitFactor(product.getAltUnitFactor())
                .altUnitPrice(product.getAltUnitPrice())
                .wholesalePrice(product.getWholesalePrice())
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .vendorName(product.getVendor() != null ? product.getVendor().getName() : null)
                .shelfLocation(product.getShelfLocation())
                .status(product.getStatus().toString())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .attributes(attributes)
                .hasVariants(hasVariants)
                .hasModifiers(hasModifiers)
                .stockQuantity(stockQuantity)
                .inStock(inStock)
                .imageUrl(product.getImageUrl())
                .sourcePawnId(product.getSourcePawnId())
                .inventoryMode(product.getInventoryMode().name())
                .productKind(product.getProductKind() != null ? product.getProductKind().name() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private AttributeDefinitionDTO mapAttributeToDTO(AttributeDefinition attr) {
        return AttributeDefinitionDTO.builder()
                .id(attr.getId())
                .code(attr.getCode())
                .name(attr.getName())
                .dataType(attr.getDataType().toString())
                .required(attr.getRequired())
                .searchable(attr.getSearchable())
                .filterable(attr.getFilterable())
                .displayOrder(attr.getDisplayOrder())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeLookupResult lookupByBarcode(String barcode) {
        // Layer 0: a per-variant barcode (fashion size/color SKU) — resolve the exact variant
        // before the product-level barcode so scanning a SKU adds that exact size/color.
        var variantHit = productVariantService.findActiveByBarcode(barcode);
        if (variantHit.isPresent()) {
            var parent = productRepository.findByIdAndDeletedFalse(variantHit.get().getProductId());
            if (parent.isPresent()) {
                return BarcodeLookupResult.builder()
                        .source(BarcodeLookupResult.Source.SHOP_VARIANT)
                        .product(mapToDTO(parent.get()))
                        .variant(variantHit.get())
                        .build();
            }
        }

        // Layer 1: shop's own inventory
        var shopProduct = productRepository.findByBarcodeAndDeletedFalse(barcode);
        if (shopProduct.isPresent()) {
            return BarcodeLookupResult.builder()
                    .source(BarcodeLookupResult.Source.SHOP)
                    .product(mapToDTO(shopProduct.get()))
                    .build();
        }

        // Layer 2: master product catalog
        var catalogEntry = productCatalogRepository.findByBarcode(barcode);
        if (catalogEntry.isPresent()) {
            var c = catalogEntry.get();
            return BarcodeLookupResult.builder()
                    .source(BarcodeLookupResult.Source.CATALOG)
                    .catalog(BarcodeLookupResult.CatalogHint.builder()
                            .barcode(c.getBarcode())
                            .name(c.getName())
                            .brand(c.getBrand())
                            .categoryHint(c.getCategoryHint())
                            .unit(c.getUnit())
                            .description(c.getDescription())
                            .build())
                    .build();
        }

        // Layer 3a: live book lookup — an ISBN-13 (978/979 EAN-13) is a book and won't be in
        // Open Food Facts, so route it to Google Books before the OFF fetch.
        if (googleBooksClient.isEnabled() && BarcodeValidator.isIsbn13(barcode)) {
            var bookHit = googleBooksClient.fetchByIsbn(barcode);
            if (bookHit.isPresent()) {
                var b = bookHit.get();
                // Persist asynchronously so the caller gets a fast response
                productCatalogService.saveFromBookAsync(b);
                return BarcodeLookupResult.builder()
                        .source(BarcodeLookupResult.Source.CATALOG)
                        .catalog(BarcodeLookupResult.CatalogHint.builder()
                                .barcode(barcode)
                                .name(b.title != null ? b.title.trim() : barcode)
                                .brand(b.publisher != null ? b.publisher.trim() : b.author)
                                .categoryHint(b.category)
                                .unit("Cuốn")
                                .imageUrl(b.imageUrl)
                                .build())
                        .build();
            }
        }

        // Layer 3: live Open Food Facts lookup — only when enabled and barcode is structurally valid
        if (openFoodFactsClient.isEnabled() && BarcodeValidator.isValid(barcode)) {
            var offProduct = openFoodFactsClient.fetchByBarcode(barcode);
            if (offProduct.isPresent()) {
                var p = offProduct.get();
                // Persist asynchronously so the caller gets a fast response
                productCatalogService.saveFromOffAsync(p);
                return BarcodeLookupResult.builder()
                        .source(BarcodeLookupResult.Source.CATALOG)
                        .catalog(BarcodeLookupResult.CatalogHint.builder()
                                .barcode(barcode)
                                .name(p.product_name != null ? p.product_name.trim() : barcode)
                                .brand(p.brands != null ? p.brands.split(",")[0].trim() : null)
                                .imageUrl(p.image_front_url)
                                .build())
                        .build();
            }
        }

        return BarcodeLookupResult.builder()
                .source(BarcodeLookupResult.Source.NONE)
                .build();
    }

    @Override
    @Transactional
    public void markAsSold(Long productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new com.tappy.pos.exception.ResourceNotFoundException(
                        messageService.getMessage("product.not.found")));
        product.setStatus(Product.ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product {} marked as sold (status → INACTIVE)", productId);
    }

    @Override
    @Transactional
    public void setVisibility(Long id, boolean active) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new com.tappy.pos.exception.ResourceNotFoundException(
                        messageService.getMessage("product.not.found")));
        product.setStatus(active ? Product.ProductStatus.ACTIVE : Product.ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product {} visibility set to {}", id, active);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryDTO getSummary() {
        return ProductSummaryDTO.builder()
                .total(productRepository.countActive())
                .outOfStock(inventoryRepository.countOutOfStock())
                .lowStock(inventoryRepository.countLowStock())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatsDTO getProductStats(Long id, int days) {
        log.info("Request: Get stats for product {} days={}", id, days);
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(days);
        java.time.LocalDate today = java.time.LocalDate.now();

        // ── Core period stats ─────────────────────────────────────────────────
        List<Object[]> periodRows = orderItemRepository.getProductPeriodStats(id, from);
        Object[] row = periodRows.isEmpty() ? new Object[4] : periodRows.get(0);
        long orderCount = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long qtySold    = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        java.math.BigDecimal revenue = row[2] != null
                ? new java.math.BigDecimal(row[2].toString()) : java.math.BigDecimal.ZERO;
        java.time.LocalDateTime lastSoldAt = row[3] != null
                ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null;

        // ── Month-on-month comparison ─────────────────────────────────────────
        java.math.BigDecimal revenueThisMonth = orderItemRepository
                .getProductMonthRevenue(id, today.getYear(), today.getMonthValue());
        java.time.LocalDate lastMonth = today.minusMonths(1);
        java.math.BigDecimal revenueLastMonth = orderItemRepository
                .getProductMonthRevenue(id, lastMonth.getYear(), lastMonth.getMonthValue());

        // ── Top customers (top 3) ─────────────────────────────────────────────
        List<Object[]> customerRows = orderItemRepository.getTopCustomersForProduct(
                id, from, org.springframework.data.domain.PageRequest.of(0, 3));
        List<ProductStatsDTO.TopBuyerDTO> topCustomers = customerRows.stream()
                .map(r -> ProductStatsDTO.TopBuyerDTO.builder()
                        .name(r[0] != null ? r[0].toString() : "")
                        .orderCount(r[1] != null ? ((Number) r[1]).longValue() : 0L)
                        .totalSpend(r[2] != null ? new java.math.BigDecimal(r[2].toString()) : java.math.BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        // ── Top employees (top 3) ─────────────────────────────────────────────
        List<Object[]> employeeRows = orderItemRepository.getTopEmployeesForProduct(
                id, from, org.springframework.data.domain.PageRequest.of(0, 3));
        List<ProductStatsDTO.TopEmployeeDTO> topEmployees = employeeRows.stream()
                .map(r -> ProductStatsDTO.TopEmployeeDTO.builder()
                        .name(r[0] != null ? r[0].toString() : "")
                        .orderCount(r[1] != null ? ((Number) r[1]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());

        return ProductStatsDTO.builder()
                .orderCount(orderCount)
                .qtySold(qtySold)
                .revenue(revenue)
                .lastSoldAt(lastSoldAt)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : java.math.BigDecimal.ZERO)
                .revenueLastMonth(revenueLastMonth != null ? revenueLastMonth : java.math.BigDecimal.ZERO)
                .topCustomers(topCustomers)
                .topEmployees(topEmployees)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getBySourcePawnId(Long pawnId) {
        log.info("Request: Get product by sourcePawnId={}", pawnId);
        return productRepository.findBySourcePawnIdAndDeletedFalse(pawnId).map(this::mapToDTO);
    }

}

