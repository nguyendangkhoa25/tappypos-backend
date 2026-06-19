package com.tappy.pos.service.product;

import com.tappy.pos.model.dto.product.BulkVariantUpdateRequest;
import com.tappy.pos.model.dto.product.GenerateVariantsRequest;
import com.tappy.pos.model.dto.product.ProductVariantDTO;
import com.tappy.pos.model.dto.product.SaveProductVariantRequest;

import java.util.List;
import java.util.Optional;

public interface ProductVariantService {

    List<ProductVariantDTO> getVariants(Long productId);

    /** Resolve an active variant by its barcode (POS scan-to-add). Empty if blank or no match. */
    Optional<ProductVariantDTO> findActiveByBarcode(String barcode);

    /** Bulk-set price/cost overrides and absolute stock across many SKUs of one product (matrix editor). */
    List<ProductVariantDTO> bulkUpdate(Long productId, BulkVariantUpdateRequest req);

    ProductVariantDTO createVariant(Long productId, SaveProductVariantRequest req);

    ProductVariantDTO updateVariant(Long productId, Long variantId, SaveProductVariantRequest req);

    void deleteVariant(Long productId, Long variantId);

    List<ProductVariantDTO> generateVariants(Long productId, GenerateVariantsRequest req);
}
