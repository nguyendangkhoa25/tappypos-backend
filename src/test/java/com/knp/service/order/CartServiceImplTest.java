package com.knp.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.order.CartItemResponse;
import com.knp.model.dto.order.CartRequest;
import com.knp.model.dto.order.CartResponse;
import com.knp.model.dto.inventory.InventoryDTO;
import com.knp.model.dto.product.ProductDTO;
import com.knp.model.entity.order.CartEntity;
import com.knp.model.entity.order.CartItemEntity;
import com.knp.model.enums.CartStatus;
import com.knp.model.enums.DiscountType;
import com.knp.repository.order.CartItemRepository;
import com.knp.repository.order.CartRepository;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.knp.service.MessageService;
import com.knp.service.inventory.InventoryService;
import com.knp.service.tenant.ShopInfoService;
import com.knp.service.customer.LoyaltyService;
import com.knp.service.product.ProductService;
import com.knp.multitenant.TenantContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ShopInfoService shopInfoService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private ProductService productService;

    @Mock
    private TenantContext tenantContext;

    private CartServiceImpl cartService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cartService = new CartServiceImpl(
            cartRepository,
            cartItemRepository,
            orderRepository,
            customerRepository,
            messageService,
            objectMapper,
            inventoryService,
            productService,
            shopInfoService,
            promotionService,
            loyaltyService,
            tenantContext
        );
    }

    // ==================== Initialize Cart Tests ====================

    @Test
    @DisplayName("Should initialize new cart with UUID")
    void testInitializeCart() {
        // Arrange
        CartEntity expectedCart = CartEntity.builder()
            .id(1L)
            .cartId("test-uuid")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.save(any(CartEntity.class))).thenReturn(expectedCart);

        // Act
        CartResponse response = cartService.initializeCart();

        // Assert
        assertNotNull(response);
        assertEquals(CartStatus.ACTIVE, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        assertTrue(response.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Get Cart Tests ====================

    @Test
    @DisplayName("Should get cart by cartId")
    void testGetCart_Success() {
        // Arrange
        String cartId = "test-cart-id";
        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.TEN)
            .total(BigDecimal.TEN)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));

        // Act
        CartResponse response = cartService.getCart(cartId);

        // Assert
        assertNotNull(response);
        assertEquals(cartId, response.getCartId());
        verify(cartRepository, times(1)).findByCartId(cartId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cart not found")
    void testGetCart_CartNotFound() {
        // Arrange
        String cartId = "non-existent-cart";
        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.empty());
        when(messageService.getMessage("cart.not.found")).thenReturn("Cart not found");

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> cartService.getCart(cartId)
        );
        assertEquals("Cart not found", exception.getMessage());
    }

    // ==================== Add Item to Cart Tests ====================

    @Test
    @DisplayName("Should add new item to cart successfully")
    void testAddItemToCart_NewItem_Success() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(2)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L)
            .productId(1L)
            .quantityInStock(10L)
            .build();

        Page<InventoryDTO> inventoryPage = new PageImpl<>(List.of(inventory));

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(inventoryPage);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.addItemToCart(cartId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("Should merge quantity when adding duplicate product")
    void testAddItemToCart_DuplicateItem_MergeQuantity() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(2)
            .build();

        CartItemEntity existingItem = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(3)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .lineSubtotal(BigDecimal.valueOf(30))
            .lineTotal(BigDecimal.valueOf(30))
            .tax(BigDecimal.valueOf(3))
            .lineGrandTotal(BigDecimal.valueOf(33))
            .discountValue(BigDecimal.ZERO)
            .discountType(DiscountType.NONE)
            .variants("{}")
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(existingItem)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .subtotal(BigDecimal.valueOf(30))
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.valueOf(3))
            .total(BigDecimal.valueOf(33))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L)
            .productId(1L)
            .quantityInStock(10L)
            .build();

        Page<InventoryDTO> inventoryPage = new PageImpl<>(List.of(inventory));

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(inventoryPage);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.addItemToCart(cartId, request);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getItems().get(0).getQuantity()); // 3 + 2
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when quantity is invalid")
    void testAddItemToCart_InvalidQuantity() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(0)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(messageService.getMessage("validation.invalid.quantity"))
            .thenReturn("Quantity must be greater than 0");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.addItemToCart(cartId, request)
        );
        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when product not found")
    void testAddItemToCart_ProductNotFound() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(999L)
            .quantity(1)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(999L)).thenThrow(new RuntimeException("Not found"));
        when(messageService.getMessage("product.not.found")).thenReturn("Product not found");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.addItemToCart(cartId, request)
        );
        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when no inventory found")
    void testAddItemToCart_NoInventory() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(1)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        Page<InventoryDTO> emptyPage = new PageImpl<>(new ArrayList<>());

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(emptyPage);
        when(messageService.getMessage("product.out.of.stock"))
            .thenReturn("Product is out of stock");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.addItemToCart(cartId, request)
        );
        assertEquals("Product is out of stock", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when insufficient stock")
    void testAddItemToCart_InsufficientStock() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(10)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L)
            .productId(1L)
            .quantityInStock(5L)
            .build();

        Page<InventoryDTO> inventoryPage = new PageImpl<>(List.of(inventory));

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(inventoryPage);
        when(messageService.getMessage("product.insufficient.stock"))
            .thenReturn("Test Product has insufficient stock. Only 5 available");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.addItemToCart(cartId, request)
        );
        assertTrue(exception.getMessage().contains("insufficient stock"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when inventory check fails")
    void testAddItemToCart_InventoryCheckFails() throws Exception {
        // Arrange
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(1)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenThrow(new RuntimeException("Database error"));
        when(messageService.getMessage("product.stock.check.failed"))
            .thenReturn("Failed to check product stock. Please try again");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.addItemToCart(cartId, request)
        );
        assertEquals("Failed to check product stock. Please try again", exception.getMessage());
    }

    // ==================== Update Item Quantity Tests ====================

    @Test
    @DisplayName("Should update cart item quantity successfully")
    void testUpdateCartItemQuantity_Success() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 1L;

        CartItemEntity item = CartItemEntity.builder()
            .id(itemId)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .lineSubtotal(BigDecimal.valueOf(20))
            .lineTotal(BigDecimal.valueOf(20))
            .tax(BigDecimal.valueOf(2))
            .lineGrandTotal(BigDecimal.valueOf(22))
            .discountValue(BigDecimal.ZERO)
            .discountType(DiscountType.NONE)
            .variants("{}")
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .subtotal(BigDecimal.valueOf(20))
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.valueOf(2))
            .total(BigDecimal.valueOf(22))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.updateCartItemQuantity(cartId, itemId, 5);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("Should remove item when quantity is 0")
    void testUpdateCartItemQuantity_RemoveItem() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 1L;

        CartItemEntity item = CartItemEntity.builder()
            .id(itemId)
            .productId(1L)
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.updateCartItemQuantity(cartId, itemId, 0);

        // Assert
        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when item not found")
    void testUpdateCartItemQuantity_ItemNotFound() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 999L;

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.item.not.found"))
            .thenReturn("Cart item not found");

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> cartService.updateCartItemQuantity(cartId, itemId, 5)
        );
        assertEquals("Cart item not found", exception.getMessage());
    }

    // ==================== Remove Item Tests ====================

    @Test
    @DisplayName("Should remove item from cart successfully")
    void testRemoveItemFromCart_Success() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 1L;

        CartItemEntity item = CartItemEntity.builder()
            .id(itemId)
            .productId(1L)
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.removeItemFromCart(cartId, itemId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Apply Discount Tests ====================

    @Test
    @DisplayName("Should apply discount to cart item successfully")
    void testApplyItemDiscount_Success() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 1L;

        CartItemEntity item = CartItemEntity.builder()
            .id(itemId)
            .productId(1L)
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .lineSubtotal(BigDecimal.valueOf(100))
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.applyItemDiscount(
            cartId, itemId, DiscountType.AMOUNT, BigDecimal.TEN, "Test discount"
        );

        // Assert
        assertNotNull(response);
        assertEquals(DiscountType.AMOUNT, response.getItems().get(0).getDiscountType());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Apply Coupon Tests ====================

    @Test
    @DisplayName("Should apply coupon to cart successfully")
    void testApplyCoupon_Success() {
        // Arrange
        String cartId = "test-cart";
        String couponCode = "SUMMER20";

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.applyCoupon(cartId, couponCode);

        // Assert
        assertNotNull(response);
        assertTrue(response.getAppliedCoupons().contains(couponCode));
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when coupon code is empty")
    void testApplyCoupon_EmptyCode() {
        // Arrange
        String cartId = "test-cart";
        when(messageService.getMessage("coupon.code.required"))
            .thenReturn("Coupon code is required");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.applyCoupon(cartId, "")
        );
        assertEquals("Coupon code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when coupon already applied")
    void testApplyCoupon_AlreadyApplied() {
        // Arrange
        String cartId = "test-cart";
        String couponCode = "SUMMER20";

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[\"SUMMER20\"]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(messageService.getMessage("coupon.already.applied"))
            .thenReturn("Coupon code has already been applied to this cart");

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> cartService.applyCoupon(cartId, couponCode)
        );
        assertEquals("Coupon code has already been applied to this cart", exception.getMessage());
    }

    // ==================== Remove Coupon Tests ====================

    @Test
    @DisplayName("Should remove coupon from cart successfully")
    void testRemoveCoupon_Success() {
        // Arrange
        String cartId = "test-cart";
        String couponCode = "SUMMER20";

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[\"SUMMER20\"]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.removeCoupon(cartId, couponCode);

        // Assert
        assertNotNull(response);
        assertFalse(response.getAppliedCoupons().contains(couponCode));
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Apply Promotion Tests ====================

    @Test
    @DisplayName("Should apply promotion to cart successfully")
    void testApplyPromotion_Success() {
        // Arrange
        String cartId = "test-cart";
        String promotionId = "PROMO123";

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.applyPromotion(cartId, promotionId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getAppliedPromotions().contains(promotionId));
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Set Customer Tests ====================

    @Test
    @DisplayName("Should set customer for cart successfully")
    void testSetCartCustomer_Success() {
        // Arrange
        String cartId = "test-cart";
        Long customerId = 42L;

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.setCartCustomer(cartId, customerId);

        // Assert
        assertNotNull(response);
        assertEquals(customerId, response.getCustomerId());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Clear Cart Tests ====================

    @Test
    @DisplayName("Should clear all items from cart successfully")
    void testClearCart_Success() {
        // Arrange
        String cartId = "test-cart";

        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.clearCart(cartId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getTotal());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    // ==================== Abandon Cart Tests ====================

    @Test
    @DisplayName("Should mark cart as abandoned successfully")
    void testAbandonCart_Success() {
        // Arrange
        String cartId = "test-cart";

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        cartService.abandonCart(cartId);

        // Assert
        ArgumentCaptor<CartEntity> captor = ArgumentCaptor.forClass(CartEntity.class);
        verify(cartRepository).save(captor.capture());
        assertEquals(CartStatus.ABANDONED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getAbandonedAt());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null quantity when updating")
    void testUpdateCartItemQuantity_NullQuantity() {
        // Arrange
        String cartId = "test-cart";
        Long itemId = 1L;

        CartItemEntity item = CartItemEntity.builder()
            .id(itemId)
            .productId(1L)
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>(List.of(item)))
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.updateCartItemQuantity(cartId, itemId, null);

        // Assert
        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should add item with variants successfully")
    void testAddItemToCart_WithVariants() throws Exception {
        // Arrange
        String cartId = "test-cart";
        Map<String, String> variants = new HashMap<>();
        variants.put("size", "M");
        variants.put("color", "Red");

        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(1)
            .variants(variants)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L)
            .cartId(cartId)
            .status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        ProductDTO product = ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .sku("SKU-001")
            .price(BigDecimal.TEN)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L)
            .productId(1L)
            .quantityInStock(10L)
            .build();

        Page<InventoryDTO> inventoryPage = new PageImpl<>(List.of(inventory));

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(inventoryPage);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.addItemToCart(cartId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertNotNull(response.getItems().get(0).getVariants());
    }
}

