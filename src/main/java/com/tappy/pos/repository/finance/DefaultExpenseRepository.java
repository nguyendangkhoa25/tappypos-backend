package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.DefaultExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefaultExpenseRepository extends JpaRepository<DefaultExpense, Long> {

    @Query(value = "SELECT * FROM default_expense WHERE deleted = FALSE ORDER BY display_order ASC, id ASC",
           nativeQuery = true)
    List<DefaultExpense> findAllActive();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM default_expense WHERE deleted = FALSE AND description = :desc)",
           nativeQuery = true)
    boolean existsByDescription(@Param("desc") String description);
}
