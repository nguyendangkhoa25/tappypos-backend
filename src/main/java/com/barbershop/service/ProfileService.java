package com.barbershop.service;

import com.barbershop.model.dto.ProfileRequest;
import com.barbershop.model.dto.UserProfile;

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
}

