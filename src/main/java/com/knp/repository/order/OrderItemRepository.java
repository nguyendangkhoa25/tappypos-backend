package com.knp.repository.order;

import com.knp.model.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    BigDecimal sumTotalCost();

    @Query("SELECT COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    BigDecimal sumCostByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year")
    BigDecimal sumCostByYear(@Param("year") int year);

    // Monthly cost breakdown: [month, cost]
    @Query("SELECT MONTH(o.completedAt), COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year GROUP BY MONTH(o.completedAt) ORDER BY MONTH(o.completedAt)")
    List<Object[]> sumCostGroupedByMonth(@Param("year") int year);

    // Daily cost breakdown: [day, cost]
    @Query("SELECT DAY(o.completedAt), COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month GROUP BY DAY(o.completedAt) ORDER BY DAY(o.completedAt)")
    List<Object[]> sumCostGroupedByDay(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    Long sumTotalItemsSold();

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    Long sumItemsSoldByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year")
    Long sumItemsSoldByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt <= :to")
    Long sumItemsSoldByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    // Top products: [productId, productName, quantity, revenue, cost]
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity), COALESCE(SUM(oi.amount), 0), COALESCE(SUM(oi.costAmount), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND (:year IS NULL OR YEAR(o.completedAt) = :year) AND (:month IS NULL OR MONTH(o.completedAt) = :month) GROUP BY oi.productId, oi.productName ORDER BY SUM(oi.amount) DESC")
    List<Object[]> findTopProducts(@Param("year") Integer year, @Param("month") Integer month);
}
