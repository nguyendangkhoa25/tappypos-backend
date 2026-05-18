package com.tappy.pos.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.order.CartItemResponse;
import com.tappy.pos.model.dto.order.CartRequest;
import com.tappy.pos.model.dto.order.CartResponse;
import com.tappy.pos.model.dto.order.CheckoutRequest;
import com.tappy.pos.model.dto.order.CheckoutResponse;
import com.tappy.pos.model.dto.order.SendToKitchenRequest;
import com.tappy.pos.model.dto.order.SendToKitchenResponse;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.product.ProductDTO;
import com.tappy.pos.model.dto.tenant.ShopInfoDTO;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.order.CartEntity;
import com.tappy.pos.model.entity.order.CartItemEntity;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.enums.CartStatus;
import com.tappy.pos.model.enums.DiscountType;
import com.tappy.pos.repository.order.CartItemRepository;
import com.tappy.pos.repository.order.CartRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.order.OrderRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.order.AddGoldItemRequest;
import com.tappy.pos.model.enums.DynamicPriceProductTypes;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.goldprice.GoldPriceService;
import com.tappy.pos.service.inventory.InventoryService;
import com.tappy.pos.service.tenant.ShopConfigService;
import com.tappy.pos.service.tenant.ShopInfoService;
import com.tappy.pos.service.customer.LoyaltyService;
import com.tappy.pos.service.product.ProductService;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.service.table.TableService;
import com.tappy.pos.service.notification.NotificationService;

import java.util.Set;

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
    private ShopConfigService shopConfigService;

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

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private GoldPriceService goldPriceService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FeatureContext featureContext;

    @Mock
    private TableService tableService;

    @Mock
    private NotificationService notificationService;

    private CartServiceImpl cartService;
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String username) {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(shopConfigService.getDouble(any(), anyDouble())).thenReturn(0.10);
        cartService = new CartServiceImpl(
            cartRepository,
            cartItemRepository,
            orderRepository,
            customerRepository,
            messageService,
            objectMapper,
            inventoryService,
            productService,
            shopConfigService,
            shopInfoService,
            promotionService,
            loyaltyService,
            activityLogService,
            tenantContext,
            goldPriceService,
            employeeRepository,
            featureContext,
            tableService,
            notificationService
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

    // ==================== Checkout Tests ====================

    private CartEntity buildActiveCartWithItem() {
        CartItemEntity item = CartItemEntity.builder()
                .id(1L)
                .productId(1L)
                .productName("Coca Cola")
                .sku("SKU-001")
                .quantity(2)
                .basePrice(new BigDecimal("15000"))
                .unitPrice(new BigDecimal("15000"))
                .unitCost(new BigDecimal("10000"))
                .discountType(DiscountType.NONE)
                .discountValue(BigDecimal.ZERO)
                .variants("{}")
                .build();
        item.recalculateLineTotal();

        CartEntity cart = CartEntity.builder()
                .id(1L)
                .cartId("cart-001")
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(item)))
                .subtotal(new BigDecimal("30000"))
                .totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .total(new BigDecimal("30000"))
                .appliedCoupons("[]")
                .appliedPromotions("[]")
                .build();
        return cart;
    }

    @Test
    @DisplayName("checkout creates COMPLETED order and returns receipt")
    void testCheckout_Success() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod("CASH");
        request.setAmountPaid(new BigDecimal("50000"));

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderNumber("ORD-20240101-12345");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);
        savedOrder.setTotalAmount(new BigDecimal("33000"));

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));

        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);
        when(shopInfoService.getShopInfo()).thenReturn(ShopInfoDTO.builder().shopName("Test Shop").build());

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-12345");
        assertThat(response.getPaymentMethod()).isEqualTo("CASH");
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository, atLeastOnce()).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("checkout throws BadRequestException when cart is empty")
    void testCheckout_EmptyCart() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-001").status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).subtotal(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .build();

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.empty")).thenReturn("Cart is empty");

        assertThatThrownBy(() -> cartService.checkout("cart-001", new CheckoutRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("checkout throws BadRequestException when cart is not ACTIVE")
    void testCheckout_NotActiveCart() {
        CartEntity cart = buildActiveCartWithItem();
        cart.setStatus(CartStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.not.active")).thenReturn("Cart not active");

        assertThatThrownBy(() -> cartService.checkout("cart-001", new CheckoutRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("checkout applies percentage discount correctly")
    void testCheckout_WithPercentageDiscount() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        CheckoutRequest request = new CheckoutRequest();
        request.setDiscountAmount(new BigDecimal("10"));
        request.setDiscountType(DiscountType.PERCENTAGE);

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderNumber("ORD-TEST");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);
        savedOrder.setTotalAmount(new BigDecimal("29700"));

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));

        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderDiscount()).isPositive();
    }

    @Test
    @DisplayName("checkout applies fixed discount correctly")
    void testCheckout_WithFixedDiscount() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        CheckoutRequest request = new CheckoutRequest();
        request.setDiscountAmount(new BigDecimal("5000"));
        request.setDiscountType(DiscountType.AMOUNT);

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderNumber("ORD-FIXED");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);
        savedOrder.setTotalAmount(new BigDecimal("27500"));

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));

        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response.getOrderDiscount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("checkout cancels pending kitchen order if provided")
    void testCheckout_CancelsPendingOrder() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        CheckoutRequest request = new CheckoutRequest();
        request.setPendingOrderId(50L);

        Order pendingOrder = new Order();
        pendingOrder.setId(50L);
        pendingOrder.setOrderNumber("KITCHEN-001");
        pendingOrder.setStatus(Order.OrderStatus.PENDING);

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderNumber("ORD-FINAL");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));

        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        cartService.checkout("cart-001", request);

        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    @DisplayName("checkout assigns customer when customerId is in request")
    void testCheckout_WithCustomer() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        Customer customer = Customer.builder().name("Nguyễn Văn A").phone("0901234567").build();
        customer.setId(10L);

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerId(10L);

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderNumber("ORD-CUST");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response.getCustomerName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getCustomerId()).isEqualTo(10L);
    }

    // ==================== SendToKitchen Tests ====================

    @Test
    @DisplayName("sendToKitchen creates PENDING order and returns kitchen ticket")
    void testSendToKitchen_Success() {
        mockSecurityContext("waiter01");
        CartEntity cart = buildActiveCartWithItem();

        SendToKitchenRequest request = new SendToKitchenRequest();
        request.setTableLabel("Bàn 5");
        request.setNotes("Ít cay");

        Order savedOrder = new Order();
        savedOrder.setId(200L);
        savedOrder.setOrderNumber("KITCHEN-20240101-001");
        savedOrder.setStatus(Order.OrderStatus.PENDING);
        savedOrder.setTableLabel("Bàn 5");

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        SendToKitchenResponse response = cartService.sendToKitchen("cart-001", request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("KITCHEN-20240101-001");
        assertThat(response.getTableLabel()).isEqualTo("Bàn 5");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("sendToKitchen throws BadRequestException for empty cart")
    void testSendToKitchen_EmptyCart() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-001").status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.empty")).thenReturn("Cart is empty");

        assertThatThrownBy(() -> cartService.sendToKitchen("cart-001", new SendToKitchenRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("sendToKitchen throws BadRequestException for non-ACTIVE cart")
    void testSendToKitchen_NotActiveCart() {
        CartEntity cart = buildActiveCartWithItem();
        cart.setStatus(CartStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.not.active")).thenReturn("Not active");

        assertThatThrownBy(() -> cartService.sendToKitchen("cart-001", new SendToKitchenRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("sendToKitchen resolves customer by ID when provided")
    void testSendToKitchen_WithCustomer() {
        mockSecurityContext("waiter01");
        CartEntity cart = buildActiveCartWithItem();

        Customer customer = Customer.builder().name("Phòng VIP").phone("0900000001").build();
        customer.setId(5L);

        SendToKitchenRequest request = new SendToKitchenRequest();
        request.setCustomerId(5L);

        Order savedOrder = new Order();
        savedOrder.setId(201L);
        savedOrder.setOrderNumber("KITCHEN-VIP");
        savedOrder.setStatus(Order.OrderStatus.PENDING);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(customerRepository.findByIdActive(5L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        SendToKitchenResponse response = cartService.sendToKitchen("cart-001", request);

        assertThat(response.getOrderId()).isEqualTo(201L);
        verify(customerRepository).findByIdActive(5L);
    }

    // ── Checkout — promotion code path ────────────────────────────────────────

    @Test
    @DisplayName("checkout applies promotion discount when promotionCode is provided")
    void testCheckout_WithPromotionCode() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        CheckoutRequest request = new CheckoutRequest();
        request.setPromotionCode("PROMO10");

        Order savedOrder = new Order();
        savedOrder.setId(300L);
        savedOrder.setOrderNumber("ORD-PROMO");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));

        when(promotionService.applyAtCheckout(eq("PROMO10"), any())).thenReturn(new BigDecimal("3000"));
        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response).isNotNull();
        verify(promotionService).applyAtCheckout(eq("PROMO10"), any());
    }

    // ── Checkout — loyalty redemption path ────────────────────────────────────

    @Test
    @DisplayName("checkout redeems loyalty points when customer and points provided")
    void testCheckout_WithLoyaltyRedemption() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();
        cart.setCustomerId(10L);

        Customer customer = Customer.builder().name("Loyal Customer").phone("0900000001").build();
        customer.setId(10L);

        CheckoutRequest request = new CheckoutRequest();
        request.setLoyaltyPointsToRedeem(100);

        Order savedOrder = new Order();
        savedOrder.setId(400L);
        savedOrder.setOrderNumber("ORD-LOYAL");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(customerRepository.findByIdActive(10L)).thenReturn(Optional.of(customer));
        when(loyaltyService.redeemPoints(eq(10L), eq(100), isNull())).thenReturn(new BigDecimal("5000"));
        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response).isNotNull();
        verify(loyaltyService).redeemPoints(eq(10L), eq(100), isNull());
    }

    // ── Checkout — fallback to Khách Lẻ ─────────────────────────────────────

    @Test
    @DisplayName("checkout falls back to Khách Lẻ customer when no customer in cart or request")
    void testCheckout_FallbackToKhachLe() {
        mockSecurityContext("cashier01");
        CartEntity cart = buildActiveCartWithItem();

        Customer khachLe = Customer.builder().name("Khách Lẻ").phone("0000000000").build();
        khachLe.setId(1L);

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerName("Walk-in");

        Order savedOrder = new Order();
        savedOrder.setId(500L);
        savedOrder.setOrderNumber("ORD-WALKIN");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.of(khachLe));
        when(inventoryService.getInventoryByProductId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(InventoryDTO.builder().id(5L).quantityInStock(10L).build())));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-001", request);

        assertThat(response).isNotNull();
        verify(customerRepository).findByPhone("0000000000");
    }

    // ── sendToKitchen — customer name from request ────────────────────────────

    @Test
    @DisplayName("sendToKitchen uses customerName from request when customerId not found")
    void testSendToKitchen_CustomerNameFallback() {
        mockSecurityContext("waiter01");
        CartEntity cart = buildActiveCartWithItem();

        SendToKitchenRequest request = new SendToKitchenRequest();
        request.setCustomerName("Table 3 Guest");

        Order savedOrder = new Order();
        savedOrder.setId(202L);
        savedOrder.setOrderNumber("KITCHEN-TBL3");
        savedOrder.setStatus(Order.OrderStatus.PENDING);

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        SendToKitchenResponse response = cartService.sendToKitchen("cart-001", request);

        assertThat(response).isNotNull();
    }

    // ── parseCoupons/parsePromotions invalid JSON path ─────────────────────────

    @Test
    @DisplayName("applyCoupon handles existing invalid JSON in appliedCoupons gracefully")
    void testApplyCoupon_InvalidJson_GracefulFallback() {
        CartEntity cart = CartEntity.builder()
            .id(1L).cartId("cart-001").status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("not-valid-json")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.applyCoupon("cart-001", "NEWCODE");

        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("removeCoupon handles invalid JSON in appliedCoupons gracefully")
    void testRemoveCoupon_InvalidJson_GracefulFallback() {
        CartEntity cart = CartEntity.builder()
            .id(1L).cartId("cart-001").status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("not-valid-json")
            .appliedPromotions("[]")
            .build();

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.removeCoupon("cart-001", "ANYCODE");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("applyPromotion handles invalid JSON in appliedPromotions gracefully")
    void testApplyPromotion_InvalidJson_GracefulFallback() {
        CartEntity cart = CartEntity.builder()
            .id(1L).cartId("cart-001").status(CartStatus.ACTIVE)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("not-valid-json")
            .build();

        when(cartRepository.findByCartId("cart-001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.applyPromotion("cart-001", "PROMO1");

        assertThat(response).isNotNull();
    }

    // ==================== Dynamic-Price (JEWELRY) Tests ====================

    @Test
    @DisplayName("Should calculate dynamic unit price for JEWELRY product using gold price")
    void testAddItemToCart_JewelryProduct_DynamicPrice() throws Exception {
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder()
            .productId(1L)
            .quantity(1)
            .build();

        CartEntity cart = CartEntity.builder()
            .id(1L).cartId(cartId).status(CartStatus.ACTIVE)
            .items(new ArrayList<>()).appliedCoupons("[]").appliedPromotions("[]")
            .build();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("gold_weight", "2.5");
        attrs.put("sell_proc_price", "300000");

        ProductDTO product = ProductDTO.builder()
            .id(1L).name("Nhẫn vàng 18K").sku("KV-001")
            .price(BigDecimal.ZERO).status("ACTIVE")
            .productTypeCode("JEWELRY")
            .categoryIds(Set.of(10L))
            .attributes(attrs)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L).productId(1L).quantityInStock(1L).build();

        GoldPriceDTO goldPrice = GoldPriceDTO.builder()
            .sell(new BigDecimal("6000000"))
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(new PageImpl<>(List.of(inventory)));
        when(goldPriceService.getPriceForCategory(10L)).thenReturn(goldPrice);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(cartId, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        // Expected: 2.5 × 6,000,000 + 300,000 = 15,300,000
        assertEquals(new BigDecimal("15300000"), response.getItems().get(0).getUnitPrice());
        verify(goldPriceService).getPriceForCategory(10L);
    }

    @Test
    @DisplayName("Should throw BadRequestException when JEWELRY product has no category")
    void testAddItemToCart_JewelryProduct_NoCategory() {
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder().productId(1L).quantity(1).build();

        CartEntity cart = CartEntity.builder()
            .id(1L).cartId(cartId).status(CartStatus.ACTIVE)
            .items(new ArrayList<>()).build();

        ProductDTO product = ProductDTO.builder()
            .id(1L).name("Nhẫn vàng").sku("KV-002")
            .price(BigDecimal.ZERO).status("ACTIVE")
            .productTypeCode("JEWELRY")
            .categoryIds(Set.of())
            .attributes(new HashMap<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(messageService.getMessage("error.product.dynamic.price.no.category"))
            .thenReturn("Jewelry product must have a gold type category");

        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(cartId, request));
    }

    @Test
    @DisplayName("Should throw BadRequestException when no gold price configured for category")
    void testAddItemToCart_JewelryProduct_NoGoldPrice() {
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder().productId(1L).quantity(1).build();

        CartEntity cart = CartEntity.builder()
            .id(1L).cartId(cartId).status(CartStatus.ACTIVE)
            .items(new ArrayList<>()).build();

        ProductDTO product = ProductDTO.builder()
            .id(1L).name("Nhẫn vàng").sku("KV-003")
            .price(BigDecimal.ZERO).status("ACTIVE")
            .productTypeCode("JEWELRY")
            .categoryIds(Set.of(99L))
            .attributes(new HashMap<>())
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(goldPriceService.getPriceForCategory(99L))
            .thenThrow(new RuntimeException("No gold price configured"));
        when(messageService.getMessage("error.product.dynamic.price.no.gold.price"))
            .thenReturn("No gold price configured for this gold type");

        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(cartId, request));
    }

    @Test
    @DisplayName("Should use proc_price fallback when sell_proc_price is absent")
    void testAddItemToCart_JewelryProduct_ProcPriceFallback() throws Exception {
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder().productId(1L).quantity(1).build();

        CartEntity cart = CartEntity.builder()
            .id(1L).cartId(cartId).status(CartStatus.ACTIVE)
            .items(new ArrayList<>()).appliedCoupons("[]").appliedPromotions("[]")
            .build();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("gold_weight", "1.0");
        attrs.put("proc_price", "200000"); // fallback field, no sell_proc_price

        ProductDTO product = ProductDTO.builder()
            .id(1L).name("Dây chuyền vàng").sku("KV-004")
            .price(BigDecimal.ZERO).status("ACTIVE")
            .productTypeCode("JEWELRY")
            .categoryIds(Set.of(10L))
            .attributes(attrs)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L).productId(1L).quantityInStock(1L).build();

        GoldPriceDTO goldPrice = GoldPriceDTO.builder()
            .sell(new BigDecimal("5000000"))
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(new PageImpl<>(List.of(inventory)));
        when(goldPriceService.getPriceForCategory(10L)).thenReturn(goldPrice);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(cartId, request);

        assertNotNull(response);
        // Expected: 1.0 × 5,000,000 + 200,000 = 5,200,000
        assertEquals(new BigDecimal("5200000"), response.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("Should calculate zero proc fee when no fee attributes present")
    void testAddItemToCart_JewelryProduct_ZeroProcFee() throws Exception {
        String cartId = "test-cart";
        CartRequest request = CartRequest.builder().productId(1L).quantity(1).build();

        CartEntity cart = CartEntity.builder()
            .id(1L).cartId(cartId).status(CartStatus.ACTIVE)
            .items(new ArrayList<>()).appliedCoupons("[]").appliedPromotions("[]")
            .build();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("gold_weight", "3.0");
        // no proc fee attributes

        ProductDTO product = ProductDTO.builder()
            .id(1L).name("Vòng tay vàng").sku("KV-005")
            .price(BigDecimal.ZERO).status("ACTIVE")
            .productTypeCode("JEWELRY")
            .categoryIds(Set.of(10L))
            .attributes(attrs)
            .build();

        InventoryDTO inventory = InventoryDTO.builder()
            .id(1L).productId(1L).quantityInStock(1L).build();

        GoldPriceDTO goldPrice = GoldPriceDTO.builder()
            .sell(new BigDecimal("4000000"))
            .build();

        when(cartRepository.findByCartId(cartId)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1L)).thenReturn(product);
        when(inventoryService.getInventoryByProductId(1L, PageRequest.of(0, 1)))
            .thenReturn(new PageImpl<>(List.of(inventory)));
        when(goldPriceService.getPriceForCategory(10L)).thenReturn(goldPrice);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(cartId, request);

        assertNotNull(response);
        // Expected: 3.0 × 4,000,000 + 0 = 12,000,000
        assertEquals(new BigDecimal("12000000"), response.getItems().get(0).getUnitPrice());
    }

    // ==================== DynamicPriceProductTypes Tests ====================

    @Test
    @DisplayName("DynamicPriceProductTypes.isDynamicPrice returns true for JEWELRY")
    void testDynamicPriceProductTypes_Jewelry() {
        assertTrue(DynamicPriceProductTypes.isDynamicPrice("JEWELRY"));
    }

    @Test
    @DisplayName("DynamicPriceProductTypes.isDynamicPrice returns false for non-dynamic types")
    void testDynamicPriceProductTypes_NonDynamic() {
        assertFalse(DynamicPriceProductTypes.isDynamicPrice("FOOD"));
        assertFalse(DynamicPriceProductTypes.isDynamicPrice("ELECTRONICS"));
        assertFalse(DynamicPriceProductTypes.isDynamicPrice("DRUG"));
    }

    @Test
    @DisplayName("DynamicPriceProductTypes.isDynamicPrice returns false for null")
    void testDynamicPriceProductTypes_Null() {
        assertFalse(DynamicPriceProductTypes.isDynamicPrice(null));
    }

    @Test
    @DisplayName("DynamicPriceProductTypes.CODES contains JEWELRY")
    void testDynamicPriceProductTypes_CodesSet() {
        assertThat(DynamicPriceProductTypes.CODES).contains("JEWELRY");
    }

    // ==================== addGoldItem Tests ====================

    @Test
    @DisplayName("addGoldItem: throws ResourceNotFoundException when cart not found")
    void addGoldItem_cartNotFound() {
        when(cartRepository.findByCartId("missing-cart")).thenReturn(Optional.empty());

        AddGoldItemRequest req = new AddGoldItemRequest();
        req.setItemType(CartItemEntity.ItemType.GOLD_IN);
        req.setGoldType("Vàng 24K");
        req.setGoldWeight(new BigDecimal("1.5"));
        req.setUnitPrice(new BigDecimal("8000000"));

        assertThatThrownBy(() -> cartService.addGoldItem("missing-cart", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("addGoldItem: throws BadRequestException when cart is not ACTIVE")
    void addGoldItem_cartNotActive() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-123").status(CartStatus.COMPLETED)
                .items(new ArrayList<>()).build();
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(cart));

        AddGoldItemRequest req = new AddGoldItemRequest();
        req.setItemType(CartItemEntity.ItemType.GOLD_IN);
        req.setGoldType("Vàng 24K");
        req.setGoldWeight(new BigDecimal("1.5"));
        req.setUnitPrice(new BigDecimal("8000000"));

        assertThatThrownBy(() -> cartService.addGoldItem("cart-123", req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addGoldItem: throws BadRequestException when itemType is STANDARD")
    void addGoldItem_standardItemTypeThrows() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-123").status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(cart));

        AddGoldItemRequest req = new AddGoldItemRequest();
        req.setItemType(CartItemEntity.ItemType.STANDARD);
        req.setGoldType("Vàng 24K");
        req.setGoldWeight(new BigDecimal("1.5"));
        req.setUnitPrice(new BigDecimal("8000000"));

        assertThatThrownBy(() -> cartService.addGoldItem("cart-123", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("GOLD_IN or GOLD_OUT");
    }

    @Test
    @DisplayName("addGoldItem: successfully adds GOLD_IN item to cart")
    void addGoldItem_goldIn_success() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-123").status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .items(new ArrayList<>()).appliedCoupons("[]").appliedPromotions("[]")
                .build();
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(cart));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        AddGoldItemRequest req = new AddGoldItemRequest();
        req.setItemType(CartItemEntity.ItemType.GOLD_IN);
        req.setGoldType("Vàng SJC 9999");
        req.setGoldBrand("SJC");
        req.setGoldWeight(new BigDecimal("2.0"));
        req.setGemWeight(new BigDecimal("0.1"));
        req.setProcPrice(new BigDecimal("50000"));
        req.setUnitPrice(new BigDecimal("8200000"));
        req.setNotes("Mua từ khách");

        CartResponse response = cartService.addGoldItem("cart-123", req);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(CartEntity.class));
    }

    @Test
    @DisplayName("addGoldItem: GOLD_OUT item uses null gold brand when none provided")
    void addGoldItem_goldOut_noBrand() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-456").status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .items(new ArrayList<>()).appliedCoupons("[]").appliedPromotions("[]")
                .build();
        when(cartRepository.findByCartId("cart-456")).thenReturn(Optional.of(cart));
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        AddGoldItemRequest req = new AddGoldItemRequest();
        req.setItemType(CartItemEntity.ItemType.GOLD_OUT);
        req.setGoldType("Vàng 18K");
        // goldBrand not set → null
        req.setGoldWeight(new BigDecimal("1.0"));
        req.setUnitPrice(new BigDecimal("7500000"));
        // procPrice not set → defaults to ZERO
        // gemWeight not set → defaults to ZERO

        CartResponse response = cartService.addGoldItem("cart-456", req);

        assertThat(response).isNotNull();
    }

    // ── updateItemCommission ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateItemCommission: clears commission when assignedEmployeeId is null")
    void updateItemCommission_clearCommission() {
        CartItemEntity item = CartItemEntity.builder()
                .id(1L).productId(1L).productName("SP A").sku("SKU-001")
                .quantity(1).basePrice(BigDecimal.valueOf(100_000))
                .unitPrice(BigDecimal.valueOf(100_000))
                .lineSubtotal(BigDecimal.valueOf(100_000))
                .lineTotal(BigDecimal.valueOf(100_000)).lineGrandTotal(BigDecimal.valueOf(100_000))
                .discountValue(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .discountType(com.tappy.pos.model.enums.DiscountType.NONE)
                .variants("{}").addedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-upd").status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(item)))
                .appliedCoupons("[]").appliedPromotions("[]").build();

        when(cartRepository.findByCartId("cart-upd")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.updateItemCommission("cart-upd", 1L, null, null);

        assertThat(response).isNotNull();
        assertThat(item.getAssignedEmployeeId()).isNull();
        assertThat(item.getCommissionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("updateItemCommission: assigns employee with commission rate from employee")
    void updateItemCommission_withEmployee() {
        CartItemEntity item = CartItemEntity.builder()
                .id(1L).productId(1L).productName("SP A").sku("SKU-001")
                .quantity(1).basePrice(BigDecimal.valueOf(100_000))
                .unitPrice(BigDecimal.valueOf(100_000))
                .lineSubtotal(BigDecimal.valueOf(100_000))
                .lineTotal(BigDecimal.valueOf(100_000)).lineGrandTotal(BigDecimal.valueOf(100_000))
                .discountValue(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .discountType(com.tappy.pos.model.enums.DiscountType.NONE)
                .variants("{}").addedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-emp").status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(item)))
                .appliedCoupons("[]").appliedPromotions("[]").build();

        Employee emp = Employee.builder().id(5L).fullName("Nhân viên A").commissionRate(BigDecimal.TEN).build();

        ProductDTO product = ProductDTO.builder().id(1L).commissionRate(null).build();

        when(cartRepository.findByCartId("cart-emp")).thenReturn(Optional.of(cart));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(emp));
        when(productService.getProductById(1L)).thenReturn(product);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.updateItemCommission("cart-emp", 1L, 5L, null);

        assertThat(response).isNotNull();
        assertThat(item.getAssignedEmployeeId()).isEqualTo(5L);
        assertThat(item.getAssignedEmployeeName()).isEqualTo("Nhân viên A");
    }

    @Test
    @DisplayName("updateItemCommission: cart not found → ResourceNotFoundException")
    void updateItemCommission_cartNotFound_throws() {
        when(cartRepository.findByCartId("no-cart")).thenReturn(Optional.empty());
        when(messageService.getMessage("cart.not.found")).thenReturn("Cart not found");

        assertThatThrownBy(() -> cartService.updateItemCommission("no-cart", 1L, null, null))
                .isInstanceOf(com.tappy.pos.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateItemCommission: item not in cart → ResourceNotFoundException")
    void updateItemCommission_itemNotFound_throws() {
        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-miss").status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .appliedCoupons("[]").appliedPromotions("[]").build();

        when(cartRepository.findByCartId("cart-miss")).thenReturn(Optional.of(cart));
        when(messageService.getMessage("cart.item.not.found")).thenReturn("Item not found");

        assertThatThrownBy(() -> cartService.updateItemCommission("cart-miss", 99L, null, null))
                .isInstanceOf(com.tappy.pos.exception.ResourceNotFoundException.class);
    }

    // ── checkout — EXCHANGE order type ────────────────────────────────────────

    @Test
    @DisplayName("checkout computes net as sellSum - buySum for EXCHANGE order type")
    void testCheckout_ExchangeOrderType() {
        mockSecurityContext("cashier01");

        CartItemEntity goldOut = CartItemEntity.builder()
                .id(1L).productId(1L).productName("Vàng bán ra")
                .sku("GOLD-OUT").quantity(1)
                .basePrice(new BigDecimal("10000000")).unitPrice(new BigDecimal("10000000"))
                .unitCost(new BigDecimal("9000000"))
                .taxRate(BigDecimal.ZERO)
                .discountType(DiscountType.NONE).discountValue(BigDecimal.ZERO)
                .itemType(CartItemEntity.ItemType.GOLD_OUT)
                .variants("{}")
                .build();
        goldOut.recalculateLineTotal();

        CartItemEntity goldIn = CartItemEntity.builder()
                .id(2L).productId(2L).productName("Vàng mua vào")
                .sku("GOLD-IN").quantity(1)
                .basePrice(new BigDecimal("4000000")).unitPrice(new BigDecimal("4000000"))
                .unitCost(new BigDecimal("3500000"))
                .taxRate(BigDecimal.ZERO)
                .discountType(DiscountType.NONE).discountValue(BigDecimal.ZERO)
                .itemType(CartItemEntity.ItemType.GOLD_IN)
                .variants("{}")
                .build();
        goldIn.recalculateLineTotal();

        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-exc").status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(goldOut, goldIn)))
                .subtotal(new BigDecimal("14000000")).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(new BigDecimal("14000000"))
                .taxRate(BigDecimal.ZERO)
                .appliedCoupons("[]").appliedPromotions("[]").build();

        CheckoutRequest request = new CheckoutRequest();
        request.setOrderType(Order.OrderType.EXCHANGE);

        Order savedOrder = new Order();
        savedOrder.setId(600L);
        savedOrder.setOrderNumber("ORD-EXC-001");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-exc")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-exc", request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-EXC-001");
        // net = GOLD_OUT total (10M) - GOLD_IN total (4M) = 6M
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("6000000"));
    }

    // ── checkout — BUY order type ─────────────────────────────────────────────

    @Test
    @DisplayName("checkout uses cart.getTotal() for BUY order type (shop pays customer)")
    void testCheckout_BuyOrderType() {
        mockSecurityContext("cashier01");

        CartItemEntity goldIn = CartItemEntity.builder()
                .id(1L).productId(1L).productName("Vàng mua vào")
                .sku("GOLD-BUY").quantity(1)
                .basePrice(new BigDecimal("5000000")).unitPrice(new BigDecimal("5000000"))
                .unitCost(new BigDecimal("5000000"))
                .taxRate(BigDecimal.ZERO)
                .discountType(DiscountType.NONE).discountValue(BigDecimal.ZERO)
                .itemType(CartItemEntity.ItemType.GOLD_IN)
                .variants("{}")
                .build();
        goldIn.recalculateLineTotal();

        CartEntity cart = CartEntity.builder()
                .id(1L).cartId("cart-buy").status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(goldIn)))
                .subtotal(new BigDecimal("5000000")).totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO).total(new BigDecimal("5000000"))
                .taxRate(BigDecimal.ZERO)
                .appliedCoupons("[]").appliedPromotions("[]").build();

        CheckoutRequest request = new CheckoutRequest();
        request.setOrderType(Order.OrderType.BUY);

        Order savedOrder = new Order();
        savedOrder.setId(700L);
        savedOrder.setOrderNumber("ORD-BUY-001");
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);

        when(cartRepository.findByCartId("cart-buy")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CheckoutResponse response = cartService.checkout("cart-buy", request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-BUY-001");
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("5000000"));
    }
}

