package com.tappy.pos.controller.order;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.entity.order.Combo;
import com.tappy.pos.model.entity.order.ComboItem;
import com.tappy.pos.repository.order.ComboRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@RequiresFeature("POS")
public class ComboController {

    private final ComboRepository comboRepository;
    private final OrderItemRepository orderItemRepository;

    private Map<String, Object> toDto(Combo c) {
        List<Map<String, Object>> items = c.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId().toString(),
                        "productName", i.getProductName(),
                        "quantity", i.getQuantity(),
                        "price", i.getPrice()))
                .collect(Collectors.toList());
        BigDecimal total = c.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId().toString());
        dto.put("name", c.getName());
        dto.put("description", c.getDescription() != null ? c.getDescription() : "");
        dto.put("price", c.getPrice());
        dto.put("active", c.getActive());
        dto.put("items", items);
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
                    .productId(Long.parseLong(d.get("productId").toString()))
                    .productName((String) d.get("productName"))
                    .quantity(((Number) d.getOrDefault("quantity", 1)).intValue())
                    .price(d.get("price") != null ? new BigDecimal(d.get("price").toString()) : BigDecimal.ZERO)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) Boolean active) {
        log.info("Endpoint: GET /combos active={}", active);
        List<Combo> combos = active != null
                ? comboRepository.findByDeletedFalseAndActive(active)
                : comboRepository.findByDeletedFalse();
        return ResponseEntity.ok(ApiResponse.success(
                combos.stream().map(this::toDto).collect(Collectors.toList()), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> body) {
        log.info("Endpoint: POST /combos");
        Combo combo = Combo.builder()
                .name((String) body.get("name"))
                .description((String) body.get("description"))
                .price(body.get("price") != null ? new BigDecimal(body.get("price").toString()) : BigDecimal.ZERO)
                .active(body.get("active") == null || Boolean.TRUE.equals(body.get("active")))
                .build();
        buildItems(combo, body);
        comboRepository.save(combo);
        return ResponseEntity.ok(ApiResponse.success(toDto(combo), "Created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("Endpoint: PUT /combos/{}", id);
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found: " + id));
        if (body.get("name") != null) combo.setName((String) body.get("name"));
        if (body.get("description") != null) combo.setDescription((String) body.get("description"));
        if (body.get("price") != null) combo.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.get("active") != null) combo.setActive((Boolean) body.get("active"));
        if (body.containsKey("items")) {
            combo.getItems().clear();
            buildItems(combo, body);
        }
        comboRepository.save(combo);
        return ResponseEntity.ok(ApiResponse.success(toDto(combo), "Updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /combos/{}", id);
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found: " + id));
        combo.setDeleted(true);
        comboRepository.save(combo);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    /**
     * GET /combos/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=day|week|month&limit=10
     *
     * Returns:
     *  - summary   : { totalSold, totalRevenue, activeCount, avgOrderValue }
     *  - ranking   : [ { comboId, comboName, qtySold, revenue, orderCount } ]
     *  - trend     : [ { label, qtySold, revenue } ]
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analytics(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Endpoint: GET /combos/analytics from={} to={} granularity={}", from, to, granularity);

        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).atTime(23, 59, 59);

        // ── Summary ──────────────────────────────────────────────────────────
        List<Object[]> sumRows = orderItemRepository.getComboSummary(fromDt, toDt);
        long   totalSold      = 0;
        double totalRevenue   = 0;
        double avgOrderValue  = 0;
        if (!sumRows.isEmpty()) {
            Object[] r   = sumRows.get(0);
            totalSold    = r[0] != null ? ((Number) r[0]).longValue()   : 0;
            totalRevenue = r[1] != null ? ((Number) r[1]).doubleValue() : 0;
            avgOrderValue= r[3] != null ? ((Number) r[3]).doubleValue() : 0;
        }
        long activeCount = comboRepository.findByDeletedFalseAndActive(true).size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSold",     totalSold);
        summary.put("totalRevenue",  totalRevenue);
        summary.put("activeCount",   activeCount);
        summary.put("avgOrderValue", avgOrderValue);

        // ── Ranking ──────────────────────────────────────────────────────────
        List<Object[]> rankRows = orderItemRepository.getComboRanking(fromDt, toDt, Math.max(1, limit));
        List<Map<String, Object>> ranking = rankRows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("comboId",    r[0] != null ? r[0].toString() : null);
            item.put("comboName",  r[1] != null ? r[1].toString() : "");
            item.put("qtySold",    r[2] != null ? ((Number) r[2]).longValue()   : 0);
            item.put("revenue",    r[3] != null ? ((Number) r[3]).doubleValue() : 0);
            item.put("orderCount", r[4] != null ? ((Number) r[4]).longValue()   : 0);
            return item;
        }).collect(Collectors.toList());

        // ── Trend ────────────────────────────────────────────────────────────
        List<Object[]> trendRows = switch (granularity) {
            case "week"  -> orderItemRepository.getComboTrendByWeek(fromDt, toDt);
            case "month" -> orderItemRepository.getComboTrendByMonth(fromDt, toDt);
            default      -> orderItemRepository.getComboTrendByDay(fromDt, toDt);
        };
        List<Map<String, Object>> trend = trendRows.stream().map(r -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label",   r[0] != null ? r[0].toString() : "");
            point.put("qtySold", r[1] != null ? ((Number) r[1]).longValue()   : 0);
            point.put("revenue", r[2] != null ? ((Number) r[2]).doubleValue() : 0);
            return point;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("ranking", ranking);
        result.put("trend",   trend);

        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
    }
}
