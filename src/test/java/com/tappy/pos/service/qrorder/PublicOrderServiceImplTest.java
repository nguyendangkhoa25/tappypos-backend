package com.tappy.pos.service.qrorder;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.qrorder.PublicTableDTO;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ShopConfigKey;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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
