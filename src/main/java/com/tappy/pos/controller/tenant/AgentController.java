package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.MasterDatabaseOnly;
import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.SaveVendorRequest;
import com.tappy.pos.model.dto.tenant.VendorDTO;
import com.tappy.pos.service.tenant.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor-admins")
@RequiredArgsConstructor
@MasterDatabaseOnly
@RequiresFeature("AGENT_MGMT")
public class AgentController {

    private final AgentService vendorService;

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
