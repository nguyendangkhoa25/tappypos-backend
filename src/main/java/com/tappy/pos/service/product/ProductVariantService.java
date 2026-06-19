package com.tappy.pos.service.product;

import com.tappy.pos.model.dto.product.BulkVariantUpdateRequest;
import com.tappy.pos.model.dto.product.GenerateVariantsRequest;
import com.tappy.pos.model.dto.product.ProductVariantDTO;
import com.tappy.pos.model.dto.product.SaveProductVariantRequest;

import java.util.List;

public interface ProductVariantService {

    List<ProductVariantDTO> getVariants(Long productId);

    /** Bulk-set price/cost overrides and absolute stock across many SKUs of one product (matrix editor). */
    List<ProductVariantDTO> bulkUpdate(Long productId, BulkVariantUpdateRequest req);

    ProductVariantDTO createVariant(Long productId, SaveProductVariantRequest req);

    ProductVariantDTO updateVariant(Long productId, Long variantId, SaveProductVariantRequest req);

    void deleteVariant(Long productId, Long variantId);

    List<ProductVariantDTO> generateVariants(Long productId, GenerateVariantsRequest req);
}
