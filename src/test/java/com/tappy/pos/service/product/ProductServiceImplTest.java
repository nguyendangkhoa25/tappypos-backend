package com.tappy.pos.service.product;

import com.tappy.pos.client.OpenFoodFactsClient;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.product.*;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductCatalog;
import com.tappy.pos.model.entity.product.ProductType;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.product.AttributeDefinition;
import com.tappy.pos.model.entity.product.AttributeGroup;
import com.tappy.pos.model.entity.product.ProductAttributeValue;
import com.tappy.pos.repository.product.ProductCatalogRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.product.ProductTypeRepository;
import com.tappy.pos.repository.product.CategoryRepository;
import com.tappy.pos.repository.product.AttributeDefinitionRepository;
import com.tappy.pos.repository.product.AttributeGroupRepository;
import com.tappy.pos.repository.product.ProductAttributeValueRepository;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.repository.vendor.VendorRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.multitenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private AttributeDefinitionRepository attributeDefinitionRepository;

    @Mock
    private AttributeGroupRepository attributeGroupRepository;

    @Mock
    private ProductAttributeValueRepository productAttributeValueRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @Mock
    private OpenFoodFactsClient openFoodFactsClient;

    @Mock
    private ProductCatalogService productCatalogService;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private CreateProductRequest createProductRequest;
    private UpdateProductRequest updateProductRequest;
    private Product product;
    private ProductType productType;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));

        // Initialize test data
        productType = ProductType.builder()
                .id(1L)
                .code("FOOD")
                .name("Food")
                .description("Food products")
                .build();

        product = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .description("Fresh red apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())  // Initialize empty set to avoid NPE
                .categories(new HashSet<>())        // Initialize empty set to avoid NPE
                .build();

        createProductRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("FOOD-001")
                .name("Apple")
                .description("Fresh red apple")
                .price(BigDecimal.valueOf(5.99))
                .status("ACTIVE")
                .attributes(new HashMap<>())
                .build();

        updateProductRequest = UpdateProductRequest.builder()
                .name("Updated Apple")
                .description("Updated description")
                .price(BigDecimal.valueOf(6.99))
                .status("ACTIVE")
                .attributes(new HashMap<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct_Success() {
        // Given
        when(productRepository.findBySkuAndDeletedFalse("FOOD-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(createProductRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSku()).isEqualTo("FOOD-001");
        assertThat(result.getName()).isEqualTo("Apple");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(5.99));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when SKU already exists")
    void testCreateProduct_SkuDuplicate() {
        // Given
        when(productRepository.findBySkuAndDeletedFalse("FOOD-001")).thenReturn(Optional.of(product));
        when(messageService.getMessage(anyString(), anyString())).thenReturn("SKU already exists");

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createProductRequest))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when product type not found")
    void testCreateProduct_ProductTypeNotFound() {
        // Given
        when(productRepository.findBySkuAndDeletedFalse("FOOD-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("Product type not found");

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createProductRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductById_Success() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

        // When
        ProductDTO result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Apple");
        verify(productRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductById_NotFound() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("Product not found");

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct_Success() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, updateProductRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProduct_Success() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void testGetAllProducts_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                Product.ProductStatus.ACTIVE, Pageable.unpaged())).thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.getAllProducts("ACTIVE", null, Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should get products by type successfully")
    void testGetProductsByType_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(
                1L, Pageable.unpaged())).thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.getProductsByType(1L, Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(1L, Pageable.unpaged());
    }

    @Test
    @DisplayName("Should search products successfully")
    void testSearchProducts_Success() {
        // Given
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.searchByKeyword("apple", Pageable.unpaged())).thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.searchProducts("apple", Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchByKeyword("apple", Pageable.unpaged());
    }

    @Test
    @DisplayName("Should get all product types")
    void testGetAllProductTypes_Success() {
        // Given
        List<ProductType> productTypes = Collections.singletonList(productType);
        when(productTypeRepository.findAll()).thenReturn(productTypes);

        // When
        List<ProductTypeDTO> result = productService.getAllProductTypes();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCode()).isEqualTo("FOOD");
    }

    @Test
    @DisplayName("Should get product type with attributes")
    void testGetProductTypeWithAttributes_Success() {
        // Given
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(attributeGroupRepository.findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(1L))
                .thenReturn(Collections.emptyList());

        // When
        ProductTypeWithAttributesDTO result = productService.getProductTypeWithAttributes(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result).isNotNull();
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("FOOD");
    }

    @Test
    @DisplayName("Should handle product type not found in list")
    void testGetProductTypeWithAttributes_TypeNotFound() {
        // Given
        when(productTypeRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.product.type.not.found", 999L))
                .thenReturn("Product type not found: 999");

        // When & Then
        assertThatThrownBy(() -> productService.getProductTypeWithAttributes(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create product with no categories or attributes")
    void testCreateProduct_MinimalData() {
        // Given
        CreateProductRequest minimalRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("MINIMAL-001")
                .name("Minimal Product")
                .price(BigDecimal.valueOf(10.00))
                .status("ACTIVE")
                .categoryIds(null)
                .attributes(null)
                .build();

        when(productRepository.findBySkuAndDeletedFalse("MINIMAL-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(minimalRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should create product with empty categories and attributes")
    void testCreateProduct_EmptyCollections() {
        // Given
        CreateProductRequest emptyRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("EMPTY-001")
                .name("Empty Product")
                .price(BigDecimal.valueOf(20.00))
                .status("INACTIVE")
                .categoryIds(Collections.emptySet())
                .attributes(Collections.emptyMap())
                .build();

        when(productRepository.findBySkuAndDeletedFalse("EMPTY-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(emptyRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }


    @Test
    @DisplayName("Should handle attribute not found in product creation")
    void testCreateProduct_AttributeNotFound() {
        // Given
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("unknownAttr", "value");
        
        CreateProductRequest attrRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("ATTR-001")
                .name("Product With Attribute")
                .price(BigDecimal.valueOf(25.00))
                .status("ACTIVE")
                .attributes(attrs)
                .build();

        // Create a product with initialized collections for the saved product
        Product savedProduct = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("ATTR-001")
                .name("Product With Attribute")
                .price(BigDecimal.valueOf(25.00))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        when(productRepository.findBySkuAndDeletedFalse("ATTR-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("unknownAttr", 1L))
                .thenReturn(Optional.empty());
        when(messageService.getMessage("error.attribute.not.found", "unknownAttr"))
                .thenReturn("Attribute not found: unknownAttr");

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(attrRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void testUpdateProduct_NotFound() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("Product not found");

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(999L, updateProductRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteProduct_NotFound() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("Product not found");

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle empty product type list")
    void testGetAllProductTypes_Empty() {
        // Given
        when(productTypeRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ProductTypeDTO> result = productService.getAllProductTypes();

        // Then
        assertThat(result).isEmpty();
        verify(productTypeRepository).findAll();
    }

    @Test
    @DisplayName("Should handle multiple product types")
    void testGetAllProductTypes_Multiple() {
        // Given
        ProductType productType2 = ProductType.builder()
                .id(2L)
                .code("BEVERAGE")
                .name("Beverage")
                .description("Beverages")
                .build();

        when(productTypeRepository.findAll()).thenReturn(List.of(productType, productType2));

        // When
        List<ProductTypeDTO> result = productService.getAllProductTypes();

        // Then
        assertThat(result).hasSize(2);
        verify(productTypeRepository).findAll();
    }

    @Test
    @DisplayName("Should throw exception when product not found in get by id")
    void testGetProductById_NotFoundException() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong())).thenReturn("Product not found");

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle product with null description")
    void testCreateProduct_NullDescription() {
        // Given
        CreateProductRequest nullDescRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("NODESC-001")
                .name("No Description Product")
                .description(null)
                .price(BigDecimal.valueOf(12.00))
                .status("ACTIVE")
                .build();

        when(productRepository.findBySkuAndDeletedFalse("NODESC-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(nullDescRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should verify save is called in update")
    void testUpdateProduct_VerifySave() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.updateProduct(1L, updateProductRequest);

        // Then
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should verify save is called in delete")
    void testDeleteProduct_VerifySave() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products with INACTIVE status")
    void testGetAllProducts_InactiveStatus() {
        // Given
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                Product.ProductStatus.INACTIVE, Pageable.unpaged())).thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.getAllProducts("INACTIVE", null, Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle empty result in getAllProducts")
    void testGetAllProducts_Empty() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
        when(productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                any(Product.ProductStatus.class), any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<ProductDTO> result = productService.getAllProducts("ACTIVE", null, Pageable.unpaged());

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should get products by type with empty result")
    void testGetProductsByType_Empty() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
        when(productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(1L, Pageable.unpaged()))
                .thenReturn(emptyPage);

        // When
        Page<ProductDTO> result = productService.getProductsByType(1L, Pageable.unpaged());

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should search products with empty result")
    void testSearchProducts_Empty() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
        when(productRepository.searchByKeyword("nonexistent", Pageable.unpaged())).thenReturn(emptyPage);

        // When
        Page<ProductDTO> result = productService.searchProducts("nonexistent", Pageable.unpaged());

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should create product with categories")
    void testCreateProduct_WithCategories() {
        // Given
        Category category = Category.builder()
                .name("Fruits")
                .build();
        category.setId(1L);

        HashSet<Long> categoryIds = new HashSet<>();
        categoryIds.add(1L);

        CreateProductRequest catRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("FRUIT-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status("ACTIVE")
                .categoryIds(categoryIds)
                .build();

        Product productWithCat = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FRUIT-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.ACTIVE)
                .categories(new HashSet<>(Collections.singletonList(category)))
                .attributeValues(new HashSet<>())
                .build();

        when(productRepository.findBySkuAndDeletedFalse("FRUIT-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(productWithCat);

        // When
        ProductDTO result = productService.createProduct(catRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryIds()).hasSize(1);
        verify(categoryRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("Should create product with single attribute")
    void testCreateProduct_WithSingleAttribute() {
        // Given
        AttributeDefinition attrDef = AttributeDefinition.builder()
                .code("color")
                .name("Color")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        attrDef.setId(1L);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("color", "red");

        CreateProductRequest attrRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("ATTR-PROD-001")
                .name("Red Apple")
                .price(BigDecimal.valueOf(6.99))
                .status("ACTIVE")
                .attributes(attrs)
                .build();

        Product savedProduct = Product.builder()
                .productType(productType)
                .sku("ATTR-PROD-001")
                .name("Red Apple")
                .price(BigDecimal.valueOf(6.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();
        savedProduct.setId(1L);

        when(productRepository.findBySkuAndDeletedFalse("ATTR-PROD-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("color", 1L))
                .thenReturn(Optional.of(attrDef));
        when(productAttributeValueRepository.save(any(ProductAttributeValue.class)))
                .thenReturn(new ProductAttributeValue());

        // When
        ProductDTO result = productService.createProduct(attrRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productAttributeValueRepository).save(any(ProductAttributeValue.class));
    }

    @Test
    @DisplayName("Should create product with multiple attributes")
    void testCreateProduct_WithMultipleAttributes() {
        // Given
        AttributeDefinition colorAttr = AttributeDefinition.builder()
                .code("color")
                .name("Color")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        colorAttr.setId(1L);

        AttributeDefinition sizeAttr = AttributeDefinition.builder()
                .code("size")
                .name("Size")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        sizeAttr.setId(2L);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("color", "red");
        attrs.put("size", "large");

        CreateProductRequest attrRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("MULTI-ATTR-001")
                .name("Multi Attribute Product")
                .price(BigDecimal.valueOf(9.99))
                .status("ACTIVE")
                .attributes(attrs)
                .build();

        Product savedProduct = Product.builder()
                .productType(productType)
                .sku("MULTI-ATTR-001")
                .name("Multi Attribute Product")
                .price(BigDecimal.valueOf(9.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();
        savedProduct.setId(1L);

        when(productRepository.findBySkuAndDeletedFalse("MULTI-ATTR-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("color", 1L))
                .thenReturn(Optional.of(colorAttr));
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("size", 1L))
                .thenReturn(Optional.of(sizeAttr));
        when(productAttributeValueRepository.save(any(ProductAttributeValue.class)))
                .thenReturn(new ProductAttributeValue());

        // When
        ProductDTO result = productService.createProduct(attrRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productAttributeValueRepository, times(2)).save(any(ProductAttributeValue.class));
    }

    @Test
    @DisplayName("Should update product with new price")
    void testUpdateProduct_WithNewPrice() {
        // Given
        UpdateProductRequest priceUpdate = UpdateProductRequest.builder()
                .price(BigDecimal.valueOf(7.99))
                .status("ACTIVE")
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, priceUpdate);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product status to INACTIVE")
    void testUpdateProduct_StatusInactive() {
        // Given
        UpdateProductRequest statusUpdate = UpdateProductRequest.builder()
                .status("INACTIVE")
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, statusUpdate);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get product by id with attributes")
    void testGetProductById_WithAttributes() {
        // Given
        AttributeDefinition attrDef = AttributeDefinition.builder()
                .code("color")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        attrDef.setId(1L);  // Set ID before using
        
        ProductAttributeValue attrValue = new ProductAttributeValue();
        attrValue.setAttribute(attrDef);  // Set attribute first
        attrValue.setValue("red");

        product.getAttributeValues().add(attrValue);

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

        // When
        ProductDTO result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNotEmpty();
    }

    @Test
    @DisplayName("Should get product type with attribute groups")
    void testGetProductTypeWithAttributes_WithGroups() {
        // Given
        AttributeGroup group = AttributeGroup.builder()
                .name("Basic Info")
                .displayOrder(1)
                .build();
        group.setId(1L);

        AttributeDefinition attr = AttributeDefinition.builder()
                .code("color")
                .name("Color")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        attr.setId(1L);

        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(attributeGroupRepository.findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(1L))
                .thenReturn(List.of(group));
        when(attributeDefinitionRepository.findByAttributeGroupIdAndDeletedFalse(1L))
                .thenReturn(List.of(attr));

        // When
        ProductTypeWithAttributesDTO result = productService.getProductTypeWithAttributes(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeGroups()).hasSize(1);
        assertThat(result.getAttributeGroups().getFirst().getAttributes()).hasSize(1);
    }

    @Test
    @DisplayName("Should get product type with multiple attribute groups")
    void testGetProductTypeWithAttributes_MultipleGroups() {
        // Given
        AttributeGroup group1 = AttributeGroup.builder()
                .name("Basic Info")
                .displayOrder(1)
                .build();
        group1.setId(1L);

        AttributeGroup group2 = AttributeGroup.builder()
                .name("Details")
                .displayOrder(2)
                .build();
        group2.setId(2L);

        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(attributeGroupRepository.findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(1L))
                .thenReturn(List.of(group1, group2));
        when(attributeDefinitionRepository.findByAttributeGroupIdAndDeletedFalse(anyLong()))
                .thenReturn(Collections.emptyList());

        // When
        ProductTypeWithAttributesDTO result = productService.getProductTypeWithAttributes(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeGroups()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle delete of product with no categories")
    void testDeleteProduct_NoCategories() {
        // Given
        Product prodToDelete = Product.builder()
                .id(2L)
                .productType(productType)
                .sku("DEL-001")
                .name("To Delete")
                .price(BigDecimal.valueOf(10.00))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        when(productRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(prodToDelete));
        when(productRepository.save(any(Product.class))).thenReturn(prodToDelete);

        // When
        productService.deleteProduct(2L);

        // Then
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should handle product with special characters in name")
    void testCreateProduct_SpecialCharactersInName() {
        // Given
        CreateProductRequest specialRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("SPEC-001")
                .name("Product™ with © special chars & symbols!")
                .price(BigDecimal.valueOf(15.50))
                .status("ACTIVE")
                .build();

        when(productRepository.findBySkuAndDeletedFalse("SPEC-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(specialRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should create product with very large price")
    void testCreateProduct_LargePrice() {
        // Given
        CreateProductRequest largePrice = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("LARGE-PRICE-001")
                .name("Luxury Item")
                .price(BigDecimal.valueOf(99999.99))
                .status("ACTIVE")
                .build();

        when(productRepository.findBySkuAndDeletedFalse("LARGE-PRICE-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(largePrice);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should create product with zero price")
    void testCreateProduct_ZeroPrice() {
        // Given
        CreateProductRequest zeroPrice = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("ZERO-PRICE-001")
                .name("Free Product")
                .price(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        when(productRepository.findBySkuAndDeletedFalse("ZERO-PRICE-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(zeroPrice);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should search products with multiple results")
    void testSearchProducts_MultipleResults() {
        // Given
        Product product2 = Product.builder()
                .id(2L)
                .productType(productType)
                .sku("FOOD-002")
                .name("Orange")
                .price(BigDecimal.valueOf(3.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())  // Initialize
                .categories(new HashSet<>())        // Initialize
                .build();

        Page<Product> productPage = new PageImpl<>(List.of(product, product2), Pageable.unpaged(), 2);
        when(productRepository.searchByKeyword("fruit", Pageable.unpaged())).thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.searchProducts("fruit", Pageable.unpaged());

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ==================== Additional Tests for Coverage ====================

    @Test
    @DisplayName("Should create product with multiple categories")
    void testCreateProduct_MultipleCategories() {
        // Given
        Category category1 = Category.builder()
                .name("Organic")
                .build();
        category1.setId(1L);
        category1.setDeleted(false);

        Category category2 = Category.builder()
                .name("Fresh")
                .build();
        category2.setId(2L);
        category2.setDeleted(false);

        CreateProductRequest multiCategoryRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("ORGANIC-FRESH-001")
                .name("Organic Fresh Apple")
                .price(BigDecimal.valueOf(7.99))
                .status("ACTIVE")
                .categoryIds(new HashSet<>(Set.of(1L, 2L)))
                .build();

        Product multiCategoryProduct = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("ORGANIC-FRESH-001")
                .name("Organic Fresh Apple")
                .price(BigDecimal.valueOf(7.99))
                .status(Product.ProductStatus.ACTIVE)
                .categories(new HashSet<>(List.of(category1, category2)))
                .attributeValues(new HashSet<>())
                .build();


        when(productRepository.findBySkuAndDeletedFalse("ORGANIC-FRESH-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category1));
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(category2));
        when(productRepository.save(any(Product.class))).thenReturn(multiCategoryProduct);

        // When
        ProductDTO result = productService.createProduct(multiCategoryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryIds()).hasSize(2);
        verify(categoryRepository, times(2)).findByIdAndDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when category not found during product creation")
    void testCreateProduct_CategoryNotFound() {
        // Given
        CreateProductRequest categoryRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("CAT-001")
                .name("Product with missing category")
                .price(BigDecimal.valueOf(10.00))
                .status("ACTIVE")
                .categoryIds(new HashSet<>(Set.of(999L)))
                .build();

        when(productRepository.findBySkuAndDeletedFalse("CAT-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(categoryRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(categoryRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update product with new categories")
    void testUpdateProduct_WithCategories() {
        // Given
        Category newCategory = Category.builder()
                .name("Premium")
                .build();
        newCategory.setId(2L);
        newCategory.setDeleted(false);

        UpdateProductRequest updateWithCatRequest = UpdateProductRequest.builder()
                .name("Updated Apple Premium")
                .description("Premium apple update")
                .price(BigDecimal.valueOf(8.99))
                .status("ACTIVE")
                .categoryIds(new HashSet<>(Set.of(2L)))
                .build();

        Product updatedProductWithCat = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Updated Apple Premium")
                .price(BigDecimal.valueOf(8.99))
                .status(Product.ProductStatus.ACTIVE)
                .categories(new HashSet<>(List.of(newCategory)))
                .attributeValues(new HashSet<>())
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProductWithCat);

        // When
        ProductDTO result = productService.updateProduct(1L, updateWithCatRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Apple Premium");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when category not found during update")
    void testUpdateProduct_CategoryNotFound() {
        // Given
        UpdateProductRequest updateWithBadCatRequest = UpdateProductRequest.builder()
                .name("Product")
                .description("Description")
                .price(BigDecimal.valueOf(10.00))
                .status("ACTIVE")
                .categoryIds(new HashSet<>(Set.of(999L)))
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(1L, updateWithBadCatRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update product with multiple attributes")
    void testUpdateProduct_MultipleAttributes() {
        // Given
        Map<String, Object> multiAttributes = new HashMap<>();
        multiAttributes.put("color", "red");
        multiAttributes.put("size", "large");

        UpdateProductRequest updateWithAttrsRequest = UpdateProductRequest.builder()
                .name("Red Large Apple")
                .description("Large red apple")
                .price(BigDecimal.valueOf(6.99))
                .status("ACTIVE")
                .attributes(multiAttributes)
                .build();

        AttributeDefinition colorAttr = AttributeDefinition.builder()
                .code("color")
                .name("Color")
                .dataType(AttributeDefinition.DataType.TEXT)
                .build();
        colorAttr.setId(1L);

        AttributeDefinition sizeAttr = AttributeDefinition.builder()
                .code("size")
                .name("Size")
                .dataType(AttributeDefinition.DataType.TEXT)
                .build();
        sizeAttr.setId(2L);

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("color", 1L))
                .thenReturn(Optional.of(colorAttr));
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("size", 1L))
                .thenReturn(Optional.of(sizeAttr));
        when(productAttributeValueRepository.findByProductIdAndAttributeId(1L, 1L))
                .thenReturn(Optional.empty());
        when(productAttributeValueRepository.findByProductIdAndAttributeId(1L, 2L))
                .thenReturn(Optional.empty());
        when(productAttributeValueRepository.save(any(ProductAttributeValue.class)))
                .thenReturn(new ProductAttributeValue());
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, updateWithAttrsRequest);

        // Then
        assertThat(result).isNotNull();
        verify(attributeDefinitionRepository, times(2)).findByCodeAndProductTypeId(anyString(), anyLong());
        verify(productAttributeValueRepository, times(2)).save(any(ProductAttributeValue.class));
    }


    @Test
    @DisplayName("Should throw exception when product type not found in getProductTypeWithAttributes")
    void testGetProductTypeWithAttributes_NotFound() {
        // Given
        when(productTypeRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), anyLong()))
                .thenReturn("Product type not found");

        // When & Then
        assertThatThrownBy(() -> productService.getProductTypeWithAttributes(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle product type with no attribute groups")
    void testGetProductTypeWithAttributes_NoGroups() {
        // Given
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(attributeGroupRepository.findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(1L))
                .thenReturn(Collections.emptyList());

        // When
        ProductTypeWithAttributesDTO result = productService.getProductTypeWithAttributes(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeGroups()).isEmpty();
    }

    @Test
    @DisplayName("Should create product with empty attributes map")
    void testCreateProduct_EmptyAttributesMap() {
        // Given
        CreateProductRequest emptyAttrRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("NOATTR-001")
                .name("No Attributes")
                .price(BigDecimal.valueOf(5.00))
                .status("ACTIVE")
                .attributes(new HashMap<>())
                .build();

        Product productNoAttr = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("NOATTR-001")
                .name("No Attributes")
                .price(BigDecimal.valueOf(5.00))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        when(productRepository.findBySkuAndDeletedFalse("NOATTR-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(productNoAttr);

        // When
        ProductDTO result = productService.createProduct(emptyAttrRequest);

        // Then
        assertThat(result).isNotNull();
        verify(attributeDefinitionRepository, never()).findByCodeAndProductTypeId(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should create product with null attributes")
    void testCreateProduct_NullAttributes() {
        // Given
        CreateProductRequest nullAttrRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("NULLATTR-001")
                .name("Null Attributes Product")
                .price(BigDecimal.valueOf(5.00))
                .status("ACTIVE")
                .attributes(null)
                .build();

        when(productRepository.findBySkuAndDeletedFalse("NULLATTR-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.createProduct(nullAttrRequest);

        // Then
        assertThat(result).isNotNull();
        verify(attributeDefinitionRepository, never()).findByCodeAndProductTypeId(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should get products by type with pagination")
    void testGetProductsByType_WithPagination() {
        // Given
        Product product2 = Product.builder()
                .id(2L)
                .productType(productType)
                .sku("FOOD-002")
                .name("Orange")
                .price(BigDecimal.valueOf(3.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())  // Initialize
                .categories(new HashSet<>())        // Initialize
                .build();

        Page<Product> productPage = new PageImpl<>(List.of(product, product2), 
                Pageable.ofSize(10).withPage(0), 2);
        when(productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(1L, 
                Pageable.ofSize(10).withPage(0)))
                .thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.getProductsByType(1L, 
                Pageable.ofSize(10).withPage(0));

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update product with null attributes")
    void testUpdateProduct_NullAttributes() {
        // Given
        UpdateProductRequest updateNullAttrRequest = UpdateProductRequest.builder()
                .name("Updated Product")
                .description("Updated")
                .price(BigDecimal.valueOf(10.00))
                .status("ACTIVE")
                .attributes(null)
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, updateNullAttrRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productAttributeValueRepository, never()).save(any(ProductAttributeValue.class));
    }

    @Test
    @DisplayName("Should update product with null categories")
    void testUpdateProduct_NullCategories() {
        // Given
        UpdateProductRequest updateNullCatRequest = UpdateProductRequest.builder()
                .name("Updated Product")
                .description("Updated")
                .price(BigDecimal.valueOf(10.00))
                .status("ACTIVE")
                .categoryIds(null)
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ProductDTO result = productService.updateProduct(1L, updateNullCatRequest);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository, never()).findByIdAndDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("Should delete product and set deleted flag")
    void testDeleteProduct_SetsDeletedFlag() {
        // Given
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should search products with uppercase conversion")
    void testSearchProducts_UppercaseKeyword() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product), Pageable.unpaged(), 1);
        when(productRepository.searchByKeyword("apple", Pageable.unpaged()))
                .thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.searchProducts("APPLE", Pageable.unpaged());

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchByKeyword("apple", Pageable.unpaged());
    }

    @Test
    @DisplayName("Should map product attributes correctly")
    void testMapToDTO_WithAttributes() {
        // Given
        AttributeDefinition colorAttr = AttributeDefinition.builder()
                .code("color")
                .name("Color")
                .dataType(AttributeDefinition.DataType.STRING)
                .build();
        colorAttr.setId(1L);

        ProductAttributeValue attrValue = ProductAttributeValue.builder()
                .product(product)
                .attribute(colorAttr)
                .build();
        attrValue.setValue("red");

        Product productWithAttr = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(Set.of(attrValue))
                .categories(new HashSet<>())
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(productWithAttr));

        // When
        ProductDTO result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).containsKey("color");
        assertThat(result.getAttributes().get("color")).isEqualTo("red");
    }

    @Test
    @DisplayName("Should get product by id with categories")
    void testGetProductById_WithCategories() {
        // Given
        Category category = Category.builder()
                .name("Organic")
                .build();
        category.setId(1L);
        category.setDeleted(false);

        Product productWithCat = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(Set.of(category))
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(productWithCat));

        // When
        ProductDTO result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryIds()).contains(1L);
    }

    @Test
    @DisplayName("Should update product status from ACTIVE to INACTIVE")
    void testUpdateProduct_StatusChange() {
        // Given
        UpdateProductRequest statusUpdateRequest = UpdateProductRequest.builder()
                .name("Apple")
                .description("Fresh apple")
                .price(BigDecimal.valueOf(5.99))
                .status("INACTIVE")
                .build();

        Product inactiveProduct = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.INACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(inactiveProduct);

        // When
        ProductDTO result = productService.updateProduct(1L, statusUpdateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
        verify(productRepository).save(any(Product.class));
    }

    // ==================== Additional Edge Case Tests for >90% Coverage ====================

    @Test
    @DisplayName("Should throw exception when creating product with null price")
    void testCreateProduct_NullPrice() {
        // Given
        CreateProductRequest nullPriceRequest = CreateProductRequest.builder()
                .productTypeId(1L)
                .sku("NULL-PRICE-001")
                .name("Invalid Product")
                .description("Product with null price")
                .price(null)
                .status("ACTIVE")
                .build();

        Product productWithNullPrice = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("NULL-PRICE-001")
                .name("Invalid Product")
                .description("Product with null price")
                .price(null)
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>())
                .categories(new HashSet<>())
                .build();

        when(productRepository.findBySkuAndDeletedFalse("NULL-PRICE-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(productWithNullPrice);

        // When
        ProductDTO result = productService.createProduct(nullPriceRequest);

        // Then
        assertThat(result).isNotNull();
    }

    // ── lookupByBarcode ───────────────────────────────────────────────────────

    @Test
    @DisplayName("lookupByBarcode: returns SHOP result when product found in shop inventory")
    void lookupByBarcode_foundInShop() {
        when(productRepository.findByBarcodeAndDeletedFalse("8936001810014")).thenReturn(Optional.of(product));

        BarcodeLookupResult result = productService.lookupByBarcode("8936001810014");

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.SHOP);
        assertThat(result.getProduct()).isNotNull();
    }

    @Test
    @DisplayName("lookupByBarcode: returns CATALOG result from master catalog when not in shop")
    void lookupByBarcode_foundInCatalog() {
        ProductCatalog catalog = ProductCatalog.builder()
                .barcode("8936001810014").name("Coca Cola").brand("Coca-Cola")
                .categoryHint("Beverages").source("OPEN_FOOD_FACTS").build();

        when(productRepository.findByBarcodeAndDeletedFalse("8936001810014")).thenReturn(Optional.empty());
        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.of(catalog));

        BarcodeLookupResult result = productService.lookupByBarcode("8936001810014");

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.CATALOG);
        assertThat(result.getCatalog().getName()).isEqualTo("Coca Cola");
    }

    @Test
    @DisplayName("lookupByBarcode: returns NONE when not found anywhere and OFF disabled")
    void lookupByBarcode_notFound_offDisabled() {
        when(productRepository.findByBarcodeAndDeletedFalse("9999999999")).thenReturn(Optional.empty());
        when(productCatalogRepository.findByBarcode("9999999999")).thenReturn(Optional.empty());
        when(openFoodFactsClient.isEnabled()).thenReturn(false);

        BarcodeLookupResult result = productService.lookupByBarcode("9999999999");

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.NONE);
    }

    @Test
    @DisplayName("lookupByBarcode: queries OFF and returns CATALOG when enabled and product found")
    void lookupByBarcode_foundInOff() {
        // 4006381333931 is a valid EAN-13 barcode (passes GS1 checksum)
        String validBarcode = "4006381333931";
        OpenFoodFactsClient.OffProduct offProduct = new OpenFoodFactsClient.OffProduct();
        offProduct.code = validBarcode;
        offProduct.product_name = "Test Product";
        offProduct.brands = "Test Brand";

        when(productRepository.findByBarcodeAndDeletedFalse(validBarcode)).thenReturn(Optional.empty());
        when(productCatalogRepository.findByBarcode(validBarcode)).thenReturn(Optional.empty());
        when(openFoodFactsClient.isEnabled()).thenReturn(true);
        when(openFoodFactsClient.fetchByBarcode(validBarcode)).thenReturn(Optional.of(offProduct));

        BarcodeLookupResult result = productService.lookupByBarcode(validBarcode);

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.CATALOG);
        assertThat(result.getCatalog().getName()).isEqualTo("Test Product");
        verify(productCatalogService).saveFromOffAsync(offProduct);
    }

    // ── generateSku ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateSku: generates SKU with type prefix and name abbreviation")
    void generateSku_success() {
        when(productRepository.findSkusByPrefix("FOOD-APPLE-")).thenReturn(List.of());

        String sku = productService.generateSku("Apple", "FOOD");

        assertThat(sku).startsWith("FOOD-APPLE-");
        assertThat(sku).endsWith("001");
    }

    @Test
    @DisplayName("generateSku: increments sequence based on existing SKUs")
    void generateSku_withExisting() {
        when(productRepository.findSkusByPrefix("FOOD-APPLE-")).thenReturn(List.of("FOOD-APPLE-001", "FOOD-APPLE-002"));

        String sku = productService.generateSku("Apple", "FOOD");

        assertThat(sku).isEqualTo("FOOD-APPLE-003");
    }

    @Test
    @DisplayName("generateSku: handles multi-word product name with abbreviation")
    void generateSku_multiWordName() {
        when(productRepository.findSkusByPrefix("FOOD-COMJUI-")).thenReturn(List.of());

        String sku = productService.generateSku("Combo Juice Pack", "FOOD");

        assertThat(sku).startsWith("FOOD-COMJUI-");
    }

    @Test
    @DisplayName("generateSku: truncates type code to 4 characters")
    void generateSku_longTypeCode() {
        when(productRepository.findSkusByPrefix(startsWith("ELEC-"))).thenReturn(List.of());

        String sku = productService.generateSku("Phone", "ELECTRONICS");

        assertThat(sku).startsWith("ELEC-");
    }

    // ── markAsSold ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsSold: sets product status to INACTIVE")
    void markAsSold_success() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.markAsSold(1L);

        verify(productRepository).save(product);
        assertThat(product.getStatus()).isEqualTo(Product.ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("markAsSold: product not found → ResourceNotFoundException")
    void markAsSold_notFound_throws() {
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("product.not.found")).thenReturn("Not found");

        assertThatThrownBy(() -> productService.markAsSold(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── JEWELRY type: auto-creates 1-unit inventory ────────────────────────────

    @Test
    @DisplayName("createProduct: JEWELRY type auto-creates 1-unit inventory at 'Quầy'")
    void createProduct_jewelryType_autoCreatesInventory() {
        ProductType jewelryType = ProductType.builder()
                .id(2L).code("JEWELRY").name("Trang Sức").build();

        Product savedJewelry = Product.builder()
                .id(5L).productType(jewelryType).sku("JWL-001").name("Nhẫn Vàng 24K")
                .price(BigDecimal.valueOf(5_000_000)).costPrice(BigDecimal.valueOf(4_000_000))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>()).categories(new HashSet<>()).build();

        CreateProductRequest req = CreateProductRequest.builder()
                .productTypeId(2L).sku("JWL-001").name("Nhẫn Vàng 24K")
                .price(BigDecimal.valueOf(5_000_000)).status("ACTIVE").build();

        when(productRepository.findBySkuAndDeletedFalse("JWL-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(2L)).thenReturn(Optional.of(jewelryType));
        when(productRepository.save(any(Product.class))).thenReturn(savedJewelry);
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-test");

        productService.createProduct(req);

        verify(inventoryRepository).save(argThat(inv ->
                inv.getQuantityInStock() == 1L && "Quầy".equals(inv.getWarehouseLocation())));
    }

    @Test
    @DisplayName("createProduct: JEWELRY type uses counter_code attribute as warehouse location")
    void createProduct_jewelryType_usesCounterCodeAsLocation() {
        ProductType jewelryType = ProductType.builder()
                .id(2L).code("JEWELRY").name("Trang Sức").build();

        Product savedJewelry = Product.builder()
                .id(6L).productType(jewelryType).sku("JWL-002").name("Dây Chuyền Vàng")
                .price(BigDecimal.valueOf(3_000_000)).costPrice(BigDecimal.valueOf(2_500_000))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>()).categories(new HashSet<>()).build();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("counter_code", "TU-A1");

        CreateProductRequest req = CreateProductRequest.builder()
                .productTypeId(2L).sku("JWL-002").name("Dây Chuyền Vàng")
                .price(BigDecimal.valueOf(3_000_000)).status("ACTIVE")
                .attributes(attrs).build();

        AttributeDefinition counterCodeAttr = AttributeDefinition.builder()
                .code("counter_code").name("Mã Tủ").dataType(AttributeDefinition.DataType.STRING).build();
        counterCodeAttr.setId(10L);

        when(productRepository.findBySkuAndDeletedFalse("JWL-002")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(2L)).thenReturn(Optional.of(jewelryType));
        when(productRepository.save(any(Product.class))).thenReturn(savedJewelry);
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("counter_code", 2L))
                .thenReturn(Optional.of(counterCodeAttr));
        when(productAttributeValueRepository.save(any())).thenReturn(new com.tappy.pos.model.entity.product.ProductAttributeValue());
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-test");

        productService.createProduct(req);

        verify(inventoryRepository).save(argThat(inv -> "TU-A1".equals(inv.getWarehouseLocation())));
    }

    // ── vendorId handling ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: with vendorId — looks up vendor and assigns it to product")
    void createProduct_withVendorId_loadsVendor() {
        com.tappy.pos.model.entity.vendor.Vendor vendor = new com.tappy.pos.model.entity.vendor.Vendor();
        vendor.setId(7L);
        vendor.setName("Nhà Cung Cấp A");

        CreateProductRequest req = CreateProductRequest.builder()
                .productTypeId(1L).sku("VENDOR-001").name("Product With Vendor")
                .price(BigDecimal.valueOf(100_000)).status("ACTIVE")
                .vendorId(7L).build();

        Product savedWithVendor = Product.builder()
                .id(10L).productType(productType).sku("VENDOR-001").name("Product With Vendor")
                .price(BigDecimal.valueOf(100_000)).status(Product.ProductStatus.ACTIVE)
                .vendor(vendor).attributeValues(new HashSet<>()).categories(new HashSet<>()).build();

        when(productRepository.findBySkuAndDeletedFalse("VENDOR-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(vendorRepository.findById(7L)).thenReturn(Optional.of(vendor));
        when(productRepository.save(any(Product.class))).thenReturn(savedWithVendor);

        ProductDTO result = productService.createProduct(req);

        assertThat(result.getVendorId()).isEqualTo(7L);
        verify(vendorRepository).findById(7L);
    }

    @Test
    @DisplayName("createProduct: vendor not found → ResourceNotFoundException")
    void createProduct_vendorNotFound_throws() {
        CreateProductRequest req = CreateProductRequest.builder()
                .productTypeId(1L).sku("BAD-VND-001").name("Product")
                .price(BigDecimal.TEN).status("ACTIVE").vendorId(999L).build();

        when(productRepository.findBySkuAndDeletedFalse("BAD-VND-001")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(vendorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProduct: with vendorId — updates vendor on product")
    void updateProduct_withVendorId_updatesVendor() {
        com.tappy.pos.model.entity.vendor.Vendor vendor = new com.tappy.pos.model.entity.vendor.Vendor();
        vendor.setId(3L);
        vendor.setName("New Vendor");

        UpdateProductRequest req = UpdateProductRequest.builder()
                .name("Updated").description("Desc").price(BigDecimal.TEN)
                .status("ACTIVE").vendorId(3L).build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(vendorRepository.findById(3L)).thenReturn(Optional.of(vendor));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.updateProduct(1L, req);

        verify(vendorRepository).findById(3L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: attribute with null value — skipped without saving attribute")
    void createProduct_nullAttributeValue_skipped() {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("color", null);
        attrs.put("size", "M");

        AttributeDefinition sizeAttr = AttributeDefinition.builder()
                .code("size").name("Size").dataType(AttributeDefinition.DataType.STRING).build();
        sizeAttr.setId(2L);

        Product savedProduct = Product.builder()
                .id(1L).productType(productType).sku("NULL-ATTR-SKIP").name("Test")
                .price(BigDecimal.TEN).status(Product.ProductStatus.ACTIVE)
                .attributeValues(new HashSet<>()).categories(new HashSet<>()).build();

        CreateProductRequest req = CreateProductRequest.builder()
                .productTypeId(1L).sku("NULL-ATTR-SKIP").name("Test")
                .price(BigDecimal.TEN).status("ACTIVE").attributes(attrs).build();

        when(productRepository.findBySkuAndDeletedFalse("NULL-ATTR-SKIP")).thenReturn(Optional.empty());
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(attributeDefinitionRepository.findByCodeAndProductTypeId("size", 1L))
                .thenReturn(Optional.of(sizeAttr));
        when(productAttributeValueRepository.save(any())).thenReturn(new com.tappy.pos.model.entity.product.ProductAttributeValue());

        productService.createProduct(req);

        verify(attributeDefinitionRepository, never()).findByCodeAndProductTypeId(eq("color"), anyLong());
        verify(productAttributeValueRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("lookupByBarcode: OFF enabled, valid barcode not found in OFF — returns NONE")
    void lookupByBarcode_offEnabled_validBarcode_notFoundInOff() {
        String validBarcode = "4006381333931";
        when(productRepository.findByBarcodeAndDeletedFalse(validBarcode)).thenReturn(Optional.empty());
        when(productCatalogRepository.findByBarcode(validBarcode)).thenReturn(Optional.empty());
        when(openFoodFactsClient.isEnabled()).thenReturn(true);
        when(openFoodFactsClient.fetchByBarcode(validBarcode)).thenReturn(Optional.empty());

        BarcodeLookupResult result = productService.lookupByBarcode(validBarcode);

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.NONE);
        verify(productCatalogService, never()).saveFromOffAsync(any());
    }

    @Test
    @DisplayName("lookupByBarcode: OFF enabled but barcode structurally invalid — skips OFF call")
    void lookupByBarcode_offEnabled_invalidBarcode_skipsOff() {
        String invalidBarcode = "NOTABARCODE";
        when(productRepository.findByBarcodeAndDeletedFalse(invalidBarcode)).thenReturn(Optional.empty());
        when(productCatalogRepository.findByBarcode(invalidBarcode)).thenReturn(Optional.empty());
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        BarcodeLookupResult result = productService.lookupByBarcode(invalidBarcode);

        assertThat(result.getSource()).isEqualTo(BarcodeLookupResult.Source.NONE);
        verify(openFoodFactsClient, never()).fetchByBarcode(anyString());
    }
}

