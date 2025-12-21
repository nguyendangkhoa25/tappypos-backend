package com.barbershop.service;

import com.barbershop.model.dto.ProductDTO;
import com.barbershop.model.entity.Product;
import com.barbershop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductDTO createProduct(ProductDTO productDTO) {
        String currentUser = getCurrentUser();

        Product product = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .priceBeforeTax(productDTO.getPriceBeforeTax())
                .tax(productDTO.getTax() != null ? productDTO.getTax() : BigDecimal.ZERO)
                .price(productDTO.getPrice())
                .durationMinutes(productDTO.getDurationMinutes())
                .commissionRate(productDTO.getCommissionRate() != null ? productDTO.getCommissionRate() : BigDecimal.ZERO)
                .quantity(productDTO.getQuantity() != null ? productDTO.getQuantity() : 0)
                .active(true)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {} by user: {}", savedProduct.getId(), currentUser);
        return mapToDTO(savedProduct);
    }

    public ProductDTO getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToDTO(product);
    }

    public List<ProductDTO> getActiveProducts() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all products with pagination, status filtering, search, and custom sorting
     * @param searchTerm - Optional search term to search in name and description
     * @param status - Optional status filter (true for active, false for inactive, null for all)
     * @param sortBy - Field to sort by (default: "id")
     * @param sortDirection - Sort direction "ASC" or "DESC" (default: "DESC")
     * @param pageable - Pagination information
     * @return Page of ProductDTOs
     */
    public Page<ProductDTO> getAllProductsWithFilters(String searchTerm, Boolean status, String sortBy, String sortDirection, Pageable pageable) {
        log.info("Fetching products with filters - searchTerm: {}, status: {}, sortBy: {}, sortDirection: {}",
                searchTerm, status, sortBy, sortDirection);

        // Create Sort object with custom sorting
        Sort.Direction direction = Sort.Direction.fromString(sortDirection != null && sortDirection.equalsIgnoreCase("ASC") ? "ASC" : "DESC");
        Sort sort = Sort.by(direction, sortBy != null && !sortBy.trim().isEmpty() ? sortBy : "id");

        // Create new Pageable with custom sort
        Pageable pageableWithSort = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Page<Product> productsPage;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Search by text and optionally filter by status
            productsPage = productRepository.searchProducts(searchTerm, status, pageableWithSort);
        } else if (status != null) {
            // Filter by status only
            productsPage = productRepository.findByActive(status, pageableWithSort);
        } else {
            // Get all products with pagination
            productsPage = productRepository.findAll(pageableWithSort);
        }

        log.info("Retrieved {} products from page {} with sort: {} {}",
                productsPage.getContent().size(), pageable.getPageNumber(), sortBy, sortDirection);
        return productsPage.map(this::mapToDTO);
    }

    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        String currentUser = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPriceBeforeTax(productDTO.getPriceBeforeTax());
        product.setTax(productDTO.getTax() != null ? productDTO.getTax() : BigDecimal.ZERO);
        product.setPrice(calculatePrice(productDTO.getPriceBeforeTax(), productDTO.getTax()));
        product.setDurationMinutes(productDTO.getDurationMinutes());
        product.setCommissionRate(productDTO.getCommissionRate() != null ? productDTO.getCommissionRate() : BigDecimal.ZERO);
        product.setQuantity(productDTO.getQuantity() != null ? productDTO.getQuantity() : 0);
        product.setUpdatedBy(currentUser);

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product: {} by user: {}", productId, currentUser);
        return mapToDTO(updatedProduct);
    }

    public ProductDTO deactivateProduct(Long productId) {
        String currentUser = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setActive(false);
        product.setUpdatedBy(currentUser);
        Product deactivatedProduct = productRepository.save(product);
        log.info("Deactivated product: {} by user: {}", productId, currentUser);
        return mapToDTO(deactivatedProduct);
    }

    public ProductDTO activateProduct(Long productId) {
        String currentUser = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setActive(true);
        product.setUpdatedBy(currentUser);
        Product activatedProduct = productRepository.save(product);
        log.info("Activated product: {} by user: {}", productId, currentUser);
        return mapToDTO(activatedProduct);
    }

    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        String currentUser = getCurrentUser();
        productRepository.deleteById(productId);
        log.info("Deleted product: {} by user: {}", productId, currentUser);
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .priceBeforeTax(product.getPriceBeforeTax())
                .tax(product.getTax())
                .price(product.getPrice())
                .durationMinutes(product.getDurationMinutes())
                .commissionRate(product.getCommissionRate())
                .quantity(product.getQuantity())
                .active(product.getActive())
                .createdBy(product.getCreatedBy())
                .updatedBy(product.getUpdatedBy())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private BigDecimal calculatePrice(BigDecimal priceBeforeTax, BigDecimal tax) {
        if (priceBeforeTax == null) {
            return BigDecimal.ZERO;
        }
        if (tax == null) {
            return priceBeforeTax;
        }
        return priceBeforeTax.add(tax);
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }
}
