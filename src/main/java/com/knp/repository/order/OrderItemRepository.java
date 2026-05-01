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

    @Query(value = "SELECT COALESCE(SUM(oi.cost_amount), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year AND EXTRACT(MONTH FROM o.completed_at) = :month",
           nativeQuery = true)
    BigDecimal sumCostByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COALESCE(SUM(oi.cost_amount), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year",
           nativeQuery = true)
    BigDecimal sumCostByYear(@Param("year") int year);

    // Monthly cost breakdown: [month, cost]
    @Query(value = "SELECT EXTRACT(MONTH FROM o.completed_at), COALESCE(SUM(oi.cost_amount), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year GROUP BY EXTRACT(MONTH FROM o.completed_at) ORDER BY EXTRACT(MONTH FROM o.completed_at)",
           nativeQuery = true)
    List<Object[]> sumCostGroupedByMonth(@Param("year") int year);

    // Daily cost breakdown: [day, cost]
    @Query(value = "SELECT EXTRACT(DAY FROM o.completed_at), COALESCE(SUM(oi.cost_amount), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year AND EXTRACT(MONTH FROM o.completed_at) = :month GROUP BY EXTRACT(DAY FROM o.completed_at) ORDER BY EXTRACT(DAY FROM o.completed_at)",
           nativeQuery = true)
    List<Object[]> sumCostGroupedByDay(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    Long sumTotalItemsSold();

    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year AND EXTRACT(MONTH FROM o.completed_at) = :month",
           nativeQuery = true)
    Long sumItemsSoldByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND EXTRACT(YEAR FROM o.completed_at) = :year",
           nativeQuery = true)
    Long sumItemsSoldByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt <= :to")
    Long sumItemsSoldByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    // Top products: [productId, productName, quantity, revenue, cost]
    @Query(value = "SELECT oi.product_id, oi.product_name, SUM(oi.quantity), COALESCE(SUM(oi.amount), 0), COALESCE(SUM(oi.cost_amount), 0) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.deleted = false AND o.status = 'COMPLETED' AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM o.completed_at) = CAST(:year AS integer)) AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM o.completed_at) = CAST(:month AS integer)) GROUP BY oi.product_id, oi.product_name ORDER BY SUM(oi.amount) DESC",
           nativeQuery = true)
    List<Object[]> findTopProducts(@Param("year") Integer year, @Param("month") Integer month);
}
