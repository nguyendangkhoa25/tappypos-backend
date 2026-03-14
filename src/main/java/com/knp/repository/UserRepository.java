package com.knp.repository;

import com.knp.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);


    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);


    /**
     * Get all users with pagination, search, and filtering
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "u.deleted <> true " +
            "ORDER BY u.id DESC")
    Page<User> findAllWithSearch(
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Get all active users
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.deletedAt IS NULL ORDER BY u.id DESC")
    Page<User> findAllActiveUsers(Pageable pageable);

    /**
     * Find active user by username
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.active = true AND u.deletedAt IS NULL")
    Optional<User> findByUsernameActive(@Param("username") String username);

    /**
     * Get all active usernames — used for broadcast notifications
     */
    @Query("SELECT u.username FROM User u WHERE u.active = true AND u.deletedAt IS NULL")
    java.util.List<String> findAllActiveUsernames();

    /**
     * Get active usernames by role name — used for targeted notifications (e.g. SHOP_OWNER)
     */
    @Query("SELECT u.username FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.active = true AND u.deletedAt IS NULL")
    java.util.List<String> findUsernamesByRole(@Param("roleName") String roleName);
}

