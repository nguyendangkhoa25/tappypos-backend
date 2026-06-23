package com.tappy.pos.service.consignment;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.consignment.ConsignmentDTO;
import com.tappy.pos.model.dto.consignment.ConsignmentItemRequest;
import com.tappy.pos.model.dto.consignment.ConsignmentRequest;
import com.tappy.pos.model.dto.consignment.ConsignmentSettlementDTO;
import com.tappy.pos.model.entity.consignment.Consignment;
import com.tappy.pos.model.entity.consignment.ConsignmentItem;
import com.tappy.pos.model.enums.ConsignmentStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.consignment.ConsignmentItemRepository;
import com.tappy.pos.repository.consignment.ConsignmentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsignmentServiceImpl Unit Tests")
class ConsignmentServiceImplTest {

    private static final String TENANT = "test-tenant";
    private static final String USER = "user1";

    @Mock private ConsignmentRepository consignmentRepository;
    @Mock private ConsignmentItemRepository consignmentItemRepository;
    @Mock private AuthContext authContext;
    @Mock private FeatureContext featureContext;
    @Mock private TenantContext tenantContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks private ConsignmentServiceImpl service;

    @Captor private ArgumentCaptor<Consignment> consignmentCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(authContext.getCurrentUsername()).thenReturn(USER);
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(any())).thenReturn("msg");
        // Repository save returns the same entity it was given.
        lenient().when(consignmentRepository.save(any(Consignment.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    // ─── Builders / fixtures ──────────────────────────────────────────────────

    private ConsignmentItemRequest itemRequest(Long productId, String name, int qty, String price) {
        ConsignmentItemRequest ir = new ConsignmentItemRequest();
        ir.setProductId(productId);
        ir.setProductName(name);
        ir.setQuantityPlaced(qty);
        ir.setUnitPrice(new BigDecimal(price));
        return ir;
    }

    private ConsignmentRequest request() {
        ConsignmentRequest r = new ConsignmentRequest();
        r.setPublisherId(42L);
        r.setPublisherName("NXB Kim Đồng");
        r.setPlacementDate(LocalDate.of(2026, 6, 23));
        r.setNote("note");
        List<ConsignmentItemRequest> items = new ArrayList<>();
        items.add(itemRequest(100L, "Sách A", 10, "20000"));
        items.add(itemRequest(200L, "Sách B", 5, "15000"));
        r.setItems(items);
        return r;
    }

    /** A persisted, owned, ACTIVE consignment in the current tenant with two items. */
    private Consignment existing(Long id, ConsignmentStatus status, String createdBy) {
        Consignment c = Consignment.builder()
                .tenantId(TENANT)
                .publisherId(42L)
                .publisherName("NXB Kim Đồng")
                .placementNumber("KG-20260623-00001")
                .placementDate(LocalDate.of(2026, 6, 23))
                .status(status)
                .note("note")
                .createdBy(createdBy)
                .build();
        c.setId(id);
        ConsignmentItem i1 = ConsignmentItem.builder()
                .tenantId(TENANT).consignment(c).productId(100L)
                .productName("Sách A").quantityPlaced(10).unitPrice(new BigDecimal("20000"))
                .build();
        ConsignmentItem i2 = ConsignmentItem.builder()
                .tenantId(TENANT).consignment(c).productId(200L)
                .productName("Sách B").quantityPlaced(5).unitPrice(new BigDecimal("15000"))
                .build();
        i1.setId(1L);
        i2.setId(2L);
        c.getItems().add(i1);
        c.getItems().add(i2);
        return c;
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: persists header + items, stamps placement number, logs activity")
    void create_success() {
        // save called twice; id assigned on first save so the stamp can use it.
        when(consignmentRepository.save(any(Consignment.class))).thenAnswer(i -> {
            Consignment c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(7L);
            }
            return c;
        });

        ConsignmentDTO dto = service.create(request());

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getStatus()).isEqualTo(ConsignmentStatus.ACTIVE);
        assertThat(dto.getStatusDisplayName()).isEqualTo(ConsignmentStatus.ACTIVE.getDisplayName());
        assertThat(dto.getPlacementNumber()).isEqualTo("KG-20260623-00007");
        assertThat(dto.getPublisherName()).isEqualTo("NXB Kim Đồng");
        assertThat(dto.getTotalQuantityPlaced()).isEqualTo(15);
        assertThat(dto.getItems()).hasSize(2);
        verify(consignmentRepository, times(2)).save(any(Consignment.class));
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(com.tappy.pos.model.enums.ActivityAction.CONSIGNMENT_CREATED),
                eq("CONSIGNMENT"), anyString(), eq("activity.consignment.created"),
                any(), any(), any());
    }

    @Test
    @DisplayName("create: persisted entity carries tenant, actor and ACTIVE status")
    void create_buildsEntityCorrectly() {
        when(consignmentRepository.save(any(Consignment.class))).thenAnswer(i -> {
            Consignment c = i.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });

        service.create(request());

        verify(consignmentRepository, times(2)).save(consignmentCaptor.capture());
        Consignment saved = consignmentCaptor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getCreatedBy()).isEqualTo(USER);
        assertThat(saved.getStatus()).isEqualTo(ConsignmentStatus.ACTIVE);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getItems().get(0).getConsignment()).isSameAs(saved);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: replaces fields + line items on an ACTIVE consignment")
    void update_success() {
        Consignment c = existing(5L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(5L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        ConsignmentRequest req = request();
        req.setPublisherName("NXB Trẻ");
        req.setNote("updated note");
        req.setItems(List.of(itemRequest(300L, "Sách C", 3, "30000")));

        ConsignmentDTO dto = service.update(5L, req);

        assertThat(dto.getPublisherName()).isEqualTo("NXB Trẻ");
        assertThat(dto.getNote()).isEqualTo("updated note");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getTotalQuantityPlaced()).isEqualTo(3);
        assertThat(c.getUpdatedBy()).isEqualTo(USER);
        verify(consignmentRepository).save(c);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(com.tappy.pos.model.enums.ActivityAction.CONSIGNMENT_UPDATED),
                eq("CONSIGNMENT"), anyString(), eq("activity.consignment.updated"),
                any(), any());
    }

    @Test
    @DisplayName("update: throws BadRequestException when consignment is not ACTIVE")
    void update_notEditable_throws() {
        Consignment c = existing(6L, ConsignmentStatus.SETTLED, USER);
        when(consignmentRepository.findById(6L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        assertThatThrownBy(() -> service.update(6L, request()))
                .isInstanceOf(BadRequestException.class);

        verify(consignmentRepository, never()).save(any());
        verify(activityLogService, never()).logAsync(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when consignment does not exist")
    void update_notFound_throws() {
        when(consignmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns DTO with items for an owned consignment")
    void getById_success() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        ConsignmentDTO dto = service.getById(8L);

        assertThat(dto.getId()).isEqualTo(8L);
        assertThat(dto.getItems()).hasSize(2);
        assertThat(dto.getTotalQuantityPlaced()).isEqualTo(15);
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when missing")
    void getById_notFound() {
        when(consignmentRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when soft-deleted")
    void getById_deleted_throws() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, USER);
        c.softDelete();
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when belongs to another tenant")
    void getById_otherTenant_throws() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, USER);
        c.setTenantId("other-tenant");
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: non-owner without VIEW_ALL gets 404 (ownership enforced)")
    void getById_nonOwner_noViewAll_throws() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, "someoneElse");
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(false);

        assertThatThrownBy(() -> service.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: non-owner WITH VIEW_ALL can read another user's consignment")
    void getById_nonOwner_withViewAll_ok() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, "someoneElse");
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        ConsignmentDTO dto = service.getById(8L);

        assertThat(dto.getCreatedBy()).isEqualTo("someoneElse");
    }

    @Test
    @DisplayName("getById: createdBy null bypasses ownership check even without VIEW_ALL")
    void getById_nullCreatedBy_ok() {
        Consignment c = existing(8L, ConsignmentStatus.ACTIVE, null);
        when(consignmentRepository.findById(8L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(false);

        ConsignmentDTO dto = service.getById(8L);

        assertThat(dto.getId()).isEqualTo(8L);
    }

    // ─── search ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: with VIEW_ALL passes null createdBy and maps results")
    void search_viewAll_passesNullCreatedBy() {
        Pageable pageable = PageRequest.of(0, 10);
        Consignment c = existing(1L, ConsignmentStatus.ACTIVE, USER);
        Page<Consignment> page = new PageImpl<>(List.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);
        when(consignmentRepository.search(eq(TENANT), eq("ACTIVE"), eq(null), eq(pageable)))
                .thenReturn(page);

        Page<ConsignmentDTO> result = service.search(ConsignmentStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        // includeItems=false in list view → items not populated
        assertThat(result.getContent().get(0).getItems()).isNull();
        verify(consignmentRepository).search(TENANT, "ACTIVE", null, pageable);
    }

    @Test
    @DisplayName("search: without VIEW_ALL scopes to current user and null status")
    void search_noViewAll_scopesToUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Consignment> page = new PageImpl<>(List.of());
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(false);
        when(consignmentRepository.search(eq(TENANT), eq(null), eq(USER), eq(pageable)))
                .thenReturn(page);

        Page<ConsignmentDTO> result = service.search(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(consignmentRepository).search(TENANT, null, USER, pageable);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes the consignment and logs cancellation")
    void delete_success() {
        Consignment c = existing(3L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(3L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        service.delete(3L);

        assertThat(c.isDeleted()).isTrue();
        verify(consignmentRepository).save(c);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(com.tappy.pos.model.enums.ActivityAction.CONSIGNMENT_CANCELLED),
                eq("CONSIGNMENT"), anyString(), eq("activity.consignment.cancelled"),
                any(), any());
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when missing")
    void delete_notFound() {
        when(consignmentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getSettlement ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getSettlement: aggregates sold quantities and amounts due per line")
    void getSettlement_success() {
        Consignment c = existing(4L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(4L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);
        // product 100 sold 4 units, product 200 sold 2 units.
        List<Object[]> rows = List.of(
                new Object[]{100L, 4L},
                new Object[]{200L, 2L});
        when(consignmentItemRepository.sumSoldByProductIds(eq(TENANT), anyList(), any(), any()))
                .thenReturn(rows);

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        ConsignmentSettlementDTO dto = service.getSettlement(4L, from, to);

        assertThat(dto.getConsignmentId()).isEqualTo(4L);
        assertThat(dto.getFrom()).isEqualTo(from);
        assertThat(dto.getTo()).isEqualTo(to);
        assertThat(dto.getTotalQuantitySold()).isEqualTo(6);
        // 4*20000 + 2*15000 = 80000 + 30000 = 110000
        assertThat(dto.getTotalAmountDue()).isEqualByComparingTo("110000");
        assertThat(dto.getLines()).hasSize(2);
        assertThat(dto.getLines().get(0).getQuantitySold()).isEqualTo(4);
        assertThat(dto.getLines().get(0).getAmountDue()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("getSettlement: product not in sold map defaults to zero sold/due")
    void getSettlement_noSalesForProduct() {
        Consignment c = existing(4L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(4L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);
        // only product 100 has sales
        when(consignmentItemRepository.sumSoldByProductIds(eq(TENANT), anyList(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 3L}));

        ConsignmentSettlementDTO dto = service.getSettlement(4L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(dto.getTotalQuantitySold()).isEqualTo(3);
        assertThat(dto.getLines().get(1).getQuantitySold()).isEqualTo(0);
        assertThat(dto.getLines().get(1).getAmountDue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getSettlement: skips the sold-query when no items have a product id")
    void getSettlement_noProductIds_skipsQuery() {
        Consignment c = existing(4L, ConsignmentStatus.ACTIVE, USER);
        // null out product ids on all items so productIds is empty
        c.getItems().forEach(i -> i.setProductId(null));
        when(consignmentRepository.findById(4L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        ConsignmentSettlementDTO dto = service.getSettlement(4L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(dto.getTotalQuantitySold()).isEqualTo(0);
        assertThat(dto.getTotalAmountDue()).isEqualByComparingTo("0");
        verify(consignmentItemRepository, never())
                .sumSoldByProductIds(any(), anyList(), any(), any());
    }

    // ─── settle ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("settle: transitions to SETTLED, stamps settlement fields, logs activity")
    void settle_success() {
        Consignment c = existing(9L, ConsignmentStatus.ACTIVE, USER);
        when(consignmentRepository.findById(9L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);
        when(consignmentItemRepository.sumSoldByProductIds(eq(TENANT), anyList(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 1L}));

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        ConsignmentDTO dto = service.settle(9L, from, to);

        assertThat(dto.getStatus()).isEqualTo(ConsignmentStatus.SETTLED);
        assertThat(c.getStatus()).isEqualTo(ConsignmentStatus.SETTLED);
        assertThat(c.getSettledFrom()).isEqualTo(from);
        assertThat(c.getSettledTo()).isEqualTo(to);
        assertThat(c.getSettledDate()).isNotNull();
        assertThat(c.getSettledAmount()).isEqualByComparingTo("20000"); // 1 * 20000
        assertThat(c.getUpdatedBy()).isEqualTo(USER);
        verify(consignmentRepository).save(c);
        verify(activityLogService).logAsync(eq(TENANT), eq(USER), any(),
                eq(com.tappy.pos.model.enums.ActivityAction.CONSIGNMENT_SETTLED),
                eq("CONSIGNMENT"), anyString(), eq("activity.consignment.settled"),
                any(), any(), any());
    }

    @Test
    @DisplayName("settle: throws ResourceNotFoundException when missing")
    void settle_notFound() {
        when(consignmentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.settle(9L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── toDTO branch: quantityPlaced null ────────────────────────────────────

    @Test
    @DisplayName("getById: handles null quantityPlaced on an item (totals as 0)")
    void getById_nullQuantityPlaced() {
        Consignment c = existing(10L, ConsignmentStatus.ACTIVE, USER);
        c.getItems().get(0).setQuantityPlaced(null);
        when(consignmentRepository.findById(10L)).thenReturn(Optional.of(c));
        when(featureContext.hasFeature("CONSIGNMENT_VIEW_ALL")).thenReturn(true);

        ConsignmentDTO dto = service.getById(10L);

        // only the second item (5) counts; first is null → 0
        assertThat(dto.getTotalQuantityPlaced()).isEqualTo(5);
    }
}
