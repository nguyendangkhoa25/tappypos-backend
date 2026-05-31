package com.tappy.pos.repository.order;

import com.tappy.pos.model.entity.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status NOT IN ('CANCELLED','VOIDED') AND EXTRACT(YEAR FROM created_at) = :year AND EXTRACT(MONTH FROM created_at) = :month",
           nativeQuery = true)
    long countOrdersThisMonth(@Param("year") int year, @Param("month") int month);

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

    // ── Revenue aggregation (SELL orders only — BUY/EXCHANGE excluded from revenue) ──

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL'",
           nativeQuery = true)
    BigDecimal sumTotalRevenue();

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year AND EXTRACT(MONTH FROM completed_at) = :month",
           nativeQuery = true)
    BigDecimal sumRevenueByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year",
           nativeQuery = true)
    BigDecimal sumRevenueByYear(@Param("year") int year);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND completed_at >= :from AND completed_at <= :to",
           nativeQuery = true)
    BigDecimal sumRevenueByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query(value = "SELECT COALESCE(SUM(tax_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL'",
           nativeQuery = true)
    BigDecimal sumTotalTax();

    @Query(value = "SELECT COALESCE(SUM(discount_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL'",
           nativeQuery = true)
    BigDecimal sumTotalDiscount();

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL'",
           nativeQuery = true)
    Long countCompleted();

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year AND EXTRACT(MONTH FROM completed_at) = :month",
           nativeQuery = true)
    Long countCompletedByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year",
           nativeQuery = true)
    Long countCompletedByYear(@Param("year") int year);

    // Monthly breakdown: [month, revenue, orderCount]
    @Query(value = "SELECT EXTRACT(MONTH FROM completed_at), COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year GROUP BY EXTRACT(MONTH FROM completed_at) ORDER BY EXTRACT(MONTH FROM completed_at)",
           nativeQuery = true)
    List<Object[]> sumRevenueGroupedByMonth(@Param("year") int year);

    // Daily breakdown: [day, revenue, orderCount]
    @Query(value = "SELECT EXTRACT(DAY FROM completed_at), COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND EXTRACT(YEAR FROM completed_at) = :year AND EXTRACT(MONTH FROM completed_at) = :month GROUP BY EXTRACT(DAY FROM completed_at) ORDER BY EXTRACT(DAY FROM completed_at)",
           nativeQuery = true)
    List<Object[]> sumRevenueGroupedByDay(@Param("year") int year, @Param("month") int month);

    // Payment method breakdown: [paymentMethod, count, totalAmount]
    @Query(value = "SELECT payment_method, COUNT(*), COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM completed_at) = CAST(:year AS integer)) AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM completed_at) = CAST(:month AS integer)) GROUP BY payment_method",
           nativeQuery = true)
    List<Object[]> groupByPaymentMethod(@Param("year") Integer year, @Param("month") Integer month);

    // Day-of-week breakdown: [dayOfWeek(0=Sun..6=Sat), revenue, orderCount]
    @Query(value = "SELECT EXTRACT(DOW FROM completed_at), COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM completed_at) = CAST(:year AS integer)) AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM completed_at) = CAST(:month AS integer)) GROUP BY EXTRACT(DOW FROM completed_at) ORDER BY EXTRACT(DOW FROM completed_at)",
           nativeQuery = true)
    List<Object[]> sumRevenueGroupedByDayOfWeek(@Param("year") Integer year, @Param("month") Integer month);

    // Hourly breakdown: [hour(0-23), revenue, orderCount]
    @Query(value = "SELECT EXTRACT(HOUR FROM completed_at), COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM completed_at) = CAST(:year AS integer)) AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM completed_at) = CAST(:month AS integer)) GROUP BY EXTRACT(HOUR FROM completed_at) ORDER BY EXTRACT(HOUR FROM completed_at)",
           nativeQuery = true)
    List<Object[]> sumRevenueGroupedByHour(@Param("year") Integer year, @Param("month") Integer month);

    // Category breakdown: [categoryName, orderCount, revenue] — STANDARD items only
    @Query(value = """
            SELECT COALESCE(c.name, 'Không phân loại') AS categoryName,
                   COUNT(DISTINCT o.id) AS orderCount,
                   COALESCE(SUM(oi.amount), 0) AS revenue
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id AND oi.item_type = 'STANDARD'
            JOIN product p ON p.id = oi.product_id
            LEFT JOIN product_category pc ON pc.product_id = p.id
            LEFT JOIN category c ON c.id = pc.category_id
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM o.completed_at) = CAST(:year AS integer))
              AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM o.completed_at) = CAST(:month AS integer))
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
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM o.completed_at) = CAST(:year AS integer))
              AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM o.completed_at) = CAST(:month AS integer))
            GROUP BY COALESCE(e.full_name, o.created_by), o.created_by
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> sumRevenueGroupedByEmployee(@Param("year") Integer year, @Param("month") Integer month);

    // Recent completed orders for dashboard (all types)
    @Query(value = "SELECT * FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' ORDER BY completed_at DESC",
           nativeQuery = true)
    List<Order> findRecentCompleted(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = :#{#status.name()}",
           nativeQuery = true)
    long countByDeletedFalseAndStatus(@Param("status") Order.OrderStatus status);

    // Total distinct customers who made SELL purchases
    @Query(value = "SELECT COUNT(DISTINCT customer_id) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' AND customer_id IS NOT NULL",
           nativeQuery = true)
    Long countDistinctCustomers();

    // ── Order type filters (global) ────────────────────────────────────────────

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.orderType = :orderType ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByOrderType(@Param("orderType") Order.OrderType orderType, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status AND o.orderType = :orderType ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByStatusAndOrderType(@Param("status") Order.OrderStatus status, @Param("orderType") Order.OrderType orderType, Pageable pageable);

    // ── Scoped list (ORDER_VIEW_ALL absent — show only own orders) ─────────────

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.createdBy = :username ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByCreatedBy(@Param("username") String username, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status AND o.createdBy = :username ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByStatusAndCreatedBy(@Param("status") Order.OrderStatus status, @Param("username") String username, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.orderType = :orderType AND o.createdBy = :username ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByOrderTypeAndCreatedBy(@Param("orderType") Order.OrderType orderType, @Param("username") String username, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status AND o.orderType = :orderType AND o.createdBy = :username ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveByStatusAndOrderTypeAndCreatedBy(@Param("status") Order.OrderStatus status, @Param("orderType") Order.OrderType orderType, @Param("username") String username, Pageable pageable);

    @Query("""
            SELECT o FROM Order o LEFT JOIN o.customer c
            WHERE o.deleted = false
              AND o.createdBy = :username
              AND (LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> searchByKeywordAndCreatedBy(@Param("keyword") String keyword, @Param("username") String username, Pageable pageable);

    // ── My Work ────────────────────────────────────────────────────────────────

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status IN :statuses ORDER BY o.createdAt ASC")
    Page<Order> findActiveByCreatedBy(@Param("username") String username, @Param("statuses") Collection<Order.OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt < :to ORDER BY o.completedAt DESC")
    Page<Order> findCompletedByCreatedByAndPeriod(@Param("username") String username, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status = 'COMPLETED' AND o.completedAt >= :from AND o.completedAt < :to")
    List<Object[]> getMyCompletedStats(@Param("username") String username, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.createdBy = :username AND o.status IN :statuses")
    Long countActiveByCreatedBy(@Param("username") String username, @Param("statuses") Collection<Order.OrderStatus> statuses);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND created_at >= :from AND created_at <= :to",
           nativeQuery = true)
    long countByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = :#{#status.name()} AND created_at >= :from AND created_at <= :to",
           nativeQuery = true)
    long countByDateRangeAndStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("status") Order.OrderStatus status);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('hour', completed_at), 'HH24:00') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label",
           nativeQuery = true)
    List<Object[]> getHourlyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('day', completed_at), 'YYYY-MM-DD') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label",
           nativeQuery = true)
    List<Object[]> getDailyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', completed_at), 'YYYY-MM-DD') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label",
           nativeQuery = true)
    List<Object[]> getWeeklyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', completed_at), 'YYYY-MM') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label",
           nativeQuery = true)
    List<Object[]> getMonthlyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('year', completed_at), 'YYYY') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label",
           nativeQuery = true)
    List<Object[]> getYearlyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT oi.product_name, MIN(oi.product_id) as productId, COALESCE(SUM(oi.quantity),0) as cnt, COALESCE(SUM(oi.amount),0) as rev " +
           "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' AND o.created_at >= :since " +
           "GROUP BY oi.product_name ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> getTopProductsSince(@Param("since") LocalDateTime since, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT oi.product_name, MIN(oi.product_id) as productId, COALESCE(SUM(oi.quantity),0) as cnt, COALESCE(SUM(oi.amount),0) as rev " +
           "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' AND o.created_at BETWEEN :from AND :to " +
           "GROUP BY oi.product_name ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> getTopProductsByRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c.name, COUNT(o.id) as orderCount, COALESCE(SUM(o.total_amount),0) as totalSpend, " +
           "CAST(c.id AS VARCHAR) as customerId " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' AND o.customer_id IS NOT NULL " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "AND o.created_at BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name ORDER BY totalSpend DESC",
           nativeQuery = true)
    List<Object[]> getTopCustomersByRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, org.springframework.data.domain.Pageable pageable);

    // Top customers sorted by order count (frequency) instead of spend
    @Query(value = "SELECT c.name, COUNT(o.id) as orderCount, COALESCE(SUM(o.total_amount),0) as totalSpend, " +
           "CAST(c.id AS VARCHAR) as customerId " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' AND o.customer_id IS NOT NULL " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "AND o.created_at BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name ORDER BY orderCount DESC",
           nativeQuery = true)
    List<Object[]> getTopCustomersByFrequency(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, org.springframework.data.domain.Pageable pageable);

    // Count customers making their FIRST EVER order in this period
    @Query(value = "SELECT COUNT(DISTINCT o.customer_id) " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "AND o.created_at BETWEEN :from AND :to " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM orders o2 " +
           "  WHERE o2.customer_id = o.customer_id " +
           "  AND o2.tenant_id = current_setting('app.current_tenant', true) " +
           "  AND o2.deleted = false " +
           "  AND o2.created_at < :from" +
           ")",
           nativeQuery = true)
    long countNewCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COALESCE(e.full_name, o.created_by) as name, u.id as userId, COUNT(o.id) as orderCount, COALESCE(SUM(o.total_amount),0) as revenue " +
           "FROM orders o " +
           "LEFT JOIN users u ON u.username = o.created_by " +
           "LEFT JOIN employees e ON e.user_id = u.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' AND o.created_by IS NOT NULL " +
           "AND o.created_at BETWEEN :from AND :to " +
           "GROUP BY COALESCE(e.full_name, o.created_by), o.created_by, u.id ORDER BY revenue DESC",
           nativeQuery = true)
    List<Object[]> getTopEmployeesByRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, org.springframework.data.domain.Pageable pageable);

    // ── Customer list analytics ───────────────────────────────────────────────

    @Query(value = "SELECT COUNT(DISTINCT o.customer_id) " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "AND o.created_at BETWEEN :from AND :to",
           nativeQuery = true)
    long countActiveCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COALESCE(SUM(o.total_amount), 0) " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "AND o.created_at BETWEEN :from AND :to",
           nativeQuery = true)
    java.math.BigDecimal getTotalRevenueFromNamedCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT c.name, COUNT(o.id) as orderCount, COALESCE(SUM(o.total_amount),0) as totalSpend, " +
           "CAST(c.id AS VARCHAR) as customerId " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "GROUP BY c.id, c.name ORDER BY totalSpend DESC",
           nativeQuery = true)
    List<Object[]> getTopCustomersAllTime(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c.name, COUNT(o.id) as orderCount, COALESCE(SUM(o.total_amount),0) as totalSpend, " +
           "CAST(c.id AS VARCHAR) as customerId " +
           "FROM orders o JOIN customers c ON o.customer_id = c.id " +
           "WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true) AND o.status = 'COMPLETED' " +
           "AND c.deleted = false AND c.phone != '0000000000' " +
           "GROUP BY c.id, c.name ORDER BY orderCount DESC",
           nativeQuery = true)
    List<Object[]> getTopCustomersAllTimeByFrequency(org.springframework.data.domain.Pageable pageable);

    // ── Customer-scoped analytics ──────────────────────────────────────────────

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId " +
           "AND completed_at BETWEEN :from AND :to", nativeQuery = true)
    BigDecimal sumRevenueByCustomerAndDateRange(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND customer_id = :customerId " +
           "AND created_at BETWEEN :from AND :to", nativeQuery = true)
    long countByCustomerAndDateRange(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND customer_id = :customerId " +
           "AND status = :status AND created_at BETWEEN :from AND :to", nativeQuery = true)
    long countByCustomerAndDateRangeAndStatus(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('day', completed_at), 'YYYY-MM-DD') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getDailyRevenueByCustomer(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', completed_at), 'YYYY-MM-DD') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getWeeklyRevenueByCustomer(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', completed_at), 'YYYY-MM') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByCustomer(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('year', completed_at), 'YYYY') as label, COALESCE(SUM(total_amount),0) as value " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId " +
           "AND completed_at BETWEEN :from AND :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getYearlyRevenueByCustomer(
            @Param("customerId") Long customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Most recent completed_at for a customer, or NULL if none. */
    @Query(value = "SELECT MAX(completed_at) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND customer_id = :customerId",
           nativeQuery = true)
    java.time.LocalDateTime findLastVisitDateByCustomer(@Param("customerId") Long customerId);

    // ── By createdBy (staff performance view) ─────────────────────────────────

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('day', completed_at), 'YYYY-MM-DD'), COALESCE(SUM(total_amount),0) " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND created_by = :createdBy " +
           "AND completed_at BETWEEN :from AND :to GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> getDailyRevenueByCreatedBy(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', completed_at), 'YYYY-MM-DD'), COALESCE(SUM(total_amount),0) " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND created_by = :createdBy " +
           "AND completed_at BETWEEN :from AND :to GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> getWeeklyRevenueByCreatedBy(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', completed_at), 'YYYY-MM'), COALESCE(SUM(total_amount),0) " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND created_by = :createdBy " +
           "AND completed_at BETWEEN :from AND :to GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByCreatedBy(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('year', completed_at), 'YYYY'), COALESCE(SUM(total_amount),0) " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND created_by = :createdBy " +
           "AND completed_at BETWEEN :from AND :to GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> getYearlyRevenueByCreatedBy(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND created_by = :createdBy AND completed_at BETWEEN :from AND :to", nativeQuery = true)
    BigDecimal sumRevenueByCreatedBy(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND created_by = :createdBy AND created_at BETWEEN :from AND :to", nativeQuery = true)
    long countByCreatedByAndDateRange(@Param("createdBy") String createdBy, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND created_by = :createdBy AND status = :status AND created_at BETWEEN :from AND :to", nativeQuery = true)
    long countByCreatedByAndDateRangeAndStatus(@Param("createdBy") String createdBy, @Param("status") String status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ── Filtered list (Report screen) ──────────────────────────────────────────

    @Query(value = """
            SELECT * FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            """,
           nativeQuery = true)
    Page<Order> findAllFiltered(
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND created_by = :username
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND created_by = :username
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:paymentMethod AS text) IS NULL OR payment_method = CAST(:paymentMethod AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            """,
           nativeQuery = true)
    Page<Order> findAllFilteredByUser(
            @Param("username") String username,
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // Staff performance list — always filters by createdBy, status optional
    @Query(value = """
            SELECT * FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND created_by = :createdBy
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM orders
            WHERE deleted = FALSE
            AND tenant_id = current_setting('app.current_tenant', true)
            AND created_by = :createdBy
            AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))
            AND (CAST(:from AS date) IS NULL OR DATE(created_at) >= CAST(:from AS date))
            AND (CAST(:to AS date)   IS NULL OR DATE(created_at) <= CAST(:to AS date))
            """,
           nativeQuery = true)
    Page<Order> findAllByCreatedBy(
            @Param("createdBy") String createdBy,
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // ── Kitchen Display ────────────────────────────────────────────────────────

    /** All PENDING + IN_PROGRESS orders for the kitchen display, oldest first. */
    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status IN ('PENDING', 'IN_PROGRESS') ORDER BY o.createdAt ASC")
    List<Order> findAllKitchenOrders();

    // Payment method breakdown by date range: [paymentMethod, count, totalAmount]
    @Query(value = "SELECT payment_method, COUNT(*), COALESCE(SUM(total_amount), 0) " +
           "FROM orders WHERE deleted = false AND tenant_id = current_setting('app.current_tenant', true) AND status = 'COMPLETED' AND order_type = 'SELL' " +
           "AND (CAST(:from AS date) IS NULL OR DATE(completed_at) >= CAST(:from AS date)) " +
           "AND (CAST(:to   AS date) IS NULL OR DATE(completed_at) <= CAST(:to   AS date)) " +
           "GROUP BY payment_method",
           nativeQuery = true)
    List<Object[]> groupByPaymentMethodDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Category breakdown by date range: [categoryName, orderCount, revenue] — STANDARD items only
    @Query(value = """
            SELECT COALESCE(c.name, 'Không phân loại') AS categoryName,
                   COUNT(DISTINCT o.id) AS orderCount,
                   COALESCE(SUM(oi.amount), 0) AS revenue
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id AND oi.item_type = 'STANDARD'
            JOIN product p ON p.id = oi.product_id
            LEFT JOIN product_category pc ON pc.product_id = p.id
            LEFT JOIN category c ON c.id = pc.category_id
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND (CAST(:from AS date) IS NULL OR DATE(o.completed_at) >= CAST(:from AS date))
              AND (CAST(:to   AS date) IS NULL OR DATE(o.completed_at) <= CAST(:to   AS date))
            GROUP BY COALESCE(c.name, 'Không phân loại')
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> sumRevenueGroupedByCategoryDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ── Employee analytics — revenue trend by date range ──────────────────────

    /**
     * Count of distinct order creators (active employees) in a date window.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT o.created_by)
            FROM orders o
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND o.completed_at >= :from AND o.completed_at < :to
            """, nativeQuery = true)
    Long countActiveEmployees(
            @Param("from") java.time.LocalDateTime from,
            @Param("to")   java.time.LocalDateTime to);

    /**
     * Revenue ranking by order creator in a date range.
     * [employeeName, userId, orderCount, revenue]
     */
    @Query(value = """
            SELECT COALESCE(e.full_name, o.created_by)              AS employee_name,
                   u.id                                              AS user_id,
                   COUNT(o.id)                                       AS order_count,
                   COALESCE(SUM(o.total_amount), 0)                 AS revenue
            FROM orders o
            LEFT JOIN users u ON u.username = o.created_by
            LEFT JOIN employees e ON e.user_id = u.id
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND o.completed_at >= :from AND o.completed_at < :to
            GROUP BY COALESCE(e.full_name, o.created_by), o.created_by, u.id
            ORDER BY revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    java.util.List<Object[]> getEmployeeRevenueRankingByDateRange(
            @Param("from")  java.time.LocalDateTime from,
            @Param("to")    java.time.LocalDateTime to,
            @Param("limit") int limit);

    /**
     * Daily revenue trend: [date, revenue]
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', o.completed_at)::DATE AS lbl,
                   COALESCE(SUM(o.total_amount), 0)         AS revenue
            FROM orders o
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND o.completed_at >= :from AND o.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    java.util.List<Object[]> getEmployeeRevenueTrendByDay(
            @Param("from") java.time.LocalDateTime from,
            @Param("to")   java.time.LocalDateTime to);

    /**
     * Weekly revenue trend: [week_start, revenue]
     */
    @Query(value = """
            SELECT DATE_TRUNC('week', o.completed_at)::DATE AS lbl,
                   COALESCE(SUM(o.total_amount), 0)          AS revenue
            FROM orders o
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND o.completed_at >= :from AND o.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    java.util.List<Object[]> getEmployeeRevenueTrendByWeek(
            @Param("from") java.time.LocalDateTime from,
            @Param("to")   java.time.LocalDateTime to);

    /**
     * Monthly revenue trend: [month_start, revenue]
     */
    @Query(value = """
            SELECT DATE_TRUNC('month', o.completed_at)::DATE AS lbl,
                   COALESCE(SUM(o.total_amount), 0)           AS revenue
            FROM orders o
            WHERE o.deleted = false AND o.tenant_id = current_setting('app.current_tenant', true)
              AND o.status = 'COMPLETED' AND o.order_type = 'SELL'
              AND o.completed_at >= :from AND o.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    java.util.List<Object[]> getEmployeeRevenueTrendByMonth(
            @Param("from") java.time.LocalDateTime from,
            @Param("to")   java.time.LocalDateTime to);
}
