package com.knp.repository.product;

import com.knp.model.entity.product.Product;
import com.knp.model.entity.product.ProductType;
import com.knp.repository.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("ProductRepository Unit Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product product;
    private ProductType productType;

    @BeforeEach
    void setUp() {
        // Clear in FK-safe order: children before parents
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        productTypeRepository.deleteAll();
        entityManager.flush();

        // Use timestamp to ensure unique product type code across test runs
        String productTypeCode = "FOOD_" + System.currentTimeMillis();
        productType = ProductType.builder()
                .code(productTypeCode)
                .name("Food")
                .description("Food products")
                .deleted(false)
                .build();
        productTypeRepository.save(productType);

        product = Product.builder()
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .description("Fresh red apple")
                .price(BigDecimal.valueOf(5.99))
                .costPrice(BigDecimal.valueOf(3.00))
                .status(Product.ProductStatus.ACTIVE)
                .deleted(false)
                .build();
        productRepository.save(product);
    }

    @Test
    @DisplayName("Should find product by SKU when not deleted")
    void testFindBySkuAndDeletedFalse_Success() {
        // When
        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("FOOD-001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should not find deleted product by SKU")
    void testFindBySkuAndDeletedFalse_NotFound() {
        // Given
        product.setDeleted(true);
        productRepository.save(product);

        // When
        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("FOOD-001");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find product by ID when not deleted")
    void testFindByIdAndDeletedFalse_Success() {
        // When
        Optional<Product> result = productRepository.findByIdAndDeletedFalse(product.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("FOOD-001");
    }

    @Test
    @DisplayName("Should find products by status")
    void testFindByDeletedFalseAndStatusOrderByCreatedAtDesc_Success() {
        // When
        Page<Product> results = productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                Product.ProductStatus.ACTIVE, PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should find products by type")
    void testFindByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc_Success() {
        // When
        Page<Product> results = productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(
                productType.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getProductType().getId()).isEqualTo(productType.getId());
    }

    @Test
    @DisplayName("Should search products by keyword")
    void testSearchByKeyword_Success() {
        // When
        Page<Product> results = productRepository.searchByKeyword("apple", PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty when keyword not found")
    void testSearchByKeyword_NotFound() {
        // When
        Page<Product> results = productRepository.searchByKeyword("nonexistent", PageRequest.of(0, 10));

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find product by SKU with unique constraint")
    void testSkuUniqueness() {
        // Given
        Product duplicateProduct = Product.builder()
                .productType(productType)
                .sku("FOOD-001")  // Duplicate SKU
                .name("Banana")
                .price(BigDecimal.valueOf(3.99))
                .costPrice(BigDecimal.valueOf(2.00))
                .status(Product.ProductStatus.ACTIVE)
                .deleted(false)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            productRepository.save(duplicateProduct);
            entityManager.flush();
        }).isNotNull();
    }

    @Test
    @DisplayName("Should persist and retrieve product with correct values")
    void testPersistProduct_Success() {
        // When
        Product saved = productRepository.findById(product.getId()).orElse(null);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getSku()).isEqualTo("FOOD-001");
        assertThat(saved.getName()).isEqualTo("Apple");
        assertThat(saved.getPrice()).isEqualTo(BigDecimal.valueOf(5.99));
    }
}

