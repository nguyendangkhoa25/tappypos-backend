package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    List<ProductAttributeValue> findByProductId(Long productId);
    Optional<ProductAttributeValue> findByProductIdAndAttributeId(Long productId, Long attributeId);
    void deleteByProductId(Long productId);
}

