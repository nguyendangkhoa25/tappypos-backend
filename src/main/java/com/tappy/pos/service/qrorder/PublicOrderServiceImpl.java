package com.tappy.pos.service.qrorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.modifier.ChosenModifierDTO;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.qrorder.PublicOrderResponse;
import com.tappy.pos.model.dto.qrorder.PublicTableDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.order.Combo;
import com.tappy.pos.model.entity.order.ComboItem;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.table.ShopTable;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.order.ComboRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.table.TableRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.modifier.ModifierService;
import com.tappy.pos.service.tenant.ShopConfigService;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicOrderServiceImpl implements PublicOrderService {

    private static final String WALK_IN_PHONE = "0000000000";
    private static final String QR_CUSTOMER = "qr-customer";
    private static final String FEATURE_TABLE_SERVICE = "TABLE_SERVICE";

    /** Floor staff who should be alerted when a customer self-submits a QR order. */
    private static final List<String> ORDER_NOTIFY_ROLES = List.of(
            RoleEnum.SHOP_OWNER.getCode(), RoleEnum.CASHIER.getCode(), RoleEnum.SERVICE_STAFF.getCode());

    private final TenantContext tenantContext;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ComboRepository comboRepository;
    private final ShopConfigService shopConfigService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final ModifierService modifierService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PublicTableDTO resolveTable(String qrToken) {
        Tenant tenant = requireQrEnabled();
        ShopTable table = findTable(qrToken);
        return PublicTableDTO.builder()
                .shopName(tenant.getName())
                .tableId(table.getId())
                .tableLabel(table.getTableNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTableDTO getShop() {
        Tenant tenant = requireQrEnabled();
        // Shop-wide QR (no table): only the shop name is needed for the page header.
        return PublicTableDTO.builder().shopName(tenant.getName()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMenuDTO getMenu() {
        Tenant tenant = requireQrEnabled();
        List<Product> products = productRepository.findActiveWithCategories(Product.ProductStatus.ACTIVE);

        // Modifier groups for all menu products in one pass (avoids N+1).
        Map<Long, List<ModifierGroupDTO>> modsByProduct = modifierService.getGroupsForProducts(
                products.stream().map(Product::getId).collect(Collectors.toList()));

        // Group by the product's first category; uncategorised items go in a trailing "Khác" bucket.
        Map<Long, PublicMenuDTO.MenuCategory.MenuCategoryBuilder> byCat = new LinkedHashMap<>();
        Map<Long, List<PublicMenuDTO.MenuItem>> itemsByCat = new HashMap<>();
        List<PublicMenuDTO.MenuItem> uncategorised = new ArrayList<>();

        for (Product p : products) {
            PublicMenuDTO.MenuItem item = PublicMenuDTO.MenuItem.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .price(p.getPrice())
                    .unit(p.getUnit())
                    .imageUrl(p.getImageUrl())
                    .modifierGroups(modsByProduct.get(p.getId()))
                    .build();
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

        // Active combos (fixed bundles) shown as their own section.
        List<PublicMenuDTO.MenuCombo> combos = comboRepository.findByDeletedFalseAndActive(true).stream()
                .map(c -> PublicMenuDTO.MenuCombo.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .price(nz(c.getPrice()))
                        .retailTotal(comboRetailTotal(c))
                        .components(c.getItems().stream()
                                .map(ci -> PublicMenuDTO.ComboComponent.builder()
                                        .name(ci.getProductName())
                                        .quantity(ci.getQuantity())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return PublicMenuDTO.builder().shopName(tenant.getName()).categories(categories).combos(combos).build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** À-la-carte sum of a combo's components (component retail price × quantity). */
    private BigDecimal comboRetailTotal(Combo combo) {
        return combo.getItems().stream()
                .map(ci -> nz(ci.getPrice()).multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public PublicOrderResponse submitOrder(String qrToken, PublicOrderRequest request) {
        Tenant tenant = requireQrEnabled();
        ShopTable table = findTable(qrToken);
        return createOrder(tenant, table, request);
    }

    @Override
    @Transactional
    public PublicOrderResponse submitShopOrder(PublicOrderRequest request) {
        Tenant tenant = requireQrEnabled();
        return createOrder(tenant, null, request);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicOrderResponse getOrderStatus(Long orderId) {
        requireQrEnabled();
        Order order = orderRepository.findById(orderId)
                .filter(o -> "QR".equals(o.getSource()))
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.order.not.found")));
        return toResponse(order);
    }

    // ── order creation (shared by per-table and shop-wide flows) ───────────────

    /** {@code table} is null for a shop-wide (no-table) QR order. */
    private PublicOrderResponse createOrder(Tenant tenant, ShopTable table, PublicOrderRequest request) {
        String tenantId = tenant.getTenantId();

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.SUBMITTED);
        order.setOrderType(Order.OrderType.SELL);
        order.setSource("QR");
        if (table != null) {
            order.setTableLabel(table.getTableNumber());
            order.setTableId(table.getId());
        }
        order.setCreatedBy(QR_CUSTOMER);
        order.setNotes(buildNotes(request.getCustomerName(), request.getNote()));
        customerRepository.findByPhoneAndTenantId(WALK_IN_PHONE, tenantId).ifPresent(order::setCustomer);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PublicOrderRequest.Line line : request.getItems()) {
            // Combo lines expand into their component items; price comes from the combo, not the client.
            if (line.getComboId() != null) {
                subtotal = subtotal.add(addComboLine(order, tenantId, line, items));
                continue;
            }
            if (line.getProductId() == null) {
                throw new BadRequestException(messageService.getMessage("error.order.line.invalid"));
            }
            // Re-price from the catalog — client-sent prices are never trusted.
            Product p = productRepository.findByIdAndDeletedFalse(line.getProductId())
                    .filter(pr -> pr.getStatus() == Product.ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.product.not.found")));

            BigDecimal unitPrice = p.getPrice();
            String modifiersJson = null;

            List<Long> optionIds = line.getModifierOptionIds();
            if (optionIds != null && !optionIds.isEmpty()) {
                validateModifierSelection(p, optionIds);
                List<ChosenModifierDTO> chosen = modifierService.resolveOptions(optionIds);
                BigDecimal delta = chosen.stream()
                        .map(ChosenModifierDTO::getPriceDelta)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                unitPrice = unitPrice.add(delta);
                modifiersJson = writeModifiers(chosen);
            }

            OrderItem oi = new OrderItem();
            oi.setTenantId(tenantId);
            oi.setOrder(order);
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setQuantity(line.getQuantity());
            oi.setUnitPrice(unitPrice);
            oi.setUnitCost(p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
            oi.setItemType(OrderItem.ItemType.STANDARD);
            oi.setStatus(OrderItem.ItemStatus.PENDING);
            oi.setNote(line.getNotes());
            oi.setModifiers(modifiersJson);
            items.add(oi);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        BigDecimal taxRate = BigDecimal.valueOf(shopConfigService.getDouble(ShopConfigKey.DEFAULT_TAX_RATE, 0.10));
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        order.setTaxPercentage(taxRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(subtotal.add(taxAmount));
        order.setOrderItems(items);

        Order saved = orderRepository.save(order);
        log.info("QR customer order submitted: {} (table={})", saved.getOrderNumber(),
                table != null ? table.getTableNumber() : "—");

        try {
            LocalizedText title = table != null
                    ? LocalizedText.of("notification.qr.order.title", table.getTableNumber())
                    : LocalizedText.of("notification.qr.order.title.notable");
            notificationService.pushToRolesAsync(
                    Notification.NotificationType.ORDER,
                    title,
                    LocalizedText.of("notification.qr.order.message", saved.getOrderNumber()),
                    "ORDER", saved.getId(),
                    ORDER_NOTIFY_ROLES,
                    tenantId);
        } catch (Exception e) {
            log.warn("Failed to push QR-order notification (order={}): {}", saved.getOrderNumber(), e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * Expand a combo into per-component {@link OrderItem}s (so the kitchen sees each dish) and return
     * the line subtotal — the combo's deal price × quantity, so the order total reflects the bundle
     * price, not the à-la-carte sum. The deal price is allocated across components proportional to
     * their retail price (integer VND, remainder onto the largest line) so the components sum exactly.
     */
    private BigDecimal addComboLine(Order order, String tenantId, PublicOrderRequest.Line line, List<OrderItem> items) {
        Combo combo = comboRepository.findByIdAndDeletedFalse(line.getComboId())
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.combo.not.found", line.getComboId())));
        List<ComboItem> comboItems = combo.getItems();
        if (comboItems.isEmpty()) {
            throw new BadRequestException(messageService.getMessage("combo.inactive"));
        }
        int comboQty = line.getQuantity();
        BigDecimal dealPrice = nz(combo.getPrice());
        BigDecimal retailTotal = comboRetailTotal(combo);

        int n = comboItems.size();
        BigDecimal[] alloc = new BigDecimal[n];
        BigDecimal running = BigDecimal.ZERO;
        int maxIdx = 0;
        for (int i = 0; i < n; i++) {
            ComboItem ci = comboItems.get(i);
            BigDecimal lineRetail = nz(ci.getPrice()).multiply(BigDecimal.valueOf(ci.getQuantity()));
            BigDecimal a = retailTotal.signum() == 0
                    ? dealPrice.divide(BigDecimal.valueOf(n), 0, RoundingMode.DOWN)
                    : dealPrice.multiply(lineRetail).divide(retailTotal, 0, RoundingMode.HALF_UP);
            alloc[i] = a;
            running = running.add(a);
            if (a.compareTo(alloc[maxIdx]) > 0) maxIdx = i;
        }
        BigDecimal remainder = dealPrice.subtract(running);
        if (remainder.signum() != 0) alloc[maxIdx] = alloc[maxIdx].add(remainder);

        // Sum the expanded line items as we build them and return THAT as the line subtotal (rather
        // than dealPrice × comboQty). Because each OrderItem total is unitPrice × quantity with a
        // whole-VND unit price, the components can't always re-sum to an arbitrary deal price; deriving
        // the subtotal from the actual lines guarantees the receipt always reconciles with the order
        // total (the deal price is matched exactly whenever each component is one unit per combo).
        BigDecimal lineSubtotal = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            ComboItem ci = comboItems.get(i);
            // Re-validate each component against the catalog — a withdrawn (inactive) or deleted
            // product must not be sellable just because it's bundled in an active combo. Mirrors the
            // ACTIVE check on standalone lines above.
            Product p = productRepository.findByIdAndDeletedFalse(ci.getProductId())
                    .filter(pr -> pr.getStatus() == Product.ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.product.not.found")));
            int qtyPerCombo = ci.getQuantity() != null ? ci.getQuantity() : 1;
            int lineQty = qtyPerCombo * comboQty;
            BigDecimal unitPrice = qtyPerCombo > 0
                    ? alloc[i].divide(BigDecimal.valueOf(qtyPerCombo), 0, RoundingMode.HALF_UP)
                    : alloc[i];
            OrderItem oi = new OrderItem();
            oi.setTenantId(tenantId);
            oi.setOrder(order);
            oi.setProductId(ci.getProductId());
            oi.setProductName(p.getName());
            oi.setQuantity(lineQty);
            oi.setUnitPrice(unitPrice);
            oi.setUnitCost(p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
            oi.setItemType(OrderItem.ItemType.STANDARD);
            oi.setStatus(OrderItem.ItemStatus.PENDING);
            oi.setComboId(combo.getId());
            if (i == 0) oi.setNote(line.getNotes());
            items.add(oi);
            lineSubtotal = lineSubtotal.add(unitPrice.multiply(BigDecimal.valueOf(lineQty)));
        }
        return lineSubtotal;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Validate the chosen options belong to this product's modifier groups and satisfy each group's
     * required / min / max constraints. Throws {@link BadRequestException} with a Vietnamese message.
     */
    private void validateModifierSelection(Product product, List<Long> optionIds) {
        List<ModifierGroupDTO> groups = modifierService.getGroupsForProduct(product.getId());

        // option id -> the group it belongs to (only options on this product's groups are valid)
        Map<Long, ModifierGroupDTO> groupByOption = new HashMap<>();
        for (ModifierGroupDTO g : groups) {
            if (g.getOptions() == null) continue;
            for (ModifierGroupDTO.OptionDTO o : g.getOptions()) {
                groupByOption.put(o.getId(), g);
            }
        }

        Map<Long, Long> countByGroup = new HashMap<>(); // groupId -> chosen count
        for (Long optId : optionIds) {
            ModifierGroupDTO g = groupByOption.get(optId);
            if (g == null) {
                throw new BadRequestException(messageService.getMessage("error.modifier.invalid"));
            }
            countByGroup.merge(g.getId(), 1L, Long::sum);
        }

        for (ModifierGroupDTO g : groups) {
            long count = countByGroup.getOrDefault(g.getId(), 0L);
            int min = Boolean.TRUE.equals(g.getRequired())
                    ? Math.max(1, g.getMinSelect() != null ? g.getMinSelect() : 0)
                    : (g.getMinSelect() != null ? g.getMinSelect() : 0);
            if (count < min) {
                throw new BadRequestException(messageService.getMessage("error.modifier.required", g.getName()));
            }
            Integer max = g.getMaxSelect();
            if (max != null && max > 0 && count > max) {
                throw new BadRequestException(messageService.getMessage("error.modifier.max", g.getName(), max));
            }
        }
    }

    private String writeModifiers(List<ChosenModifierDTO> chosen) {
        if (chosen == null || chosen.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(chosen);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to serialise modifiers: {}", e.getMessage());
            return null;
        }
    }

    /** Verify there is a tenant context and the shop has QR ordering (TABLE_SERVICE) enabled. */
    private Tenant requireQrEnabled() {
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new ResourceNotFoundException(messageService.getMessage("error.qr.ordering.unavailable"));
        }
        String features = tenant.getFeatures();
        Set<String> set = features == null ? Set.of()
                : Arrays.stream(features.split(",")).map(String::trim).collect(Collectors.toSet());
        if (!set.contains(FEATURE_TABLE_SERVICE)) {
            throw new ResourceNotFoundException(messageService.getMessage("error.qr.ordering.unavailable"));
        }
        return tenant;
    }

    private ShopTable findTable(String qrToken) {
        return tableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.table.not.found")));
    }

    private String buildNotes(String customerName, String note) {
        StringBuilder sb = new StringBuilder();
        if (customerName != null && !customerName.isBlank()) sb.append(customerName.strip());
        if (note != null && !note.isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(note.strip());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String generateOrderNumber() {
        String datePart = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = java.util.concurrent.ThreadLocalRandom.current().nextInt(10000, 99999);
        return "QR-" + datePart + "-" + seq;
    }

    private PublicOrderResponse toResponse(Order o) {
        return PublicOrderResponse.builder()
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus().name())
                .build();
    }
}
