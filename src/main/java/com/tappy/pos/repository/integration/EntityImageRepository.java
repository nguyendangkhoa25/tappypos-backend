package com.tappy.pos.repository.integration;

import com.tappy.pos.model.entity.integration.EntityImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityImageRepository extends JpaRepository<EntityImage, Long> {

    List<EntityImage> findByEntityTypeAndEntityIdAndDeletedFalseOrderByUploadedAtAsc(
            String entityType, Long entityId);

    Optional<EntityImage> findByIdAndDeletedFalse(Long id);
}
