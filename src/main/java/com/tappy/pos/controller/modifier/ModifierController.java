package com.tappy.pos.controller.modifier;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;
import com.tappy.pos.model.dto.modifier.SetProductModifiersRequest;
import com.tappy.pos.service.modifier.ModifierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class ModifierController {

    private final ModifierService modifierService;

    @GetMapping("/modifier-groups")
    public ResponseEntity<ApiResponse<List<ModifierGroupDTO>>> listGroups() {
        return ResponseEntity.ok(ApiResponse.success(modifierService.listGroups()));
    }

    @PostMapping("/modifier-groups")
    public ResponseEntity<ApiResponse<ModifierGroupDTO>> createGroup(@RequestBody @Valid SaveModifierGroupRequest req) {
        log.info("POST /modifier-groups - {}", req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(modifierService.createGroup(req)));
    }

    @PutMapping("/modifier-groups/{id}")
    public ResponseEntity<ApiResponse<ModifierGroupDTO>> updateGroup(
            @PathVariable Long id, @RequestBody @Valid SaveModifierGroupRequest req) {
        log.info("PUT /modifier-groups/{}", id);
        return ResponseEntity.ok(ApiResponse.success(modifierService.updateGroup(id, req)));
    }

    @DeleteMapping("/modifier-groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        log.info("DELETE /modifier-groups/{}", id);
        modifierService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/products/{productId}/modifier-groups")
    public ResponseEntity<ApiResponse<List<ModifierGroupDTO>>> getProductGroups(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(modifierService.getGroupsForProduct(productId)));
    }

    @PutMapping("/products/{productId}/modifier-groups")
    public ResponseEntity<ApiResponse<Void>> setProductGroups(
            @PathVariable Long productId, @RequestBody SetProductModifiersRequest req) {
        log.info("PUT /products/{}/modifier-groups - {} groups", productId, req.getGroupIds() != null ? req.getGroupIds().size() : 0);
        modifierService.setProductGroups(productId, req.getGroupIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
