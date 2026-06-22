package com.tappy.pos.service.qrorder;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.room.GuestRequestRequest;
import com.tappy.pos.model.dto.room.PublicRoomDTO;
import com.tappy.pos.model.dto.room.PublicRoomOrderResult;
import com.tappy.pos.model.dto.room.RoomRequestDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.room.RoomEntity;
import com.tappy.pos.model.entity.room.RoomRequestEntity;
import com.tappy.pos.model.entity.room.RoomStayEntity;
import com.tappy.pos.model.entity.room.RoomStayItemEntity;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.room.RoomRepository;
import com.tappy.pos.repository.room.RoomRequestRepository;
import com.tappy.pos.repository.room.RoomStayItemRepository;
import com.tappy.pos.repository.room.RoomStayRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicRoomServiceImpl implements PublicRoomService {

    private static final String FEATURE_ROOM = "ROOM";
    private static final String QR_SOURCE = "QR";
    private static final Set<String> REQUEST_TYPES = Set.of("SERVICE", "CLEANING", "SUPPLIES", "CHECKOUT", "OTHER");

    private final TenantContext tenantContext;
    private final RoomRepository roomRepository;
    private final RoomStayRepository stayRepository;
    private final RoomStayItemRepository itemRepository;
    private final RoomRequestRepository requestRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PublicRoomDTO resolveRoom(String qrToken) {
        Tenant tenant = requireRoomEnabled();
        RoomEntity room = findRoom(qrToken);
        RoomStayEntity stay = stayRepository
                .findFirstByRoomIdAndStatusAndDeletedFalse(room.getId(), "IN_HOUSE").orElse(null);
        return PublicRoomDTO.builder()
                .shopName(tenant.getName())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .hasActiveStay(stay != null)
                .stayId(stay != null ? stay.getId() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMenuDTO getMenu() {
        Tenant tenant = requireRoomEnabled();
        List<Product> products = productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE);

        Map<Long, PublicMenuDTO.MenuCategory.MenuCategoryBuilder> byCat = new LinkedHashMap<>();
        Map<Long, List<PublicMenuDTO.MenuItem>> itemsByCat = new HashMap<>();
        List<PublicMenuDTO.MenuItem> uncategorised = new ArrayList<>();

        for (Product p : products) {
            PublicMenuDTO.MenuItem item = PublicMenuDTO.MenuItem.builder()
                    .id(p.getId()).name(p.getName()).description(p.getDescription())
                    .price(p.getPrice()).unit(p.getUnit()).imageUrl(p.getImageUrl()).build();
            Category cat = (p.getCategories() == null || p.getCategories().isEmpty())
                    ? null : p.getCategories().iterator().next();
            if (cat == null) {
                uncategorised.add(item);
            } else {
                byCat.computeIfAbsent(cat.getId(), k -> PublicMenuDTO.MenuCategory.builder().id(cat.getId()).name(cat.getName()));
                itemsByCat.computeIfAbsent(cat.getId(), k -> new ArrayList<>()).add(item);
            }
        }

        List<PublicMenuDTO.MenuCategory> categories = byCat.entrySet().stream()
                .map(e -> e.getValue().items(itemsByCat.get(e.getKey())).build())
                .sorted(Comparator.comparing(PublicMenuDTO.MenuCategory::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        if (!uncategorised.isEmpty()) {
            categories.add(PublicMenuDTO.MenuCategory.builder()
                    .id(null).name(messageService.getMessage("qr.menu.uncategorised")).items(uncategorised).build());
        }
        return PublicMenuDTO.builder().shopName(tenant.getName()).categories(categories).build();
    }

    @Override
    @Transactional
    public PublicRoomOrderResult submitOrder(String qrToken, PublicOrderRequest request) {
        Tenant tenant = requireRoomEnabled();
        String tenantId = tenant.getTenantId();
        RoomEntity room = findRoom(qrToken);
        RoomStayEntity stay = stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(room.getId(), "IN_HOUSE")
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.room.qr.no.active.stay")));

        BigDecimal added = BigDecimal.ZERO;
        int count = 0;
        for (PublicOrderRequest.Line line : request.getItems()) {
            // Re-price from the catalog — client-sent prices are never trusted.
            Product p = productRepository.findByIdAndDeletedFalse(line.getProductId())
                    .filter(pr -> pr.getStatus() == Product.ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.product.not.found")));
            RoomStayItemEntity item = RoomStayItemEntity.builder()
                    .tenantId(tenantId)
                    .stayId(stay.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(line.getQuantity())
                    .unitPrice(p.getPrice())
                    .source(QR_SOURCE)
                    .note(line.getNotes())
                    .createdBy(QR_SOURCE)
                    .build();
            itemRepository.save(item);
            added = added.add(p.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            count += line.getQuantity();
        }

        notifyReception(tenantId,
                LocalizedText.of("notification.room.qr.order.title", room.getRoomNumber()),
                LocalizedText.of("notification.room.qr.order.message", room.getRoomNumber(), count),
                "ROOM_STAY", stay.getId());
        log.info("QR room order: room={} stay={} items={} total={}", room.getRoomNumber(), stay.getId(), count, added);

        return PublicRoomOrderResult.builder().stayId(stay.getId()).itemCount(count).addedTotal(added).build();
    }

    @Override
    @Transactional
    public RoomRequestDTO submitRequest(String qrToken, GuestRequestRequest request) {
        Tenant tenant = requireRoomEnabled();
        String tenantId = tenant.getTenantId();
        RoomEntity room = findRoom(qrToken);
        RoomStayEntity stay = stayRepository.findFirstByRoomIdAndStatusAndDeletedFalse(room.getId(), "IN_HOUSE").orElse(null);

        String type = request.getRequestType() != null ? request.getRequestType().toUpperCase() : "";
        if (!REQUEST_TYPES.contains(type)) type = "OTHER";

        RoomRequestEntity req = RoomRequestEntity.builder()
                .tenantId(tenantId)
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .stayId(stay != null ? stay.getId() : null)
                .requestType(type)
                .message(request.getMessage())
                .status("NEW")
                .build();
        RoomRequestEntity saved = requestRepository.save(req);

        notifyReception(tenantId,
                LocalizedText.of("notification.room.qr.request.title", room.getRoomNumber()),
                LocalizedText.of("notification.room.qr.request.message", room.getRoomNumber(),
                        messageService.getMessage("room.requestType." + type)),
                "ROOM_REQUEST", saved.getId());
        log.info("QR room request: room={} type={} id={}", room.getRoomNumber(), type, saved.getId());

        return toDTO(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void notifyReception(String tenantId, LocalizedText title, LocalizedText message, String targetType, Long targetId) {
        try {
            notificationService.pushToRolesAsync(
                    Notification.NotificationType.ORDER, title, message, targetType, targetId,
                    List.of(RoleEnum.SHOP_OWNER.getCode(), RoleEnum.CASHIER.getCode()),
                    tenantId);
        } catch (Exception e) {
            log.warn("Failed to push room QR notification (room target {}): {}", targetId, e.getMessage());
        }
    }

    /** Verify there is a tenant context and the shop has the ROOM feature enabled. */
    private Tenant requireRoomEnabled() {
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new ResourceNotFoundException(messageService.getMessage("error.room.qr.unavailable"));
        }
        String features = tenant.getFeatures();
        Set<String> set = features == null ? Set.of()
                : Arrays.stream(features.split(",")).map(String::trim).collect(Collectors.toSet());
        if (!set.contains(FEATURE_ROOM)) {
            throw new ResourceNotFoundException(messageService.getMessage("error.room.qr.unavailable"));
        }
        return tenant;
    }

    private RoomEntity findRoom(String qrToken) {
        return roomRepository.findByQrTokenAndDeletedFalse(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.room.not.found", qrToken)));
    }

    private RoomRequestDTO toDTO(RoomRequestEntity r) {
        return RoomRequestDTO.builder()
                .id(r.getId()).roomId(r.getRoomId()).roomNumber(r.getRoomNumber()).stayId(r.getStayId())
                .requestType(r.getRequestType()).message(r.getMessage()).status(r.getStatus())
                .handledBy(r.getHandledBy()).handledAt(r.getHandledAt()).createdAt(r.getCreatedAt())
                .build();
    }
}
