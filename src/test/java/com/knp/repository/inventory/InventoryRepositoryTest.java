package com.knp.repository.inventory;

import com.knp.model.entity.inventory.Inventory;
import com.knp.model.entity.product.Product;
import com.knp.model.entity.product.ProductType;
import com.knp.repository.product.ProductRepository;
import com.knp.repository.product.ProductTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("InventoryRepository Unit Tests")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Inventory inventory;
    private Product product;
    private ProductType productType;

    @BeforeEach
    void setUp() {
        // Delete in correct order to avoid foreign key violations
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        productTypeRepository.deleteAll();
        entityManager.flush();

        productType = ProductType.builder()
                .code("FOOD")
                .name("Food")
                .deleted(false)
                .build();
        productTypeRepository.save(productType);

        product = Product.builder()
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .costPrice(BigDecimal.valueOf(3.00))
                .status(Product.ProductStatus.ACTIVE)
                .deleted(false)
                .build();
        productRepository.save(product);

        inventory = Inventory.builder()
                .product(product)
                .quantityInStock(100L)
                .reorderLevel(10L)
                .reorderQuantity(50L)
                .unitCost(BigDecimal.valueOf(2.50))
                .warehouseLocation("Shelf A1")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .batchNumber("BATCH-001")
                .status(Inventory.InventoryStatus.ACTIVE)
                .inventoryType(Inventory.InventoryType.RETAIL)
                .deleted(false)
                .build();
        inventoryRepository.save(inventory);
    }

    @Test
    @DisplayName("Should find inventory by ID when not deleted")
    void testFindByIdAndDeletedFalse_Success() {
        // When
        Optional<Inventory> result = inventoryRepository.findByIdAndDeletedFalse(inventory.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getQuantityInStock()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should not find deleted inventory by ID")
    void testFindByIdAndDeletedFalse_NotFound() {
        // Given
        inventory.setDeleted(true);
        inventoryRepository.save(inventory);

        // When
        Optional<Inventory> result = inventoryRepository.findByIdAndDeletedFalse(inventory.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find all inventory for a product")
    void testFindByProductIdAndDeletedFalseOrderByCreatedAtDesc_Success() {
        // When
        Page<Inventory> results = inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(
                product.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("Should find low stock items")
    void testFindLowStockItems_Success() {
        // When
        List<Inventory> results = inventoryRepository.findLowStockItems();

        // Then
        assertThat(results).isEmpty();  // 100 > 10 (reorder level)
    }

    @Test
    @DisplayName("Should find low stock when quantity is below reorder level")
    void testFindLowStockItems_Found() {
        // Given
        inventory.setQuantityInStock(5L);  // Less than reorder level (10)
        inventoryRepository.save(inventory);

        // When
        List<Inventory> results = inventoryRepository.findLowStockItems();

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).isLowStock()).isTrue();
    }

    @Test
    @DisplayName("Should find expired items")
    void testFindExpiredItems_Success() {
        // When
        List<Inventory> results = inventoryRepository.findExpiredItems();

        // Then
        assertThat(results).isEmpty();  // Expiry is in future
    }

    @Test
    @DisplayName("Should find items expiring soon")
    void testFindExpiringItems_Success() {
        // When
        java.time.LocalDate expiryThreshold = java.time.LocalDate.now().plusDays(30);
        List<Inventory> results = inventoryRepository.findExpiringSoon(expiryThreshold);

        // Then
        assertThat(results).isEmpty();  // 2027-12-31 is not expiring soon
    }

    @Test
    @DisplayName("Should find inventory by warehouse location")
    void testFindByWarehouseLocation_Success() {
        // When
        Page<Inventory> results = inventoryRepository.findByWarehouseLocation(
                "Shelf A1", PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getWarehouseLocation()).isEqualTo("Shelf A1");
    }

    @Test
    @DisplayName("Should find inventory by type")
    void testFindByInventoryType_Success() {
        // When
        Page<Inventory> results = inventoryRepository.findByInventoryType(
                Inventory.InventoryType.RETAIL, PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getInventoryType()).isEqualTo(Inventory.InventoryType.RETAIL);
    }

    @Test
    @DisplayName("Should calculate total inventory value")
    void testCalculateTotalInventoryValue_Success() {
        // When
        Double result = inventoryRepository.calculateTotalInventoryValue();

        // Then
        assertThat(result).isNotNull();
        // Total value = 100 * 2.50 = 250.00
        assertThat(result).isEqualTo(250.00);
    }

    @Test
    @DisplayName("Should find inventory by product ID")
    void testFindByProductId_Success() {
        // When
        Optional<Inventory> result = inventoryRepository.findByProductId(product.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("Should persist and retrieve inventory with correct values")
    void testPersistInventory_Success() {
        // When
        Inventory saved = inventoryRepository.findById(inventory.getId()).orElse(null);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getQuantityInStock()).isEqualTo(100L);
        assertThat(saved.getBatchNumber()).isEqualTo("BATCH-001");
        assertThat(saved.getUnitCost()).isEqualByComparingTo(BigDecimal.valueOf(2.50));
    }

    @Test
    @DisplayName("Should support multiple batches per product")
    void testMultipleBatchesPerProduct_Success() {
        // Given
        Inventory batch2 = Inventory.builder()
                .product(product)
                .quantityInStock(200L)
                .reorderLevel(20L)
                .reorderQuantity(100L)
                .unitCost(BigDecimal.valueOf(2.40))
                .warehouseLocation("Shelf A2")
                .expiryDate(LocalDate.of(2028, 6, 30))
                .batchNumber("BATCH-002")
                .status(Inventory.InventoryStatus.ACTIVE)
                .inventoryType(Inventory.InventoryType.RETAIL)
                .deleted(false)
                .build();
        inventoryRepository.save(batch2);

        // When
        Page<Inventory> results = inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(
                product.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should calculate total value with multiple batches")
    void testCalculateTotalValue_MultipleBatches_Success() {
        // Given
        Inventory batch2 = Inventory.builder()
                .product(product)
                .quantityInStock(50L)
                .reorderLevel(10L)
                .reorderQuantity(25L)
                .unitCost(BigDecimal.valueOf(2.40))
                .warehouseLocation("Shelf B1")
                .expiryDate(LocalDate.of(2028, 6, 30))
                .batchNumber("BATCH-002")
                .status(Inventory.InventoryStatus.ACTIVE)
                .inventoryType(Inventory.InventoryType.RETAIL)
                .deleted(false)
                .build();
        inventoryRepository.save(batch2);

        // When
        Double result = inventoryRepository.calculateTotalInventoryValue();

        // Then
        // Total = (100 * 2.50) + (50 * 2.40) = 250 + 120 = 370
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(370.00);
    }

    @Test
    @DisplayName("Should find inventory by status")
    void testFindByStatus_Success() {
        // When
        Page<Inventory> results = inventoryRepository.findByStatus(Inventory.InventoryStatus.ACTIVE, PageRequest.of(0, 10));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
    }
}

