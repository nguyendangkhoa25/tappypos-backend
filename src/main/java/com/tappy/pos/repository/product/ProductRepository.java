package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySkuAndDeletedFalse(String sku);
    Optional<Product> findByBarcodeAndDeletedFalse(String barcode);
    Optional<Product> findByIdAndDeletedFalse(Long id);
    Page<Product> findByDeletedFalseAndStatusOrderByCreatedAtDesc(Product.ProductStatus status, Pageable pageable);
    Page<Product> findByProductTypeIdAndDeletedFalseOrderByCreatedAtDesc(Long productTypeId, Pageable pageable);

    // Public QR menu: all active products for the tenant (RLS-scoped), eager-fetch categories.
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.categories WHERE p.deleted = false AND p.status = :status")
    List<Product> findActiveWithCategories(@Param("status") Product.ProductStatus status);
    
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
           "(LOWER(p.name) LIKE %:searchTerm% OR LOWER(p.sku) LIKE %:searchTerm%)")
    Page<Product> searchByKeyword(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.categories c WHERE p.deleted = false AND c.id = :categoryId AND " +
           "(LOWER(p.name) LIKE %:searchTerm% OR LOWER(p.sku) LIKE %:searchTerm%)")
    Page<Product> searchByKeywordAndCategoryId(@Param("searchTerm") String searchTerm,
                                               @Param("categoryId") Long categoryId,
                                               Pageable pageable);

    /**
     * Keyword search with optional category, product-type, and pawn-origin filters.
     * Powers the POS search box when a pawn shop has a product-type chip and/or the
     * "pawn items only" toggle active. Each filter is skipped when its argument is null/false.
     */
    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
           "AND (LOWER(p.name) LIKE %:searchTerm% OR LOWER(p.sku) LIKE %:searchTerm%) " +
           "AND (:productTypeId IS NULL OR p.productType.id = :productTypeId) " +
           "AND (:pawnOriginOnly = FALSE OR p.sourcePawnId IS NOT NULL) " +
           "AND (:categoryId IS NULL OR EXISTS (SELECT 1 FROM p.categories c WHERE c.id = :categoryId))")
    Page<Product> searchByKeywordFiltered(@Param("searchTerm") String searchTerm,
                                          @Param("categoryId") Long categoryId,
                                          @Param("productTypeId") Long productTypeId,
                                          @Param("pawnOriginOnly") boolean pawnOriginOnly,
                                          Pageable pageable);

    @Query("SELECT p.sku FROM Product p WHERE p.deleted = false AND p.sku LIKE :prefix%")
    List<String> findSkusByPrefix(@Param("prefix") String prefix);

    /** Every existing barcode that starts with the given numeric prefix (tenant-scoped via RLS).
     *  Used to derive the next sequence for auto-generated internal EAN-13 barcodes. */
    @Query("SELECT p.barcode FROM Product p WHERE p.barcode LIKE :prefix%")
    List<String> findBarcodesByPrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM Product p JOIN p.categories c WHERE p.deleted = false AND p.status = :status AND c.id = :categoryId ORDER BY p.createdAt DESC")
    Page<Product> findByStatusAndCategoryId(@Param("status") Product.ProductStatus status, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false AND p.status = 'ACTIVE'")
    long countActive();

    Optional<Product> findBySourcePawnIdAndDeletedFalse(Long sourcePawnId);

    Page<Product> findByDeletedFalseAndStatusAndSourcePawnIdIsNotNullOrderByCreatedAtDesc(
            Product.ProductStatus status, Pageable pageable);

    Page<Product> findByDeletedFalseAndStatusAndProductTypeIdOrderByCreatedAtDesc(
            Product.ProductStatus status, Long productTypeId, Pageable pageable);

    Page<Product> findByDeletedFalseAndStatusAndProductTypeIdAndSourcePawnIdIsNotNullOrderByCreatedAtDesc(
            Product.ProductStatus status, Long productTypeId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.categories c " +
           "WHERE p.deleted = false AND p.status = :status AND c.id = :categoryId " +
           "AND p.sourcePawnId IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Product> findByStatusAndCategoryIdAndSourcePawnIdIsNotNull(
            @Param("status") Product.ProductStatus status,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}

