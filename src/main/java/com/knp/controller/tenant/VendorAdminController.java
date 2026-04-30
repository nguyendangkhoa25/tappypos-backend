package com.knp.controller.tenant;

import com.knp.annotation.MasterDatabaseOnly;
import com.knp.annotation.RequiresFeature;
import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.tenant.SaveVendorRequest;
import com.knp.model.dto.tenant.VendorDTO;
import com.knp.service.tenant.VendorAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor-admins")
@RequiredArgsConstructor
@MasterDatabaseOnly
@RequiresFeature("VENDOR_MGMT")
public class VendorAdminController {

    private final VendorAdminService vendorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getAll(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAll(search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorDTO>> create(@RequestBody SaveVendorRequest request) {
        VendorDTO created = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDTO>> update(
            @PathVariable Long id,
            @RequestBody SaveVendorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
