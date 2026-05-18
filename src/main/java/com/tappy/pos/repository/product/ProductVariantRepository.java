package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndDeletedAtIsNull(Long productId);

    boolean existsByProductIdAndDeletedAtIsNull(Long productId);

    @Query("SELECT DISTINCT v.product.id FROM ProductVariant v WHERE v.product.id IN :ids AND v.deletedAt IS NULL")
    Set<Long> findProductIdsWithActiveVariants(@Param("ids") Collection<Long> productIds);

    Optional<ProductVariant> findByIdAndProductIdAndDeletedAtIsNull(Long id, Long productId);

    boolean existsBySkuAndDeletedAtIsNull(String sku);
}
