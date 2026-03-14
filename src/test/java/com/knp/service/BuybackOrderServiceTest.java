package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.buyback.BuybackOrderDTO;
import com.knp.model.dto.buyback.CreateBuybackOrderRequest;
import com.knp.model.entity.BuybackOrder;
import com.knp.model.entity.BuybackOrder.OrderStatus;
import com.knp.model.entity.BuybackOrder.OrderType;
import com.knp.model.entity.BuybackOrderItem;
import com.knp.model.entity.BuybackOrderItem.ItemType;
import com.knp.repository.BuybackOrderRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuybackOrderService Unit Tests")
class BuybackOrderServiceTest {

    @Mock
    private BuybackOrderRepository repository;

    @InjectMocks
    private BuybackOrderService service;

    private BuybackOrder pendingOrder;
    private BuybackOrder completedOrder;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff01", null, Collections.emptyList()));

        pendingOrder = BuybackOrder.builder()
                .orderNumber("BB-000001")
                .type(OrderType.BUY)
                .status(OrderStatus.PENDING)
                .customerName("Nguyễn Văn A")
                .customerPhone("0901234567")
                .paymentMethod("CASH")
                .buyTotal(new BigDecimal("1500000"))
                .saleTotal(BigDecimal.ZERO)
                .netAmount(new BigDecimal("1500000"))
                .createdBy("staff01")
                .items(new ArrayList<>())
                .build();
        pendingOrder.setId(1L);

        completedOrder = BuybackOrder.builder()
                .orderNumber("BB-000002")
                .type(OrderType.BUY)
                .status(OrderStatus.COMPLETED)
                .customerName("Trần Thị B")
                .paymentMethod("CASH")
                .buyTotal(new BigDecimal("2000000"))
                .saleTotal(BigDecimal.ZERO)
                .netAmount(new BigDecimal("2000000"))
                .createdBy("staff01")
                .items(new ArrayList<>())
                .build();
        completedOrder.setId(2L);

        pageable = PageRequest.of(0, 20);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active orders when no filter")
    void testGetAll_NoFilter() {
        Page<BuybackOrder> page = new PageImpl<>(List.of(pendingOrder));
        when(repository.findAllActive(pageable)).thenReturn(page);

        Page<BuybackOrderDTO> result = service.getAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllActive(pageable);
    }

    @Test
    @DisplayName("Should filter by type only")
    void testGetAll_FilterByType() {
        Page<BuybackOrder> page = new PageImpl<>(List.of(pendingOrder));
        when(repository.findByType(OrderType.BUY, pageable)).thenReturn(page);

        Page<BuybackOrderDTO> result = service.getAll("BUY", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByType(OrderType.BUY, pageable);
    }

    @Test
    @DisplayName("Should filter by status only")
    void testGetAll_FilterByStatus() {
        Page<BuybackOrder> page = new PageImpl<>(List.of(pendingOrder));
        when(repository.findByStatus(OrderStatus.PENDING, pageable)).thenReturn(page);

        Page<BuybackOrderDTO> result = service.getAll(null, "PENDING", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByStatus(OrderStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("Should filter by both type and status")
    void testGetAll_FilterByTypeAndStatus() {
        Page<BuybackOrder> page = new PageImpl<>(List.of(pendingOrder));
        when(repository.findByTypeAndStatus(OrderType.BUY, OrderStatus.PENDING, pageable)).thenReturn(page);

        Page<BuybackOrderDTO> result = service.getAll("BUY", "PENDING", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByTypeAndStatus(OrderType.BUY, OrderStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("Should fall back to findAllActive for unknown filter values")
    void testGetAll_UnknownFilterValues_FallBackToAll() {
        Page<BuybackOrder> page = new PageImpl<>(List.of(pendingOrder));
        when(repository.findAllActive(pageable)).thenReturn(page);

        Page<BuybackOrderDTO> result = service.getAll("INVALID", "UNKNOWN", pageable);

        verify(repository).findAllActive(pageable);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return order with items by id")
    void testGetById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        BuybackOrderDTO result = service.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("BB-000001");
        assertThat(result.getBuyItems()).isNotNull();
        assertThat(result.getSaleItems()).isNotNull();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent id")
    void testGetById_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for soft-deleted order")
    void testGetById_SoftDeleted() {
        pendingOrder.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create (BUY type) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create BUY order with buy items")
    void testCreate_BuyOrder() {
        CreateBuybackOrderRequest req = buildBuyRequest("BUY", null);
        when(repository.findMaxId()).thenReturn(0L);
        when(repository.save(any(BuybackOrder.class))).thenReturn(pendingOrder);

        BuybackOrderDTO result = service.create(req);

        assertThat(result).isNotNull();
        verify(repository).save(any(BuybackOrder.class));
    }

    @Test
    @DisplayName("Should create EXCHANGE order with both buy and sale items")
    void testCreate_ExchangeOrder() {
        CreateBuybackOrderRequest req = buildExchangeRequest();
        BuybackOrder exchangeOrder = BuybackOrder.builder()
                .orderNumber("BB-000003")
                .type(OrderType.EXCHANGE)
                .status(OrderStatus.PENDING)
                .paymentMethod("CASH")
                .buyTotal(new BigDecimal("1000000"))
                .saleTotal(new BigDecimal("800000"))
                .netAmount(new BigDecimal("200000"))
                .createdBy("staff01")
                .items(new ArrayList<>())
                .build();
        exchangeOrder.setId(3L);

        when(repository.findMaxId()).thenReturn(2L);
        when(repository.save(any(BuybackOrder.class))).thenReturn(exchangeOrder);

        BuybackOrderDTO result = service.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("BB-000003");
    }

    @Test
    @DisplayName("Should auto-complete order when status=COMPLETED in request")
    void testCreate_WithCompletedStatus_AutoCompletes() {
        CreateBuybackOrderRequest req = buildBuyRequest("BUY", "COMPLETED");
        BuybackOrder savedOrder = BuybackOrder.builder()
                .orderNumber("BB-000001")
                .type(OrderType.BUY)
                .status(OrderStatus.COMPLETED)
                .paymentMethod("CASH")
                .buyTotal(BigDecimal.ZERO)
                .saleTotal(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .createdBy("staff01")
                .items(new ArrayList<>())
                .build();
        savedOrder.setId(5L);

        when(repository.findMaxId()).thenReturn(4L);
        when(repository.save(any(BuybackOrder.class))).thenReturn(savedOrder);

        BuybackOrderDTO result = service.create(req);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should default to PENDING status when not specified")
    void testCreate_DefaultsPendingStatus() {
        CreateBuybackOrderRequest req = buildBuyRequest("BUY", null);
        when(repository.findMaxId()).thenReturn(0L);
        when(repository.save(any(BuybackOrder.class))).thenAnswer(inv -> {
            BuybackOrder o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        BuybackOrderDTO result = service.create(req);

        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should generate order number from max id + 1")
    void testCreate_GeneratesOrderNumber() {
        CreateBuybackOrderRequest req = buildBuyRequest("BUY", null);
        when(repository.findMaxId()).thenReturn(5L);
        when(repository.save(any(BuybackOrder.class))).thenAnswer(inv -> {
            BuybackOrder o = inv.getArgument(0);
            o.setId(6L);
            return o;
        });

        service.create(req);

        verify(repository).save(argThat(o -> "BB-000006".equals(o.getOrderNumber())));
    }

    @Test
    @DisplayName("Should handle null maxId and start from BB-000001")
    void testCreate_NullMaxId_StartsFromOne() {
        CreateBuybackOrderRequest req = buildBuyRequest("BUY", null);
        when(repository.findMaxId()).thenReturn(null);
        when(repository.save(any(BuybackOrder.class))).thenAnswer(inv -> {
            BuybackOrder o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        service.create(req);

        verify(repository).save(argThat(o -> "BB-000001".equals(o.getOrderNumber())));
    }

    // ── complete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should complete a PENDING order")
    void testComplete_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(repository.save(any(BuybackOrder.class))).thenReturn(pendingOrder);

        BuybackOrderDTO result = service.complete(1L);

        assertThat(result).isNotNull();
        verify(repository).save(any(BuybackOrder.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when completing already-COMPLETED order")
    void testComplete_AlreadyCompleted() {
        when(repository.findById(2L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> service.complete(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("Should throw BadRequestException when completing CANCELLED order")
    void testComplete_Cancelled() {
        BuybackOrder cancelled = BuybackOrder.builder()
                .orderNumber("BB-000003")
                .type(OrderType.BUY)
                .status(OrderStatus.CANCELLED)
                .paymentMethod("CASH")
                .buyTotal(BigDecimal.ZERO)
                .saleTotal(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        cancelled.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.complete(3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when completing non-existent order")
    void testComplete_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should cancel a PENDING order")
    void testCancel_PendingOrder() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(repository.save(any(BuybackOrder.class))).thenReturn(pendingOrder);

        BuybackOrderDTO result = service.cancel(1L);

        assertThat(result).isNotNull();
        verify(repository).save(any(BuybackOrder.class));
    }

    @Test
    @DisplayName("Should cancel a COMPLETED order")
    void testCancel_CompletedOrder() {
        when(repository.findById(2L)).thenReturn(Optional.of(completedOrder));
        when(repository.save(any(BuybackOrder.class))).thenReturn(completedOrder);

        BuybackOrderDTO result = service.cancel(2L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw BadRequestException when order is already cancelled")
    void testCancel_AlreadyCancelled() {
        BuybackOrder cancelled = BuybackOrder.builder()
                .orderNumber("BB-000003")
                .type(OrderType.BUY)
                .status(OrderStatus.CANCELLED)
                .paymentMethod("CASH")
                .buyTotal(BigDecimal.ZERO)
                .saleTotal(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        cancelled.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.cancel(3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cancelling non-existent order")
    void testCancel_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DTO mapping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should split items into buyItems and saleItems in DTO")
    void testGetById_SplitsItemsByType() {
        BuybackOrderItem buyItem = BuybackOrderItem.builder()
                .itemType(ItemType.BUY)
                .commodityName("Vàng 24K")
                .totalPrice(new BigDecimal("1500000"))
                .build();
        buyItem.setId(10L);

        BuybackOrderItem saleItem = BuybackOrderItem.builder()
                .itemType(ItemType.SALE)
                .productName("Nhẫn vàng")
                .totalPrice(new BigDecimal("800000"))
                .build();
        saleItem.setId(11L);

        pendingOrder.setItems(List.of(buyItem, saleItem));
        when(repository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        BuybackOrderDTO dto = service.getById(1L);

        assertThat(dto.getBuyItems()).hasSize(1);
        assertThat(dto.getSaleItems()).hasSize(1);
        assertThat(dto.getBuyItems().get(0).getCommodityName()).isEqualTo("Vàng 24K");
        assertThat(dto.getSaleItems().get(0).getProductName()).isEqualTo("Nhẫn vàng");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateBuybackOrderRequest buildBuyRequest(String type, String status) {
        CreateBuybackOrderRequest.BuyItemRequest buyItem = new CreateBuybackOrderRequest.BuyItemRequest();
        buyItem.setCommodityName("Vàng 24K");
        buyItem.setUnit("chỉ");
        buyItem.setWeight(new java.math.BigDecimal("1.0"));
        buyItem.setPricePerUnit(new BigDecimal("1500000"));
        buyItem.setTotalPrice(new BigDecimal("1500000"));

        CreateBuybackOrderRequest req = new CreateBuybackOrderRequest();
        req.setType(type);
        req.setStatus(status);
        req.setCustomerName("Nguyễn Văn A");
        req.setPaymentMethod("CASH");
        req.setBuyTotal(new BigDecimal("1500000"));
        req.setSaleTotal(BigDecimal.ZERO);
        req.setNetAmount(new BigDecimal("1500000"));
        req.setBuyItems(List.of(buyItem));
        return req;
    }

    private CreateBuybackOrderRequest buildExchangeRequest() {
        CreateBuybackOrderRequest.BuyItemRequest buyItem = new CreateBuybackOrderRequest.BuyItemRequest();
        buyItem.setCommodityName("Vàng 24K cũ");
        buyItem.setUnit("chỉ");
        buyItem.setWeight(new java.math.BigDecimal("1.0"));
        buyItem.setPricePerUnit(new BigDecimal("1000000"));
        buyItem.setTotalPrice(new BigDecimal("1000000"));

        CreateBuybackOrderRequest.SaleItemRequest saleItem = new CreateBuybackOrderRequest.SaleItemRequest();
        saleItem.setProductName("Nhẫn vàng mới");
        saleItem.setQuantity(1);
        saleItem.setUnitPrice(new BigDecimal("800000"));
        saleItem.setTotalPrice(new BigDecimal("800000"));

        CreateBuybackOrderRequest req = new CreateBuybackOrderRequest();
        req.setType("EXCHANGE");
        req.setCustomerName("Nguyễn Văn A");
        req.setPaymentMethod("CASH");
        req.setBuyTotal(new BigDecimal("1000000"));
        req.setSaleTotal(new BigDecimal("800000"));
        req.setNetAmount(new BigDecimal("200000"));
        req.setBuyItems(List.of(buyItem));
        req.setSaleItems(List.of(saleItem));
        return req;
    }
}
