package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductType;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@DisplayName("ProductRepository Integration Tests")
class ProductRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private ProductType newProductType() {
        ProductType pt = ProductType.builder()
                .code("TYPE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .name("Food")
                .tenantId("tenant-01")
                .build();
        return entityManager.persist(pt);
    }

    private Product buildProduct(ProductType pt, String sku, String name, boolean deleted) {
        return Product.builder()
                .productType(pt)
                .sku(sku)
                .name(name)
                .price(BigDecimal.valueOf(10_000))
                .costPrice(BigDecimal.valueOf(7_000))
                .status(Product.ProductStatus.ACTIVE)
                .deleted(deleted)
                .tenantId("tenant-01")
                .build();
    }

    @Test
    @DisplayName("findBySkuAndDeletedFalse: returns product when not deleted")
    void findBySkuAndDeletedFalse_existingActiveSku_returnsProduct() {
        ProductType pt = newProductType();
        entityManager.persistAndFlush(buildProduct(pt, "ACT-SKU-01", "Apple", false));

        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("ACT-SKU-01");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("findBySkuAndDeletedFalse: returns empty when product is soft-deleted")
    void findBySkuAndDeletedFalse_deletedProduct_returnsEmpty() {
        ProductType pt = newProductType();
        entityManager.persistAndFlush(buildProduct(pt, "DEL-SKU-01", "Banana", true));

        Optional<Product> result = productRepository.findBySkuAndDeletedFalse("DEL-SKU-01");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchByKeyword: finds product by partial name match (case-insensitive)")
    void searchByKeyword_partialNameMatch_returnsMatchingProducts() {
        ProductType pt = newProductType();
        entityManager.persist(buildProduct(pt, "SRCH-SKU-01", "Orange Juice", false));
        entityManager.persistAndFlush(buildProduct(pt, "SRCH-SKU-02", "Apple Cider", false));

        Page<Product> result = productRepository.searchByKeyword("apple cider", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactly("Apple Cider");
    }

    @Test
    @DisplayName("searchByKeyword: finds product by partial SKU match")
    void searchByKeyword_skuMatch_returnsMatchingProducts() {
        ProductType pt = newProductType();
        entityManager.persist(buildProduct(pt, "BEVQ-001", "Cola", false));
        entityManager.persistAndFlush(buildProduct(pt, "XYZQ-001", "Chips", false));

        Page<Product> result = productRepository.searchByKeyword("bevq-", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("BEVQ-001");
    }

    @Test
    @DisplayName("searchByKeyword: excludes soft-deleted products")
    void searchByKeyword_deletedProduct_notIncluded() {
        ProductType pt = newProductType();
        entityManager.persistAndFlush(buildProduct(pt, "DELMNG-001", "Deleted Mangosteen", true));

        Page<Product> result = productRepository.searchByKeyword("mangosteen", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findSkusByPrefix: returns only SKUs starting with given prefix")
    void findSkusByPrefix_withMatchingPrefix_returnsSkus() {
        ProductType pt = newProductType();
        entityManager.persist(buildProduct(pt, "PFXTEST-010", "Rice", false));
        entityManager.persist(buildProduct(pt, "PFXTEST-011", "Bread", false));
        entityManager.persistAndFlush(buildProduct(pt, "OTHERP-001", "Water", false));

        List<String> skus = productRepository.findSkusByPrefix("PFXTEST-");

        assertThat(skus).containsExactlyInAnyOrder("PFXTEST-010", "PFXTEST-011");
        assertThat(skus).doesNotContain("OTHERP-001");
    }

    @Test
    @DisplayName("findByDeletedFalseAndStatus: paginates active products by status")
    void findByDeletedFalseAndStatus_activeProducts_returnsPaged() {
        ProductType pt = newProductType();
        entityManager.persist(buildProduct(pt, "STAT-SKU-01", "Tomato", false));
        entityManager.persist(buildProduct(pt, "STAT-SKU-02", "Potato", false));
        Product inactive = buildProduct(pt, "STAT-SKU-03", "Stale Bread", false);
        inactive.setStatus(Product.ProductStatus.INACTIVE);
        entityManager.persistAndFlush(inactive);

        Pageable pageable = PageRequest.of(0, 100);
        Page<Product> result = productRepository
                .findByDeletedFalseAndStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE, pageable);

        assertThat(result.getContent())
                .extracting(Product::getName)
                .contains("Tomato", "Potato")
                .doesNotContain("Stale Bread");
    }

    @Test
    @DisplayName("findByIdAndDeletedFalse: returns present for active, empty for deleted")
    void findByIdAndDeletedFalse_activeVsDeleted() {
        ProductType pt = newProductType();
        Product active = entityManager.persistAndFlush(buildProduct(pt, "ID-SKU-01", "Egg", false));
        Product deleted = entityManager.persistAndFlush(buildProduct(pt, "ID-SKU-02", "Old Egg", true));

        assertThat(productRepository.findByIdAndDeletedFalse(active.getId())).isPresent();
        assertThat(productRepository.findByIdAndDeletedFalse(deleted.getId())).isEmpty();
    }

    @Test
    @DisplayName("searchByKeyword: empty result when no match")
    void searchByKeyword_noMatch_returnsEmpty() {
        ProductType pt = newProductType();
        entityManager.persistAndFlush(buildProduct(pt, "NOM-SKU-01", "Milk", false));

        Page<Product> result = productRepository.searchByKeyword("zzznomatch999xyz", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}
