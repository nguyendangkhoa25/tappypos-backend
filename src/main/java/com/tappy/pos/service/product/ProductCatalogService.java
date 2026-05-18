package com.tappy.pos.service.product;

import com.tappy.pos.client.OpenFoodFactsClient.OffProduct;
import com.tappy.pos.model.dto.product.ProductCatalogDTO;
import com.tappy.pos.model.dto.product.ProductCatalogSyncResult;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ProductCatalogService {

    ProductCatalogSyncResult syncFromOpenFoodFacts(int maxPages);

    Page<ProductCatalogDTO> search(String query, int page, int size);

    Optional<ProductCatalogDTO> findByBarcode(String barcode);

    long getTotalCount();

    long countBySource(String source);

    /** Persists an OFF product to the local catalog asynchronously (fire-and-forget). */
    void saveFromOffAsync(OffProduct offProduct);
}
