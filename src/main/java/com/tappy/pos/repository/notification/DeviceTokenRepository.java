package com.tappy.pos.repository.notification;

import com.tappy.pos.model.entity.notification.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByExpoPushTokenAndDeletedFalse(String expoPushToken);

    /** Active push tokens for the given usernames (RLS-scoped to the current tenant). */
    @Query("SELECT d.expoPushToken FROM DeviceToken d WHERE d.userId IN :usernames AND d.deleted = false")
    List<String> findActiveTokensByUserIds(@Param("usernames") Collection<String> usernames);
}
