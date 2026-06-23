package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    List<ProductAttributeValue> findByProductId(Long productId);
    Optional<ProductAttributeValue> findByProductIdAndAttributeId(Long productId, Long attributeId);
    void deleteByProductId(Long productId);

    /**
     * Batch lookup of which products (within the given set) are prescription-required drugs —
     * i.e. carry a truthy `prescription_required` BOOLEAN attribute. One query per POS page, so no
     * per-row N+1; returns empty for non-pharmacy shops (the attribute does not exist there).
     */
    @Query("SELECT pav.product.id FROM ProductAttributeValue pav " +
           "WHERE pav.product.id IN :productIds " +
           "AND pav.attribute.code = 'prescription_required' " +
           "AND pav.valueBoolean = true")
    List<Long> findPrescriptionRequiredProductIds(@Param("productIds") Collection<Long> productIds);
}

