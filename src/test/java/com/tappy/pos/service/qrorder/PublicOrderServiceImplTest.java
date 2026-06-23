package com.tappy.pos.service.qrorder;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.modifier.ChosenModifierDTO;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.qrorder.PublicOrderResponse;
import com.tappy.pos.model.dto.qrorder.PublicTableDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.notification.NotificationService;
import com.tappy.pos.service.tenant.ShopConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicOrderServiceImpl Unit Tests")
class PublicOrderServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private TableRepository tableRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;
    @Mock private com.tappy.pos.service.modifier.ModifierService modifierService;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks private PublicOrderServiceImpl service;

    private static final String TOKEN = "tok-123";

    private Tenant tenant(String features) {
        return Tenant.builder().tenantId("shop1").name("Quán Test").features(features).build();
    }

    private ShopTable table() {
        ShopTable t = ShopTable.builder().tableNumber("A1").qrToken(TOKEN).build();
        t.setId(7L);
        t.setTenantId("shop1");
        return t;
    }

    private Product product(long id, String name, String price) {
        Product p = Product.builder().sku("SKU" + id).name(name)
                .price(new BigDecimal(price)).costPrice(BigDecimal.ZERO)
                .status(Product.ProductStatus.ACTIVE).build();
        p.setId(id);
        return p;
    }

    private PublicOrderRequest request(long productId, int qty) {
        PublicOrderRequest req = new PublicOrderRequest();
        PublicOrderRequest.Line line = new PublicOrderRequest.Line();
        line.setProductId(productId);
        line.setQuantity(qty);
        req.setItems(List.of(line));
        return req;
    }

    private Category category(long id, String name) {
        Category c = Category.builder().name(name).build();
        c.setId(id);
        c.setTenantId("shop1");
        return c;
    }

    private ModifierGroupDTO.OptionDTO option(long id, String name, String delta) {
        return ModifierGroupDTO.OptionDTO.builder()
                .id(id).name(name).priceDelta(delta == null ? null : new BigDecimal(delta)).build();
    }

    private ModifierGroupDTO group(long id, String name, boolean required, Integer min, Integer max,
                                   ModifierGroupDTO.OptionDTO... opts) {
        return ModifierGroupDTO.builder()
                .id(id).name(name).required(required).minSelect(min).maxSelect(max)
                .options(List.of(opts)).build();
    }

    private PublicOrderRequest requestWithModifiers(long productId, int qty, List<Long> optionIds) {
        PublicOrderRequest req = new PublicOrderRequest();
        PublicOrderRequest.Line line = new PublicOrderRequest.Line();
        line.setProductId(productId);
        line.setQuantity(qty);
        line.setModifierOptionIds(optionIds);
        req.setItems(List.of(line));
        return req;
    }

    // ── getShop ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getShop returns only the shop name for a shop-wide QR (no table)")
    void getShop_returnsShopNameOnly() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));

        PublicTableDTO dto = service.getShop();

        assertThat(dto.getShopName()).isEqualTo("Quán Test");
        assertThat(dto.getTableId()).isNull();
        assertThat(dto.getTableLabel()).isNull();
    }

    @Test
    @DisplayName("getShop is refused when there is no tenant context")
    void getShop_refusedWithoutTenant() {
        when(tenantContext.getCurrentTenant()).thenReturn(null);
        lenient().when(messageService.getMessage("error.qr.ordering.unavailable")).thenReturn("unavailable");

        assertThatThrownBy(() -> service.getShop())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMenu ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMenu groups by category, attaches modifier groups, and adds an uncategorised bucket")
    void getMenu_groupsCategoriesAndModifiersAndUncategorised() {
        Product withCat = product(1L, "Trà sữa", "30000");
        withCat.getCategories().add(category(10L, "Đồ uống"));
        Product noCat = product(2L, "Món lạ", "20000");

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE))
                .thenReturn(List.of(withCat, noCat));
        ModifierGroupDTO mod = group(100L, "Size", true, 1, 1, option(1000L, "L", "5000"));
        when(modifierService.getGroupsForProducts(anyList()))
                .thenReturn(Map.of(1L, List.of(mod)));
        when(messageService.getMessage("qr.menu.uncategorised")).thenReturn("Khác");

        PublicMenuDTO menu = service.getMenu();

        assertThat(menu.getShopName()).isEqualTo("Quán Test");
        assertThat(menu.getCategories()).hasSize(2);
        PublicMenuDTO.MenuCategory drinks = menu.getCategories().get(0);
        assertThat(drinks.getName()).isEqualTo("Đồ uống");
        assertThat(drinks.getId()).isEqualTo(10L);
        assertThat(drinks.getItems()).hasSize(1);
        assertThat(drinks.getItems().get(0).getModifierGroups()).containsExactly(mod);
        // trailing uncategorised bucket
        PublicMenuDTO.MenuCategory other = menu.getCategories().get(1);
        assertThat(other.getId()).isNull();
        assertThat(other.getName()).isEqualTo("Khác");
        assertThat(other.getItems()).hasSize(1);
        assertThat(other.getItems().get(0).getName()).isEqualTo("Món lạ");
    }

    @Test
    @DisplayName("getMenu with only categorised products produces no uncategorised bucket")
    void getMenu_noUncategorisedBucket() {
        Product withCat = product(1L, "Cà phê", "25000");
        withCat.getCategories().add(category(10L, "Đồ uống"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE))
                .thenReturn(List.of(withCat));
        when(modifierService.getGroupsForProducts(anyList())).thenReturn(Map.of());

        PublicMenuDTO menu = service.getMenu();

        assertThat(menu.getCategories()).hasSize(1);
        assertThat(menu.getCategories().get(0).getName()).isEqualTo("Đồ uống");
    }

    // ── submitShopOrder (no-table) ───────────────────────────────────────────────

    @Test
    @DisplayName("submitShopOrder creates a shop-wide order with no table and pushes a no-table notification")
    void submitShopOrder_noTable() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phở", "50000")));
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderResponse resp = service.submitShopOrder(request(1L, 1));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getTableId()).isNull();
        assertThat(saved.getTableLabel()).isNull();
        assertThat(saved.getSource()).isEqualTo("QR");
        assertThat(resp.getOrderNumber()).startsWith("QR-");

        // no-table notification title key is fired
        ArgumentCaptor<LocalizedText> titleCap = ArgumentCaptor.forClass(LocalizedText.class);
        verify(notificationService).pushToRolesAsync(
                eq(Notification.NotificationType.ORDER), titleCap.capture(), any(LocalizedText.class),
                eq("ORDER"), any(), anyList(), eq("shop1"));
        assertThat(titleCap.getValue().key()).isEqualTo("notification.qr.order.title.notable");
    }

    @Test
    @DisplayName("submitShopOrder swallows a notification-push failure and still returns the order")
    void submitShopOrder_notificationFailureSwallowed() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phở", "50000")));
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("push down"))
                .when(notificationService).pushToRolesAsync(any(), any(), any(), any(), any(), anyList(), any());

        PublicOrderResponse resp = service.submitShopOrder(request(1L, 1));

        assertThat(resp.getOrderNumber()).startsWith("QR-");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("submitOrder attaches the walk-in customer when one exists")
    void submitOrder_attachesWalkInCustomer() {
        com.tappy.pos.model.entity.customer.Customer walkIn = new com.tappy.pos.model.entity.customer.Customer();
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phở", "50000")));
        when(customerRepository.findByPhoneAndTenantId(any(), eq("shop1"))).thenReturn(Optional.of(walkIn));
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submitOrder(TOKEN, request(1L, 1));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomer()).isSameAs(walkIn);
    }

    @Test
    @DisplayName("submitOrder builds notes from customer name and note")
    void submitOrder_buildsNotes() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phở", "50000")));
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderRequest req = request(1L, 1);
        req.setCustomerName("  Anh Ba  ");
        req.setNote("  ít cay  ");
        service.submitOrder(TOKEN, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("Anh Ba — ít cay");
    }

    // ── modifier resolution + validation ─────────────────────────────────────────

    @Test
    @DisplayName("submitOrder resolves chosen modifiers, adds the price delta and serialises them")
    void submitOrder_withValidModifiers() throws Exception {
        Product p = product(1L, "Trà sữa", "30000");
        ModifierGroupDTO size = group(100L, "Size", true, 1, 1, option(1000L, "L", "5000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(size));
        when(modifierService.resolveOptions(List.of(1000L))).thenReturn(List.of(
                ChosenModifierDTO.builder().groupName("Size").optionName("L").priceDelta(new BigDecimal("5000")).build()));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{\"optionName\":\"L\"}]");
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(1000L)));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        // 30000 base + 5000 delta
        assertThat(captor.getValue().getOrderItems().get(0).getUnitPrice()).isEqualByComparingTo("35000");
        assertThat(captor.getValue().getOrderItems().get(0).getModifiers()).isEqualTo("[{\"optionName\":\"L\"}]");
    }

    @Test
    @DisplayName("submitOrder returns null modifiers JSON when serialisation fails")
    void submitOrder_modifierSerialisationFails() throws Exception {
        Product p = product(1L, "Trà sữa", "30000");
        ModifierGroupDTO size = group(100L, "Size", false, 0, 1, option(1000L, "L", "5000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(size));
        when(modifierService.resolveOptions(List.of(1000L))).thenReturn(List.of(
                ChosenModifierDTO.builder().groupName("Size").optionName("L").priceDelta(new BigDecimal("5000")).build()));
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(1000L)));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderItems().get(0).getModifiers()).isNull();
    }

    @Test
    @DisplayName("submitOrder rejects a modifier option that does not belong to the product")
    void submitOrder_invalidModifierOption() {
        Product p = product(1L, "Trà sữa", "30000");
        ModifierGroupDTO size = group(100L, "Size", false, 0, 1, option(1000L, "L", "5000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(size));
        lenient().when(messageService.getMessage("error.modifier.invalid")).thenReturn("invalid");

        assertThatThrownBy(() -> service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(9999L))))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitOrder rejects when a required group has no selection")
    void submitOrder_requiredGroupNotSelected() {
        Product p = product(1L, "Trà sữa", "30000");
        // group A (required) is missing; only group B's option is chosen
        ModifierGroupDTO required = group(100L, "Size", true, 1, 1, option(1000L, "L", "5000"));
        ModifierGroupDTO optional = group(200L, "Topping", false, 0, 2, option(2000L, "Trân châu", "3000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(required, optional));
        lenient().when(messageService.getMessage(eq("error.modifier.required"), any(Object[].class))).thenReturn("required");

        assertThatThrownBy(() -> service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(2000L))))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitOrder rejects when a group's max selection is exceeded")
    void submitOrder_maxExceeded() {
        Product p = product(1L, "Trà sữa", "30000");
        ModifierGroupDTO topping = group(200L, "Topping", false, 0, 1,
                option(2000L, "Trân châu", "3000"), option(2001L, "Thạch", "3000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(topping));
        lenient().when(messageService.getMessage(eq("error.modifier.max"), any(), any())).thenReturn("too many");

        assertThatThrownBy(() -> service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(2000L, 2001L))))
                .isInstanceOf(BadRequestException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitOrder accepts a selection that satisfies min and max constraints")
    void submitOrder_validMinMax() throws Exception {
        Product p = product(1L, "Trà sữa", "30000");
        // optional group with null minSelect → min defaults to 0; max 2 satisfied
        ModifierGroupDTO topping = group(200L, "Topping", false, null, 2,
                option(2000L, "Trân châu", "3000"));

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(p));
        when(modifierService.getGroupsForProduct(1L)).thenReturn(List.of(topping));
        when(modifierService.resolveOptions(List.of(2000L))).thenReturn(List.of(
                ChosenModifierDTO.builder().groupName("Topping").optionName("Trân châu")
                        .priceDelta(new BigDecimal("3000")).build()));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submitOrder(TOKEN, requestWithModifiers(1L, 1, List.of(2000L)));

        verify(orderRepository).save(any(Order.class));
    }

    // ── getOrderStatus ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderStatus returns the status of a QR-sourced order")
    void getOrderStatus_qrOrder() {
        Order order = new Order();
        order.setId(5L);
        order.setOrderNumber("QR-1");
        order.setStatus(Order.OrderStatus.SUBMITTED);
        order.setSource("QR");

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        PublicOrderResponse resp = service.getOrderStatus(5L);

        assertThat(resp.getOrderId()).isEqualTo(5L);
        assertThat(resp.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("getOrderStatus throws when the order is not QR-sourced (filtered out)")
    void getOrderStatus_nonQrOrderFiltered() {
        Order order = new Order();
        order.setId(6L);
        order.setSource("POS");

        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        lenient().when(messageService.getMessage("error.order.not.found")).thenReturn("not found");

        assertThatThrownBy(() -> service.getOrderStatus(6L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderStatus throws when the order does not exist")
    void getOrderStatus_notFound() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(orderRepository.findById(7L)).thenReturn(Optional.empty());
        lenient().when(messageService.getMessage("error.order.not.found")).thenReturn("not found");

        assertThatThrownBy(() -> service.getOrderStatus(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── requireQrEnabled edge cases ──────────────────────────────────────────────

    @Test
    @DisplayName("resolveTable is refused when the tenant has no features at all (null)")
    void resolveTable_refusedWhenFeaturesNull() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant(null));
        lenient().when(messageService.getMessage("error.qr.ordering.unavailable")).thenReturn("unavailable");

        assertThatThrownBy(() -> service.resolveTable(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resolveTable throws when the QR token matches no table")
    void resolveTable_tableNotFound() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.empty());
        lenient().when(messageService.getMessage("error.table.not.found")).thenReturn("no table");

        assertThatThrownBy(() -> service.resolveTable(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("submitOrder prices from the catalog and computes tax (client cannot set price)")
    void submitOrder_pricesFromCatalog() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("POS,TABLE_SERVICE,ORDER"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phở", "50000")));
        when(shopConfigService.getDouble(any(ShopConfigKey.class), anyDouble())).thenReturn(0.10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submitOrder(TOKEN, request(1L, 2));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(Order.OrderStatus.SUBMITTED);
        assertThat(saved.getSource()).isEqualTo("QR");
        assertThat(saved.getTableId()).isEqualTo(7L);
        assertThat(saved.getTableLabel()).isEqualTo("A1");
        assertThat(saved.getOrderItems()).hasSize(1);
        // unit price comes from the catalog, regardless of any client input
        assertThat(saved.getOrderItems().get(0).getUnitPrice()).isEqualByComparingTo("50000");
        assertThat(saved.getOrderItems().get(0).getQuantity()).isEqualTo(2);
        // 50000 × 2 = 100000 subtotal; +10% tax = 110000
        assertThat(saved.getTaxAmount()).isEqualByComparingTo("10000");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("110000");
    }

    @Test
    @DisplayName("submitOrder is refused when the shop does not have TABLE_SERVICE")
    void submitOrder_refusedWithoutFeature() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("POS,ORDER"));
        lenient().when(messageService.getMessage("error.qr.ordering.unavailable")).thenReturn("unavailable");

        assertThatThrownBy(() -> service.submitOrder(TOKEN, request(1L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitOrder rejects an unknown/inactive product")
    void submitOrder_rejectsUnknownProduct() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        lenient().when(messageService.getMessage("error.product.not.found")).thenReturn("no product");

        assertThatThrownBy(() -> service.submitOrder(TOKEN, request(99L, 1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resolveTable returns shop + table label")
    void resolveTable_returnsShopAndTable() {
        when(tenantContext.getCurrentTenant()).thenReturn(tenant("TABLE_SERVICE"));
        when(tableRepository.findByQrToken(TOKEN)).thenReturn(Optional.of(table()));

        PublicTableDTO dto = service.resolveTable(TOKEN);

        assertThat(dto.getShopName()).isEqualTo("Quán Test");
        assertThat(dto.getTableLabel()).isEqualTo("A1");
        assertThat(dto.getTableId()).isEqualTo(7L);
    }
}
