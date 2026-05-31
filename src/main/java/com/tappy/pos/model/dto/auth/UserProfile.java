package com.tappy.pos.model.dto.auth;

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
    private String nickname;
    private String avatar;
    /** R2 public URL for the user's avatar (preferred over legacy base64 `avatar`). */
    private String avatarUrl;
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

