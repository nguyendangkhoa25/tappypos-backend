package com.tappy.pos.controller.order;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.service.order.ComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@RequiresFeature("POS")
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) Boolean active) {
        log.info("Endpoint: GET /combos active={}", active);
        return ResponseEntity.ok(ApiResponse.success(comboService.list(active), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> body) {
        log.info("Endpoint: POST /combos");
        return ResponseEntity.ok(ApiResponse.success(comboService.create(body), "Created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("Endpoint: PUT /combos/{}", id);
        return ResponseEntity.ok(ApiResponse.success(comboService.update(id, body), "Updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /combos/{}", id);
        comboService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analytics(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Endpoint: GET /combos/analytics from={} to={} granularity={}", from, to, granularity);
        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).atTime(23, 59, 59);
        return ResponseEntity.ok(ApiResponse.success(
                comboService.getAnalytics(fromDt, toDt, granularity, limit), "OK"));
    }
}
