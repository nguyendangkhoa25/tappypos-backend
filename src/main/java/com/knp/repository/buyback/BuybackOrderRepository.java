package com.knp.repository.buyback;

import com.knp.model.entity.buyback.BuybackOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BuybackOrderRepository extends JpaRepository<BuybackOrder, Long> {

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findAllActive(Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.status = :status ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByStatus(@Param("status") BuybackOrder.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.type = :type ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByType(@Param("type") BuybackOrder.OrderType type, Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.type = :type AND o.status = :status ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByTypeAndStatus(
            @Param("type") BuybackOrder.OrderType type,
            @Param("status") BuybackOrder.OrderStatus status,
            Pageable pageable);

    @Query("SELECT MAX(o.id) FROM BuybackOrder o")
    Long findMaxId();

    // Total items (lots) purchased from customers (BUY type, COMPLETED)
    @Query("SELECT COUNT(i) FROM BuybackOrder o JOIN o.items i WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND i.itemType = 'BUY'")
    Long sumTotalItemsBought();

    @Query("SELECT COUNT(i) FROM BuybackOrder o JOIN o.items i WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND i.itemType = 'BUY' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    Long sumItemsBoughtByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(i) FROM BuybackOrder o JOIN o.items i WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND i.itemType = 'BUY' AND YEAR(o.completedAt) = :year")
    Long sumItemsBoughtByYear(@Param("year") int year);

    // Total money paid to customers for buyback (BUY type, COMPLETED)
    @Query("SELECT COALESCE(SUM(o.buyTotal), 0) FROM BuybackOrder o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY'")
    java.math.BigDecimal sumTotalBuybackSpent();

    @Query("SELECT COALESCE(SUM(o.buyTotal), 0) FROM BuybackOrder o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    java.math.BigDecimal sumBuybackSpentByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.buyTotal), 0) FROM BuybackOrder o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND YEAR(o.completedAt) = :year")
    java.math.BigDecimal sumBuybackSpentByYear(@Param("year") int year);

    // Count completed buyback BUY orders
    @Query("SELECT COUNT(o) FROM BuybackOrder o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY'")
    Long countCompletedBuyOrders();

    @Query("SELECT COUNT(i) FROM BuybackOrder o JOIN o.items i WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND i.itemType = 'BUY' AND o.completedAt >= :from AND o.completedAt <= :to")
    Long sumItemsBoughtByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.buyTotal), 0) FROM BuybackOrder o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.type = 'BUY' AND o.completedAt >= :from AND o.completedAt <= :to")
    java.math.BigDecimal sumBuybackSpentByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
