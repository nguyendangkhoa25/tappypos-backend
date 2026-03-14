package com.knp.repository;

import com.knp.model.entity.Order;
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
