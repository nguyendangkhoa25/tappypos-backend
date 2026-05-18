package com.tappy.pos.service.product;

import com.tappy.pos.model.dto.product.*;
import com.tappy.pos.model.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(CreateProductRequest request);
    ProductDTO getProductById(Long id);
    ProductDTO updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    Page<ProductDTO> getAllProducts(String status, Long categoryId, Pageable pageable);
    Page<ProductDTO> getProductsByType(Long productTypeId, Pageable pageable);
    Page<ProductDTO> searchProducts(String searchTerm, Pageable pageable);
    List<ProductTypeDTO> getAllProductTypes();
    ProductTypeWithAttributesDTO getProductTypeWithAttributes(Long productTypeId);
    String generateSku(String name, String typeCode);
    BarcodeLookupResult lookupByBarcode(String barcode);
    void markAsSold(Long productId);
    void setVisibility(Long id, boolean active);
    ProductSummaryDTO getSummary();
}

