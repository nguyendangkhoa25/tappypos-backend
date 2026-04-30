package com.knp.repository.product;

import com.knp.model.entity.product.AttributeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeGroupRepository extends JpaRepository<AttributeGroup, Long> {
    List<AttributeGroup> findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(Long productTypeId);
}

