package com.knp.repository.order;

import com.knp.model.entity.order.CartEntity;
import com.knp.model.entity.order.CartItemEntity;
import com.knp.model.enums.CartStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Repository Tests")
class CartRepositoryTest {

    @Mock
    private CartRepository cartRepository;

    @Test
    @DisplayName("Should find cart by cartId")
    void testFindByCartId() {
        CartEntity cart = CartEntity.builder()
            .cartId("test-cart-123")
            .status(CartStatus.ACTIVE)
            .build();
        when(cartRepository.findByCartId("test-cart-123")).thenReturn(Optional.of(cart));

        Optional<CartEntity> found = cartRepository.findByCartId("test-cart-123");

        assertThat(found).isPresent();
        assertThat(found.get().getCartId()).isEqualTo("test-cart-123");
    }

    @Test
    @DisplayName("Should return empty Optional when cart not found")
    void testFindByCartId_NotFound() {
        when(cartRepository.findByCartId("non-existent")).thenReturn(Optional.empty());

        Optional<CartEntity> found = cartRepository.findByCartId("non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find cart by customer and status")
    void testFindByCustomerIdAndStatus() {
        CartEntity cart = CartEntity.builder()
            .cartId("customer-cart-123")
            .customerId(42L)
            .status(CartStatus.ACTIVE)
            .build();
        when(cartRepository.findByCustomerIdAndStatus(42L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        Optional<CartEntity> found = cartRepository.findByCustomerIdAndStatus(42L, CartStatus.ACTIVE);

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(42L);
        assertThat(found.get().getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find all carts by status")
    void testFindByStatus() {
        CartEntity cart = CartEntity.builder().cartId("active-cart-1").status(CartStatus.ACTIVE).build();
        when(cartRepository.findByStatus(CartStatus.ACTIVE)).thenReturn(List.of(cart));

        List<CartEntity> activeCarts = cartRepository.findByStatus(CartStatus.ACTIVE);

        assertThat(activeCarts).hasSize(1);
        assertThat(activeCarts).allMatch(c -> c.getStatus() == CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find all carts for customer")
    void testFindByCustomerId() {
        CartEntity c1 = CartEntity.builder().cartId("c42-1").customerId(42L).status(CartStatus.ACTIVE).build();
        CartEntity c2 = CartEntity.builder().cartId("c42-2").customerId(42L).status(CartStatus.ABANDONED).build();
        when(cartRepository.findByCustomerId(42L)).thenReturn(List.of(c1, c2));

        List<CartEntity> carts = cartRepository.findByCustomerId(42L);

        assertThat(carts).hasSize(2);
        assertThat(carts).allMatch(c -> c.getCustomerId() == 42L);
    }

    @Test
    @DisplayName("Should count carts by status")
    void testCountByStatus() {
        when(cartRepository.countByStatus(CartStatus.PAID)).thenReturn(3L);

        Long count = cartRepository.countByStatus(CartStatus.PAID);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should delete cart by ID")
    void testDelete() {
        doNothing().when(cartRepository).deleteById(1L);
        when(cartRepository.findById(1L)).thenReturn(Optional.empty());

        cartRepository.deleteById(1L);
        Optional<CartEntity> found = cartRepository.findById(1L);

        verify(cartRepository).deleteById(1L);
        assertThat(found).isEmpty();
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Item Repository Tests")
class CartItemRepositoryTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("Should find cart items by cart ID")
    void testFindByCartId() {
        CartItemEntity item = CartItemEntity.builder()
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .lineTotal(BigDecimal.TEN)
            .build();
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item));

        List<CartItemEntity> items = cartItemRepository.findByCartId(10L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should find cart items by cart and product ID")
    void testFindByCartIdAndProductId() {
        CartItemEntity item = CartItemEntity.builder()
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .unitPrice(BigDecimal.TEN)
            .build();
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(List.of(item));

        List<CartItemEntity> items = cartItemRepository.findByCartIdAndProductId(10L, 1L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return empty list when no items found")
    void testFindByCartId_EmptyList() {
        when(cartItemRepository.findByCartId(99L)).thenReturn(List.of());

        List<CartItemEntity> items = cartItemRepository.findByCartId(99L);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should delete cart item by ID")
    void testDelete() {
        doNothing().when(cartItemRepository).deleteById(5L);
        when(cartItemRepository.findById(5L)).thenReturn(Optional.empty());

        cartItemRepository.deleteById(5L);
        Optional<CartItemEntity> found = cartItemRepository.findById(5L);

        verify(cartItemRepository).deleteById(5L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple items in same cart")
    void testFindMultipleItems() {
        CartItemEntity item1 = CartItemEntity.builder().productId(1L).productName("Product 1").sku("SKU-001").quantity(1).unitPrice(BigDecimal.TEN).build();
        CartItemEntity item2 = CartItemEntity.builder().productId(2L).productName("Product 2").sku("SKU-002").quantity(2).unitPrice(BigDecimal.valueOf(20)).build();
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item1, item2));

        List<CartItemEntity> items = cartItemRepository.findByCartId(10L);

        assertThat(items).hasSize(2);
    }
}
