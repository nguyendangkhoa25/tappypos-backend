package com.knp.repository.product;

import com.knp.model.entity.product.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    List<AttributeDefinition> findByProductTypeIdAndDeletedFalse(Long productTypeId);
    Optional<AttributeDefinition> findByCodeAndProductTypeId(String code, Long productTypeId);
    List<AttributeDefinition> findByAttributeGroupIdAndDeletedFalse(Long attributeGroupId);
}

