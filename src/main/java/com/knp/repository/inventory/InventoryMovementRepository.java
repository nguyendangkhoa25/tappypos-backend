package com.knp.repository.inventory;

import com.knp.model.entity.inventory.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    /**
     * Find all movements for an inventory
     */
    List<InventoryMovement> findByInventoryIdOrderByMovementDateDesc(Long inventoryId);

    /**
     * Find movements for an inventory with pagination
     */
    Page<InventoryMovement> findByInventoryIdOrderByMovementDateDesc(Long inventoryId, Pageable pageable);

    /**
     * Find movements by type
     */
    List<InventoryMovement> findByMovementTypeOrderByMovementDateDesc(String movementType);

    /**
     * Find movements between dates
     */
    @Query("SELECT im FROM InventoryMovement im WHERE im.movementDate BETWEEN :startDate AND :endDate ORDER BY im.movementDate DESC")
    List<InventoryMovement> findMovementsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find movements by reference number (PO, SO, etc.)
     */
    List<InventoryMovement> findByReferenceNumberAndDeletedFalse(String referenceNumber);

    /**
     * Find movements for a product
     */
    @Query("SELECT im FROM InventoryMovement im WHERE im.inventory.product.id = :productId ORDER BY im.movementDate DESC")
    List<InventoryMovement> findMovementsByProduct(@Param("productId") Long productId);

    /**
     * Find recent movements (last 30 days)
     */
    @Query("SELECT im FROM InventoryMovement im WHERE im.movementDate >= :since ORDER BY im.movementDate DESC")
    List<InventoryMovement> findRecentMovements(@Param("since") LocalDateTime since);
}

