package com.tappy.pos.repository.notification;

import com.tappy.pos.model.entity.notification.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserId(String userId);

    List<NotificationPreference> findByUserIdIn(List<String> userIds);
}
