package com.knp.repository.product;

import com.knp.model.entity.product.VariantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VariantTypeRepository extends JpaRepository<VariantType, Long> {

    @Query("SELECT v FROM VariantType v WHERE v.deleted = false ORDER BY v.sortOrder ASC, v.name ASC")
    List<VariantType> findAllActive();

    @Query("SELECT v FROM VariantType v WHERE v.deleted = false AND (v.productTypeId IS NULL OR v.productTypeId = :productTypeId) ORDER BY v.sortOrder ASC, v.name ASC")
    List<VariantType> findForProductType(@Param("productTypeId") Long productTypeId);

    boolean existsByNameAndDeletedFalse(String name);
}
