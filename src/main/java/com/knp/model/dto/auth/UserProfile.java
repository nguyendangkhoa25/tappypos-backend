package com.knp.model.dto.auth;

import lombok.*;

import java.util.Set;

/**
 * UserProfile DTO
 * Used for profile retrieval and update responses
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private String colorPreference;
    private String lang;
    private Boolean active;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Boolean accountNonExpired;
    private String requireAction;
    private String notes;
    private Set<String> roles;
}

