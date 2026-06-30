package com.tappy.pos.service.product;

import com.tappy.pos.model.dto.product.ProductSuggestionDTO;
import com.tappy.pos.model.dto.product.ProductSuggestionRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductSuggestionService {

    Page<ProductSuggestionDTO> search(String name, String shopType, String productType, int page, int size);

    ProductSuggestionDTO create(ProductSuggestionRequest request);

    ProductSuggestionDTO update(Long id, ProductSuggestionRequest request);

    void delete(Long id);

    List<String> getProductTypeCodes();
}
