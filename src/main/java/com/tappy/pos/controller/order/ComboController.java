package com.tappy.pos.controller.order;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.entity.order.Combo;
import com.tappy.pos.model.entity.order.ComboItem;
import com.tappy.pos.repository.order.ComboRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@RequiresFeature("POS")
public class ComboController {

    private final ComboRepository comboRepository;

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
}
