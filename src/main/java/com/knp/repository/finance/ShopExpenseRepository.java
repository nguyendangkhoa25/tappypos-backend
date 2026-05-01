package com.knp.repository.finance;

import com.knp.model.entity.finance.ShopExpense;
import com.knp.model.enums.ExpenseCategory;
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
}
