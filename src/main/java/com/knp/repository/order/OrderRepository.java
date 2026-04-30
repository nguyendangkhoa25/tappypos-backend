package com.knp.repository.order;

import com.knp.model.entity.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.deleted = false ORDER BY o.createdAt DESC")
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false ORDER BY o.createdAt DESC")
    Page<Order> findAllActive(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByStatus(@Param("status") Order.OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Order o LEFT JOIN o.customer c
            WHERE o.deleted = false
              AND (LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── Revenue aggregation ────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    BigDecimal sumRevenueByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year")
    BigDecimal sumRevenueByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt <= :to")
    BigDecimal sumRevenueByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.taxAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    BigDecimal sumTotalTax();

    @Query("SELECT COALESCE(SUM(o.discountAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    BigDecimal sumTotalDiscount();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    Long countCompleted();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    Long countCompletedByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year")
    Long countCompletedByYear(@Param("year") int year);

    // Monthly breakdown: [month, revenue, orderCount]
    @Query("SELECT MONTH(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year GROUP BY MONTH(o.completedAt) ORDER BY MONTH(o.completedAt)")
    List<Object[]> sumRevenueGroupedByMonth(@Param("year") int year);

    // Daily breakdown: [day, revenue, orderCount]
    @Query("SELECT DAY(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month GROUP BY DAY(o.completedAt) ORDER BY DAY(o.completedAt)")
    List<Object[]> sumRevenueGroupedByDay(@Param("year") int year, @Param("month") int month);

    // Payment method breakdown: [paymentMethod, count, totalAmount]
    @Query("SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND (:year IS NULL OR YEAR(o.completedAt) = :year) AND (:month IS NULL OR MONTH(o.completedAt) = :month) GROUP BY o.paymentMethod")
    List<Object[]> groupByPaymentMethod(@Param("year") Integer year, @Param("month") Integer month);

    // Day-of-week breakdown: [dayOfWeek(1=Sun..7=Sat), revenue, orderCount]
    @Query("SELECT DAYOFWEEK(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND (:year IS NULL OR YEAR(o.completedAt) = :year) AND (:month IS NULL OR MONTH(o.completedAt) = :month) GROUP BY DAYOFWEEK(o.completedAt) ORDER BY DAYOFWEEK(o.completedAt)")
    List<Object[]> sumRevenueGroupedByDayOfWeek(@Param("year") Integer year, @Param("month") Integer month);

    // Hourly breakdown: [hour(0-23), revenue, orderCount]
    @Query("SELECT HOUR(o.completedAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND (:year IS NULL OR YEAR(o.completedAt) = :year) AND (:month IS NULL OR MONTH(o.completedAt) = :month) GROUP BY HOUR(o.completedAt) ORDER BY HOUR(o.completedAt)")
    List<Object[]> sumRevenueGroupedByHour(@Param("year") Integer year, @Param("month") Integer month);

    // Category breakdown: [categoryName, orderCount, revenue]
    @Query(value = """
            SELECT COALESCE(c.name, 'Không phân loại') AS categoryName,
                   COUNT(DISTINCT o.id) AS orderCount,
                   COALESCE(SUM(oi.amount), 0) AS revenue
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN product p ON p.id = oi.product_id
            LEFT JOIN product_category pc ON pc.product_id = p.id
            LEFT JOIN category c ON c.id = pc.category_id
            WHERE o.deleted = 0 AND o.status = 'COMPLETED'
              AND (:year IS NULL OR YEAR(o.completed_at) = :year)
              AND (:month IS NULL OR MONTH(o.completed_at) = :month)
            GROUP BY COALESCE(c.name, 'Không phân loại')
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> sumRevenueGroupedByCategory(@Param("year") Integer year, @Param("month") Integer month);

    // Top employees: [employeeName, orderCount, revenue]
    @Query(value = """
            SELECT COALESCE(e.full_name, o.created_by) AS employeeName,
                   COUNT(o.id) AS orderCount,
                   COALESCE(SUM(o.total_amount), 0) AS revenue
            FROM orders o
            LEFT JOIN users u ON u.username = o.created_by
            LEFT JOIN employees e ON e.user_id = u.id
            WHERE o.deleted = 0 AND o.status = 'COMPLETED'
              AND (:year IS NULL OR YEAR(o.completed_at) = :year)
              AND (:month IS NULL OR MONTH(o.completed_at) = :month)
            GROUP BY COALESCE(e.full_name, o.created_by), o.created_by
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> sumRevenueGroupedByEmployee(@Param("year") Integer year, @Param("month") Integer month);

    // Recent completed orders for dashboard
    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' ORDER BY o.completedAt DESC")
    List<Order> findRecentCompleted(Pageable pageable);

    long countByDeletedFalseAndStatus(Order.OrderStatus status);

    // Total customers
    @Query("SELECT COUNT(DISTINCT o.customer.id) FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND o.customer IS NOT NULL")
    Long countDistinctCustomers();

    // ── My Work ────────────────────────────────────────────────────────────────

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status IN :statuses ORDER BY o.createdAt ASC")
    Page<Order> findActiveByCreatedBy(@Param("username") String username, @Param("statuses") Collection<Order.OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt < :to ORDER BY o.completedAt DESC")
    Page<Order> findCompletedByCreatedByAndPeriod(@Param("username") String username, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt < :to")
    Object[] getMyCompletedStats(@Param("username") String username, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status IN :statuses")
    Long countActiveByCreatedBy(@Param("username") String username, @Param("statuses") Collection<Order.OrderStatus> statuses);
}
