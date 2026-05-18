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

import java.util.*;
import java.util.stream.Collectors;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.client.OpenFoodFactsClient;
import com.tappy.pos.util.BarcodeValidator;

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
    private final ProductCatalogService productCatalogService;
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;

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
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + request.getVendorId()));
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
                .shelfLocation(request.getShelfLocation())
                .vendor(vendor)
                .status(Product.ProductStatus.valueOf(request.getStatus()))
                .deleted(false)
                .build();

        // Add categories if provided
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findByIdAndDeletedFalse(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
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
                        .orElseThrow(() -> {
                            String errorMsg = messageService.getMessage("error.attribute.not.found", entry.getKey());
                            log.error("Attribute not found: {}", entry.getKey());
                            return new ResourceNotFoundException(errorMsg);
                        });

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

        // Jewelry items are unique pieces — auto-create 1-unit inventory on product creation.
        if ("JEWELRY".equals(productType.getCode())) {
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
            log.info("Auto-created 1-unit inventory for jewelry product {}", savedProduct.getId());
        }

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", savedProduct.getId().toString(),
                "Created product: " + savedProduct.getName(), null);

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
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        // commissionRate: null = inherit employee default; explicit value (incl. 0) = override
        product.setCommissionRate(request.getCommissionRate());
        if (request.getDurationMinutes() != null) {
            product.setDurationMinutes(request.getDurationMinutes());
        }
        product.setUnit(request.getUnit());
        product.setShelfLocation(request.getShelfLocation());
        product.setStatus(Product.ProductStatus.valueOf(request.getStatus()));

        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + request.getVendorId()));
            product.setVendor(vendor);
        } else {
            product.setVendor(null);
        }

        // Update categories
        if (request.getCategoryIds() != null) {
            product.getCategories().clear();
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findByIdAndDeletedFalse(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
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
                        .orElseThrow(() -> {
                            String errorMsg = messageService.getMessage("error.attribute.not.found", entry.getKey());
                            log.error("Attribute not found: {}", entry.getKey());
                            return new ResourceNotFoundException(errorMsg);
                        });

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
                "Updated product: " + updated.getName(), null);

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
        product.softDelete();
        productRepository.save(product);
        log.info("Product deleted: {}", id);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCT_DELETED, "PRODUCT", id.toString(),
                "Deleted product: " + productName, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(String status, Long categoryId, Pageable pageable) {
        log.info("Getting all products with status: {}, categoryId: {}", status, categoryId);
        Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status.toUpperCase());
        Page<Product> products = categoryId != null
                ? productRepository.findByStatusAndCategoryId(productStatus, categoryId, pageable)
                : productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(productStatus, pageable);

        List<Long> productIds = products.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        Set<Long> productIdsWithVariants = productIds.isEmpty()
                ? Collections.emptySet()
                : productVariantRepository.findProductIdsWithActiveVariants(productIds);

        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId())));
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
        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId())));
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
        return products.map(p -> mapToDTOWithVariantFlag(p, productIdsWithVariants.contains(p.getId())));
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

    private ProductDTO mapToDTOWithVariantFlag(Product product, boolean hasVariants) {
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

        boolean isService = "SERVICE".equals(product.getProductType().getCode());
        Long stockQuantity = null;
        Boolean inStock = null;
        if (!isService) {
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
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .vendorName(product.getVendor() != null ? product.getVendor().getName() : null)
                .shelfLocation(product.getShelfLocation())
                .status(product.getStatus().toString())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .attributes(attributes)
                .hasVariants(hasVariants)
                .stockQuantity(stockQuantity)
                .inStock(inStock)
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

        boolean isService = "SERVICE".equals(product.getProductType().getCode());
        Long stockQuantity = null;
        Boolean inStock = null;
        if (!isService) {
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
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .vendorName(product.getVendor() != null ? product.getVendor().getName() : null)
                .shelfLocation(product.getShelfLocation())
                .status(product.getStatus().toString())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .attributes(attributes)
                .hasVariants(hasVariants)
                .stockQuantity(stockQuantity)
                .inStock(inStock)
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
}

