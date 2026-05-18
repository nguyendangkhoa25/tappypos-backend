package com.tappy.pos.service.product;

import com.tappy.pos.client.OpenFoodFactsClient;
import com.tappy.pos.client.OpenFoodFactsClient.OffProduct;
import com.tappy.pos.client.OpenFoodFactsClient.OffSearchResponse;
import com.tappy.pos.model.dto.product.ProductCatalogDTO;
import com.tappy.pos.model.dto.product.ProductCatalogSyncResult;
import com.tappy.pos.model.entity.product.ProductCatalog;
import com.tappy.pos.repository.product.ProductCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCatalogServiceImpl Unit Tests")
class ProductCatalogServiceImplTest {

    @Mock private ProductCatalogRepository productCatalogRepository;
    @Mock private OpenFoodFactsClient openFoodFactsClient;

    @InjectMocks
    private ProductCatalogServiceImpl productCatalogService;

    private ProductCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = ProductCatalog.builder()
                .barcode("8936001810014")
                .name("Nước Ngọt Coca Cola")
                .brand("Coca-Cola")
                .categoryHint("Beverages")
                .imageUrl("https://example.com/img.jpg")
                .source("OPEN_FOOD_FACTS")
                .build();
        catalog.setId(1L);
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search returns mapped page of results")
    void testSearch_ReturnsResults() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(productCatalogRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
                "coca", "coca", pageable)).thenReturn(new PageImpl<>(List.of(catalog)));

        Page<ProductCatalogDTO> result = productCatalogService.search("coca", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Nước Ngọt Coca Cola");
    }

    @Test
    @DisplayName("search with null query uses empty string")
    void testSearch_NullQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(productCatalogRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
                "", "", pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<ProductCatalogDTO> result = productCatalogService.search(null, 0, 10);

        assertThat(result.getContent()).isEmpty();
        verify(productCatalogRepository).findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase("", "", pageable);
    }

    @Test
    @DisplayName("search trims whitespace from query")
    void testSearch_TrimsQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(productCatalogRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
                "coca", "coca", pageable)).thenReturn(new PageImpl<>(List.of()));

        productCatalogService.search("  coca  ", 0, 10);

        verify(productCatalogRepository).findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase("coca", "coca", pageable);
    }

    // ── findByBarcode ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByBarcode returns DTO when catalog entry found")
    void testFindByBarcode_Found() {
        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.of(catalog));

        Optional<ProductCatalogDTO> result = productCatalogService.findByBarcode("8936001810014");

        assertThat(result).isPresent();
        assertThat(result.get().getBarcode()).isEqualTo("8936001810014");
    }

    @Test
    @DisplayName("findByBarcode returns empty Optional when not found")
    void testFindByBarcode_NotFound() {
        when(productCatalogRepository.findByBarcode("9999999")).thenReturn(Optional.empty());

        Optional<ProductCatalogDTO> result = productCatalogService.findByBarcode("9999999");

        assertThat(result).isEmpty();
    }

    // ── getTotalCount / countBySource ─────────────────────────────────────────

    @Test
    @DisplayName("getTotalCount delegates to repository.count()")
    void testGetTotalCount() {
        when(productCatalogRepository.count()).thenReturn(500L);

        assertThat(productCatalogService.getTotalCount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("countBySource delegates to repository")
    void testCountBySource() {
        when(productCatalogRepository.countBySource("OPEN_FOOD_FACTS")).thenReturn(300L);

        assertThat(productCatalogService.countBySource("OPEN_FOOD_FACTS")).isEqualTo(300L);
    }

    // ── syncFromOpenFoodFacts ─────────────────────────────────────────────────

    @Test
    @DisplayName("syncFromOpenFoodFacts returns disabled result when OFF is disabled")
    void testSync_Disabled() {
        when(openFoodFactsClient.isEnabled()).thenReturn(false);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("disabled");
        verify(productCatalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts inserts new products from OFF")
    void testSync_InsertsNewProducts() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffProduct product = new OffProduct();
        product.code = "8936001810014";
        product.product_name = "Nước Ngọt";
        product.brands = "Coca-Cola";
        product.categories_tags = List.of("en:beverages");

        OffSearchResponse response = new OffSearchResponse();
        response.products = List.of(product);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();

        when(openFoodFactsClient.search(1, 200)).thenReturn(response);
        when(openFoodFactsClient.search(2, 200)).thenReturn(emptyResponse);
        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.empty());
        when(productCatalogRepository.save(any(ProductCatalog.class))).thenReturn(catalog);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getInserted()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(0);
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts updates existing OFF products")
    void testSync_UpdatesExistingOffProducts() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffProduct product = new OffProduct();
        product.code = "8936001810014";
        product.product_name = "New Name";
        product.brands = "Coca-Cola";

        OffSearchResponse response = new OffSearchResponse();
        response.products = List.of(product);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();

        when(openFoodFactsClient.search(1, 200)).thenReturn(response);
        when(openFoodFactsClient.search(2, 200)).thenReturn(emptyResponse);
        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.of(catalog));
        when(productCatalogRepository.save(any(ProductCatalog.class))).thenReturn(catalog);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getInserted()).isEqualTo(0);
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts skips non-OFF existing entries")
    void testSync_SkipsManualEntries() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffProduct product = new OffProduct();
        product.code = "8936001810014";
        product.product_name = "Product";
        product.brands = "Brand";

        OffSearchResponse response = new OffSearchResponse();
        response.products = List.of(product);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();

        ProductCatalog manualEntry = ProductCatalog.builder()
                .barcode("8936001810014").name("Manual").source("MANUAL").build();

        when(openFoodFactsClient.search(1, 200)).thenReturn(response);
        when(openFoodFactsClient.search(2, 200)).thenReturn(emptyResponse);
        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.of(manualEntry));

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.getSkipped()).isEqualTo(1);
        verify(productCatalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts skips products with null barcode")
    void testSync_SkipsNullBarcode() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffProduct noBarcode = new OffProduct();
        noBarcode.code = null;
        noBarcode.product_name = "Some Product";

        OffSearchResponse response = new OffSearchResponse();
        response.products = List.of(noBarcode);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();

        when(openFoodFactsClient.search(1, 200)).thenReturn(response);
        when(openFoodFactsClient.search(2, 200)).thenReturn(emptyResponse);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.getSkipped()).isEqualTo(1);
        verify(productCatalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts skips products with null name")
    void testSync_SkipsNullName() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffProduct noName = new OffProduct();
        noName.code = "1234567890";
        noName.product_name = null;

        OffSearchResponse response = new OffSearchResponse();
        response.products = List.of(noName);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();

        when(openFoodFactsClient.search(1, 200)).thenReturn(response);
        when(openFoodFactsClient.search(2, 200)).thenReturn(emptyResponse);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.getSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts returns failure when OFF throws exception")
    void testSync_ExceptionStopsSync() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);
        when(openFoodFactsClient.search(1, 200)).thenThrow(new RuntimeException("Connection refused"));

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(3);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("syncFromOpenFoodFacts stops when response is empty page")
    void testSync_StopsOnEmptyPage() {
        when(openFoodFactsClient.isEnabled()).thenReturn(true);

        OffSearchResponse emptyResponse = new OffSearchResponse();
        emptyResponse.products = Collections.emptyList();
        when(openFoodFactsClient.search(1, 200)).thenReturn(emptyResponse);

        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPagesProcessed()).isEqualTo(0);
        verify(openFoodFactsClient, times(1)).search(anyInt(), anyInt());
    }

    // ── saveFromOffAsync ──────────────────────────────────────────────────────

    @Test
    @DisplayName("saveFromOffAsync saves new product from OFF")
    void testSaveFromOffAsync_SavesNew() {
        OffProduct product = new OffProduct();
        product.code = "1234567890";
        product.product_name = "Test Product";
        product.brands = "Test Brand";
        product.categories_tags = List.of("en:test-category");

        when(productCatalogRepository.findByBarcode("1234567890")).thenReturn(Optional.empty());
        when(productCatalogRepository.save(any(ProductCatalog.class))).thenReturn(catalog);

        productCatalogService.saveFromOffAsync(product);

        verify(productCatalogRepository).save(argThat(c ->
                "1234567890".equals(c.getBarcode()) && "OPEN_FOOD_FACTS".equals(c.getSource())));
    }

    @Test
    @DisplayName("saveFromOffAsync skips if barcode already in catalog")
    void testSaveFromOffAsync_SkipsExisting() {
        OffProduct product = new OffProduct();
        product.code = "8936001810014";
        product.product_name = "Already there";

        when(productCatalogRepository.findByBarcode("8936001810014")).thenReturn(Optional.of(catalog));

        productCatalogService.saveFromOffAsync(product);

        verify(productCatalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveFromOffAsync does nothing for null product")
    void testSaveFromOffAsync_NullProduct() {
        productCatalogService.saveFromOffAsync(null);

        verify(productCatalogRepository, never()).findByBarcode(anyString());
    }

    @Test
    @DisplayName("saveFromOffAsync does nothing for blank barcode")
    void testSaveFromOffAsync_BlankBarcode() {
        OffProduct product = new OffProduct();
        product.code = "   ";
        product.product_name = "Something";

        productCatalogService.saveFromOffAsync(product);

        verify(productCatalogRepository, never()).findByBarcode(anyString());
    }
}
