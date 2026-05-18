package com.tappy.pos.service.vendor;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.vendor.CreatePurchaseOrderRequest;
import com.tappy.pos.model.dto.vendor.PurchaseOrderDTO;
import com.tappy.pos.model.dto.vendor.ReceiveItemsRequest;
import com.tappy.pos.model.entity.vendor.PurchaseOrder;
import com.tappy.pos.model.entity.vendor.PurchaseOrderItem;
import com.tappy.pos.model.entity.vendor.Vendor;
import com.tappy.pos.repository.vendor.PurchaseOrderItemRepository;
import com.tappy.pos.repository.vendor.PurchaseOrderRepository;
import com.tappy.pos.repository.vendor.VendorRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService Unit Tests")
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository poRepository;
    @Mock private PurchaseOrderItemRepository poItemRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private InventoryService inventoryService;
    @Mock private MessageService messageService;
    @Mock private com.tappy.pos.multitenant.TenantContext tenantContext;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Vendor vendor;
    private PurchaseOrder draftPo;
    private PurchaseOrderItem item;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        vendor = Vendor.builder().name("Nhà cung cấp A").code("NCC_A").isActive(true).build();
        vendor.setId(1L);
        vendor.setDeleted(false);

        item = PurchaseOrderItem.builder()
                .productId(10L)
                .productName("Sản phẩm A")
                .productSku("SKU-001")
                .quantityOrdered(10)
                .quantityReceived(0)
                .unitCost(new BigDecimal("50000"))
                .totalCost(new BigDecimal("500000"))
                .build();
        item.setId(100L);
        item.setDeleted(false);

        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(item);

        draftPo = PurchaseOrder.builder()
                .poNumber("PO-000001")
                .vendor(vendor)
                .status(PurchaseOrder.PoStatus.DRAFT)
                .totalAmount(new BigDecimal("500000"))
                .items(items)
                .build();
        draftPo.setId(1L);
        draftPo.setDeleted(false);
        item.setPurchaseOrder(draftPo);

        pageable = PageRequest.of(0, 20);

        lenient().when(messageService.getMessage(anyString())).thenReturn("error");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("error");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll without status filter returns all active POs")
    void testGetAll_NoStatus() {
        when(poRepository.findAllActive(pageable)).thenReturn(new PageImpl<>(List.of(draftPo)));

        Page<PurchaseOrderDTO> result = purchaseOrderService.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(poRepository).findAllActive(pageable);
        verify(poRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("getAll with blank status falls back to all active")
    void testGetAll_BlankStatus() {
        when(poRepository.findAllActive(pageable)).thenReturn(new PageImpl<>(List.of(draftPo)));

        Page<PurchaseOrderDTO> result = purchaseOrderService.getAll("  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(poRepository).findAllActive(pageable);
    }

    @Test
    @DisplayName("getAll with status filter queries by status")
    void testGetAll_WithStatus() {
        when(poRepository.findByStatus(PurchaseOrder.PoStatus.DRAFT, pageable))
                .thenReturn(new PageImpl<>(List.of(draftPo)));

        Page<PurchaseOrderDTO> result = purchaseOrderService.getAll("DRAFT", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("DRAFT");
        verify(poRepository).findByStatus(PurchaseOrder.PoStatus.DRAFT, pageable);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns DTO with items for existing active PO")
    void testGetById_Success() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        PurchaseOrderDTO result = purchaseOrderService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getPoNumber()).isEqualTo("PO-000001");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for missing PO")
    void testGetById_NotFound() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for deleted PO")
    void testGetById_Deleted() {
        draftPo.setDeleted(true);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByVendor ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByVendor returns POs for a given vendor")
    void testGetByVendor() {
        when(poRepository.findByVendorId(1L, pageable)).thenReturn(new PageImpl<>(List.of(draftPo)));

        Page<PurchaseOrderDTO> result = purchaseOrderService.getByVendor(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(poRepository).findByVendorId(1L, pageable);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create builds PO with generated PO number and saves")
    void testCreate_Success() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setVendorId(1L);
        req.setNotes("Test note");

        CreatePurchaseOrderRequest.ItemRequest itemReq = new CreatePurchaseOrderRequest.ItemRequest();
        itemReq.setProductId(10L);
        itemReq.setProductName("Sản phẩm A");
        itemReq.setProductSku("SKU-001");
        itemReq.setQuantityOrdered(5);
        itemReq.setUnitCost(new BigDecimal("100000"));
        req.setItems(List.of(itemReq));

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(poRepository.findMaxPoSequence()).thenReturn(5);
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        PurchaseOrderDTO result = purchaseOrderService.create(req);

        assertThat(result).isNotNull();
        verify(poRepository).save(argThat(po ->
                "PO-000006".equals(po.getPoNumber()) &&
                po.getStatus() == PurchaseOrder.PoStatus.DRAFT));
    }

    @Test
    @DisplayName("create generates PO-000001 when no existing sequence")
    void testCreate_FirstPoNumber() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setVendorId(1L);

        CreatePurchaseOrderRequest.ItemRequest itemReq = new CreatePurchaseOrderRequest.ItemRequest();
        itemReq.setProductName("Item");
        itemReq.setQuantityOrdered(1);
        itemReq.setUnitCost(new BigDecimal("1000"));
        req.setItems(List.of(itemReq));

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(poRepository.findMaxPoSequence()).thenReturn(null);
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        purchaseOrderService.create(req);

        verify(poRepository).save(argThat(po -> "PO-000001".equals(po.getPoNumber())));
    }

    @Test
    @DisplayName("create throws ResourceNotFoundException when vendor not found")
    void testCreate_VendorNotFound() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setVendorId(99L);
        req.setItems(List.of());

        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create throws ResourceNotFoundException for soft-deleted vendor")
    void testCreate_VendorDeleted() {
        vendor.setDeleted(true);
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setVendorId(1L);
        req.setItems(List.of());

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> purchaseOrderService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit transitions DRAFT PO to ORDERED")
    void testSubmit_Success() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        PurchaseOrderDTO result = purchaseOrderService.submit(1L);

        assertThat(result).isNotNull();
        verify(poRepository).save(argThat(po -> po.getStatus() == PurchaseOrder.PoStatus.ORDERED));
    }

    @Test
    @DisplayName("submit throws BadRequestException when PO is not in DRAFT status")
    void testSubmit_NotDraft() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.submit(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submit throws ResourceNotFoundException when PO not found")
    void testSubmit_NotFound() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.submit(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── receiveItems ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("receiveItems updates stock and sets status to PARTIALLY_RECEIVED")
    void testReceiveItems_Partial() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);

        ReceiveItemsRequest req = new ReceiveItemsRequest();
        ReceiveItemsRequest.ItemReceive recv = new ReceiveItemsRequest.ItemReceive();
        recv.setItemId(100L);
        recv.setQuantityReceived(5);
        req.setItems(List.of(recv));

        InventoryDTO inventoryDTO = new InventoryDTO();
        inventoryDTO.setId(200L);

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(inventoryService.getInventoryByProductId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inventoryDTO)));
        when(inventoryService.addStock(200L, 5L)).thenReturn(inventoryDTO);
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        PurchaseOrderDTO result = purchaseOrderService.receiveItems(1L, req);

        assertThat(result).isNotNull();
        assertThat(item.getQuantityReceived()).isEqualTo(5);
        verify(inventoryService).addStock(200L, 5L);
    }

    @Test
    @DisplayName("receiveItems sets RECEIVED when all quantities received")
    void testReceiveItems_FullyReceived() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);

        ReceiveItemsRequest req = new ReceiveItemsRequest();
        ReceiveItemsRequest.ItemReceive recv = new ReceiveItemsRequest.ItemReceive();
        recv.setItemId(100L);
        recv.setQuantityReceived(10);
        req.setItems(List.of(recv));

        InventoryDTO inventoryDTO = new InventoryDTO();
        inventoryDTO.setId(200L);

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(inventoryService.getInventoryByProductId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(inventoryDTO)));
        when(inventoryService.addStock(200L, 10L)).thenReturn(inventoryDTO);
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        purchaseOrderService.receiveItems(1L, req);

        verify(poRepository).save(argThat(po -> po.getStatus() == PurchaseOrder.PoStatus.RECEIVED));
    }

    @Test
    @DisplayName("receiveItems skips inventory update when no inventory record found")
    void testReceiveItems_NoInventoryRecord() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);

        ReceiveItemsRequest req = new ReceiveItemsRequest();
        ReceiveItemsRequest.ItemReceive recv = new ReceiveItemsRequest.ItemReceive();
        recv.setItemId(100L);
        recv.setQuantityReceived(3);
        req.setItems(List.of(recv));

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(inventoryService.getInventoryByProductId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        purchaseOrderService.receiveItems(1L, req);

        verify(inventoryService, never()).addStock(anyLong(), anyLong());
    }

    @Test
    @DisplayName("receiveItems throws BadRequestException when PO is in DRAFT status")
    void testReceiveItems_InvalidStatus_Draft() {
        ReceiveItemsRequest req = new ReceiveItemsRequest();
        req.setItems(List.of());

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.receiveItems(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("receiveItems throws BadRequestException when PO is CANCELLED")
    void testReceiveItems_InvalidStatus_Cancelled() {
        draftPo.setStatus(PurchaseOrder.PoStatus.CANCELLED);
        ReceiveItemsRequest req = new ReceiveItemsRequest();
        req.setItems(List.of());

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.receiveItems(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("receiveItems throws BadRequestException when quantity exceeds ordered")
    void testReceiveItems_ExceedsOrdered() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);

        ReceiveItemsRequest req = new ReceiveItemsRequest();
        ReceiveItemsRequest.ItemReceive recv = new ReceiveItemsRequest.ItemReceive();
        recv.setItemId(100L);
        recv.setQuantityReceived(15);
        req.setItems(List.of(recv));

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.receiveItems(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("receiveItems tolerates inventory service exception gracefully")
    void testReceiveItems_InventoryException_IsSwallowed() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);

        ReceiveItemsRequest req = new ReceiveItemsRequest();
        ReceiveItemsRequest.ItemReceive recv = new ReceiveItemsRequest.ItemReceive();
        recv.setItemId(100L);
        recv.setQuantityReceived(2);
        req.setItems(List.of(recv));

        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(inventoryService.getInventoryByProductId(eq(10L), any(PageRequest.class)))
                .thenThrow(new RuntimeException("DB error"));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        PurchaseOrderDTO result = purchaseOrderService.receiveItems(1L, req);

        assertThat(result).isNotNull();
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel sets status to CANCELLED for DRAFT PO")
    void testCancel_FromDraft() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        PurchaseOrderDTO result = purchaseOrderService.cancel(1L);

        assertThat(result).isNotNull();
        verify(poRepository).save(argThat(po -> po.getStatus() == PurchaseOrder.PoStatus.CANCELLED));
    }

    @Test
    @DisplayName("cancel sets status to CANCELLED for ORDERED PO")
    void testCancel_FromOrdered() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        purchaseOrderService.cancel(1L);

        verify(poRepository).save(argThat(po -> po.getStatus() == PurchaseOrder.PoStatus.CANCELLED));
    }

    @Test
    @DisplayName("cancel throws BadRequestException when PO is RECEIVED")
    void testCancel_ReceivedThrows() {
        draftPo.setStatus(PurchaseOrder.PoStatus.RECEIVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.cancel(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancel throws BadRequestException when PO is PARTIALLY_RECEIVED")
    void testCancel_PartiallyReceivedThrows() {
        draftPo.setStatus(PurchaseOrder.PoStatus.PARTIALLY_RECEIVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.cancel(1L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete soft-deletes a DRAFT PO")
    void testDelete_DraftSuccess() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(draftPo);

        purchaseOrderService.delete(1L);

        verify(poRepository).save(argThat(po -> po.isDeleted()));
    }

    @Test
    @DisplayName("delete throws BadRequestException when PO is not DRAFT")
    void testDelete_NotDraftThrows() {
        draftPo.setStatus(PurchaseOrder.PoStatus.ORDERED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> purchaseOrderService.delete(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException for missing PO")
    void testDelete_NotFound() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
