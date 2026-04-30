package com.knp.service.product;

import com.knp.exception.DuplicateResourceException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.enums.ActivityAction;
import com.knp.multitenant.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.knp.model.dto.product.*;
import com.knp.model.entity.product.*;
import com.knp.model.entity.vendor.*;
import com.knp.repository.product.*;
import com.knp.repository.vendor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.knp.service.audit.ActivityLogService;

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
                .productType(productType)
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .costPrice(request.getCostPrice() != null ? request.getCostPrice() : java.math.BigDecimal.ZERO)
                .unit(request.getUnit())
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
        product.setPrice(request.getPrice());
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        product.setUnit(request.getUnit());
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
    public Page<ProductDTO> getAllProducts(String status, Pageable pageable) {
        log.info("Getting all products with status: {}", status);
        Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status.toUpperCase());
        Page<Product> products = productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(productStatus, pageable);
        return products.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByType(Long productTypeId, Pageable pageable) {
        log.info("Getting products by type: {}", productTypeId);
        Page<Product> products = productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(productTypeId, pageable);
        return products.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String searchTerm, Pageable pageable) {
        log.info("Searching products with term: {}", searchTerm);
        Page<Product> products = productRepository.searchByKeyword(searchTerm.toLowerCase(), pageable);
        return products.map(this::mapToDTO);
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

    private ProductDTO mapToDTO(Product product) {
        Map<String, Object> attributes = new HashMap<>();
        for (ProductAttributeValue attrValue : product.getAttributeValues()) {
            attributes.put(attrValue.getAttribute().getCode(), attrValue.getValue());
        }

        Set<Long> categoryIds = product.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        return ProductDTO.builder()
                .id(product.getId())
                .productTypeId(product.getProductType().getId())
                .productTypeName(product.getProductType().getName())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .unit(product.getUnit())
                .vendorId(product.getVendor() != null ? product.getVendor().getId() : null)
                .vendorName(product.getVendor() != null ? product.getVendor().getName() : null)
                .status(product.getStatus().toString())
                .categoryIds(categoryIds)
                .attributes(attributes)
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
}

