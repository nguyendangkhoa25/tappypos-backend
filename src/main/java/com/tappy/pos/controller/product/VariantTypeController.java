package com.tappy.pos.controller.product;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.variant.SaveVariantTypeRequest;
import com.tappy.pos.model.dto.variant.VariantTypeDTO;
import com.tappy.pos.service.product.VariantTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/variant-types")
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class VariantTypeController {

    private final VariantTypeService variantTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VariantTypeDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(variantTypeService.getAll()));
    }

    @GetMapping("/for-product-type/{productTypeId}")
    public ResponseEntity<ApiResponse<List<VariantTypeDTO>>> getForProductType(
            @PathVariable Long productTypeId) {
        return ResponseEntity.ok(ApiResponse.success(
                variantTypeService.getForProductType(productTypeId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VariantTypeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(variantTypeService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VariantTypeDTO>> create(
            @RequestBody @Valid SaveVariantTypeRequest req) {
        log.info("POST /variant-types - name={}", req.getName());
        VariantTypeDTO dto = variantTypeService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VariantTypeDTO>> update(
            @PathVariable Long id,
            @RequestBody @Valid SaveVariantTypeRequest req) {
        log.info("PUT /variant-types/{}", id);
        return ResponseEntity.ok(ApiResponse.success(variantTypeService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /variant-types/{}", id);
        variantTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
