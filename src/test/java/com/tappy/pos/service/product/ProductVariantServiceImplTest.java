package com.tappy.pos.service.product;

import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.product.GenerateVariantsRequest;
import com.tappy.pos.model.dto.product.ProductVariantDTO;
import com.tappy.pos.model.dto.product.SaveProductVariantRequest;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.model.entity.product.VariantType;
import com.tappy.pos.model.entity.product.VariantTypeOption;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.repository.product.VariantTypeRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductVariantService Unit Tests")
class ProductVariantServiceImplTest {

    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ProductRepository        productRepository;
    @Mock private VariantTypeRepository    variantTypeRepository;
    @Mock private InventoryRepository      inventoryRepository;
    @Mock private MessageService           messageService;

    @InjectMocks
    private ProductVariantServiceImpl variantService;

    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .sku("FOOD-APPLE-001")
                .price(BigDecimal.valueOf(10_000))
                .deleted(false)
                .build();
        product.setId(1L);

        variant = ProductVariant.builder()
                .sku("FOOD-APPLE-001-RED")
                .variantOptions(Map.of("Color", "Red"))
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        variant.setId(10L);
        // variant.product is set via the product reference
    }

    // ── getVariants ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getVariants: product not found → ResourceNotFoundException")
    void getVariants_productNotFound_throws() {
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> variantService.getVariants(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getVariants: returns list of variants for product")
    void getVariants_success() {
        variant = ProductVariant.builder()
                .sku("FOOD-APPLE-001-RED")
                .variantOptions(Map.of("Color", "Red"))
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        variant.setId(10L);
        // Need product reference for mapToDTO
        ProductVariant variantWithProduct = spy(variant);
        doReturn(product).when(variantWithProduct).getProduct();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByProductIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(variantWithProduct));

        List<ProductVariantDTO> result = variantService.getVariants(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("FOOD-APPLE-001-RED");
    }

    // ── createVariant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createVariant: success — creates and returns DTO")
    void createVariant_success() {
        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("FOOD-APPLE-001-RED")
                .priceOverride(BigDecimal.valueOf(12_000))
                .variantOptions(Map.of("Color", "Red"))
                .build();

        ProductVariant savedVariant = ProductVariant.builder()
                .sku("FOOD-APPLE-001-RED")
                .priceOverride(BigDecimal.valueOf(12_000))
                .variantOptions(Map.of("Color", "Red"))
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        savedVariant.setId(10L);
        ProductVariant savedSpy = spy(savedVariant);
        doReturn(product).when(savedSpy).getProduct();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull("FOOD-APPLE-001-RED")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedSpy);

        ProductVariantDTO result = variantService.createVariant(1L, req);

        assertThat(result.getSku()).isEqualTo("FOOD-APPLE-001-RED");
        assertThat(result.getPriceOverride()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("createVariant: duplicate SKU → DuplicateResourceException")
    void createVariant_duplicateSku_throws() {
        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("EXISTING-SKU")
                .variantOptions(Map.of())
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull("EXISTING-SKU")).thenReturn(true);
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Duplicate SKU");

        assertThatThrownBy(() -> variantService.createVariant(1L, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("createVariant: product not found → ResourceNotFoundException")
    void createVariant_productNotFound_throws() {
        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("SKU-001").variantOptions(Map.of()).build();

        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> variantService.createVariant(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateVariant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateVariant: success — updates existing variant")
    void updateVariant_success() {
        ProductVariant existingVariant = ProductVariant.builder()
                .sku("OLD-SKU")
                .variantOptions(Map.of("Color", "Blue"))
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        existingVariant.setId(10L);
        ProductVariant savedSpy = spy(existingVariant);
        doReturn(product).when(savedSpy).getProduct();

        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("NEW-SKU")
                .variantOptions(Map.of("Color", "Green"))
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull("NEW-SKU")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedSpy);

        ProductVariantDTO result = variantService.updateVariant(1L, 10L, req);

        assertThat(result).isNotNull();
        verify(productVariantRepository).save(existingVariant);
    }

    @Test
    @DisplayName("updateVariant: same SKU — no uniqueness check")
    void updateVariant_sameSku_noUniquenessCheck() {
        ProductVariant existingVariant = ProductVariant.builder()
                .sku("SAME-SKU")
                .variantOptions(Map.of())
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        existingVariant.setId(10L);
        ProductVariant savedSpy = spy(existingVariant);
        doReturn(product).when(savedSpy).getProduct();

        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("SAME-SKU")
                .variantOptions(Map.of("Size", "L"))
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedSpy);

        variantService.updateVariant(1L, 10L, req);

        verify(productVariantRepository, never()).existsBySkuAndDeletedAtIsNull(anyString());
    }

    @Test
    @DisplayName("updateVariant: variant not found → ResourceNotFoundException")
    void updateVariant_variantNotFound_throws() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(99L, 1L))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        SaveProductVariantRequest req = SaveProductVariantRequest.builder()
                .sku("ANY-SKU").variantOptions(Map.of()).build();

        assertThatThrownBy(() -> variantService.updateVariant(1L, 99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteVariant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteVariant: success — soft deletes the variant")
    void deleteVariant_success() {
        ProductVariant existing = ProductVariant.builder()
                .sku("FOOD-APPLE-001-RED")
                .variantOptions(Map.of())
                .status(ProductVariant.VariantStatus.ACTIVE)
                .build();
        existing.setId(10L);

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(existing));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(existing);

        variantService.deleteVariant(1L, 10L);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(productVariantRepository).save(existing);
    }

    @Test
    @DisplayName("deleteVariant: variant not found → ResourceNotFoundException")
    void deleteVariant_variantNotFound_throws() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(99L, 1L))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("Not found");

        assertThatThrownBy(() -> variantService.deleteVariant(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── generateVariants ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateVariants: generates cartesian product — 2 colors × 2 sizes = 4 variants")
    void generateVariants_cartesianProduct() {
        VariantTypeOption red   = buildOption("Red");
        VariantTypeOption blue  = buildOption("Blue");
        VariantTypeOption small = buildOption("S");
        VariantTypeOption large = buildOption("L");

        VariantType colorType = VariantType.builder().name("Color")
                .options(List.of(red, blue)).build();
        colorType.setId(1L);

        VariantType sizeType = VariantType.builder().name("Size")
                .options(List.of(small, large)).build();
        sizeType.setId(2L);

        GenerateVariantsRequest req = GenerateVariantsRequest.builder()
                .variantTypeIds(List.of(1L, 2L))
                .baseSku("FOOD-APPLE-001")
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(colorType));
        when(variantTypeRepository.findById(2L)).thenReturn(Optional.of(sizeType));
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull(anyString())).thenReturn(false);

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            ProductVariant spy = spy(v);
            doReturn(product).when(spy).getProduct();
            return spy;
        });

        List<ProductVariantDTO> result = variantService.generateVariants(1L, req);

        assertThat(result).hasSize(4);
        verify(productVariantRepository, times(4)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("generateVariants: existing SKU — skips that combination")
    void generateVariants_skipExistingSku() {
        VariantTypeOption red  = buildOption("Red");
        VariantTypeOption blue = buildOption("Blue");

        VariantType colorType = VariantType.builder().name("Color")
                .options(List.of(red, blue)).build();
        colorType.setId(1L);

        GenerateVariantsRequest req = GenerateVariantsRequest.builder()
                .variantTypeIds(List.of(1L))
                .baseSku("BASE")
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(colorType));
        // "BASE-RED" already exists; "BASE-BLUE" is new
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull("BASE-RED")).thenReturn(true);
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull("BASE-BLUE")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            ProductVariant sp = spy(v);
            doReturn(product).when(sp).getProduct();
            return sp;
        });

        List<ProductVariantDTO> result = variantService.generateVariants(1L, req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).endsWith("BLUE");
    }

    @Test
    @DisplayName("generateVariants: uses product SKU as base when baseSku not provided")
    void generateVariants_usesProductSkuAsBase() {
        VariantTypeOption red = buildOption("Red");
        VariantType colorType = VariantType.builder().name("Color")
                .options(List.of(red)).build();
        colorType.setId(1L);

        GenerateVariantsRequest req = GenerateVariantsRequest.builder()
                .variantTypeIds(List.of(1L))
                .baseSku(null)
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(colorType));
        when(productVariantRepository.existsBySkuAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            ProductVariant sp = spy(v);
            doReturn(product).when(sp).getProduct();
            return sp;
        });

        List<ProductVariantDTO> result = variantService.generateVariants(1L, req);

        // Base SKU should be product.getSku() = "FOOD-APPLE-001"
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).startsWith("FOOD-APPLE-001-");
    }

    @Test
    @DisplayName("generateVariants: variant type not found → ResourceNotFoundException")
    void generateVariants_variantTypeNotFound_throws() {
        GenerateVariantsRequest req = GenerateVariantsRequest.builder()
                .variantTypeIds(List.of(99L))
                .baseSku("BASE")
                .build();

        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(variantTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.generateVariants(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private VariantTypeOption buildOption(String value) {
        VariantTypeOption opt = VariantTypeOption.builder().value(value).build();
        return opt;
    }
}
