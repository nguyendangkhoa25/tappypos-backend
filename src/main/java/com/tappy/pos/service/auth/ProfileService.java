package com.tappy.pos.service.auth;

import com.tappy.pos.model.dto.auth.ProfileRequest;
import com.tappy.pos.model.dto.auth.UserProfile;
import org.springframework.web.multipart.MultipartFile;

/**
 * ProfileService Interface
 * Defines contract for user profile management operations
 */
public interface ProfileService {
    /**
     * Get user profile by username
     */
    UserProfile getUserDetail(String username);

    /**
     * Update user profile color preference
     */
    UserProfile updateProfileColor(String username, ProfileRequest request);

    /**
     * Update user profile avatar
     */
    UserProfile updateProfileAvatar(String username, ProfileRequest request);

    /**
     * Update user profile password
     */
    UserProfile updateProfilePassword(String username, ProfileRequest request);

    /**
     * Update user basic info (fullName, email)
     */
    UserProfile updateProfileInfo(String username, ProfileRequest request);

    /**
     * Update user language preference
     */
    UserProfile updateProfileLang(String username, ProfileRequest request);

    /**
     * Upload a new avatar image for the user (multipart).
     * Resizes to 512×512 JPEG, stores in R2 under avatars/{userId}.jpg.
     */
    UserProfile uploadAvatar(String username, MultipartFile file);

    /**
     * Delete the current user's avatar from R2 and clear avatarUrl.
     */
    void deleteAvatar(String username);

    /**
     * Get user preferences JSON (autocomplete history, etc.)
     */
    String getPreferences(String username);

    /**
     * Save user preferences JSON
     */
    void updatePreferences(String username, String preferences);
}

