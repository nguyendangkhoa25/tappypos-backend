package com.tappy.pos.service.product;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.variant.SaveVariantTypeRequest;
import com.tappy.pos.model.dto.variant.VariantTypeDTO;
import com.tappy.pos.model.entity.product.ProductType;
import com.tappy.pos.model.entity.product.VariantType;
import com.tappy.pos.model.entity.product.VariantTypeOption;
import com.tappy.pos.repository.product.ProductTypeRepository;
import com.tappy.pos.repository.product.VariantTypeRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VariantTypeService Unit Tests")
class VariantTypeServiceTest {

    @Mock private VariantTypeRepository variantTypeRepository;
    @Mock private ProductTypeRepository productTypeRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private VariantTypeService variantTypeService;

    private VariantType variantType;
    private SaveVariantTypeRequest saveRequest;

    @BeforeEach
    void setUp() {
        VariantTypeOption opt1 = VariantTypeOption.builder().value("Đỏ").sortOrder(0).build();
        opt1.setId(10L);
        opt1.setDeleted(false);

        VariantTypeOption opt2 = VariantTypeOption.builder().value("Xanh").sortOrder(1).build();
        opt2.setId(11L);
        opt2.setDeleted(false);

        List<VariantTypeOption> options = new ArrayList<>();
        options.add(opt1);
        options.add(opt2);

        variantType = VariantType.builder()
                .name("Màu sắc")
                .description("Màu của sản phẩm")
                .productTypeId(5L)
                .sortOrder(1)
                .options(options)
                .build();
        variantType.setId(1L);
        variantType.setDeleted(false);

        saveRequest = new SaveVariantTypeRequest();
        saveRequest.setName("Màu sắc");
        saveRequest.setDescription("Màu của sản phẩm");
        saveRequest.setProductTypeId(5L);
        saveRequest.setSortOrder(1);
        saveRequest.setOptions(List.of("Đỏ", "Xanh", "Vàng"));

        lenient().when(messageService.getMessage(anyString())).thenReturn("error");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("error");
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll returns all active variant types")
    void testGetAll() {
        when(variantTypeRepository.findAllActive()).thenReturn(List.of(variantType));
        when(productTypeRepository.findById(5L)).thenReturn(Optional.of(
                ProductType.builder().name("CLOTHING").build()));

        List<VariantTypeDTO> result = variantTypeService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Màu sắc");
    }

    @Test
    @DisplayName("getAll returns empty list when none exist")
    void testGetAll_Empty() {
        when(variantTypeRepository.findAllActive()).thenReturn(List.of());

        List<VariantTypeDTO> result = variantTypeService.getAll();

        assertThat(result).isEmpty();
    }

    // ── getForProductType ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getForProductType returns variant types for given product type")
    void testGetForProductType() {
        when(variantTypeRepository.findForProductType(5L)).thenReturn(List.of(variantType));
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        List<VariantTypeDTO> result = variantTypeService.getForProductType(5L);

        assertThat(result).hasSize(1);
        verify(variantTypeRepository).findForProductType(5L);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns DTO with options for active variant type")
    void testGetById_Success() {
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        VariantTypeDTO result = variantTypeService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Màu sắc");
        assertThat(result.getOptions()).hasSize(2);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for missing variant type")
    void testGetById_NotFound() {
        when(variantTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantTypeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for deleted variant type")
    void testGetById_Deleted() {
        variantType.setDeleted(true);
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));

        assertThatThrownBy(() -> variantTypeService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create saves variant type with options")
    void testCreate_Success() {
        when(variantTypeRepository.existsByNameAndDeletedFalse("Màu sắc")).thenReturn(false);
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        VariantTypeDTO result = variantTypeService.create(saveRequest);

        assertThat(result).isNotNull();
        verify(variantTypeRepository).save(argThat(vt ->
                "Màu sắc".equals(vt.getName()) && vt.getOptions().size() == 3));
    }

    @Test
    @DisplayName("create deduplicates options")
    void testCreate_DeduplicatesOptions() {
        saveRequest.setOptions(List.of("Đỏ", "Đỏ", "Xanh"));
        when(variantTypeRepository.existsByNameAndDeletedFalse("Màu sắc")).thenReturn(false);
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        variantTypeService.create(saveRequest);

        verify(variantTypeRepository).save(argThat(vt -> vt.getOptions().size() == 2));
    }

    @Test
    @DisplayName("create uses sortOrder 0 when not specified")
    void testCreate_DefaultSortOrder() {
        saveRequest.setSortOrder(null);
        when(variantTypeRepository.existsByNameAndDeletedFalse("Màu sắc")).thenReturn(false);
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        variantTypeService.create(saveRequest);

        verify(variantTypeRepository).save(argThat(vt -> vt.getSortOrder() == 0));
    }

    @Test
    @DisplayName("create throws BadRequestException when name already exists")
    void testCreate_DuplicateName() {
        when(variantTypeRepository.existsByNameAndDeletedFalse("Màu sắc")).thenReturn(true);

        assertThatThrownBy(() -> variantTypeService.create(saveRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create handles null options list gracefully")
    void testCreate_NullOptions() {
        saveRequest.setOptions(null);
        when(variantTypeRepository.existsByNameAndDeletedFalse("Màu sắc")).thenReturn(false);
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        variantTypeService.create(saveRequest);

        verify(variantTypeRepository).save(argThat(vt -> vt.getOptions().isEmpty()));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update replaces options and saves")
    void testUpdate_Success() {
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        saveRequest.setOptions(List.of("Đỏ", "Trắng"));
        VariantTypeDTO result = variantTypeService.update(1L, saveRequest);

        assertThat(result).isNotNull();
        verify(variantTypeRepository).save(any(VariantType.class));
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for missing variant type")
    void testUpdate_NotFound() {
        when(variantTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantTypeService.update(99L, saveRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update applies new sortOrder when provided")
    void testUpdate_NewSortOrder() {
        saveRequest.setSortOrder(5);
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        variantTypeService.update(1L, saveRequest);

        verify(variantTypeRepository).save(argThat(vt -> vt.getSortOrder() == 5));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete soft-deletes variant type and all its options")
    void testDelete_Success() {
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(variantTypeRepository.save(any(VariantType.class))).thenReturn(variantType);

        variantTypeService.delete(1L);

        assertThat(variantType.isDeleted()).isTrue();
        assertThat(variantType.getOptions()).allMatch(o -> o.isDeleted());
        verify(variantTypeRepository).save(variantType);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException for missing variant type")
    void testDelete_NotFound() {
        when(variantTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantTypeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DTO mapping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("mapToDTO includes productTypeName when product type found")
    void testMapToDTO_WithProductTypeName() {
        ProductType pt = ProductType.builder().name("Thời Trang").build();
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(productTypeRepository.findById(5L)).thenReturn(Optional.of(pt));

        VariantTypeDTO dto = variantTypeService.getById(1L);

        assertThat(dto.getProductTypeName()).isEqualTo("Thời Trang");
    }

    @Test
    @DisplayName("mapToDTO excludes deleted options from DTO")
    void testMapToDTO_ExcludesDeletedOptions() {
        variantType.getOptions().get(0).softDelete();
        when(variantTypeRepository.findById(1L)).thenReturn(Optional.of(variantType));
        when(productTypeRepository.findById(5L)).thenReturn(Optional.empty());

        VariantTypeDTO dto = variantTypeService.getById(1L);

        assertThat(dto.getOptions()).hasSize(1);
    }
}
