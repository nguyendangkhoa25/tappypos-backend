package com.tappy.pos.controller.recipe;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.recipe.IngredientConsumptionDTO;
import com.tappy.pos.model.dto.recipe.ProduceRequest;
import com.tappy.pos.model.dto.recipe.ProductionBatchDTO;
import com.tappy.pos.model.dto.recipe.ProductionSummaryDTO;
import com.tappy.pos.service.recipe.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/production")
@RequiredArgsConstructor
@RequiresFeature("RECIPE")
@Slf4j
public class ProductionController {

    private final ProductionService productionService;

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<ProductionBatchDTO>> produce(@Valid @RequestBody ProduceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productionService.produce(request), "Production run completed"));
    }

    @PostMapping("/runs/{id}/spoil")
    public ResponseEntity<ApiResponse<ProductionBatchDTO>> spoil(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productionService.markSpoiled(id), "Batch marked spoiled"));
    }

    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<Page<ProductionBatchDTO>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productionService.listBatches(from, to, PageRequest.of(page, size)), "Production runs retrieved"));
    }

    @GetMapping("/reports/consumption")
    public ResponseEntity<ApiResponse<List<IngredientConsumptionDTO>>> consumption(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(productionService.getConsumption(from, to), "Ingredient consumption retrieved"));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<ProductionSummaryDTO>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(productionService.getSummary(from, to), "Production summary retrieved"));
    }
}
