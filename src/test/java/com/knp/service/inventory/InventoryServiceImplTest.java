package com.knp.service.inventory;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.inventory.CreateInventoryRequest;
import com.knp.model.dto.inventory.InventoryDTO;
import com.knp.model.dto.inventory.UpdateInventoryRequest;
import com.knp.model.entity.inventory.Inventory;
import com.knp.model.entity.product.Product;
import com.knp.model.entity.product.ProductType;
import com.knp.repository.inventory.InventoryRepository;
import com.knp.repository.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import com.knp.service.MessageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private CreateInventoryRequest createInventoryRequest;
    private UpdateInventoryRequest updateInventoryRequest;
    private Inventory inventory;
    private Product product;

    @BeforeEach
    void setUp() {
        // Initialize test data
        ProductType productType = ProductType.builder()
                .id(1L)
                .code("FOOD")
                .name("Food")
                .deleted(false)
                .build();

        product = Product.builder()
                .id(1L)
                .productType(productType)
                .sku("FOOD-001")
                .name("Apple")
                .price(BigDecimal.valueOf(5.99))
                .status(Product.ProductStatus.ACTIVE)
                .attributeValues(new java.util.HashSet<>())  // Initialize to avoid NPE
                .categories(new java.util.HashSet<>())        // Initialize to avoid NPE
                .deleted(false)
                .build();

        inventory = Inventory.builder()
                .id(1L)
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

        createInventoryRequest = CreateInventoryRequest.builder()
                .productId(1L)
                .quantityInStock(100L)
                .reorderLevel(10L)
                .reorderQuantity(50L)
                .unitCost(BigDecimal.valueOf(2.50))
                .warehouseLocation("Shelf A1")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .batchNumber("BATCH-001")
                .status("ACTIVE")
                .inventoryType("RETAIL")
                .build();

        updateInventoryRequest = UpdateInventoryRequest.builder()
                .quantityInStock(150L)
                .reorderLevel(15L)
                .reorderQuantity(75L)
                .unitCost(BigDecimal.valueOf(2.75))
                .warehouseLocation("Shelf A2")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .batchNumber("BATCH-001")
                .status("ACTIVE")
                .inventoryType("RETAIL")
                .build();
    }

    @Test
    @DisplayName("Should create inventory successfully")
    void testCreateInventory_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        InventoryDTO result = inventoryService.createInventory(createInventoryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getQuantityInStock()).isEqualTo(100L);
        assertThat(result.getBatchNumber()).isEqualTo("BATCH-001");
        verify(productRepository).findById(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should get inventory by ID successfully")
    void testGetInventoryById_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        // When
        InventoryDTO result = inventoryService.getInventoryById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getQuantityInStock()).isEqualTo(100L);
        verify(inventoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when inventory not found")
    void testGetInventoryById_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.getInventoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update inventory successfully")
    void testUpdateInventory_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        InventoryDTO result = inventoryService.updateInventory(1L, updateInventoryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should delete inventory successfully")
    void testDeleteInventory_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        inventoryService.deleteInventory(1L);

        // Then
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should get all inventory with pagination")
    void testGetAllInventory_Success() {
        // Given
        Page<Inventory> inventoryPage = new PageImpl<>(Collections.singletonList(inventory), PageRequest.of(0, 10), 1);
        doReturn(inventoryPage).when(inventoryRepository).findAllActive(any());

        // When
        Page<InventoryDTO> result = inventoryService.getAllInventory(PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getQuantityInStock()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should get inventory by product ID with pagination")
    void testGetInventoryByProductId_Success() {
        // Given
        Page<Inventory> inventoryPage = new PageImpl<>(Collections.singletonList(inventory), PageRequest.of(0, 10), 1);
        doReturn(inventoryPage).when(inventoryRepository).findByProductIdAndDeletedFalseOrderByCreatedAtDesc(eq(1L), any());

        // When
        Page<InventoryDTO> result = inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should add stock successfully")
    void testAddStock_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        InventoryDTO result = inventoryService.addStock(1L, 50L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should remove stock successfully")
    void testRemoveStock_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        InventoryDTO result = inventoryService.removeStock(1L, 10L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw exception when removing more stock than available")
    void testRemoveStock_InsufficientStock() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThatThrownBy(() -> inventoryService.removeStock(1L, 200L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should update quantity successfully")
    void testUpdateQuantity_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        InventoryDTO result = inventoryService.updateInventoryQuantity(1L, 200L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should get low stock items")
    void testGetLowStockItems_Success() {
        // Given
        List<Inventory> lowStockItems = Collections.singletonList(inventory);
        when(inventoryRepository.findLowStockItems()).thenReturn(lowStockItems);

        // When
        List<InventoryDTO> result = inventoryService.getLowStockItems();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(inventoryRepository).findLowStockItems();
    }

    @Test
    @DisplayName("Should get expired items")
    void testGetExpiredItems_Success() {
        // Given
        List<Inventory> expiredItems = Collections.emptyList();
        when(inventoryRepository.findExpiredItems()).thenReturn(expiredItems);

        // When
        List<InventoryDTO> result = inventoryService.getExpiredItems();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(inventoryRepository).findExpiredItems();
    }

    @Test
    @DisplayName("Should get expiring soon items")
    void testGetExpiringItems_Success() {
        // Given
        List<Inventory> expiringItems = Collections.singletonList(inventory);
        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        when(inventoryRepository.findExpiringSoon(expiryThreshold)).thenReturn(expiringItems);

        // When
        List<InventoryDTO> result = inventoryService.getExpiringItems();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(inventoryRepository).findExpiringSoon(expiryThreshold);
    }

    @Test
    @DisplayName("Should search inventory by keyword")
    void testSearchInventory_Success() {
        // Given
        Page<Inventory> searchResults = new PageImpl<>(Collections.singletonList(inventory), PageRequest.of(0, 10), 1);
        // searchInventory catches NumberFormatException and calls with 0L when keyword is not a number
        doReturn(searchResults).when(inventoryRepository).searchByKeyword(eq(0L), eq("BATCH"), any());

        // When
        Page<InventoryDTO> result = inventoryService.searchInventory("BATCH", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should get inventory by warehouse location")
    void testGetByWarehouseLocation_Success() {
        // Given
        Page<Inventory> warehouseItems = new PageImpl<>(Collections.singletonList(inventory));
        when(inventoryRepository.findByWarehouseLocation("Shelf A1", Pageable.unpaged()))
                .thenReturn(warehouseItems);

        // When
        Page<InventoryDTO> result = inventoryService.getByWarehouseLocation("Shelf A1", Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).findByWarehouseLocation("Shelf A1", Pageable.unpaged());
    }

    @Test
    @DisplayName("Should get inventory by type")
    void testGetByType_Success() {
        // Given
        Page<Inventory> typeItems = new PageImpl<>(Collections.singletonList(inventory));
        when(inventoryRepository.findByInventoryType(Inventory.InventoryType.RETAIL, Pageable.unpaged()))
                .thenReturn(typeItems);

        // When
        Page<InventoryDTO> result = inventoryService.getByType("RETAIL", Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).findByInventoryType(Inventory.InventoryType.RETAIL, Pageable.unpaged());
    }

    @Test
    @DisplayName("Should calculate total inventory value")
    void testCalculateTotalValue_Success() {
        // Given
        when(inventoryRepository.calculateTotalInventoryValue()).thenReturn(250.00);

        // When
        Double result = inventoryService.calculateTotalValue();

        // Then
        assertThat(result).isEqualTo(250.00);
        verify(inventoryRepository).calculateTotalInventoryValue();
    }

    // ============= Additional Tests for >90% Coverage =============

    @Test
    @DisplayName("Should throw exception when deleting non-existent inventory")
    void testDeleteInventory_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.inventory.not.found", 999L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.deleteInventory(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return empty list when no low stock items")
    void testGetLowStockItems_Empty() {
        // Given
        when(inventoryRepository.findLowStockItems()).thenReturn(Collections.emptyList());

        // When
        List<InventoryDTO> result = inventoryService.getLowStockItems();

        // Then
        assertThat(result).isEmpty();
        verify(inventoryRepository).findLowStockItems();
    }

    @Test
    @DisplayName("Should return empty list when no expired items")
    void testGetExpiredItems_Empty() {
        // Given
        when(inventoryRepository.findExpiredItems()).thenReturn(Collections.emptyList());

        // When
        List<InventoryDTO> result = inventoryService.getExpiredItems();

        // Then
        assertThat(result).isEmpty();
        verify(inventoryRepository).findExpiredItems();
    }

    @Test
    @DisplayName("Should get expiring soon items successfully")
    void testGetExpiringSoon_Success() {
        // Given
        Inventory expiringItem = Inventory.builder()
                .id(4L)
                .product(product)
                .quantityInStock(10L)
                .reorderLevel(5L)
                .reorderQuantity(10L)
                .unitCost(BigDecimal.valueOf(2.50))
                .warehouseLocation("Shelf B1")
                .expiryDate(LocalDate.now().plusDays(5))
                .batchNumber("BATCH-002")
                .status(Inventory.InventoryStatus.ACTIVE)
                .inventoryType(Inventory.InventoryType.RETAIL)
                .deleted(false)
                .build();
        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        when(inventoryRepository.findExpiringSoon(expiryThreshold)).thenReturn(List.of(expiringItem));

        // When
        List<InventoryDTO> result = inventoryService.getExpiringSoon();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(inventoryRepository).findExpiringSoon(expiryThreshold);
    }

    @Test
    @DisplayName("Should search inventory by product ID")
    void testSearchInventory_ByProductId() {
        // Given
        Inventory searchInventory = Inventory.builder()
                .id(1L)
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
        Page<Inventory> searchResults = new PageImpl<>(List.of(searchInventory), PageRequest.of(0, 10), 1);
        doReturn(searchResults).when(inventoryRepository)
                .searchByKeyword(eq(1L), eq("1"), any(Pageable.class));

        // When
        Page<InventoryDTO> result = inventoryService.searchInventory("1", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).searchByKeyword(eq(1L), eq("1"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should search inventory by batch number")
    void testSearchInventory_ByBatchNumber() {
        // Given
        Inventory searchInventory = Inventory.builder()
                .id(1L)
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
        Page<Inventory> searchResults = new PageImpl<>(List.of(searchInventory), PageRequest.of(0, 10), 1);
        doReturn(searchResults).when(inventoryRepository)
                .searchByKeyword(eq(0L), eq("BATCH123"), any(Pageable.class));

        // When
        Page<InventoryDTO> result = inventoryService.searchInventory("BATCH123", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).searchByKeyword(eq(0L), eq("BATCH123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get inventory by warehouse location with pagination")
    void testGetInventoryByWarehouse_Success() {
        // Given
        Page<Inventory> warehouseItems = new PageImpl<>(List.of(inventory));
        when(inventoryRepository.findByWarehouseLocation("Shelf A1", PageRequest.of(0, 10)))
                .thenReturn(warehouseItems);

        // When
        Page<InventoryDTO> result = inventoryService.getInventoryByWarehouse("Shelf A1", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).findByWarehouseLocation("Shelf A1", PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should get inventory by type with pagination")
    void testGetInventoryByType_Success() {
        // Given
        Page<Inventory> typeItems = new PageImpl<>(List.of(inventory));
        when(inventoryRepository.findByInventoryType(Inventory.InventoryType.RETAIL, PageRequest.of(0, 10)))
                .thenReturn(typeItems);

        // When
        Page<InventoryDTO> result = inventoryService.getInventoryByType("RETAIL", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(inventoryRepository).findByInventoryType(Inventory.InventoryType.RETAIL, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should throw exception when updating quantity for non-existent inventory")
    void testUpdateInventoryQuantity_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.inventory.not.found", 999L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.updateInventoryQuantity(999L, 150L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating quantity for deleted inventory")
    void testUpdateInventoryQuantity_Deleted() {
        // Given
        inventory.softDelete();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(messageService.getMessage("error.inventory.not.found", 1L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.updateInventoryQuantity(1L, 150L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when adding stock to non-existent inventory")
    void testAddStock_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.inventory.not.found", 999L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.addStock(999L, 50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when adding stock to deleted inventory")
    void testAddStock_Deleted() {
        // Given
        inventory.softDelete();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(messageService.getMessage("error.inventory.not.found", 1L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.addStock(1L, 50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when removing stock from non-existent inventory")
    void testRemoveStock_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.inventory.not.found", 999L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.removeStock(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when removing stock from deleted inventory")
    void testRemoveStock_Deleted() {
        // Given
        inventory.softDelete();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(messageService.getMessage("error.inventory.not.found", 1L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.removeStock(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should calculate total inventory value with null result")
    void testCalculateTotalInventoryValue_Null() {
        // Given
        when(inventoryRepository.calculateTotalInventoryValue()).thenReturn(null);

        // When
        Double result = inventoryService.calculateTotalInventoryValue();

        // Then
        assertThat(result).isEqualTo(0.0);
        verify(inventoryRepository).calculateTotalInventoryValue();
    }

    @Test
    @DisplayName("Should get all inventory with empty result")
    void testGetAllInventory_Empty() {
        // Given
        Page<Inventory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(inventoryRepository.findAllActive(PageRequest.of(0, 10)))
                .thenReturn(emptyPage);

        // When
        Page<InventoryDTO> result = inventoryService.getAllInventory(PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(inventoryRepository).findAllActive(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should get inventory by product ID with empty result")
    void testGetInventoryByProductId_Empty() {
        // Given
        Page<Inventory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(999L, PageRequest.of(0, 10)))
                .thenReturn(emptyPage);

        // When
        Page<InventoryDTO> result = inventoryService.getInventoryByProductId(999L, PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(inventoryRepository).findByProductIdAndDeletedFalseOrderByCreatedAtDesc(999L, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should throw exception when getting deleted inventory by ID")
    void testGetInventoryById_Deleted() {
        // Given
        inventory.softDelete();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(messageService.getMessage("error.inventory.not.found", 1L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.getInventoryById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating deleted inventory")
    void testUpdateInventory_Deleted() {
        // Given
        inventory.softDelete();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(messageService.getMessage("error.inventory.not.found", 1L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.updateInventory(1L, updateInventoryRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent inventory")
    void testUpdateInventory_NotFound() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.inventory.not.found", 999L))
                .thenReturn("Inventory not found");

        // When & Then
        assertThatThrownBy(() -> inventoryService.updateInventory(999L, updateInventoryRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

