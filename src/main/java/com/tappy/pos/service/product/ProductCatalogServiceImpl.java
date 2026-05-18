package com.tappy.pos.service.product;

import com.tappy.pos.client.OpenFoodFactsClient;
import com.tappy.pos.client.OpenFoodFactsClient.OffProduct;
import com.tappy.pos.client.OpenFoodFactsClient.OffSearchResponse;
import com.tappy.pos.model.dto.product.ProductCatalogDTO;
import com.tappy.pos.model.dto.product.ProductCatalogSyncResult;
import com.tappy.pos.model.entity.product.ProductCatalog;
import com.tappy.pos.repository.product.ProductCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final String SOURCE_OFF = "OPEN_FOOD_FACTS";
    private static final int PAGE_SIZE = 200;

    private final ProductCatalogRepository productCatalogRepository;
    private final OpenFoodFactsClient openFoodFactsClient;

    @Override
    @Transactional
    public ProductCatalogSyncResult syncFromOpenFoodFacts(int maxPages) {
        if (!openFoodFactsClient.isEnabled()) {
            log.warn("OFF integration is disabled (off.enabled=false) — sync skipped");
            return ProductCatalogSyncResult.builder()
                    .success(false)
                    .errorMessage("Open Food Facts integration is disabled. Set off.enabled=true to enable it.")
                    .build();
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int totalFetched = 0;
        int pagesProcessed = 0;

        for (int page = 1; page <= maxPages; page++) {
            OffSearchResponse response;
            try {
                response = openFoodFactsClient.search(page, PAGE_SIZE);
            } catch (Exception e) {
                log.error("Stopping sync at page {} due to error: {}", page, e.getMessage());
                String msg = e.getMessage();
                // Extract just the status line — the full body is an HTML page
                if (msg != null && msg.contains(":")) {
                    msg = msg.substring(0, msg.indexOf(':') + 50).trim();
                    if (msg.length() > 120) msg = msg.substring(0, 120) + "...";
                }
                return ProductCatalogSyncResult.builder()
                        .inserted(inserted).updated(updated).skipped(skipped)
                        .totalFetched(totalFetched).pagesProcessed(pagesProcessed)
                        .success(false)
                        .errorMessage(msg)
                        .build();
            }

            List<OffProduct> products = response.products;
            if (products == null || products.isEmpty()) {
                log.info("No more products on page {}, stopping sync", page);
                break;
            }

            totalFetched += products.size();
            pagesProcessed++;

            for (OffProduct offProduct : products) {
                String barcode = offProduct.code;
                if (barcode == null || barcode.isBlank()) {
                    skipped++;
                    continue;
                }
                barcode = barcode.trim();

                String name = offProduct.product_name;
                if (name == null || name.isBlank()) {
                    skipped++;
                    continue;
                }
                name = name.trim();

                String brand = parseBrand(offProduct.brands);
                String categoryHint = parseCategoryHint(offProduct.categories_tags);
                String imageUrl = offProduct.image_front_url;

                Optional<ProductCatalog> existing = productCatalogRepository.findByBarcode(barcode);
                if (existing.isPresent()) {
                    ProductCatalog catalog = existing.get();
                    // Only update if source is OPEN_FOOD_FACTS (don't overwrite manual entries)
                    if (SOURCE_OFF.equals(catalog.getSource())) {
                        catalog.setName(name);
                        catalog.setBrand(brand);
                        catalog.setImageUrl(imageUrl);
                        catalog.setCategoryHint(categoryHint);
                        productCatalogRepository.save(catalog);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    ProductCatalog catalog = ProductCatalog.builder()
                            .barcode(barcode)
                            .name(name)
                            .brand(brand)
                            .categoryHint(categoryHint)
                            .imageUrl(imageUrl)
                            .source(SOURCE_OFF)
                            .build();
                    productCatalogRepository.save(catalog);
                    inserted++;
                }
            }

            log.info("Processed page {}/{}: fetched={}, inserted={}, updated={}, skipped={}",
                    page, maxPages, products.size(), inserted, updated, skipped);
        }

        return ProductCatalogSyncResult.builder()
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .totalFetched(totalFetched)
                .pagesProcessed(pagesProcessed)
                .success(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCatalogDTO> search(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String q = (query == null || query.isBlank()) ? "" : query.trim();
        Page<ProductCatalog> entities = productCatalogRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(q, q, pageable);
        return entities.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductCatalogDTO> findByBarcode(String barcode) {
        return productCatalogRepository.findByBarcode(barcode).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return productCatalogRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySource(String source) {
        return productCatalogRepository.countBySource(source);
    }

    @Override
    @Async
    @Transactional
    public void saveFromOffAsync(OffProduct offProduct) {
        if (offProduct == null || offProduct.code == null || offProduct.code.isBlank()) return;
        String barcode = offProduct.code.trim();
        if (productCatalogRepository.findByBarcode(barcode).isPresent()) return; // already stored
        try {
            ProductCatalog catalog = ProductCatalog.builder()
                    .barcode(barcode)
                    .name(offProduct.product_name != null ? offProduct.product_name.trim() : barcode)
                    .brand(parseBrand(offProduct.brands))
                    .categoryHint(parseCategoryHint(offProduct.categories_tags))
                    .imageUrl(offProduct.image_front_url)
                    .source(SOURCE_OFF)
                    .build();
            productCatalogRepository.save(catalog);
            log.info("Saved OFF product to catalog: barcode={} name={}", barcode, catalog.getName());
        } catch (Exception e) {
            log.warn("Failed to save OFF product barcode={} to catalog: {}", barcode, e.getMessage());
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private ProductCatalogDTO toDTO(ProductCatalog entity) {
        return ProductCatalogDTO.builder()
                .id(entity.getId())
                .barcode(entity.getBarcode())
                .name(entity.getName())
                .brand(entity.getBrand())
                .categoryHint(entity.getCategoryHint())
                .unit(entity.getUnit())
                .imageUrl(entity.getImageUrl())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Takes the first comma-separated brand value and trims it.
     */
    private String parseBrand(String brands) {
        if (brands == null || brands.isBlank()) return null;
        String[] parts = brands.split(",");
        return parts[0].trim();
    }

    /**
     * Takes the first category tag, strips "en:" or "vn:" prefix,
     * replaces hyphens with spaces, and title-cases the result.
     */
    private String parseCategoryHint(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        String tag = tags.get(0);
        if (tag == null) return null;
        // Strip language prefix (en:, vn:, fr:, etc.)
        if (tag.contains(":")) {
            tag = tag.substring(tag.indexOf(':') + 1);
        }
        // Replace hyphens with spaces
        tag = tag.replace('-', ' ').trim();
        if (tag.isBlank()) return null;
        // Title-case
        return toTitleCase(tag);
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
