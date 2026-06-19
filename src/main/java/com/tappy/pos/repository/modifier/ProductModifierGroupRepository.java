package com.tappy.pos.repository.modifier;

import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProductModifierGroupRepository extends JpaRepository<ProductModifierGroup, Long> {

    List<ProductModifierGroup> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    void deleteByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @Query("SELECT DISTINCT p.productId FROM ProductModifierGroup p WHERE p.productId IN :ids")
    Set<Long> findProductIdsWithModifiers(@Param("ids") Collection<Long> ids);
}
