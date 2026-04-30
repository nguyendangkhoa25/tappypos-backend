package com.knp.controller.auth;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.auth.PermissionsMatrixDTO;
import com.knp.model.dto.auth.RoleDTO;
import com.knp.service.auth.RoleFeatureService;
import com.knp.service.auth.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("USER")
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


