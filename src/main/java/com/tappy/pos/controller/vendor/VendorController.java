package com.tappy.pos.controller.vendor;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.vendor.SaveVendorRequest;
import com.tappy.pos.model.dto.vendor.VendorDTO;
import com.tappy.pos.service.vendor.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @RequiresFeature("VENDOR")
    public ResponseEntity<ApiResponse<Page<VendorDTO>>> getAll(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAll(keyword, pageable), "Vendors retrieved"));
    }

    // Open — used by POS brand picker and pawn form without requiring VENDOR feature
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getAllForSelect() {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAllForSelect(), "Vendors retrieved"));
    }

    @GetMapping("/{id}")
    @RequiresFeature("VENDOR")
    public ResponseEntity<ApiResponse<VendorDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getById(id), "Vendor retrieved"));
    }

    @PostMapping
    @RequiresFeature("VENDOR")
    public ResponseEntity<ApiResponse<VendorDTO>> create(@Valid @RequestBody SaveVendorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(vendorService.create(req), "Vendor created"));
    }

    @PutMapping("/{id}")
    @RequiresFeature("VENDOR")
    public ResponseEntity<ApiResponse<VendorDTO>> update(@PathVariable Long id,
                                                          @Valid @RequestBody SaveVendorRequest req) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.update(id, req), "Vendor updated"));
    }

    @DeleteMapping("/{id}")
    @RequiresFeature("VENDOR")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vendor deleted"));
    }
}
