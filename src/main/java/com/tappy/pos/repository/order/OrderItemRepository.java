package com.tappy.pos.repository.order;

import com.tappy.pos.model.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ── Salary / Commission queries ───────────────────────────────────────────

    @Query(value = """
            SELECT COALESCE(SUM(oi.commission_amount), 0)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.is_salary_calculated = false
              AND oi.included_in_salary_id IS NULL
              AND o.deleted = false
              AND o.status = 'COMPLETED'
              AND EXTRACT(YEAR  FROM o.completed_at) = :year
              AND EXTRACT(MONTH FROM o.completed_at) = :month
            """, nativeQuery = true)
    BigDecimal sumPendingCommissionByEmployeeAndMonth(
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year);

    @Modifying
    @Query(value = """
            UPDATE order_items oi
            SET included_in_salary_id = :salaryId
            FROM orders o
            WHERE oi.order_id = o.id
              AND oi.assigned_employee_id = :employeeId
              AND oi.is_salary_calculated = false
              AND oi.included_in_salary_id IS NULL
              AND o.deleted = false
              AND o.status = 'COMPLETED'
              AND EXTRACT(YEAR  FROM o.completed_at) = :year
              AND EXTRACT(MONTH FROM o.completed_at) = :month
            """, nativeQuery = true)
    int linkItemsToSalary(
            @Param("salaryId") Long salaryId,
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year);

    @Modifying
    @Query(value = "UPDATE order_items SET is_salary_calculated = true WHERE included_in_salary_id = :salaryId",
           nativeQuery = true)
    int markSalaryCalculated(@Param("salaryId") Long salaryId);

    @Modifying
    @Query(value = "UPDATE order_items SET included_in_salary_id = NULL WHERE included_in_salary_id = :salaryId",
           nativeQuery = true)
    int unlinkFromSalary(@Param("salaryId") Long salaryId);

    @Query(value = """
            SELECT oi.id, o.order_number, oi.product_name, oi.quantity,
                   oi.amount, oi.commission_rate, oi.commission_amount, o.completed_at
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.included_in_salary_id = :salaryId
            ORDER BY o.completed_at
            """, nativeQuery = true)
    List<Object[]> findCommissionItemsBySalaryId(@Param("salaryId") Long salaryId);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE o.deleted = false AND o.status = 'COMPLETED'")
    Long sumTotalItemsSold();

    // ── Item-level work queue (MY_WORK feature) ───────────────────────────────

    @Query(value = """
            SELECT oi.id, o.id AS order_id, o.order_number, c.name AS customer_name,
                   oi.product_id, oi.product_name, oi.quantity, oi.unit_price, oi.amount,
                   COALESCE(p.duration_minutes, 0) AS duration_minutes,
                   oi.status, oi.completed_at, oi.assigned_employee_id, oi.assigned_employee_name,
                   o.created_at AS order_created_at
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN customers c ON c.id = o.customer_id
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE oi.assigned_employee_id = :employeeId
              AND o.deleted = false
            ORDER BY o.created_at ASC
            """, nativeQuery = true,
         countQuery = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.assigned_employee_id = :employeeId AND o.deleted = false
            """)
    org.springframework.data.domain.Page<Object[]> findWorkItemsByEmployeeId(
            @Param("employeeId") Long employeeId,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT oi.id, o.id AS order_id, o.order_number, c.name AS customer_name,
                   oi.product_id, oi.product_name, oi.quantity, oi.unit_price, oi.amount,
                   COALESCE(p.duration_minutes, 0) AS duration_minutes,
                   oi.status, oi.completed_at, oi.assigned_employee_id, oi.assigned_employee_name,
                   o.created_at AS order_created_at
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN customers c ON c.id = o.customer_id
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status IN ('PENDING', 'IN_PROGRESS')
              AND o.deleted = false
            ORDER BY o.created_at ASC
            """, nativeQuery = true,
         countQuery = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status IN ('PENDING', 'IN_PROGRESS') AND o.deleted = false
            """)
    org.springframework.data.domain.Page<Object[]> findPendingWorkItemsByEmployeeId(
            @Param("employeeId") Long employeeId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :itemId AND oi.assignedEmployeeId = :employeeId")
    java.util.Optional<OrderItem> findByIdAndAssignedEmployeeId(
            @Param("itemId") Long itemId,
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT oi.id, o.id AS order_id, o.order_number, c.name AS customer_name,
                   oi.product_id, oi.product_name, oi.quantity, oi.unit_price, oi.amount,
                   COALESCE(p.duration_minutes, 0) AS duration_minutes,
                   oi.status, oi.completed_at, oi.assigned_employee_id, oi.assigned_employee_name,
                   o.created_at AS order_created_at
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN customers c ON c.id = o.customer_id
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE oi.assigned_employee_id IS NULL
              AND oi.status = 'PENDING'
              AND o.deleted = false
              AND o.status NOT IN ('CANCELLED', 'COMPLETED', 'VOIDED')
            ORDER BY o.created_at ASC
            """, nativeQuery = true,
         countQuery = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.assigned_employee_id IS NULL
              AND oi.status = 'PENDING'
              AND o.deleted = false
              AND o.status NOT IN ('CANCELLED', 'COMPLETED', 'VOIDED')
            """)
    org.springframework.data.domain.Page<Object[]> findAvailableWorkItems(
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :itemId AND oi.assignedEmployeeId IS NULL")
    java.util.Optional<OrderItem> findByIdAndAssignedEmployeeIdIsNull(@Param("itemId") Long itemId);

    // ── Completed work items — list, summary, trend ───────────────────────────

    @Query(value = """
            SELECT oi.id, o.id AS order_id, o.order_number, c.name AS customer_name,
                   oi.product_id, oi.product_name, oi.quantity, oi.unit_price, oi.amount,
                   COALESCE(p.duration_minutes, 0) AS duration_minutes,
                   oi.status, oi.completed_at, oi.assigned_employee_id, oi.assigned_employee_name,
                   o.created_at AS order_created_at,
                   COALESCE(oi.commission_rate, 0) AS commission_rate,
                   COALESCE(oi.commission_amount, 0) AS commission_amount
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN customers c ON c.id = o.customer_id
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
              AND (CAST(:keyword AS text) IS NULL
                   OR LOWER(oi.product_name) LIKE '%' || LOWER(CAST(:keyword AS text)) || '%'
                   OR LOWER(o.order_number)  LIKE '%' || LOWER(CAST(:keyword AS text)) || '%'
                   OR LOWER(COALESCE(c.name, '')) LIKE '%' || LOWER(CAST(:keyword AS text)) || '%')
            ORDER BY oi.completed_at DESC
            """, nativeQuery = true,
         countQuery = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN customers c ON c.id = o.customer_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
              AND (CAST(:keyword AS text) IS NULL
                   OR LOWER(oi.product_name) LIKE '%' || LOWER(CAST(:keyword AS text)) || '%'
                   OR LOWER(o.order_number)  LIKE '%' || LOWER(CAST(:keyword AS text)) || '%'
                   OR LOWER(COALESCE(c.name, '')) LIKE '%' || LOWER(CAST(:keyword AS text)) || '%')
            """)
    org.springframework.data.domain.Page<Object[]> findCompletedWorkItems(
            @Param("employeeId") Long employeeId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT COUNT(*),
                   COALESCE(SUM(oi.amount), 0),
                   COALESCE(SUM(COALESCE(p.duration_minutes, 0) * oi.quantity), 0),
                   COALESCE(SUM(oi.commission_amount), 0)
            FROM order_items oi
            LEFT JOIN product p ON p.id = oi.product_id
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
            """, nativeQuery = true)
    List<Object[]> getWorkItemStats(
            @Param("employeeId") Long employeeId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    // label = hour-of-day integer (0..23), used for DAY granularity
    @Query(value = """
            SELECT EXTRACT(HOUR FROM oi.completed_at)::INT AS lbl,
                   COUNT(*),
                   COALESCE(SUM(oi.amount), 0)
            FROM order_items oi
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    List<Object[]> getWorkItemTrendByHour(
            @Param("employeeId") Long employeeId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    // label = date string, used for WEEK / MONTH granularity
    @Query(value = """
            SELECT DATE_TRUNC('day', oi.completed_at)::DATE AS lbl,
                   COUNT(*),
                   COALESCE(SUM(oi.amount), 0)
            FROM order_items oi
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    List<Object[]> getWorkItemTrendByDay(
            @Param("employeeId") Long employeeId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    // label = month integer (1..12), used for YEAR granularity
    @Query(value = """
            SELECT EXTRACT(MONTH FROM oi.completed_at)::INT AS lbl,
                   COUNT(*),
                   COALESCE(SUM(oi.amount), 0)
            FROM order_items oi
            WHERE oi.assigned_employee_id = :employeeId
              AND oi.status = 'COMPLETED'
              AND oi.completed_at >= :from
              AND oi.completed_at < :to
            GROUP BY lbl ORDER BY lbl
            """, nativeQuery = true)
    List<Object[]> getWorkItemTrendByMonth(
            @Param("employeeId") Long employeeId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

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
