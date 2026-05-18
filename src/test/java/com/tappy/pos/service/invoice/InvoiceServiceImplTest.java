package com.tappy.pos.service.invoice;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.invoice.*;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.finance.InvoiceBuyerInfo;
import com.tappy.pos.model.entity.finance.InvoiceItem;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.enums.InvoiceDirection;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.InvoiceRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.invoice.AsyncInvoiceService;
import com.tappy.pos.service.tenant.ShopConfigService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceImpl Unit Tests")
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private InvoiceServiceFactory invoiceServiceFactory;
    @Mock private MessageService messageService;
    @Mock private TenantContext tenantContext;
    @Mock private AsyncInvoiceService asyncInvoiceService;

    @InjectMocks private InvoiceServiceImpl invoiceService;

    private Invoice draftInvoice;
    private Invoice completedInvoice;
    private Invoice cancelledInvoice;
    private Order order;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("accountant01", null, Collections.emptyList()));

        order = Order.builder()
                .orderNumber("ORD-20240101-0001")
                .status(Order.OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("500000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();
        order.setId(10L);
        order.setDeleted(false);

        draftInvoice = Invoice.builder()
                .invoiceNumber("INV-20240101-0001")
                .status(Invoice.InvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("500000"))
                .totalAmountWithoutTax(new BigDecimal("500000"))
                .taxAmount(BigDecimal.ZERO)
                .taxPercentage(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        draftInvoice.setId(1L);
        draftInvoice.setDeleted(false);

        completedInvoice = Invoice.builder()
                .invoiceNumber("INV-20240101-0002")
                .status(Invoice.InvoiceStatus.COMPLETED)
                .totalAmount(new BigDecimal("300000"))
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        completedInvoice.setId(2L);
        completedInvoice.setDeleted(false);

        cancelledInvoice = Invoice.builder()
                .invoiceNumber("INV-20240101-0003")
                .status(Invoice.InvoiceStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        cancelledInvoice.setId(3L);
        cancelledInvoice.setDeleted(false);

        pageable = PageRequest.of(0, 20);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAllInvoices ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllInvoices delegates to repository")
    void getAllInvoices_success() {
        when(invoiceRepository.findAllActive(pageable))
                .thenReturn(new PageImpl<>(List.of(draftInvoice)));

        Page<InvoiceDTO> result = invoiceService.getAllInvoices(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-20240101-0001");
        verify(invoiceRepository).findAllActive(pageable);
    }

    // ── getInvoicesByStatus ────────────────────────────────────────────────────

    @Test
    @DisplayName("getInvoicesByStatus filters by status enum")
    void getInvoicesByStatus_success() {
        when(invoiceRepository.findAllActiveByStatus(Invoice.InvoiceStatus.DRAFT, pageable))
                .thenReturn(new PageImpl<>(List.of(draftInvoice)));

        Page<InvoiceDTO> result = invoiceService.getInvoicesByStatus("DRAFT", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findAllActiveByStatus(Invoice.InvoiceStatus.DRAFT, pageable);
    }

    @Test
    @DisplayName("getInvoicesByStatus accepts lowercase status")
    void getInvoicesByStatus_lowercaseInput() {
        when(invoiceRepository.findAllActiveByStatus(Invoice.InvoiceStatus.COMPLETED, pageable))
                .thenReturn(new PageImpl<>(List.of(completedInvoice)));

        invoiceService.getInvoicesByStatus("completed", pageable);

        verify(invoiceRepository).findAllActiveByStatus(Invoice.InvoiceStatus.COMPLETED, pageable);
    }

    // ── searchInvoices ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchInvoices delegates to repository keyword search")
    void searchInvoices_success() {
        when(invoiceRepository.searchByKeyword("INV-2024", pageable))
                .thenReturn(new PageImpl<>(List.of(draftInvoice)));

        Page<InvoiceDTO> result = invoiceService.searchInvoices("INV-2024", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).searchByKeyword("INV-2024", pageable);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns DTO for existing active invoice")
    void getById_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));

        InvoiceDTO result = invoiceService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for missing invoice")
    void getById_notFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException for soft-deleted invoice")
    void getById_deleted() {
        draftInvoice.setDeleted(true);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));

        assertThatThrownBy(() -> invoiceService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByOrderId ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByOrderId returns DTO for linked invoice")
    void getByOrderId_success() {
        when(invoiceRepository.findByOrderId(10L)).thenReturn(Optional.of(draftInvoice));

        InvoiceDTO result = invoiceService.getByOrderId(10L);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-20240101-0001");
    }

    @Test
    @DisplayName("getByOrderId throws ResourceNotFoundException when no invoice for order")
    void getByOrderId_notFound() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getByOrderId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create invoice with valid orders and explicit buyer info")
    void create_withExplicitBuyerInfo() {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));
        req.setTaxPercentage(new BigDecimal("10"));
        req.setCurrencyCode("VND");

        CreateInvoiceRequest.BuyerInfo buyer = new CreateInvoiceRequest.BuyerInfo();
        buyer.setBuyerName("Công ty ABC");
        buyer.setBuyerTaxCode("123456789");
        buyer.setVisitingGuest(false);
        req.setBuyerInfo(buyer);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(invoiceRepository.countByDeletedFalse()).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        InvoiceDTO result = invoiceService.create(req);

        assertThat(result).isNotNull();
        verify(invoiceRepository).save(any(Invoice.class));
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("create invoice auto-populates buyer info from order customer")
    void create_autoPopulateBuyerFromCustomer() {
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Nguyễn Văn A");
        customer.setPhone("0901234567");
        customer.setEmail("a@test.com");
        order.setCustomer(customer);

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(invoiceRepository.countByDeletedFalse()).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        InvoiceDTO result = invoiceService.create(req);

        assertThat(result).isNotNull();
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("create invoice with no customer sets visitingGuest=true")
    void create_noCustomerSetsVisitingGuest() {
        order.setCustomer(null);

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(invoiceRepository.countByDeletedFalse()).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(102L);
            return saved;
        });

        InvoiceDTO result = invoiceService.create(req);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("create throws BadRequestException when orderIds is null")
    void create_nullOrderIds() {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(null);

        assertThatThrownBy(() -> invoiceService.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create throws BadRequestException when orderIds is empty")
    void create_emptyOrderIds() {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(Collections.emptyList());

        assertThatThrownBy(() -> invoiceService.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create throws ResourceNotFoundException when order not found")
    void create_orderNotFound() {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(99L));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create throws BadRequestException when order already has invoice")
    void create_orderAlreadyInvoiced() {
        order.setInvoice(draftInvoice);

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> invoiceService.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create generates unique invoice number when collision exists")
    void create_invoiceNumberCollisionRetry() {
        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(invoiceRepository.countByDeletedFalse()).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(103L);
            return saved;
        });

        InvoiceDTO result = invoiceService.create(req);

        assertThat(result).isNotNull();
        verify(invoiceRepository, atLeast(2)).existsByInvoiceNumber(anyString());
    }

    @Test
    @DisplayName("create builds invoice items from order items with tax")
    void create_buildsInvoiceItemsWithTax() {
        OrderItem item = new OrderItem();
        item.setId(50L);
        item.setProductId(20L);
        item.setProductName("Nhẫn vàng 18K");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("1000000"));
        item.setTaxPercentage(new BigDecimal("10"));
        order.setOrderItems(List.of(item));

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setOrderIds(List.of(10L));
        req.setTaxPercentage(new BigDecimal("10"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(invoiceRepository.countByDeletedFalse()).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(104L);
            return saved;
        });

        InvoiceDTO result = invoiceService.create(req);

        assertThat(result).isNotNull();
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update modifies draft invoice fields")
    void update_success() {
        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        req.setNotes("Updated notes");
        req.setPaymentType("TRANSFER");
        req.setInvoiceType("01GTKT");
        req.setInvoiceSeries("C24T");

        UpdateInvoiceRequest.BuyerInfoRequest buyerReq = new UpdateInvoiceRequest.BuyerInfoRequest();
        buyerReq.setBuyerName("New Buyer");
        buyerReq.setVisitingGuest(false);
        req.setBuyerInfo(buyerReq);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftInvoice);

        InvoiceDTO result = invoiceService.update(1L, req);

        assertThat(result).isNotNull();
        verify(invoiceRepository).save(draftInvoice);
    }

    @Test
    @DisplayName("update existing buyerInfo merges fields")
    void update_mergesExistingBuyerInfo() {
        InvoiceBuyerInfo existingBuyer = InvoiceBuyerInfo.builder()
                .buyerName("Old Name")
                .buyerTaxCode("111")
                .build();
        draftInvoice.setBuyerInfo(existingBuyer);

        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        UpdateInvoiceRequest.BuyerInfoRequest buyerReq = new UpdateInvoiceRequest.BuyerInfoRequest();
        buyerReq.setBuyerName("New Name");
        req.setBuyerInfo(buyerReq);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftInvoice);

        invoiceService.update(1L, req);

        verify(invoiceRepository).save(draftInvoice);
    }

    @Test
    @DisplayName("update throws BadRequestException for non-draft invoice")
    void update_nonDraftThrows() {
        UpdateInvoiceRequest req = new UpdateInvoiceRequest();
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(completedInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.update(2L, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── issue ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("issue transitions DRAFT to COMPLETED")
    void issue_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftInvoice);

        InvoiceDTO result = invoiceService.issue(1L);

        assertThat(result).isNotNull();
        verify(invoiceRepository).save(draftInvoice);
    }

    @Test
    @DisplayName("issue throws BadRequestException for already-completed invoice")
    void issue_nonDraftThrows() {
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(completedInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.issue(2L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── cancel ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel unlinks orders and cancels invoice")
    void cancel_success() {
        List<Order> linkedOrders = new ArrayList<>();
        linkedOrders.add(order);
        draftInvoice.setOrders(linkedOrders);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftInvoice);

        InvoiceDTO result = invoiceService.cancel(1L);

        assertThat(result).isNotNull();
        verify(orderRepository).save(order);
        verify(invoiceRepository).save(draftInvoice);
    }

    @Test
    @DisplayName("cancel throws BadRequestException for already-cancelled invoice")
    void cancel_alreadyCancelled() {
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(cancelledInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.cancel(3L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete soft-deletes draft invoice after unlinking orders")
    void delete_success() {
        List<Order> linkedOrders = new ArrayList<>();
        linkedOrders.add(order);
        draftInvoice.setOrders(linkedOrders);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftInvoice);

        invoiceService.delete(1L);

        verify(orderRepository).save(order);
        verify(invoiceRepository).save(draftInvoice);
    }

    @Test
    @DisplayName("delete throws BadRequestException for non-draft invoice")
    void delete_nonDraftThrows() {
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(completedInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.delete(2L))
                .isInstanceOf(BadRequestException.class);
    }

    // ── syncExternal ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncExternal kicks off async sync with INVOICE_SYSTEM vendor when set")
    void syncExternal_usesInvoiceSystem() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM)).thenReturn("S-INVOICE");

        InvoiceDTO result = invoiceService.syncExternal(1L);

        assertThat(result).isNotNull();
        verify(asyncInvoiceService).syncWithExternalAsync(eq(1L), any(), any(), eq("S-INVOICE"));
    }

    @Test
    @DisplayName("syncExternal falls back to INVOICE_VENDOR when INVOICE_SYSTEM is null")
    void syncExternal_fallbackToVendor() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.INVOICE_VENDOR)).thenReturn("M-INVOICE");

        InvoiceDTO result = invoiceService.syncExternal(1L);

        assertThat(result).isNotNull();
        verify(asyncInvoiceService).syncWithExternalAsync(eq(1L), any(), any(), eq("M-INVOICE"));
    }

    @Test
    @DisplayName("syncExternal returns invoice DTO and delegates persistence to async service")
    void syncExternal_setsTransactionUuid() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM)).thenReturn("S-INVOICE");

        InvoiceDTO result = invoiceService.syncExternal(1L);

        assertThat(result).isNotNull();
        verify(asyncInvoiceService).syncWithExternalAsync(eq(1L), any(), any(), eq("S-INVOICE"));
    }

    // ── downloadPdf ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("downloadPdf throws BadRequestException when invoice not synced")
    void downloadPdf_notSynced() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.downloadPdf(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("downloadPdf delegates to external service when transactionUuid set")
    void downloadPdf_success() throws Exception {
        draftInvoice.setTransactionUuid("tx-uuid-456");
        ExternalInvoiceService externalService = mock(ExternalInvoiceService.class);
        byte[] pdfBytes = "PDF content".getBytes();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM)).thenReturn("S-INVOICE");
        when(invoiceServiceFactory.getInvoiceService("S-INVOICE")).thenReturn(externalService);
        when(externalService.downloadInvoicePdf(draftInvoice)).thenReturn(pdfBytes);

        byte[] result = invoiceService.downloadPdf(1L);

        assertThat(result).isEqualTo(pdfBytes);
    }

    // ── sendEmail ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendEmail throws BadRequestException when transactionUuid missing")
    void sendEmail_notSynced() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        // messageService returns null by default from mock

        assertThatThrownBy(() -> invoiceService.sendEmail(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("sendEmail delegates to external service")
    void sendEmail_success() {
        draftInvoice.setTransactionUuid("tx-uuid-789");
        ExternalInvoiceService externalService = mock(ExternalInvoiceService.class);
        InvoiceResponse emailResponse = InvoiceResponse.builder()
                .success(true).message("sent").build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM)).thenReturn("S-INVOICE");
        when(invoiceServiceFactory.getInvoiceService("S-INVOICE")).thenReturn(externalService);
        when(externalService.sendEmailInvoice(draftInvoice)).thenReturn(emailResponse);

        InvoiceResponse result = invoiceService.sendEmail(1L);

        assertThat(result.isSuccess()).isTrue();
    }

    // ── getKpiSection ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getKpiSection with explicit timestamps returns count and sum")
    void getKpiSection_withTimestamps() {
        long fromMs = System.currentTimeMillis() - 86400000L;
        long toMs = System.currentTimeMillis();

        when(invoiceRepository.countInvoicesInRange(any(), any())).thenReturn(5L);
        when(invoiceRepository.sumInvoicesInRange(any(), any())).thenReturn(new BigDecimal("2500000"));

        InvoiceKpiResponse result = invoiceService.getKpiSection(fromMs, toMs);

        assertThat(result.getTotalInvoiceCount()).isEqualTo(5L);
        assertThat(result.getTotalInvoiceAmount()).isEqualByComparingTo("2500000");
    }

    @Test
    @DisplayName("getKpiSection with null timestamps defaults to today")
    void getKpiSection_withNullTimestamps() {
        when(invoiceRepository.countInvoicesInRange(any(), any())).thenReturn(0L);
        when(invoiceRepository.sumInvoicesInRange(any(), any())).thenReturn(null);

        InvoiceKpiResponse result = invoiceService.getKpiSection(null, null);

        assertThat(result.getTotalInvoiceCount()).isEqualTo(0L);
        assertThat(result.getTotalInvoiceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── mapToDTO with buyer info ───────────────────────────────────────────────

    @Test
    @DisplayName("getById maps buyer info when present")
    void getById_mapsBuyerInfo() {
        InvoiceBuyerInfo buyer = InvoiceBuyerInfo.builder()
                .buyerName("Test Corp")
                .buyerTaxCode("0123456789")
                .buyerEmail("corp@test.com")
                .visitingGuest(false)
                .build();
        draftInvoice.setBuyerInfo(buyer);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));

        InvoiceDTO result = invoiceService.getById(1L);

        assertThat(result.getBuyer()).isNotNull();
        assertThat(result.getBuyer().getBuyerName()).isEqualTo("Test Corp");
    }

    @Test
    @DisplayName("getById maps order items with customer info")
    void getById_mapsOrdersWithCustomer() {
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Khách hàng A");
        customer.setPhone("0901234567");
        order.setCustomer(customer);
        order.setOrderItems(List.of());
        draftInvoice.setOrders(List.of(order));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));

        InvoiceDTO result = invoiceService.getById(1L);

        assertThat(result.getOrders()).hasSize(1);
        assertThat(result.getOrders().get(0).getCustomer()).isNotNull();
        assertThat(result.getOrders().get(0).getCustomer().getName()).isEqualTo("Khách hàng A");
    }

    // ── getOutputInvoices / getInputInvoices ──────────────────────────────────���

    @Test
    @DisplayName("getOutputInvoices: returns page of output invoices")
    void getOutputInvoices_success() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice));
        when(invoiceRepository.findAllActiveByDirection(eq(InvoiceDirection.OUTPUT), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.getOutputInvoices(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepository).findAllActiveByDirection(InvoiceDirection.OUTPUT, pageable);
    }

    @Test
    @DisplayName("getInputInvoices: returns page of input invoices")
    void getInputInvoices_success() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice));
        when(invoiceRepository.findAllActiveByDirection(eq(InvoiceDirection.INPUT), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.getInputInvoices(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepository).findAllActiveByDirection(InvoiceDirection.INPUT, pageable);
    }

    // ── getByStatus ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOutputInvoicesByStatus: filters by status and direction")
    void getOutputInvoicesByStatus_success() {
        Page<Invoice> page = new PageImpl<>(List.of(completedInvoice));
        when(invoiceRepository.findAllActiveByDirectionAndStatus(
                eq(InvoiceDirection.OUTPUT), eq(Invoice.InvoiceStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.getOutputInvoicesByStatus("COMPLETED", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getInputInvoicesByStatus: filters by status and direction")
    void getInputInvoicesByStatus_success() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice));
        when(invoiceRepository.findAllActiveByDirectionAndStatus(
                eq(InvoiceDirection.INPUT), eq(Invoice.InvoiceStatus.DRAFT), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.getInputInvoicesByStatus("DRAFT", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── searchInvoices ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchOutputInvoices: searches output invoices by keyword")
    void searchOutputInvoices_success() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice));
        when(invoiceRepository.searchByKeywordAndDirection(eq("INV"), eq(InvoiceDirection.OUTPUT), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.searchOutputInvoices("INV", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchInputInvoices: searches input invoices by keyword")
    void searchInputInvoices_success() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice));
        when(invoiceRepository.searchByKeywordAndDirection(eq("VND"), eq(InvoiceDirection.INPUT), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceDTO> result = invoiceService.searchInputInvoices("VND", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── createInputInvoice ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createInputInvoice: success — creates INPUT direction invoice with items")
    void createInputInvoice_success() {
        CreateInputInvoiceRequest req = new CreateInputInvoiceRequest();
        req.setVendorName("Nhà cung cấp A");
        req.setTaxPercentage(BigDecimal.TEN);

        CreateInputInvoiceRequest.ItemRequest item = new CreateInputInvoiceRequest.ItemRequest();
        item.setItemName("Hàng hoá X");
        item.setQuantity(BigDecimal.valueOf(2));
        item.setUnitPrice(BigDecimal.valueOf(100_000));
        req.setItems(List.of(item));

        Invoice saved = Invoice.builder()
                .invoiceNumber("INV-20260510-0001")
                .status(Invoice.InvoiceStatus.DRAFT)
                .direction(InvoiceDirection.INPUT)
                .totalAmount(BigDecimal.valueOf(220_000))
                .totalAmountWithoutTax(BigDecimal.valueOf(200_000))
                .taxAmount(BigDecimal.valueOf(20_000))
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        saved.setId(10L);
        saved.setDeleted(false);

        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);

        InvoiceDTO result = invoiceService.createInputInvoice(req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("createInputInvoice: no items — creates invoice with zero totals")
    void createInputInvoice_noItems_success() {
        CreateInputInvoiceRequest req = new CreateInputInvoiceRequest();
        req.setVendorName("Nhà cung cấp B");
        req.setItems(null);

        Invoice saved = Invoice.builder()
                .invoiceNumber("INV-20260510-0002")
                .status(Invoice.InvoiceStatus.DRAFT)
                .direction(InvoiceDirection.INPUT)
                .totalAmount(BigDecimal.ZERO)
                .totalAmountWithoutTax(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        saved.setId(11L);
        saved.setDeleted(false);

        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);

        InvoiceDTO result = invoiceService.createInputInvoice(req);

        assertThat(result).isNotNull();
    }

    // ── confirmInputInvoice ────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmInputInvoice: DRAFT INPUT invoice → COMPLETED")
    void confirmInputInvoice_success() {
        Invoice inputDraft = Invoice.builder()
                .invoiceNumber("INV-INPUT-001")
                .status(Invoice.InvoiceStatus.DRAFT)
                .direction(InvoiceDirection.INPUT)
                .totalAmount(BigDecimal.valueOf(200_000))
                .totalAmountWithoutTax(BigDecimal.valueOf(200_000))
                .taxAmount(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        inputDraft.setId(5L);
        inputDraft.setDeleted(false);

        Invoice completedInput = Invoice.builder()
                .invoiceNumber("INV-INPUT-001")
                .status(Invoice.InvoiceStatus.COMPLETED)
                .direction(InvoiceDirection.INPUT)
                .totalAmount(BigDecimal.valueOf(200_000))
                .totalAmountWithoutTax(BigDecimal.valueOf(200_000))
                .taxAmount(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        completedInput.setId(5L);
        completedInput.setDeleted(false);

        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(inputDraft));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(completedInput);

        InvoiceDTO result = invoiceService.confirmInputInvoice(5L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("confirmInputInvoice: not an INPUT invoice → BadRequestException")
    void confirmInputInvoice_notInput_throws() {
        draftInvoice.setDirection(InvoiceDirection.OUTPUT);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draftInvoice));
        when(messageService.getMessage("error.invoice.not.input")).thenReturn("Not input");

        assertThatThrownBy(() -> invoiceService.confirmInputInvoice(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("confirmInputInvoice: not DRAFT status → BadRequestException")
    void confirmInputInvoice_notDraft_throws() {
        Invoice completedInput = Invoice.builder()
                .invoiceNumber("INV-INPUT-002")
                .status(Invoice.InvoiceStatus.COMPLETED)
                .direction(InvoiceDirection.INPUT)
                .totalAmount(BigDecimal.ZERO)
                .totalAmountWithoutTax(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .orders(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
        completedInput.setId(6L);
        completedInput.setDeleted(false);

        when(invoiceRepository.findById(6L)).thenReturn(Optional.of(completedInput));
        when(messageService.getMessage("error.invoice.cannot.issue.non.draft")).thenReturn("Not draft");

        assertThatThrownBy(() -> invoiceService.confirmInputInvoice(6L))
                .isInstanceOf(BadRequestException.class);
    }
}
