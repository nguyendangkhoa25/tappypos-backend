package com.tappy.pos.controller.vehicle;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.vehicle.CreateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.SellVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.UpdateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.VehicleUnitDTO;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import com.tappy.pos.service.vehicle.VehicleUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Per-unit vehicle registry (chiếc xe theo số khung / số máy). Gated by PRODUCT — a vehicle unit
 * is a sub-capability of the product catalog, not a separate feature. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4b.
 */
@RestController
@RequestMapping("/vehicle-units")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PRODUCT")
public class VehicleUnitController {

    private final VehicleUnitService vehicleUnitService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleUnitDTO>> create(@Valid @RequestBody CreateVehicleUnitRequest request) {
        log.info("Request: Register vehicle unit for product {}", request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(vehicleUnitService.create(request), "Vehicle unit created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleUnitDTO>>> search(
            @RequestParam(required = false) VehicleUnitStatus status,
            @RequestParam(required = false) Long productId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                vehicleUnitService.search(status, productId, pageable), "OK"));
    }

    /** Tra cứu xe theo số khung / số máy / biển số (+ bảo hành). */
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<List<VehicleUnitDTO>>> lookup(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(vehicleUnitService.lookup(keyword), "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleUnitDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehicleUnitService.getById(id), "OK"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleUnitDTO>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateVehicleUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(vehicleUnitService.update(id, request), "Vehicle unit updated"));
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<ApiResponse<VehicleUnitDTO>> markSold(
            @PathVariable Long id, @Valid @RequestBody SellVehicleUnitRequest request) {
        log.info("Request: Mark vehicle unit {} sold", id);
        return ResponseEntity.ok(ApiResponse.success(vehicleUnitService.markSold(id, request), "Vehicle unit sold"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleUnitService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vehicle unit deleted"));
    }
}
