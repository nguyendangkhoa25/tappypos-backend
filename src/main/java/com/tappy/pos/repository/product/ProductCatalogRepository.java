package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.ProductCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, Long> {
    Optional<ProductCatalog> findByBarcode(String barcode);

    Page<ProductCatalog> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
            String name, String brand, Pageable pageable);

    long countBySource(String source);
}
