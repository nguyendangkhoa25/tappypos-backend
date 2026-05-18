package com.tappy.pos.service.inventory;

import com.tappy.pos.model.dto.inventory.CreateInventoryRequest;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.inventory.UpdateInventoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    /**
     * Create a new inventory record
     */
    InventoryDTO createInventory(CreateInventoryRequest request);

    /**
     * Get inventory by ID
     */
    InventoryDTO getInventoryById(Long id);

    /**
     * Get inventory by product ID (may return multiple batches)
     */
    Page<InventoryDTO> getInventoryByProductId(Long productId, Pageable pageable);

    /**
     * Get inventory for a specific (product, variant) pair.
     * Returns empty if no record exists — callers treat that as out-of-stock.
     */
    Page<InventoryDTO> getInventoryByProductIdAndVariantId(Long productId, Long variantId, Pageable pageable);

    /**
     * Get all inventory items with pagination
     */
    Page<InventoryDTO> getAllInventory(Pageable pageable);

    /**
     * Update inventory record
     */
    InventoryDTO updateInventory(Long id, UpdateInventoryRequest request);

    /**
     * Delete inventory (soft delete)
     */
    void deleteInventory(Long id);

    /**
     * Get low stock items
     */
    List<InventoryDTO> getLowStockItems();

    /**
     * Get expired items
     */
    List<InventoryDTO> getExpiredItems();

    /**
     * Get items expiring soon (within 30 days)
     */
    List<InventoryDTO> getExpiringSoon();

    /**
     * Get items expiring soon (within 30 days) - alias
     */
    List<InventoryDTO> getExpiringItems();

    /**
     * Search inventory by keyword
     */
    Page<InventoryDTO> searchInventory(String keyword, Pageable pageable);

    /**
     * Get inventory by warehouse location
     */
    Page<InventoryDTO> getInventoryByWarehouse(String location, Pageable pageable);

    /**
     * Get inventory by warehouse location - alias
     */
    Page<InventoryDTO> getByWarehouseLocation(String location, Pageable pageable);

    /**
     * Get inventory by type
     */
    Page<InventoryDTO> getInventoryByType(String type, Pageable pageable);

    /**
     * Get inventory by type - alias
     */
    Page<InventoryDTO> getByType(String type, Pageable pageable);

    /**
     * Calculate total inventory value
     */
    Double calculateTotalInventoryValue();

    /**
     * Calculate total value - alias
     */
    Double calculateTotalValue();

    /**
     * Update inventory quantity (for stock movements)
     */
    InventoryDTO updateInventoryQuantity(Long id, Long newQuantity);

    /**
     * Add stock to inventory
     */
    InventoryDTO addStock(Long id, Long quantity);

    /**
     * Remove stock from inventory
     */
    InventoryDTO removeStock(Long id, Long quantity);

    /**
     * Locate products by name, SKU, batch, zone, aisle, shelf, or bin keyword
     */
    List<InventoryDTO> locateProduct(String keyword);
}

