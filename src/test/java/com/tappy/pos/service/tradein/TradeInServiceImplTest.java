package com.tappy.pos.service.tradein;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tradein.CreateTradeInRequest;
import com.tappy.pos.model.dto.tradein.TradeInDTO;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductType;
import com.tappy.pos.model.entity.tradein.TradeInEntity;
import com.tappy.pos.model.entity.vehicle.VehicleUnitEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.TradeInMode;
import com.tappy.pos.model.enums.TradeInStatus;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.product.ProductTypeRepository;
import com.tappy.pos.repository.tradein.TradeInRepository;
import com.tappy.pos.repository.vehicle.VehicleUnitRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
@DisplayName("TradeInServiceImpl Unit Tests")
class TradeInServiceImplTest {

    private static final String TENANT = "test-tenant";
    private static final String USER = "user1";
    private static final String VIEW_ALL = "TRADE_IN_VIEW_ALL";

    @Mock private TradeInRepository tradeInRepository;
    @Mock private VehicleUnitRepository vehicleUnitRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductTypeRepository productTypeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private FeatureContext featureContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks private TradeInServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(authContext.getCurrentUsername()).thenReturn(USER);
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(any())).thenReturn("msg");
        lenient().when(messageService.getMessage(any(), any(Object[].class))).thenReturn("msg");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateTradeInRequest baseRequest() {
        CreateTradeInRequest r = new CreateTradeInRequest();
        r.setBrand("Honda");
        r.setModel("Wave");
        r.setVehicleType("MOTORBIKE");
        r.setTradeValue(new BigDecimal("10000000"));
        return r;
    }

    private ProductType productType() {
        ProductType type = new ProductType();
        type.setId(7L);
        return type;
    }

    private void stubResaleProductType() {
        when(productTypeRepository.findByCode(anyString())).thenReturn(Optional.of(productType()));
    }

    private void stubProductSaveReturnsWithId(Long id) {
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(id);
            return p;
        });
    }

    private void stubUnitSaveReturnsWithId(Long id) {
        when(vehicleUnitRepository.save(any(VehicleUnitEntity.class))).thenAnswer(i -> {
            VehicleUnitEntity u = i.getArgument(0);
            u.setId(id);
            return u;
        });
    }

    private void stubTradeInSaveReturnsWithId(Long id) {
        when(tradeInRepository.save(any(TradeInEntity.class))).thenAnswer(i -> {
            TradeInEntity e = i.getArgument(0);
            if (e.getId() == null) e.setId(id);
            return e;
        });
    }

    private TradeInEntity existing(TradeInStatus status) {
        return TradeInEntity.builder()
                .id(100L)
                .tenantId(TENANT)
                .tradeInNumber("TI-1")
                .tradeValue(new BigDecimal("5000000"))
                .mode(TradeInMode.STANDALONE)
                .status(status)
                .createdBy(USER)
                .build();
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: NETTED default mode + netAmount computed when newPrice present")
    void create_netted_default_computesNetAmount() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(null); // → defaults to NETTED
        r.setNewPrice(new BigDecimal("30000000"));
        r.setSellerName("Anh Ba");
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        TradeInDTO dto = service.create(r);

        assertThat(dto.getMode()).isEqualTo(TradeInMode.NETTED);
        assertThat(dto.getStatus()).isEqualTo(TradeInStatus.COMPLETED);
        assertThat(dto.getResaleProductId()).isEqualTo(11L);
        assertThat(dto.getResaleUnitId()).isEqualTo(22L);
        assertThat(dto.getNetAmount()).isEqualByComparingTo(new BigDecimal("20000000"));
        assertThat(dto.getSellerName()).isEqualTo("Anh Ba");
        // seller name already provided → no customer lookup
        verify(customerRepository, never()).findByIdActiveAndTenantId(any(), any());
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(ActivityAction.TRADE_IN_CREATED), eq("TRADE_IN"), anyString(),
                anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create: NETTED but newPrice null → netAmount stays null")
    void create_netted_noNewPrice_netAmountNull() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.NETTED);
        r.setNewPrice(null);
        r.setSellerName("Khach");
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        TradeInDTO dto = service.create(r);

        assertThat(dto.getNetAmount()).isNull();
    }

    @Test
    @DisplayName("create: STANDALONE mode → netAmount null even with newPrice")
    void create_standalone_netAmountNull() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setNewPrice(new BigDecimal("30000000"));
        r.setSellerName("Khach");
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        TradeInDTO dto = service.create(r);

        assertThat(dto.getMode()).isEqualTo(TradeInMode.STANDALONE);
        assertThat(dto.getNetAmount()).isNull();
    }

    @Test
    @DisplayName("create: blank seller name with sellerId → resolved from customer record")
    void create_resolvesSellerNameFromCustomer() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("  "); // blank
        r.setSellerId(55L);
        Customer c = Customer.builder().name("Nguyen Van A").build();
        when(customerRepository.findByIdActiveAndTenantId(55L, TENANT)).thenReturn(Optional.of(c));
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        TradeInDTO dto = service.create(r);

        assertThat(dto.getSellerName()).isEqualTo("Nguyen Van A");
        verify(customerRepository).findByIdActiveAndTenantId(55L, TENANT);
    }

    @Test
    @DisplayName("create: blank seller name with sellerId but customer not found → name null")
    void create_sellerIdNotFound_nameNull() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName(null);
        r.setSellerId(55L);
        when(customerRepository.findByIdActiveAndTenantId(55L, TENANT)).thenReturn(Optional.empty());
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        TradeInDTO dto = service.create(r);

        assertThat(dto.getSellerName()).isNull();
    }

    @Test
    @DisplayName("create: resalePrice provided → used for unit currentValue and resale product price")
    void create_withResalePrice() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        r.setResalePrice(new BigDecimal("12500000"));
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        service.create(r);

        ArgumentCaptor<VehicleUnitEntity> unitCap = ArgumentCaptor.forClass(VehicleUnitEntity.class);
        verify(vehicleUnitRepository).save(unitCap.capture());
        assertThat(unitCap.getValue().getCurrentValue()).isEqualByComparingTo(new BigDecimal("12500000"));
        assertThat(unitCap.getValue().getPurchasePrice()).isEqualByComparingTo(new BigDecimal("10000000"));
        assertThat(unitCap.getValue().getStatus()).isEqualTo(VehicleUnitStatus.TRADED_IN);

        ArgumentCaptor<Product> prodCap = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(prodCap.capture());
        assertThat(prodCap.getValue().getPrice()).isEqualByComparingTo(new BigDecimal("12500000"));
        assertThat(prodCap.getValue().getCostPrice()).isEqualByComparingTo(new BigDecimal("10000000"));
    }

    @Test
    @DisplayName("create: resalePrice null → currentValue falls back to tradeValue")
    void create_resalePriceNull_fallsBackToTradeValue() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        r.setResalePrice(null);
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        service.create(r);

        ArgumentCaptor<VehicleUnitEntity> unitCap = ArgumentCaptor.forClass(VehicleUnitEntity.class);
        verify(vehicleUnitRepository).save(unitCap.capture());
        assertThat(unitCap.getValue().getCurrentValue()).isEqualByComparingTo(new BigDecimal("10000000"));
    }

    @Test
    @DisplayName("create: blank vehicleType → defaults product type code MOTORBIKE")
    void create_blankVehicleType_defaultsMotorbike() {
        CreateTradeInRequest r = baseRequest();
        r.setVehicleType("   "); // blank → MOTORBIKE
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        when(productTypeRepository.findByCode("MOTORBIKE")).thenReturn(Optional.of(productType()));
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        service.create(r);

        verify(productTypeRepository).findByCode("MOTORBIKE");
    }

    @Test
    @DisplayName("create: blank brand and model → resale product name defaults to 'Xe cũ thu vào'")
    void create_blankBrandModel_defaultName() {
        CreateTradeInRequest r = baseRequest();
        r.setBrand(null);
        r.setModel(null);
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        service.create(r);

        ArgumentCaptor<Product> prodCap = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(prodCap.capture());
        assertThat(prodCap.getValue().getName()).isEqualTo("Xe cũ thu vào (đã qua sử dụng)");
    }

    @Test
    @DisplayName("create: brand+model present → resale product name composed from them")
    void create_composesProductNameFromBrandModel() {
        CreateTradeInRequest r = baseRequest(); // Honda / Wave
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        stubResaleProductType();
        stubProductSaveReturnsWithId(11L);
        stubUnitSaveReturnsWithId(22L);
        stubTradeInSaveReturnsWithId(33L);

        service.create(r);

        ArgumentCaptor<Product> prodCap = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(prodCap.capture());
        assertThat(prodCap.getValue().getName()).isEqualTo("Honda Wave (đã qua sử dụng)");
    }

    @Test
    @DisplayName("create: product type code not seeded → BadRequestException")
    void create_unseededProductType_throws() {
        CreateTradeInRequest r = baseRequest();
        r.setMode(TradeInMode.STANDALONE);
        r.setSellerName("Khach");
        when(productTypeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BadRequestException.class);
        verify(productRepository, never()).save(any());
    }

    // ── getById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: not found → ResourceNotFoundException")
    void getById_notFound_throws() {
        when(tradeInRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: VIEW_ALL feature present → returns DTO regardless of owner")
    void getById_viewAll_returns() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setCreatedBy("someoneElse");
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);

        TradeInDTO dto = service.getById(100L);

        assertThat(dto.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getById: no VIEW_ALL but caller is owner → returns DTO")
    void getById_ownerNoViewAll_returns() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED); // createdBy = USER
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);

        TradeInDTO dto = service.getById(100L);

        assertThat(dto.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getById: no VIEW_ALL and not owner → ResourceNotFoundException")
    void getById_notOwnerNoViewAll_throws() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setCreatedBy("someoneElse");
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);

        assertThatThrownBy(() -> service.getById(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── search ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: VIEW_ALL present → findAllActive (all tenant rows)")
    void search_viewAll_usesFindAllActive() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TradeInEntity> page = new PageImpl<>(List.of(existing(TradeInStatus.COMPLETED)));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(true);
        when(tradeInRepository.findAllActive(TradeInStatus.COMPLETED, pageable)).thenReturn(page);

        Page<TradeInDTO> result = service.search(TradeInStatus.COMPLETED, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(tradeInRepository).findAllActive(TradeInStatus.COMPLETED, pageable);
        verify(tradeInRepository, never()).findAllActiveByCreatedBy(any(), any(), any());
    }

    @Test
    @DisplayName("search: no VIEW_ALL → findAllActiveByCreatedBy (own rows only)")
    void search_noViewAll_usesFindByCreatedBy() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TradeInEntity> page = new PageImpl<>(List.of(existing(TradeInStatus.COMPLETED)));
        when(featureContext.hasFeature(VIEW_ALL)).thenReturn(false);
        when(tradeInRepository.findAllActiveByCreatedBy(null, USER, pageable)).thenReturn(page);

        Page<TradeInDTO> result = service.search(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(tradeInRepository).findAllActiveByCreatedBy(null, USER, pageable);
        verify(tradeInRepository, never()).findAllActive(any(), any());
    }

    // ── cancel ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: not found → ResourceNotFoundException")
    void cancel_notFound_throws() {
        when(tradeInRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1L, "reason"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancel: already cancelled → BadRequestException")
    void cancel_alreadyCancelled_throws() {
        TradeInEntity e = existing(TradeInStatus.CANCELLED);
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.cancel(100L, "reason"))
                .isInstanceOf(BadRequestException.class);
        verify(tradeInRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel: success with resale unit not sold → unit set DAMAGED + entity CANCELLED")
    void cancel_success_unitNotSold_setDamaged() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setResaleUnitId(22L);
        VehicleUnitEntity unit = VehicleUnitEntity.builder()
                .id(22L).status(VehicleUnitStatus.TRADED_IN).build();
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(vehicleUnitRepository.findByIdAndDeletedFalse(22L)).thenReturn(Optional.of(unit));
        when(tradeInRepository.save(any(TradeInEntity.class))).thenAnswer(i -> i.getArgument(0));

        TradeInDTO dto = service.cancel(100L, "khach doi y");

        assertThat(dto.getStatus()).isEqualTo(TradeInStatus.CANCELLED);
        assertThat(dto.getCanceledReason()).isEqualTo("khach doi y");
        assertThat(unit.getStatus()).isEqualTo(VehicleUnitStatus.DAMAGED);
        verify(vehicleUnitRepository).save(unit);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(ActivityAction.TRADE_IN_CANCELLED), eq("TRADE_IN"), anyString(),
                anyString(), any(), any());
    }

    @Test
    @DisplayName("cancel: resale unit already SOLD → not changed, not re-saved")
    void cancel_unitSold_notTouched() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setResaleUnitId(22L);
        VehicleUnitEntity unit = VehicleUnitEntity.builder()
                .id(22L).status(VehicleUnitStatus.SOLD).build();
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(vehicleUnitRepository.findByIdAndDeletedFalse(22L)).thenReturn(Optional.of(unit));
        when(tradeInRepository.save(any(TradeInEntity.class))).thenAnswer(i -> i.getArgument(0));

        TradeInDTO dto = service.cancel(100L, "reason");

        assertThat(dto.getStatus()).isEqualTo(TradeInStatus.CANCELLED);
        assertThat(unit.getStatus()).isEqualTo(VehicleUnitStatus.SOLD);
        verify(vehicleUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel: resaleUnitId null → skips unit lookup")
    void cancel_noResaleUnit_skipsLookup() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setResaleUnitId(null);
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(tradeInRepository.save(any(TradeInEntity.class))).thenAnswer(i -> i.getArgument(0));

        TradeInDTO dto = service.cancel(100L, "reason");

        assertThat(dto.getStatus()).isEqualTo(TradeInStatus.CANCELLED);
        verify(vehicleUnitRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("cancel: resale unit not found → entity still cancelled, no unit save")
    void cancel_unitNotFound_stillCancels() {
        TradeInEntity e = existing(TradeInStatus.COMPLETED);
        e.setResaleUnitId(22L);
        when(tradeInRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(e));
        when(vehicleUnitRepository.findByIdAndDeletedFalse(22L)).thenReturn(Optional.empty());
        when(tradeInRepository.save(any(TradeInEntity.class))).thenAnswer(i -> i.getArgument(0));

        TradeInDTO dto = service.cancel(100L, "reason");

        assertThat(dto.getStatus()).isEqualTo(TradeInStatus.CANCELLED);
        verify(vehicleUnitRepository, never()).save(any());
        verify(tradeInRepository, times(1)).save(any());
    }
}
