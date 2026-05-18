package com.tappy.pos.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionsMatrixDTO {
    private List<FeatureInfo> features;
    private List<RolePermissions> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureInfo {
        private String name;
        private String displayName;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissions {
        private String roleName;
        private String description;
        private List<String> featureNames;
    }
}
