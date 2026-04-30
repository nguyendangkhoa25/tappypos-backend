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

    @Query("SELECT e FROM ShopExpense e WHERE e.deleted = false " +
           "AND (:from IS NULL OR e.expenseDate >= :from) " +
           "AND (:to   IS NULL OR e.expenseDate <= :to) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "ORDER BY e.expenseDate DESC, e.id DESC")
    Page<ShopExpense> search(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("category") ExpenseCategory category,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShopExpense e " +
           "WHERE e.deleted = false AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    BigDecimal sumByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShopExpense e " +
           "WHERE e.deleted = false AND YEAR(e.expenseDate) = :year")
    BigDecimal sumByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShopExpense e WHERE e.deleted = false")
    BigDecimal sumAll();

    // Returns [month, total] pairs for a given year
    @Query("SELECT MONTH(e.expenseDate), COALESCE(SUM(e.amount), 0) " +
           "FROM ShopExpense e WHERE e.deleted = false AND YEAR(e.expenseDate) = :year " +
           "GROUP BY MONTH(e.expenseDate)")
    List<Object[]> sumGroupedByMonth(@Param("year") int year);

    // Returns [day, total] pairs for a given year+month
    @Query("SELECT DAY(e.expenseDate), COALESCE(SUM(e.amount), 0) " +
           "FROM ShopExpense e WHERE e.deleted = false " +
           "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
           "GROUP BY DAY(e.expenseDate)")
    List<Object[]> sumGroupedByDay(@Param("year") int year, @Param("month") int month);

    // Returns [category, total] — for expense breakdown chart
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
           "FROM ShopExpense e WHERE e.deleted = false " +
           "AND (:year IS NULL OR YEAR(e.expenseDate) = :year) " +
           "AND (:month IS NULL OR MONTH(e.expenseDate) = :month) " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumGroupedByCategory(@Param("year") Integer year, @Param("month") Integer month);
}
