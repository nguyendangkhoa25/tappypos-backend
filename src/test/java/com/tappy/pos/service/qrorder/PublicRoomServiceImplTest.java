package com.tappy.pos.service.qrorder;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.room.GuestRequestRequest;
import com.tappy.pos.model.dto.room.PublicRoomDTO;
import com.tappy.pos.model.dto.room.PublicRoomOrderResult;
import com.tappy.pos.model.dto.room.RoomRequestDTO;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.room.RoomEntity;
import com.tappy.pos.model.entity.room.RoomRequestEntity;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.model.entity.room.RoomStayItemEntity;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomRequestRepository;
import com.tappy.pos.repository.room.RoomStayItemRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PublicRoomServiceImpl Unit Tests")
class PublicRoomServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomStayRepository stayRepository;
    @Mock private RoomStayItemRepository itemRepository;
    @Mock private RoomRequestRepository requestRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MessageService messageService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PublicRoomServiceImpl service;

    private Tenant tenant;
    private RoomEntity room;
    private RoomStayEntity stay;

    @BeforeEach
    void setUp() {
        when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));

        tenant = Tenant.builder()
                .tenantId("shop1").name("Hotel A").features("ROOM,ORDER").build();
        room = RoomEntity.builder().id(1L).roomNumber("101").qrToken("tok").build();
        stay = RoomStayEntity.builder().id(50L).build();

        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
    }

    private Product activeProduct(long id, String price) {
        return Product.builder()
                .id(id).name("Item" + id).price(new BigDecimal(price))
                .unit("cái").status(Product.ProductStatus.ACTIVE).build();
    }

    // ── requireRoomEnabled guard ──────────────────────────────────────────────

    @Test
    @DisplayName("resolveRoom throws when no tenant context")
    void resolveRoom_noTenant() {
        when(tenantContext.getCurrentTenant()).thenReturn(null);
        assertThatThrownBy(() -> service.resolveRoom("tok"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resolveRoom throws when ROOM feature not enabled")
    void resolveRoom_featureDisabled() {
        tenant.setFeatures("ORDER,POS");
        assertThatThrownBy(() -> service.resolveRoom("tok"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resolveRoom tolerates null features")
    void resolveRoom_nullFeatures() {
        tenant.setFeatures(null);
        assertThatThrownBy(() -> service.resolveRoom("tok"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── resolveRoom ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveRoom reports active stay")
    void resolveRoom_withStay() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.of(stay));

        PublicRoomDTO dto = service.resolveRoom("tok");

        assertThat(dto.getShopName()).isEqualTo("Hotel A");
        assertThat(dto.getRoomNumber()).isEqualTo("101");
        assertThat(dto.isHasActiveStay()).isTrue();
        assertThat(dto.getStayId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("resolveRoom reports no active stay")
    void resolveRoom_noStay() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.empty());

        PublicRoomDTO dto = service.resolveRoom("tok");

        assertThat(dto.isHasActiveStay()).isFalse();
        assertThat(dto.getStayId()).isNull();
    }

    @Test
    @DisplayName("resolveRoom throws when room token unknown")
    void resolveRoom_roomNotFound() {
        when(roomRepository.findByQrTokenAndDeletedFalse("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveRoom("bad"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMenu ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMenu groups products by category and appends uncategorised bucket")
    void getMenu() {
        Category drinks = Category.builder().id(7L).name("Đồ uống").build();
        Product withCat = activeProduct(1, "10000");
        withCat.setCategories(Set.of(drinks));
        Product noCat = activeProduct(2, "20000");
        noCat.setCategories(Set.of());
        when(productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE))
                .thenReturn(List.of(withCat, noCat));

        PublicMenuDTO menu = service.getMenu();

        assertThat(menu.getShopName()).isEqualTo("Hotel A");
        assertThat(menu.getCategories()).hasSize(2); // one real + uncategorised
        assertThat(menu.getCategories()).anyMatch(c -> "Đồ uống".equals(c.getName()));
    }

    @Test
    @DisplayName("getMenu with only categorised products has no uncategorised bucket")
    void getMenu_allCategorised() {
        Category c = Category.builder().id(7L).name("Đồ uống").build();
        Product p = activeProduct(1, "10000");
        p.setCategories(Set.of(c));
        when(productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE)).thenReturn(List.of(p));

        PublicMenuDTO menu = service.getMenu();

        assertThat(menu.getCategories()).hasSize(1);
    }

    // ── submitOrder ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitOrder re-prices from catalog, saves items, notifies reception")
    void submitOrder_ok() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.of(stay));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeProduct(1, "10000")));
        when(productRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(activeProduct(2, "5000")));

        PublicOrderRequest req = new PublicOrderRequest();
        PublicOrderRequest.Line l1 = new PublicOrderRequest.Line();
        l1.setProductId(1L); l1.setQuantity(2); l1.setNotes("ít đá");
        PublicOrderRequest.Line l2 = new PublicOrderRequest.Line();
        l2.setProductId(2L); l2.setQuantity(3);
        req.setItems(List.of(l1, l2));

        PublicRoomOrderResult result = service.submitOrder("tok", req);

        assertThat(result.getStayId()).isEqualTo(50L);
        assertThat(result.getItemCount()).isEqualTo(5);
        assertThat(result.getAddedTotal()).isEqualByComparingTo("35000"); // 2*10000 + 3*5000
        verify(itemRepository, times(2)).save(any(RoomStayItemEntity.class));
        verify(notificationService).pushToRolesAsync(any(),
                any(com.tappy.pos.model.i18n.LocalizedText.class),
                any(com.tappy.pos.model.i18n.LocalizedText.class),
                eq("ROOM_STAY"), eq(50L), any(), eq("shop1"));
    }

    @Test
    @DisplayName("submitOrder throws when no active stay")
    void submitOrder_noStay() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.empty());
        PublicOrderRequest req = new PublicOrderRequest();
        PublicOrderRequest.Line l = new PublicOrderRequest.Line();
        l.setProductId(1L); l.setQuantity(1);
        req.setItems(List.of(l));

        assertThatThrownBy(() -> service.submitOrder("tok", req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitOrder rejects an inactive/unknown product")
    void submitOrder_inactiveProduct() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.of(stay));
        Product inactive = activeProduct(1, "10000");
        inactive.setStatus(Product.ProductStatus.INACTIVE);
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inactive));
        PublicOrderRequest req = new PublicOrderRequest();
        PublicOrderRequest.Line l = new PublicOrderRequest.Line();
        l.setProductId(1L); l.setQuantity(1);
        req.setItems(List.of(l));

        assertThatThrownBy(() -> service.submitOrder("tok", req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── submitRequest ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitRequest persists a known request type and notifies reception")
    void submitRequest_knownType() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.of(stay));
        when(requestRepository.save(any())).thenAnswer(i -> {
            RoomRequestEntity r = i.getArgument(0);
            r.setId(77L);
            return r;
        });
        GuestRequestRequest req = new GuestRequestRequest();
        req.setRequestType("cleaning");
        req.setMessage("Please clean");

        RoomRequestDTO dto = service.submitRequest("tok", req);

        assertThat(dto.getId()).isEqualTo(77L);
        assertThat(dto.getRequestType()).isEqualTo("CLEANING");
        assertThat(dto.getStatus()).isEqualTo("NEW");
        verify(notificationService).pushToRolesAsync(any(),
                any(com.tappy.pos.model.i18n.LocalizedText.class),
                any(com.tappy.pos.model.i18n.LocalizedText.class),
                eq("ROOM_REQUEST"), eq(77L), any(), eq("shop1"));
    }

    @Test
    @DisplayName("submitRequest defaults unknown type to OTHER and tolerates no stay")
    void submitRequest_unknownTypeNoStay() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(i -> {
            RoomRequestEntity r = i.getArgument(0);
            r.setId(78L);
            return r;
        });
        GuestRequestRequest req = new GuestRequestRequest();
        req.setRequestType("WEIRD");

        RoomRequestDTO dto = service.submitRequest("tok", req);

        assertThat(dto.getRequestType()).isEqualTo("OTHER");
        assertThat(dto.getStayId()).isNull();
    }

    @Test
    @DisplayName("submitRequest treats a null request type as OTHER")
    void submitRequest_nullType() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(i -> {
            RoomRequestEntity r = i.getArgument(0);
            r.setId(79L);
            return r;
        });
        GuestRequestRequest req = new GuestRequestRequest();
        req.setRequestType(null);

        RoomRequestDTO dto = service.submitRequest("tok", req);

        assertThat(dto.getRequestType()).isEqualTo("OTHER");
    }

    @Test
    @DisplayName("notification failure does not break submitRequest")
    void submitRequest_notificationFailureSwallowed() {
        when(roomRepository.findByQrTokenAndDeletedFalse("tok")).thenReturn(Optional.of(room));
        when(stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(1L, "IN_HOUSE")).thenReturn(Optional.of(stay));
        when(requestRepository.save(any())).thenAnswer(i -> {
            RoomRequestEntity r = i.getArgument(0);
            r.setId(80L);
            return r;
        });
        doThrow(new RuntimeException("notif down")).when(notificationService)
                .pushToRolesAsync(any(),
                        any(com.tappy.pos.model.i18n.LocalizedText.class),
                        any(com.tappy.pos.model.i18n.LocalizedText.class),
                        anyString(), anyLong(), any(), anyString());
        GuestRequestRequest req = new GuestRequestRequest();
        req.setRequestType("SERVICE");

        RoomRequestDTO dto = service.submitRequest("tok", req);

        assertThat(dto.getId()).isEqualTo(80L);
    }
}
