package com.knp.repository;

import com.knp.model.entity.AttributeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeGroupRepository extends JpaRepository<AttributeGroup, Long> {
    List<AttributeGroup> findByProductTypeIdAndDeletedFalseOrderByDisplayOrder(Long productTypeId);
}

