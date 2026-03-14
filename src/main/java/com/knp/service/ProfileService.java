package com.knp.service;

import com.knp.model.dto.ProfileRequest;
import com.knp.model.dto.UserProfile;

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
}

