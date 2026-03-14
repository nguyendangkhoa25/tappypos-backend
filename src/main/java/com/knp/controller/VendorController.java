package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.vendor.SaveVendorRequest;
import com.knp.model.dto.vendor.VendorDTO;
import com.knp.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VendorDTO>>> getAll(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAll(keyword, pageable), "Vendors retrieved"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getAllForSelect() {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAllForSelect(), "Vendors retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getById(id), "Vendor retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorDTO>> create(@Valid @RequestBody SaveVendorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(vendorService.create(req), "Vendor created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDTO>> update(@PathVariable Long id,
                                                          @Valid @RequestBody SaveVendorRequest req) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.update(id, req), "Vendor updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vendor deleted"));
    }
}
