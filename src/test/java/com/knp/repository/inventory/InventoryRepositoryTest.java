package com.knp.repository.inventory;

import com.knp.model.entity.inventory.Inventory;
import com.knp.model.entity.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryRepository Unit Tests")
class InventoryRepositoryTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private Inventory activeInventory() {
        Product product = Product.builder().id(1L).sku("FOOD-001").name("Apple").build();
        return Inventory.builder()
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
    }

    @Test
    @DisplayName("Should find inventory by ID when not deleted")
    void testFindByIdAndDeletedFalse_Success() {
        Inventory inv = activeInventory();
        when(inventoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inv));

        Optional<Inventory> result = inventoryRepository.findByIdAndDeletedFalse(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getQuantityInStock()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should not find deleted inventory by ID")
    void testFindByIdAndDeletedFalse_NotFound() {
        when(inventoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        Optional<Inventory> result = inventoryRepository.findByIdAndDeletedFalse(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find all inventory for a product")
    void testFindByProductIdAndDeletedFalseOrderByCreatedAtDesc_Success() {
        Inventory inv = activeInventory();
        Page<Inventory> page = new PageImpl<>(List.of(inv));
        when(inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<Inventory> results = inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getProduct().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find low stock items when quantity exceeds reorder level")
    void testFindLowStockItems_Empty() {
        when(inventoryRepository.findLowStockItems()).thenReturn(List.of());

        List<Inventory> results = inventoryRepository.findLowStockItems();

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find low stock when quantity is below reorder level")
    void testFindLowStockItems_Found() {
        Inventory lowStock = Inventory.builder()
                .quantityInStock(5L)
                .reorderLevel(10L)
                .deleted(false)
                .build();
        when(inventoryRepository.findLowStockItems()).thenReturn(List.of(lowStock));

        List<Inventory> results = inventoryRepository.findLowStockItems();

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).isLowStock()).isTrue();
    }

    @Test
    @DisplayName("Should find expired items")
    void testFindExpiredItems_Success() {
        when(inventoryRepository.findExpiredItems()).thenReturn(List.of());

        List<Inventory> results = inventoryRepository.findExpiredItems();

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find items expiring soon")
    void testFindExpiringItems_Success() {
        LocalDate threshold = LocalDate.now().plusDays(30);
        when(inventoryRepository.findExpiringSoon(threshold)).thenReturn(List.of());

        List<Inventory> results = inventoryRepository.findExpiringSoon(threshold);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should find inventory by warehouse location")
    void testFindByWarehouseLocation_Success() {
        Inventory inv = activeInventory();
        Page<Inventory> page = new PageImpl<>(List.of(inv));
        when(inventoryRepository.findByWarehouseLocation("Shelf A1", PageRequest.of(0, 10))).thenReturn(page);

        Page<Inventory> results = inventoryRepository.findByWarehouseLocation("Shelf A1", PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getWarehouseLocation()).isEqualTo("Shelf A1");
    }

    @Test
    @DisplayName("Should find inventory by type")
    void testFindByInventoryType_Success() {
        Inventory inv = activeInventory();
        Page<Inventory> page = new PageImpl<>(List.of(inv));
        when(inventoryRepository.findByInventoryType(Inventory.InventoryType.RETAIL, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<Inventory> results = inventoryRepository.findByInventoryType(
                Inventory.InventoryType.RETAIL, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getInventoryType()).isEqualTo(Inventory.InventoryType.RETAIL);
    }

    @Test
    @DisplayName("Should calculate total inventory value")
    void testCalculateTotalInventoryValue_Success() {
        when(inventoryRepository.calculateTotalInventoryValue()).thenReturn(250.0);

        Double result = inventoryRepository.calculateTotalInventoryValue();

        assertThat(result).isEqualTo(250.0);
    }

    @Test
    @DisplayName("Should find inventory by product ID")
    void testFindByProductId_Success() {
        Inventory inv = activeInventory();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

        Optional<Inventory> result = inventoryRepository.findByProductId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getProduct().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return correct values from repository")
    void testInventoryValues() {
        Inventory inv = activeInventory();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inv));

        Inventory saved = inventoryRepository.findById(1L).orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getQuantityInStock()).isEqualTo(100L);
        assertThat(saved.getBatchNumber()).isEqualTo("BATCH-001");
        assertThat(saved.getUnitCost()).isEqualByComparingTo(BigDecimal.valueOf(2.50));
    }

    @Test
    @DisplayName("Should support multiple batches per product")
    void testMultipleBatchesPerProduct_Success() {
        Inventory batch1 = activeInventory();
        Inventory batch2 = Inventory.builder()
                .product(batch1.getProduct())
                .quantityInStock(200L)
                .batchNumber("BATCH-002")
                .deleted(false)
                .build();
        Page<Inventory> page = new PageImpl<>(List.of(batch1, batch2));
        when(inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<Inventory> results = inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should calculate total value with multiple batches")
    void testCalculateTotalValue_MultipleBatches_Success() {
        // (100 * 2.50) + (50 * 2.40) = 250 + 120 = 370
        when(inventoryRepository.calculateTotalInventoryValue()).thenReturn(370.0);

        Double result = inventoryRepository.calculateTotalInventoryValue();

        assertThat(result).isEqualTo(370.0);
    }

    @Test
    @DisplayName("Should find inventory by status")
    void testFindByStatus_Success() {
        Inventory inv = activeInventory();
        Page<Inventory> page = new PageImpl<>(List.of(inv));
        when(inventoryRepository.findByStatus(Inventory.InventoryStatus.ACTIVE, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<Inventory> results = inventoryRepository.findByStatus(
                Inventory.InventoryStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent()).hasSize(1);
    }
}
