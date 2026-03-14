package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.PermissionsMatrixDTO;
import com.knp.model.dto.RoleDTO;
import com.knp.service.RoleFeatureService;
import com.knp.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;
    private final RoleFeatureService roleFeatureService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        log.info("Endpoint: GET /roles");
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles(), "Roles retrieved successfully"));
    }

    @GetMapping("/permissions-matrix")
    public ResponseEntity<ApiResponse<PermissionsMatrixDTO>> getPermissionsMatrix() {
        log.info("Endpoint: GET /roles/permissions-matrix");
        return ResponseEntity.ok(ApiResponse.success(roleFeatureService.getPermissionsMatrix(), "Permissions matrix retrieved successfully"));
    }

    @GetMapping("/{roleName}/features")
    public ResponseEntity<ApiResponse<List<String>>> getRoleFeatures(@PathVariable String roleName) {
        log.info("Endpoint: GET /roles/{}/features", roleName);
        List<String> features = roleFeatureService.getActiveFeatureNamesByRoleName(roleName);
        return ResponseEntity.ok(ApiResponse.success(features, "Role features retrieved successfully"));
    }

    @PutMapping("/{roleName}/features")
    public ResponseEntity<ApiResponse<Void>> setRoleFeatures(
            @PathVariable String roleName,
            @RequestBody List<String> featureNames) {
        log.info("Endpoint: PUT /roles/{}/features - features: {}", roleName, featureNames);
        roleFeatureService.setRoleFeatures(roleName, featureNames);
        return ResponseEntity.ok(ApiResponse.success(null, "Role features updated successfully"));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByCode(@PathVariable String code) {
        log.info("Endpoint: GET /roles/{}", code);
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleByCode(code), "Role retrieved successfully"));
    }
}


