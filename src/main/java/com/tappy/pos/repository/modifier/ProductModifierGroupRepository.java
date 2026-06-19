package com.tappy.pos.repository.modifier;

import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductModifierGroupRepository extends JpaRepository<ProductModifierGroup, Long> {

    List<ProductModifierGroup> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    void deleteByProductId(Long productId);
}
