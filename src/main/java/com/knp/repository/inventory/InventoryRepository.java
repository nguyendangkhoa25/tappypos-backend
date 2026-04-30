package com.knp.repository.inventory;

import com.knp.model.entity.inventory.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Find inventory by ID when not deleted
     */
    Optional<Inventory> findByIdAndDeletedFalse(Long id);

    /**
     * Find inventory by product ID
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Find all inventory records for a product (supports multiple batches)
     */
    Page<Inventory> findByProductIdAndDeletedFalseOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * Find all active inventory items
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.status = 'ACTIVE'")
    Page<Inventory> findAllActive(Pageable pageable);

    /**
     * Find inventory items that are low in stock
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.quantityInStock <= i.reorderLevel")
    List<Inventory> findLowStockItems();

    /**
     * Find inventory by warehouse location
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.warehouseLocation = :location")
    Page<Inventory> findByWarehouseLocation(@Param("location") String location, Pageable pageable);

    /**
     * Find expired inventory items
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.expiryDate IS NOT NULL AND i.expiryDate < CURRENT_DATE")
    List<Inventory> findExpiredItems();

    /**
     * Find inventory items expiring soon (within 30 days from today)
     * This method needs to be called with expiryDate <= today + 30 days
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.expiryDate IS NOT NULL " +
            "AND i.expiryDate >= CURRENT_DATE AND i.expiryDate <= :expiryThreshold")
    List<Inventory> findExpiringSoon(@Param("expiryThreshold") java.time.LocalDate expiryThreshold);

    /**
     * Search inventory by product name, SKU, ID, batch number, or warehouse location
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND " +
            "(i.product.id = :searchTerm OR LOWER(i.product.name) LIKE LOWER(CONCAT('%', :searchKeyword, '%')) " +
            "OR LOWER(i.product.sku) LIKE LOWER(CONCAT('%', :searchKeyword, '%')) " +
            "OR LOWER(i.batchNumber) LIKE LOWER(CONCAT('%', :searchKeyword, '%')) " +
            "OR LOWER(i.warehouseLocation) LIKE LOWER(CONCAT('%', :searchKeyword, '%')))")
    Page<Inventory> searchByKeyword(@Param("searchTerm") Long searchTerm, @Param("searchKeyword") String searchKeyword, Pageable pageable);

    /**
     * Find inventory by inventory type
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.inventoryType = :type")
    Page<Inventory> findByInventoryType(@Param("type") Inventory.InventoryType type, Pageable pageable);

    /**
     * Count total inventory value
     */
    @Query("SELECT SUM(i.quantityInStock * i.unitCost) FROM Inventory i WHERE i.deleted = false AND i.status = 'ACTIVE'")
    Double calculateTotalInventoryValue();

    /**
     * Find active inventory by product ID
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.product.id = :productId AND i.status = 'ACTIVE'")
    Optional<Inventory> findActiveByProductId(@Param("productId") Long productId);

    /**
     * Locate products by name, SKU, batch, or any shelf location field
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.status = 'ACTIVE' AND " +
            "(LOWER(i.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.product.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.batchNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.zone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.aisle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.shelf) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.bin) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.warehouseLocation) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Inventory> locateByKeyword(@Param("keyword") String keyword);

    /**
     * Find inventory items by status
     */
    @Query("SELECT i FROM Inventory i WHERE i.deleted = false AND i.status = :status")
    Page<Inventory> findByStatus(@Param("status") Inventory.InventoryStatus status, Pageable pageable);

}

