package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.ProductSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSuggestionRepository extends JpaRepository<ProductSuggestion, Long> {

    /**
     * Paginated native search. The query already carries its own {@code ORDER BY},
     * so callers MUST pass an UNSORTED {@link Pageable} (e.g. {@code PageRequest.of(page, size)}).
     * Filtering on the {@code shop_types} TEXT[] column uses {@code = ANY(...)}.
     */
    @Query(value = "SELECT * FROM product_suggestions WHERE " +
            "(:name = '' OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(COALESCE(name_en, '')) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:shopType = '' OR :shopType = ANY(shop_types)) " +
            "AND (:productType = '' OR product_type_code = :productType) " +
            "ORDER BY display_order, name",
            countQuery = "SELECT count(*) FROM product_suggestions WHERE " +
                    "(:name = '' OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
                    "OR LOWER(COALESCE(name_en, '')) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                    "AND (:shopType = '' OR :shopType = ANY(shop_types)) " +
                    "AND (:productType = '' OR product_type_code = :productType)",
            nativeQuery = true)
    Page<ProductSuggestion> search(@Param("name") String name,
                                   @Param("shopType") String shopType,
                                   @Param("productType") String productType,
                                   Pageable pageable);

    @Query(value = "SELECT DISTINCT product_type_code FROM product_suggestions ORDER BY 1",
            nativeQuery = true)
    List<String> findDistinctProductTypeCodes();
}
