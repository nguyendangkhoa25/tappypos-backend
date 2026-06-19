package com.tappy.pos.service.order;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.order.ExchangeOrderItemRequest;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductVariant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.inventory.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl exchange (size-swap) Unit Tests")
class OrderServiceExchangeTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks private OrderServiceImpl service;

    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        lenient().when(messageService.getMessage(anyString())).thenReturn("err");
        lenient().when(messageService.getMessage(anyString(), (Object[]) any())).thenReturn("err");

        product = Product.builder().name("Áo").price(new BigDecimal("100000")).build();
        product.setId(1L);

        OrderItem item = OrderItem.builder()
                .productId(1L).variantId(10L).productName("Áo").quantity(1)
                .unitPrice(new BigDecimal("100000")).build();
        item.setId(100L);

        order = new Order();
        order.setId(5L);
        order.setOrderNumber("ORD-5");
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.getOrderItems().add(item);
    }

    private ProductVariant newVariant(BigDecimal priceOverride) {
        ProductVariant v = ProductVariant.builder()
                .product(product).sku("AO-L").priceOverride(priceOverride)
                .status(ProductVariant.VariantStatus.ACTIVE).build();
        v.setId(11L);
        return v;
    }

    private InventoryDTO inv(long id, long qty) {
        return InventoryDTO.builder().id(id).quantityInStock(qty).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("exchange: swaps variant, deducts new stock, restocks returned, logs")
    void exchange_success() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(11L, 1L))
                .thenReturn(Optional.of(newVariant(null))); // null override → uses product price 100000 == item price
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(1L), eq(11L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(900L, 5L))));
        when(inventoryService.getInventoryByProductIdAndVariantId(eq(1L), eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(inv(800L, 0L))));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        service.exchangeOrderItem(5L, 100L, new ExchangeOrderItemRequest(11L));

        verify(inventoryService).removeStock(900L, 1L); // new variant deducted
        verify(inventoryService).addStock(800L, 1L);    // returned variant restocked
        // item now points at the new variant
        org.assertj.core.api.Assertions.assertThat(order.getOrderItems().get(0).getVariantId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("exchange: different price → BadRequest, no stock movement")
    void exchange_priceMismatch_throws() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productVariantRepository.findByIdAndProductIdAndDeletedAtIsNull(11L, 1L))
                .thenReturn(Optional.of(newVariant(new BigDecimal("150000")))); // != 100000

        assertThatThrownBy(() -> service.exchangeOrderItem(5L, 100L, new ExchangeOrderItemRequest(11L)))
                .isInstanceOf(BadRequestException.class);

        verify(inventoryService, never()).removeStock(anyLong(), anyLong());
        verify(inventoryService, never()).addStock(anyLong(), anyLong());
    }

    @Test
    @DisplayName("exchange: order not completed → BadRequest")
    void exchange_notCompleted_throws() {
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.exchangeOrderItem(5L, 100L, new ExchangeOrderItemRequest(11L)))
                .isInstanceOf(BadRequestException.class);
    }
}
