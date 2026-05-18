package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.ShopExpense;
import com.tappy.pos.model.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShopExpenseRepository extends JpaRepository<ShopExpense, Long> {

    // CAST(:from AS date) tells PostgreSQL the type even when the value is NULL
    @Query(value = "SELECT * FROM shop_expense WHERE deleted = FALSE " +
           "AND (CAST(:from AS date) IS NULL OR expense_date >= CAST(:from AS date)) " +
           "AND (CAST(:to AS date)   IS NULL OR expense_date <= CAST(:to AS date)) " +
           "AND (:category IS NULL OR category = :category) " +
           "ORDER BY expense_date DESC, id DESC",
           countQuery = "SELECT COUNT(*) FROM shop_expense WHERE deleted = FALSE " +
           "AND (CAST(:from AS date) IS NULL OR expense_date >= CAST(:from AS date)) " +
           "AND (CAST(:to AS date)   IS NULL OR expense_date <= CAST(:to AS date)) " +
           "AND (:category IS NULL OR category = :category)",
           nativeQuery = true)
    Page<ShopExpense> search(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("category") String category,
            Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM shop_expense " +
           "WHERE deleted = FALSE " +
           "AND EXTRACT(YEAR FROM expense_date) = :year " +
           "AND EXTRACT(MONTH FROM expense_date) = :month",
           nativeQuery = true)
    BigDecimal sumByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM shop_expense " +
           "WHERE deleted = FALSE AND EXTRACT(YEAR FROM expense_date) = :year",
           nativeQuery = true)
    BigDecimal sumByYear(@Param("year") int year);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM shop_expense WHERE deleted = FALSE",
           nativeQuery = true)
    BigDecimal sumAll();

    @Query(value = "SELECT EXTRACT(MONTH FROM expense_date), COALESCE(SUM(amount), 0) " +
           "FROM shop_expense WHERE deleted = FALSE AND EXTRACT(YEAR FROM expense_date) = :year " +
           "GROUP BY EXTRACT(MONTH FROM expense_date)",
           nativeQuery = true)
    List<Object[]> sumGroupedByMonth(@Param("year") int year);

    @Query(value = "SELECT EXTRACT(DAY FROM expense_date), COALESCE(SUM(amount), 0) " +
           "FROM shop_expense WHERE deleted = FALSE " +
           "AND EXTRACT(YEAR FROM expense_date) = :year " +
           "AND EXTRACT(MONTH FROM expense_date) = :month " +
           "GROUP BY EXTRACT(DAY FROM expense_date)",
           nativeQuery = true)
    List<Object[]> sumGroupedByDay(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT category, COALESCE(SUM(amount), 0) " +
           "FROM shop_expense WHERE deleted = FALSE " +
           "AND (:year IS NULL OR EXTRACT(YEAR FROM expense_date) = :year) " +
           "AND (:month IS NULL OR EXTRACT(MONTH FROM expense_date) = :month) " +
           "GROUP BY category ORDER BY SUM(amount) DESC",
           nativeQuery = true)
    List<Object[]> sumGroupedByCategory(@Param("year") Integer year, @Param("month") Integer month);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM shop_expense WHERE deleted = FALSE AND expense_date >= :from AND expense_date <= :to", nativeQuery = true)
    java.math.BigDecimal sumByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY-MM-DD') as label, COALESCE(SUM(amount),0) as value FROM shop_expense WHERE deleted = FALSE AND expense_date >= :from AND expense_date <= :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getDailyChart(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', expense_date), 'YYYY-MM-DD') as label, COALESCE(SUM(amount),0) as value FROM shop_expense WHERE deleted = FALSE AND expense_date >= :from AND expense_date <= :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getWeeklyChart(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY-MM') as label, COALESCE(SUM(amount),0) as value FROM shop_expense WHERE deleted = FALSE AND expense_date >= :from AND expense_date <= :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getMonthlyChart(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY') as label, COALESCE(SUM(amount),0) as value FROM shop_expense WHERE deleted = FALSE AND expense_date >= :from AND expense_date <= :to GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getYearlyChart(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM shop_expense WHERE deleted = FALSE " +
           "AND expense_date >= :from AND expense_date <= :to AND category IN (:categories)",
           nativeQuery = true)
    BigDecimal sumByDateRangeAndCategories(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("categories") List<String> categories);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM shop_expense WHERE deleted = FALSE " +
           "AND description = :desc AND expense_date >= :from AND expense_date <= :to)",
           nativeQuery = true)
    boolean existsByDescriptionAndDateRange(
            @Param("desc") String description,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
