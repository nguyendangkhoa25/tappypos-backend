package com.tappy.pos.repository.recipe;

import com.tappy.pos.model.entity.recipe.ProductionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** RLS auto-scopes every query to the current tenant. */
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {

    Optional<ProductionBatch> findByIdAndDeletedFalse(Long id);

    @Query("""
            SELECT b FROM ProductionBatch b
            WHERE b.deleted = false
              AND b.createdAt >= :from AND b.createdAt <= :to
            ORDER BY b.createdAt DESC
            """)
    Page<ProductionBatch> findInRange(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      Pageable pageable);

    @Query("""
            SELECT b FROM ProductionBatch b
            WHERE b.deleted = false AND b.createdAt >= :from AND b.createdAt <= :to
            ORDER BY b.createdAt DESC
            """)
    List<ProductionBatch> findAllInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
