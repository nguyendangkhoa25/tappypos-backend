package com.tappy.pos.service.stocktake;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.inventory.AdjustInventoryRequest;
import com.tappy.pos.model.dto.stocktake.CreateStocktakeSessionRequest;
import com.tappy.pos.model.dto.stocktake.StocktakeCountDTO;
import com.tappy.pos.model.dto.stocktake.StocktakeProductLineDTO;
import com.tappy.pos.model.dto.stocktake.StocktakeSessionDTO;
import com.tappy.pos.model.dto.stocktake.UpsertCountRequest;
import com.tappy.pos.model.entity.inventory.Inventory;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.stocktake.StocktakeCountEntity;
import com.tappy.pos.model.entity.stocktake.StocktakeSessionEntity;
import com.tappy.pos.model.enums.StocktakeStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.stocktake.StocktakeCountRepository;
import com.tappy.pos.repository.stocktake.StocktakeSessionRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.inventory.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StocktakeServiceImpl Unit Tests")
class StocktakeServiceImplTest {

    @Mock private StocktakeSessionRepository sessionRepository;
    @Mock private StocktakeCountRepository countRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks
    private StocktakeServiceImpl service;

    private StocktakeSessionEntity session;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));

        session = StocktakeSessionEntity.builder()
                .id(10L)
                .name("June count")
                .status(StocktakeStatus.IN_PROGRESS)
                .build();

        product = Product.builder()
                .id(100L)
                .name("Coca")
                .sku("SKU1")
                .barcode("8930000")
                .build();

        inventory = Inventory.builder()
                .id(500L)
                .quantityInStock(20L)
                .product(product)
                .build();

        // sensible defaults for the summary helper
        when(countRepository.countBySessionIdAndDeletedFalse(anyLong())).thenReturn(0L);
        when(countRepository.findDiscrepancies(anyLong())).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "x"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createSession ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSession starts a new session when none open")
    void createSession_new() {
        CreateStocktakeSessionRequest req = new CreateStocktakeSessionRequest();
        req.setName("June count");
        req.setNote("note");
        when(sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenAnswer(i -> {
            StocktakeSessionEntity s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        StocktakeSessionDTO dto = service.createSession(req);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getStatus()).isEqualTo(StocktakeStatus.IN_PROGRESS);
        assertThat(dto.getStartedBy()).isEqualTo("alice");
        verify(activityLogService).logAsync(eq("shop1"), eq("alice"), any(), any(), eq("STOCKTAKE"),
                anyString(), anyString(), any());
    }

    @Test
    @DisplayName("createSession resumes an already-open session")
    void createSession_resumesOpen() {
        when(sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));

        StocktakeSessionDTO dto = service.createSession(new CreateStocktakeSessionRequest());

        assertThat(dto.getId()).isEqualTo(10L);
        verify(sessionRepository, never()).save(any());
        verify(activityLogService, never()).logAsync(any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── listSessions / getSession / getActiveSession ─────────────────────────

    @Test
    @DisplayName("listSessions without status filter")
    void listSessions_noStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StocktakeSessionEntity> page = new PageImpl<>(List.of(session));
        when(sessionRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<StocktakeSessionDTO> result = service.listSessions(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(sessionRepository, never()).findByStatusAndDeletedFalseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("listSessions with status filter")
    void listSessions_withStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sessionRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(StocktakeStatus.COMPLETED, pageable))
                .thenReturn(new PageImpl<>(List.of(session)));

        Page<StocktakeSessionDTO> result = service.listSessions(StocktakeStatus.COMPLETED, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getSession includes counts")
    void getSession_found() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(10L)).thenReturn(List.of());

        StocktakeSessionDTO dto = service.getSession(10L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getCounts()).isEmpty();
    }

    @Test
    @DisplayName("getSession throws when missing")
    void getSession_notFound() {
        when(sessionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSession(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getActiveSession returns null when none active")
    void getActiveSession_none() {
        when(sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        assertThat(service.getActiveSession()).isNull();
    }

    @Test
    @DisplayName("getActiveSession returns active session")
    void getActiveSession_present() {
        when(sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        assertThat(service.getActiveSession()).isNotNull();
    }

    // ── lookup ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lookup returns product line with expected and already-counted qty")
    void lookup_found() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(productRepository.findByBarcodeAndDeletedFalse("8930000")).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.of(inventory));
        StocktakeCountEntity existing = StocktakeCountEntity.builder().countedQty(7L).build();
        when(countRepository.findBySessionIdAndProductIdAndDeletedFalse(10L, 100L)).thenReturn(Optional.of(existing));

        StocktakeProductLineDTO dto = service.lookup(10L, " 8930000 ");

        assertThat(dto.getProductId()).isEqualTo(100L);
        assertThat(dto.getExpectedQty()).isEqualTo(20L);
        assertThat(dto.getAlreadyCountedQty()).isEqualTo(7L);
    }

    @Test
    @DisplayName("lookup defaults expected qty to 0 when no inventory and not yet counted")
    void lookup_noInventory() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(productRepository.findByBarcodeAndDeletedFalse("8930000")).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.empty());
        when(countRepository.findBySessionIdAndProductIdAndDeletedFalse(10L, 100L)).thenReturn(Optional.empty());

        StocktakeProductLineDTO dto = service.lookup(10L, "8930000");

        assertThat(dto.getExpectedQty()).isEqualTo(0L);
        assertThat(dto.getAlreadyCountedQty()).isNull();
    }

    @Test
    @DisplayName("lookup rejects blank barcode")
    void lookup_blankBarcode() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> service.lookup(10L, "  "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("lookup throws when product not found")
    void lookup_productNotFound() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(productRepository.findByBarcodeAndDeletedFalse(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lookup(10L, "xxx"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── upsertCount ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("upsertCount creates a new count snapshotting expected qty")
    void upsertCount_create() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setProductId(100L);
        req.setCountedQty(18L);
        req.setNote("short by 2");
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.of(inventory));
        when(countRepository.findBySessionIdAndProductIdAndDeletedFalse(10L, 100L)).thenReturn(Optional.empty());
        when(countRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StocktakeCountDTO dto = service.upsertCount(10L, req);

        assertThat(dto.getExpectedQty()).isEqualTo(20L);
        assertThat(dto.getCountedQty()).isEqualTo(18L);
        assertThat(dto.getDifference()).isEqualTo(-2L);
        assertThat(dto.getNote()).isEqualTo("short by 2");
        assertThat(dto.getCountedBy()).isEqualTo("alice");
    }

    @Test
    @DisplayName("upsertCount updates an existing count keeping original expected qty")
    void upsertCount_update() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setProductId(100L);
        req.setCountedQty(25L);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.of(inventory));
        StocktakeCountEntity existing = StocktakeCountEntity.builder()
                .id(1L).sessionId(10L).productId(100L).expectedQty(20L).countedQty(5L).build();
        when(countRepository.findBySessionIdAndProductIdAndDeletedFalse(10L, 100L)).thenReturn(Optional.of(existing));
        when(countRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StocktakeCountDTO dto = service.upsertCount(10L, req);

        assertThat(dto.getExpectedQty()).isEqualTo(20L);
        assertThat(dto.getCountedQty()).isEqualTo(25L);
        assertThat(dto.getDifference()).isEqualTo(5L);
    }

    @Test
    @DisplayName("upsertCount resolves product by barcode when productId absent")
    void upsertCount_byBarcode() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setBarcode(" 8930000 ");
        req.setCountedQty(20L);
        when(productRepository.findByBarcodeAndDeletedFalse("8930000")).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.empty());
        when(countRepository.findBySessionIdAndProductIdAndDeletedFalse(10L, 100L)).thenReturn(Optional.empty());
        when(countRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StocktakeCountDTO dto = service.upsertCount(10L, req);

        assertThat(dto.getExpectedQty()).isEqualTo(0L);
        assertThat(dto.getDifference()).isEqualTo(20L);
    }

    @Test
    @DisplayName("upsertCount rejects when neither productId nor barcode provided")
    void upsertCount_noIdentifier() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setCountedQty(1L);
        assertThatThrownBy(() -> service.upsertCount(10L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("upsertCount throws when product id not found")
    void upsertCount_productIdNotFound() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setProductId(404L);
        req.setCountedQty(1L);
        when(productRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsertCount(10L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("upsertCount rejects when session not in progress")
    void upsertCount_sessionNotActive() {
        session.setStatus(StocktakeStatus.COMPLETED);
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        UpsertCountRequest req = new UpsertCountRequest();
        req.setProductId(100L);
        req.setCountedQty(1L);
        assertThatThrownBy(() -> service.upsertCount(10L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── deleteCount ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCount soft-deletes a count belonging to the session")
    void deleteCount_ok() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        StocktakeCountEntity count = StocktakeCountEntity.builder().id(1L).sessionId(10L).build();
        when(countRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(count));

        service.deleteCount(10L, 1L);

        assertThat(count.isDeleted()).isTrue();
        verify(countRepository).save(count);
    }

    @Test
    @DisplayName("deleteCount throws when count belongs to a different session")
    void deleteCount_wrongSession() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        StocktakeCountEntity count = StocktakeCountEntity.builder().id(1L).sessionId(99L).build();
        when(countRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(count));

        assertThatThrownBy(() -> service.deleteCount(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── discrepancies / uncounted ────────────────────────────────────────────

    @Test
    @DisplayName("getDiscrepancies maps counts with products fetched in batch")
    void getDiscrepancies() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        StocktakeCountEntity count = StocktakeCountEntity.builder()
                .id(1L).sessionId(10L).productId(100L).expectedQty(20L).countedQty(18L).difference(-2L).build();
        when(countRepository.findDiscrepancies(10L)).thenReturn(List.of(count));
        when(productRepository.findAllById(List.of(100L))).thenReturn(List.of(product));

        List<StocktakeCountDTO> result = service.getDiscrepancies(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("Coca");
        assertThat(result.get(0).getDifference()).isEqualTo(-2L);
    }

    @Test
    @DisplayName("getUncounted returns all active inventory when nothing counted yet")
    void getUncounted_nothingCounted() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(countRepository.findCountedProductIds(10L)).thenReturn(List.of());
        when(inventoryRepository.findAllProductLevelActive()).thenReturn(List.of(inventory));

        List<StocktakeProductLineDTO> result = service.getUncounted(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(100L);
        assertThat(result.get(0).getExpectedQty()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getUncounted excludes already-counted products")
    void getUncounted_someCounted() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(countRepository.findCountedProductIds(10L)).thenReturn(List.of(100L));
        Inventory other = Inventory.builder().id(501L).quantityInStock(3L).product(null).build();
        when(inventoryRepository.findProductLevelActiveExcluding(List.of(100L))).thenReturn(List.of(other));

        List<StocktakeProductLineDTO> result = service.getUncounted(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isNull();
        assertThat(result.get(0).getExpectedQty()).isEqualTo(3L);
    }

    // ── apply ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("apply adjusts inventory by delta, marks counts applied, completes session")
    void apply_adjustsAndCompletes() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        StocktakeCountEntity count = StocktakeCountEntity.builder()
                .id(1L).sessionId(10L).productId(100L).countedQty(18L).applied(false).note("n").build();
        when(countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(10L)).thenReturn(List.of(count));
        when(inventoryRepository.findProductLevelInventory(100L)).thenReturn(Optional.of(inventory));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(10L)).thenReturn(List.of(count));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        StocktakeSessionDTO dto = service.apply(10L);

        ArgumentCaptor<AdjustInventoryRequest> cap = ArgumentCaptor.forClass(AdjustInventoryRequest.class);
        verify(inventoryService).adjustByProductId(cap.capture());
        assertThat(cap.getValue().getQuantity()).isEqualTo(-2L); // 18 counted - 20 current
        assertThat(cap.getValue().getProductId()).isEqualTo(100L);
        assertThat(count.getApplied()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(StocktakeStatus.COMPLETED);
        assertThat(dto.getCompletedBy()).isEqualTo("alice");
        verify(activityLogService).logAsync(eq("shop1"), eq("alice"), any(), any(), eq("STOCKTAKE"),
                anyString(), anyString(), any());
    }

    @Test
    @DisplayName("apply skips counts already applied and zero-delta counts")
    void apply_skipsNoOp() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        StocktakeCountEntity already = StocktakeCountEntity.builder()
                .id(1L).sessionId(10L).productId(100L).countedQty(18L).applied(true).build();
        StocktakeCountEntity zeroDelta = StocktakeCountEntity.builder()
                .id(2L).sessionId(10L).productId(101L).countedQty(20L).applied(false).build();
        when(countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(10L))
                .thenReturn(List.of(already, zeroDelta));
        when(inventoryRepository.findProductLevelInventory(101L)).thenReturn(Optional.of(inventory)); // qty 20
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        service.apply(10L);

        verify(inventoryService, never()).adjustByProductId(any());
        // zeroDelta still gets marked applied
        assertThat(zeroDelta.getApplied()).isTrue();
    }

    @Test
    @DisplayName("apply rejects when session not in progress")
    void apply_notActive() {
        session.setStatus(StocktakeStatus.CANCELLED);
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> service.apply(10L)).isInstanceOf(BadRequestException.class);
    }

    // ── cancel ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel sets status CANCELLED and logs")
    void cancel_ok() {
        when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StocktakeSessionDTO dto = service.cancel(10L);

        assertThat(dto.getStatus()).isEqualTo(StocktakeStatus.CANCELLED);
        assertThat(dto.getCompletedBy()).isEqualTo("alice");
        verify(activityLogService).logAsync(eq("shop1"), eq("alice"), any(), any(), eq("STOCKTAKE"),
                anyString(), anyString(), any());
    }

    @Test
    @DisplayName("currentUser tolerates missing authentication")
    void currentUser_noAuth() {
        SecurityContextHolder.clearContext();
        when(sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenAnswer(i -> {
            StocktakeSessionEntity s = i.getArgument(0);
            s.setId(11L);
            return s;
        });

        StocktakeSessionDTO dto = service.createSession(new CreateStocktakeSessionRequest());

        assertThat(dto.getStartedBy()).isNull();
    }
}
