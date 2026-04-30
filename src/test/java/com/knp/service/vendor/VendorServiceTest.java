package com.knp.service.vendor;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.vendor.SaveVendorRequest;
import com.knp.model.dto.vendor.VendorDTO;
import com.knp.model.entity.vendor.Vendor;
import com.knp.repository.vendor.VendorRepository;
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
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.knp.service.MessageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService Unit Tests")
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private VendorService vendorService;

    private Vendor vendor;
    private SaveVendorRequest saveRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .name("Công ty Vàng Bạc ABC")
                .code("ABC")
                .contactName("Nguyễn Văn A")
                .email("abc@example.com")
                .phone("0901234567")
                .address("123 Đường ABC, Hà Nội")
                .taxId("0123456789")
                .paymentTerms("NET_30")
                .isActive(true)
                .notes("Nhà cung cấp uy tín")
                .build();
        vendor.setId(1L);
        vendor.setDeleted(false);

        saveRequest = new SaveVendorRequest();
        saveRequest.setName("Công ty Vàng Bạc ABC");
        saveRequest.setCode("abc");
        saveRequest.setContactName("Nguyễn Văn A");
        saveRequest.setEmail("abc@example.com");
        saveRequest.setPhone("0901234567");
        saveRequest.setAddress("123 Đường ABC, Hà Nội");
        saveRequest.setTaxId("0123456789");
        saveRequest.setPaymentTerms("NET_30");
        saveRequest.setIsActive(true);

        pageable = PageRequest.of(0, 20);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all vendors when no keyword")
    void testGetAll_NoKeyword() {
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.findAllActive(pageable)).thenReturn(page);

        Page<VendorDTO> result = vendorService.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(vendorRepository).findAllActive(pageable);
        verify(vendorRepository, never()).search(anyString(), any());
    }

    @Test
    @DisplayName("Should search vendors when keyword provided")
    void testGetAll_WithKeyword() {
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.search("ABC", pageable)).thenReturn(page);

        Page<VendorDTO> result = vendorService.getAll("ABC", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(vendorRepository).search("ABC", pageable);
        verify(vendorRepository, never()).findAllActive(any());
    }

    @Test
    @DisplayName("Should trim keyword before searching")
    void testGetAll_TrimsKeyword() {
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.search("ABC", pageable)).thenReturn(page);

        vendorService.getAll("  ABC  ", pageable);

        verify(vendorRepository).search("ABC", pageable);
    }

    @Test
    @DisplayName("Should use findAllActive for blank keyword")
    void testGetAll_BlankKeyword_FallsBackToAll() {
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.findAllActive(pageable)).thenReturn(page);

        vendorService.getAll("   ", pageable);

        verify(vendorRepository).findAllActive(pageable);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return vendor DTO by id")
    void testGetById_Success() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        VendorDTO result = vendorService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("ABC");
        assertThat(result.getName()).isEqualTo("Công ty Vàng Bạc ABC");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for missing vendor")
    void testGetById_NotFound() {
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for soft-deleted vendor")
    void testGetById_SoftDeleted() {
        vendor.setDeleted(true);
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> vendorService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllForSelect ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active vendors for select dropdown")
    void testGetAllForSelect_ReturnsList() {
        when(vendorRepository.findAllActiveForSelect()).thenReturn(List.of(vendor));

        List<VendorDTO> result = vendorService.getAllForSelect();

        assertThat(result).hasSize(1);
        verify(vendorRepository).findAllActiveForSelect();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create vendor with uppercased code")
    void testCreate_Success() {
        when(vendorRepository.findByCode("ABC")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        VendorDTO result = vendorService.create(saveRequest);

        assertThat(result).isNotNull();
        verify(vendorRepository).findByCode("ABC");
        verify(vendorRepository).save(argThat(v -> "ABC".equals(v.getCode())));
    }

    @Test
    @DisplayName("Should throw BadRequestException for duplicate vendor code")
    void testCreate_DuplicateCode() {
        when(vendorRepository.findByCode("ABC")).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> vendorService.create(saveRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should default paymentTerms to NET_30 when not provided")
    void testCreate_DefaultPaymentTerms() {
        saveRequest.setPaymentTerms(null);
        when(vendorRepository.findByCode("ABC")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        vendorService.create(saveRequest);

        verify(vendorRepository).save(argThat(v -> "NET_30".equals(v.getPaymentTerms())));
    }

    @Test
    @DisplayName("Should default isActive to true when not provided")
    void testCreate_DefaultIsActive() {
        saveRequest.setIsActive(null);
        when(vendorRepository.findByCode("ABC")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        vendorService.create(saveRequest);

        verify(vendorRepository).save(argThat(v -> Boolean.TRUE.equals(v.getIsActive())));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update vendor when code unchanged")
    void testUpdate_SameCode() {
        saveRequest.setCode("ABC");
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        VendorDTO result = vendorService.update(1L, saveRequest);

        assertThat(result).isNotNull();
        verify(vendorRepository, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("Should check uniqueness when code changes")
    void testUpdate_NewCodeUnique() {
        saveRequest.setCode("XYZ");
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByCode("XYZ")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        vendorService.update(1L, saveRequest);

        verify(vendorRepository).findByCode("XYZ");
    }

    @Test
    @DisplayName("Should throw BadRequestException when new code already used by another vendor")
    void testUpdate_NewCodeConflict() {
        saveRequest.setCode("XYZ");
        Vendor other = Vendor.builder().name("Other").code("XYZ").isActive(true).build();
        other.setId(2L);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByCode("XYZ")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> vendorService.update(1L, saveRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent vendor")
    void testUpdate_NotFound() {
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.update(99L, saveRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should soft-delete an active vendor")
    void testDelete_Success() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);

        vendorService.delete(1L);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent vendor")
    void testDelete_NotFound() {
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DTO mapping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should map all vendor fields to DTO")
    void testMapToDTO_AllFields() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        VendorDTO dto = vendorService.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Công ty Vàng Bạc ABC");
        assertThat(dto.getCode()).isEqualTo("ABC");
        assertThat(dto.getContactName()).isEqualTo("Nguyễn Văn A");
        assertThat(dto.getEmail()).isEqualTo("abc@example.com");
        assertThat(dto.getPhone()).isEqualTo("0901234567");
        assertThat(dto.getPaymentTerms()).isEqualTo("NET_30");
        assertThat(dto.getIsActive()).isTrue();
    }
}
