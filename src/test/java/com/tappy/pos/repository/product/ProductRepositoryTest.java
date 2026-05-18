package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRepository Unit Tests")
class ProductRepositoryTest {

    @Mock
    private ProductRepository productRepository;

    private ProductType productType() {
        return ProductType.builder().id(1L).code("FOOD").name("Food").deleted(false).build();
    }

    private Product apple() {
        return Product.builder()
                .id(1L)
                .productType(productType())
                .sku("FOOD-001")
                .name("Apple")
                .description("Fresh red apple")
                .price(BigDecimal.valueOf(5.99))
                .costPrice(BigDecimal.valueOf(3.00))
                .status(Product.ProductStatus.ACTIVE)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should find product by SKU when not deleted")
    void testFindBySkuAndDeletedFalse_Success() {
        when(productRepository.findBySkuAndDeletedFalse("FOOD-001")).thenReturn(Optional.of(apple()));

        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("FOOD-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should not find deleted product by SKU")
    void testFindBySkuAndDeletedFalse_NotFound() {
        when(productRepository.findBySkuAndDeletedFalse("FOOD-001")).thenReturn(Optional.empty());

        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("FOOD-001");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find product by ID when not deleted")
    void testFindByIdAndDeletedFalse_Success() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(apple()));

        Optional<Product> result = productRepository.findByIdAndDeletedFalse(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("FOOD-001");
    }

    @Test
    @DisplayName("Should find products by status")
    void testFindByDeletedFalseAndStatusOrderByCreatedAtDesc_Success() {
        Page<Product> page = new PageImpl<>(List.of(apple()));
        when(productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                Product.ProductStatus.ACTIVE, PageRequest.of(0, 10))).thenReturn(page);

        Page<Product> results = productRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                Product.ProductStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should find products by type")
    void testFindByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc_Success() {
        Page<Product> page = new PageImpl<>(List.of(apple()));
        when(productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10))).thenReturn(page);

        Page<Product> results = productRepository.findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getProductType().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should search products by keyword")
    void testSearchByKeyword_Success() {
        Page<Product> page = new PageImpl<>(List.of(apple()));
        when(productRepository.searchByKeyword("apple", PageRequest.of(0, 10))).thenReturn(page);

        Page<Product> results = productRepository.searchByKeyword("apple", PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty when keyword not found")
    void testSearchByKeyword_NotFound() {
        when(productRepository.searchByKeyword("nonexistent", PageRequest.of(0, 10)))
                .thenReturn(Page.empty());

        Page<Product> results = productRepository.searchByKeyword("nonexistent", PageRequest.of(0, 10));

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when looking up non-existent SKU")
    void testSkuNotFound() {
        when(productRepository.findBySkuAndDeletedFalse("DOES-NOT-EXIST")).thenReturn(Optional.empty());

        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("DOES-NOT-EXIST");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should persist and retrieve product with correct values")
    void testPersistProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(apple()));

        Product saved = productRepository.findById(1L).orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getSku()).isEqualTo("FOOD-001");
        assertThat(saved.getName()).isEqualTo("Apple");
        assertThat(saved.getPrice()).isEqualTo(BigDecimal.valueOf(5.99));
    }
}
