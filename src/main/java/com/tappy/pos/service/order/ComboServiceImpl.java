package com.tappy.pos.service.order;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.entity.order.Combo;
import com.tappy.pos.model.entity.order.ComboItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.repository.order.ComboRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboServiceImpl implements ComboService {

    private final ComboRepository     comboRepository;
    private final OrderItemRepository orderItemRepository;
    private final TenantContext       tenantContext;
    private final MessageService      messageService;
    private final ActivityLogService  activityLogService;
    private final AuthContext         authContext;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> toDto(Combo c) {
        List<Map<String, Object>> items = c.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "productId",   i.getProductId().toString(),
                        "productName", i.getProductName(),
                        "quantity",    i.getQuantity(),
                        "price",       i.getPrice()))
                .collect(Collectors.toList());
        BigDecimal total = c.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",                   c.getId().toString());
        dto.put("name",                 c.getName());
        dto.put("description",          c.getDescription() != null ? c.getDescription() : "");
        dto.put("price",                c.getPrice());
        dto.put("active",               c.getActive());
        dto.put("items",                items);
        dto.put("totalIndividualPrice", total);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private void buildItems(Combo combo, Map<String, Object> body) {
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");
        if (itemsData == null) return;
        for (Map<String, Object> d : itemsData) {
            combo.getItems().add(ComboItem.builder()
                    .combo(combo)
                    .tenantId(combo.getTenantId())
                    .productId(Long.parseLong(d.get("productId").toString()))
                    .productName((String) d.get("productName"))
                    .quantity(((Number) d.getOrDefault("quantity", 1)).intValue())
                    .price(d.get("price") != null
                            ? new BigDecimal(d.get("price").toString()) : BigDecimal.ZERO)
                    .build());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> list(Boolean active) {
        List<Combo> combos = active != null
                ? comboRepository.findByDeletedFalseAndActive(active)
                : comboRepository.findByDeletedFalse();
        return combos.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Combo combo = Combo.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name((String) body.get("name"))
                .description((String) body.get("description"))
                .price(body.get("price") != null
                        ? new BigDecimal(body.get("price").toString()) : BigDecimal.ZERO)
                .active(body.get("active") == null || Boolean.TRUE.equals(body.get("active")))
                .build();
        buildItems(combo, body);
        comboRepository.save(combo);
        log.info("Combo created — id: {}", combo.getId());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.COMBO_CREATED, "COMBO", String.valueOf(combo.getId()),
                "activity.combo.created", null);
        return toDto(combo);
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.combo.not.found", id)));
        if (body.get("name")        != null) combo.setName((String) body.get("name"));
        if (body.get("description") != null) combo.setDescription((String) body.get("description"));
        if (body.get("price")       != null) combo.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.get("active")      != null) combo.setActive((Boolean) body.get("active"));
        if (body.containsKey("items")) {
            combo.getItems().clear();
            buildItems(combo, body);
        }
        comboRepository.save(combo);
        log.info("Combo updated — id: {}", id);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.COMBO_UPDATED, "COMBO", String.valueOf(id),
                "activity.combo.updated", null);
        return toDto(combo);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.combo.not.found", id)));
        combo.setDeleted(true);
        comboRepository.save(combo);
        log.info("Combo deleted — id: {}", id);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.COMBO_DELETED, "COMBO", String.valueOf(id),
                "activity.combo.deleted", null);
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getAnalytics(LocalDateTime from, LocalDateTime to,
                                            String granularity, int limit) {
        // Summary
        List<Object[]> sumRows = orderItemRepository.getComboSummary(from, to);
        long   totalSold    = 0;
        double totalRevenue = 0;
        double avgOrderValue = 0;
        if (!sumRows.isEmpty()) {
            Object[] r    = sumRows.get(0);
            totalSold     = r[0] != null ? ((Number) r[0]).longValue()   : 0;
            totalRevenue  = r[1] != null ? ((Number) r[1]).doubleValue() : 0;
            avgOrderValue = r[3] != null ? ((Number) r[3]).doubleValue() : 0;
        }
        long activeCount = comboRepository.findByDeletedFalseAndActive(true).size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSold",     totalSold);
        summary.put("totalRevenue",  totalRevenue);
        summary.put("activeCount",   activeCount);
        summary.put("avgOrderValue", avgOrderValue);

        // Ranking
        List<Map<String, Object>> ranking = orderItemRepository
                .getComboRanking(from, to, Math.max(1, limit))
                .stream().map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("comboId",    r[0] != null ? r[0].toString() : null);
                    item.put("comboName",  r[1] != null ? r[1].toString() : "");
                    item.put("qtySold",    r[2] != null ? ((Number) r[2]).longValue()   : 0L);
                    item.put("revenue",    r[3] != null ? ((Number) r[3]).doubleValue() : 0.0);
                    item.put("orderCount", r[4] != null ? ((Number) r[4]).longValue()   : 0L);
                    return item;
                }).collect(Collectors.toList());

        // Trend
        List<Object[]> trendRows = switch (granularity) {
            case "week"  -> orderItemRepository.getComboTrendByWeek(from, to);
            case "month" -> orderItemRepository.getComboTrendByMonth(from, to);
            default      -> orderItemRepository.getComboTrendByDay(from, to);
        };
        List<Map<String, Object>> trend = trendRows.stream().map(r -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label",   r[0] != null ? r[0].toString() : "");
            point.put("qtySold", r[1] != null ? ((Number) r[1]).longValue()   : 0L);
            point.put("revenue", r[2] != null ? ((Number) r[2]).doubleValue() : 0.0);
            return point;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("ranking", ranking);
        result.put("trend",   trend);
        return result;
    }
}
