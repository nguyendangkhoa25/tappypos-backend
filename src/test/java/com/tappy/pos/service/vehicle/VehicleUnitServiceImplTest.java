package com.tappy.pos.service.vehicle;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.vehicle.CreateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.SellVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.UpdateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.VehicleUnitDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.vehicle.VehicleUnitEntity;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.vehicle.VehicleUnitRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleUnitServiceImpl Unit Tests")
class VehicleUnitServiceImplTest {

    @Mock private VehicleUnitRepository vehicleUnitRepository;
    @Mock private ProductRepository productRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks private VehicleUnitServiceImpl service;

    private Product product;

    @BeforeEach
    void setUp() {
        lenient().when(authContext.getCurrentUsername()).thenReturn("user1");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("test-tenant");
        lenient().when(messageService.getMessage(any())).thenReturn("msg");

        product = Product.builder().name("Honda Wave").sku("SKU-1").build();
        product.setId(100L);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private VehicleUnitEntity newEntity() {
        VehicleUnitEntity e = VehicleUnitEntity.builder()
                .id(1L)
                .tenantId("test-tenant")
                .productId(100L)
                .status(VehicleUnitStatus.IN_STOCK)
                .deleted(false)
                .build();
        return e;
    }

    // ── create ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("create — with frame number logs the .frame activity variant")
    void create_withFrameNo_success() {
        CreateVehicleUnitRequest request = new CreateVehicleUnitRequest();
        request.setProductId(100L);
        request.setFrameNo("FRAME123");
        request.setEngineNo("ENGINE123");
        request.setLicensePlate("59-X1 12345");
        request.setColor("Đỏ");
        request.setOdometerKm(1000);
        request.setPurchasePrice(new BigDecimal("20000000"));
        request.setCurrentValue(new BigDecimal("22000000"));
        request.setConditionGrade("Mới");
        request.setWarrantyMonths(12);
        request.setPaperworkStatus("Đủ");
        request.setNotes("note");
        request.setLegacyId("L-1");

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse("FRAME123", "test-tenant"))
                .thenReturn(false);
        when(vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse("ENGINE123", "test-tenant"))
                .thenReturn(false);
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> {
            VehicleUnitEntity e = i.getArgument(0);
            e.setId(5L);
            return e;
        });

        VehicleUnitDTO dto = service.create(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getFrameNo()).isEqualTo("FRAME123");
        assertThat(dto.getStatus()).isEqualTo(VehicleUnitStatus.IN_STOCK);
        assertThat(dto.getProductName()).isEqualTo("Honda Wave");
        assertThat(dto.getProductSku()).isEqualTo("SKU-1");
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.created.frame"),
                any(), any(), any());
    }

    @Test
    @DisplayName("create — blank frame number logs the plain (no-frame) activity variant")
    void create_blankFrameNo_success() {
        CreateVehicleUnitRequest request = new CreateVehicleUnitRequest();
        request.setProductId(100L);
        request.setFrameNo("   ");
        request.setEngineNo(null);

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VehicleUnitDTO dto = service.create(request);

        assertThat(dto.getFrameNo()).isNull();
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.created"),
                any(), any());
    }

    @Test
    @DisplayName("create — product not found throws ResourceNotFoundException")
    void create_productNotFound_throws() {
        CreateVehicleUnitRequest request = new CreateVehicleUnitRequest();
        request.setProductId(999L);
        when(productRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — duplicate frame number throws BadRequestException")
    void create_duplicateFrame_throws() {
        CreateVehicleUnitRequest request = new CreateVehicleUnitRequest();
        request.setProductId(100L);
        request.setFrameNo("DUP");

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse("DUP", "test-tenant"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — duplicate engine number throws BadRequestException")
    void create_duplicateEngine_throws() {
        CreateVehicleUnitRequest request = new CreateVehicleUnitRequest();
        request.setProductId(100L);
        request.setEngineNo("ENG-DUP");

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse("ENG-DUP", "test-tenant"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    // ── update ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("update — applies every provided field and a non-SOLD status")
    void update_allFields_success() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo("OLD-FRAME");
        entity.setEngineNo("OLD-ENGINE");

        UpdateVehicleUnitRequest request = new UpdateVehicleUnitRequest();
        request.setFrameNo("NEW-FRAME");
        request.setEngineNo("NEW-ENGINE");
        request.setLicensePlate("59-Z9 99999");
        request.setColor("Đen");
        request.setOdometerKm(500);
        request.setPurchasePrice(new BigDecimal("15000000"));
        request.setCurrentValue(new BigDecimal("16000000"));
        request.setConditionGrade("Cũ");
        request.setWarrantyMonths(6);
        request.setPaperworkStatus("Thiếu");
        request.setNotes("updated");
        request.setStatus(VehicleUnitStatus.RESERVED);

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse("NEW-FRAME", "test-tenant"))
                .thenReturn(false);
        when(vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse("NEW-ENGINE", "test-tenant"))
                .thenReturn(false);
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        VehicleUnitDTO dto = service.update(1L, request);

        assertThat(entity.getFrameNo()).isEqualTo("NEW-FRAME");
        assertThat(entity.getEngineNo()).isEqualTo("NEW-ENGINE");
        assertThat(entity.getLicensePlate()).isEqualTo("59-Z9 99999");
        assertThat(entity.getColor()).isEqualTo("Đen");
        assertThat(entity.getOdometerKm()).isEqualTo(500);
        assertThat(entity.getConditionGrade()).isEqualTo("Cũ");
        assertThat(entity.getWarrantyMonths()).isEqualTo(6);
        assertThat(entity.getPaperworkStatus()).isEqualTo("Thiếu");
        assertThat(entity.getNotes()).isEqualTo("updated");
        assertThat(entity.getStatus()).isEqualTo(VehicleUnitStatus.RESERVED);
        assertThat(dto).isNotNull();
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.updated"), any(), any());
    }

    @Test
    @DisplayName("update — SOLD status in request is ignored (not applied)")
    void update_soldStatus_ignored() {
        VehicleUnitEntity entity = newEntity();
        entity.setStatus(VehicleUnitStatus.IN_STOCK);

        UpdateVehicleUnitRequest request = new UpdateVehicleUnitRequest();
        request.setStatus(VehicleUnitStatus.SOLD);

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        service.update(1L, request);

        assertThat(entity.getStatus()).isEqualTo(VehicleUnitStatus.IN_STOCK);
    }

    @Test
    @DisplayName("update — changed frame that duplicates throws BadRequestException")
    void update_frameChangedDuplicate_throws() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo("OLD");

        UpdateVehicleUnitRequest request = new UpdateVehicleUnitRequest();
        request.setFrameNo("DUP");

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse("DUP", "test-tenant"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — changed engine that duplicates throws BadRequestException")
    void update_engineChangedDuplicate_throws() {
        VehicleUnitEntity entity = newEntity();
        entity.setEngineNo("OLD-E");

        UpdateVehicleUnitRequest request = new UpdateVehicleUnitRequest();
        request.setEngineNo("DUP-E");

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse("DUP-E", "test-tenant"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — entity not found throws ResourceNotFoundException")
    void update_notFound_throws() {
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, new UpdateVehicleUnitRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getById ──────────────────────────────────────────────────────────────
    @Test
    @DisplayName("getById — returns the mapped DTO")
    void getById_success() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo("F-GET");
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        VehicleUnitDTO dto = service.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFrameNo()).isEqualTo("F-GET");
        assertThat(dto.getProductName()).isEqualTo("Honda Wave");
    }

    @Test
    @DisplayName("getById — not found throws ResourceNotFoundException")
    void getById_notFound_throws() {
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById — product missing yields null product fields in DTO")
    void getById_productMissing_nullProductFields() {
        VehicleUnitEntity entity = newEntity();
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        VehicleUnitDTO dto = service.getById(1L);

        assertThat(dto.getProductName()).isNull();
        assertThat(dto.getProductSku()).isNull();
    }

    // ── search ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("search — returns a mapped page")
    void search_success() {
        VehicleUnitEntity entity = newEntity();
        Pageable pageable = PageRequest.of(0, 10);
        Page<VehicleUnitEntity> page = new PageImpl<>(List.of(entity));

        when(vehicleUnitRepository.search(VehicleUnitStatus.IN_STOCK, 100L, pageable)).thenReturn(page);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        Page<VehicleUnitDTO> result = service.search(VehicleUnitStatus.IN_STOCK, 100L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    // ── lookup ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("lookup — blank keyword returns empty list without hitting the repository")
    void lookup_blank_returnsEmpty() {
        List<VehicleUnitDTO> result = service.lookup("   ");

        assertThat(result).isEmpty();
        verify(vehicleUnitRepository, never()).lookup(anyString());
    }

    @Test
    @DisplayName("lookup — non-blank keyword maps the repository results")
    void lookup_nonBlank_maps() {
        VehicleUnitEntity entity = newEntity();
        entity.setWarrantyExp(LocalDate.now().plusDays(10));
        when(vehicleUnitRepository.lookup("frame")).thenReturn(List.of(entity));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        List<VehicleUnitDTO> result = service.lookup("  frame  ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarrantyActive()).isTrue();
    }

    // ── markSold ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("markSold — warranty months from request, frame present logs .frame variant")
    void markSold_warrantyFromRequest_withFrame() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo("F-SELL");
        entity.setWarrantyMonths(3);

        SellVehicleUnitRequest request = new SellVehicleUnitRequest();
        request.setOrderId(77L);
        request.setCustomerId(9L);
        request.setCustomerName("Khách A");
        request.setWarrantyMonths(12);
        request.setPaperworkStatus("Đủ");

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        VehicleUnitDTO dto = service.markSold(1L, request);

        assertThat(entity.getStatus()).isEqualTo(VehicleUnitStatus.SOLD);
        assertThat(entity.getSoldTo()).isEqualTo(9L);
        assertThat(entity.getSoldToName()).isEqualTo("Khách A");
        assertThat(entity.getSoldOrderId()).isEqualTo(77L);
        assertThat(entity.getWarrantyMonths()).isEqualTo(12);
        assertThat(entity.getWarrantyExp()).isEqualTo(LocalDate.now().plusMonths(12));
        assertThat(entity.getPaperworkStatus()).isEqualTo("Đủ");
        assertThat(dto.getWarrantyActive()).isTrue();
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.sold.frame"),
                any(), any(), any());
    }

    @Test
    @DisplayName("markSold — warranty months fall back to entity, null frame logs plain variant")
    void markSold_warrantyFromEntity_nullFrame() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo(null);
        entity.setWarrantyMonths(6);

        SellVehicleUnitRequest request = new SellVehicleUnitRequest();
        request.setWarrantyMonths(null);

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        service.markSold(1L, request);

        assertThat(entity.getWarrantyExp()).isEqualTo(LocalDate.now().plusMonths(6));
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.sold"), any(), any());
    }

    @Test
    @DisplayName("markSold — no warranty months leaves warranty unset (warrantyActive null)")
    void markSold_noWarranty() {
        VehicleUnitEntity entity = newEntity();
        entity.setFrameNo("F");
        entity.setWarrantyMonths(null);

        SellVehicleUnitRequest request = new SellVehicleUnitRequest();

        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        VehicleUnitDTO dto = service.markSold(1L, request);

        assertThat(entity.getWarrantyExp()).isNull();
        assertThat(dto.getWarrantyActive()).isNull();
    }

    @Test
    @DisplayName("markSold — already SOLD throws BadRequestException")
    void markSold_alreadySold_throws() {
        VehicleUnitEntity entity = newEntity();
        entity.setStatus(VehicleUnitStatus.SOLD);
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.markSold(1L, new SellVehicleUnitRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    // ── delete ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("delete — soft-deletes the unit and logs the activity")
    void delete_success() {
        VehicleUnitEntity entity = newEntity();
        entity.setStatus(VehicleUnitStatus.IN_STOCK);
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(vehicleUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(1L);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), any(), any(),
                eq("VEHICLE_UNIT"), anyString(), eq("activity.vehicle.unit.deleted"), any(), any());
        verify(vehicleUnitRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("delete — SOLD unit throws BadRequestException")
    void delete_sold_throws() {
        VehicleUnitEntity entity = newEntity();
        entity.setStatus(VehicleUnitStatus.SOLD);
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("toDTO — expired warranty yields warrantyActive false")
    void toDTO_expiredWarranty_inactive() {
        VehicleUnitEntity entity = newEntity();
        entity.setWarrantyExp(LocalDate.now().minusDays(1));
        when(vehicleUnitRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));

        VehicleUnitDTO dto = service.getById(1L);

        assertThat(dto.getWarrantyActive()).isFalse();
    }
}
