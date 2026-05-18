package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.RefreshToken;
import com.tappy.pos.model.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RefreshTokenRepository - Database access for RefreshToken entities
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.active = true")
    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.active = true and rt.expiryDate > :expiryDate")
    List<RefreshToken> findAllByUserAndActive(User user, long expiryDate);

    /**
     * Find all active refresh tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.active = true")
    List<RefreshToken> findByUser(User user);

    /**
     * Delete all refresh tokens for a user (logout all devices)
     */
    void deleteByUser(User user);
}

