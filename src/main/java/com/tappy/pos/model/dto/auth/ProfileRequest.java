package com.tappy.pos.model.dto.auth;

import lombok.*;

/**
 * ProfileRequest DTO
 * Used for profile update operations (avatar, color, password)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {
    private String username;
    private String fullName;
    private String email;
    private String avatar;
    private String colorPreference;
    private String lang;
    private String oldPassword;
    private String newPassword;
    private String preferences;
}

