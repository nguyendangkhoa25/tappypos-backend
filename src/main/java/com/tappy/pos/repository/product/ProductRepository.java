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
    
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
           "(LOWER(p.name) LIKE %:searchTerm% OR LOWER(p.sku) LIKE %:searchTerm%)")
    Page<Product> searchByKeyword(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p.sku FROM Product p WHERE p.deleted = false AND p.sku LIKE :prefix%")
    List<String> findSkusByPrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM Product p JOIN p.categories c WHERE p.deleted = false AND p.status = :status AND c.id = :categoryId ORDER BY p.createdAt DESC")
    Page<Product> findByStatusAndCategoryId(@Param("status") Product.ProductStatus status, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false AND p.status = 'ACTIVE'")
    long countActive();
}

