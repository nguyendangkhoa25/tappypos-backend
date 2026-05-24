package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.ShopInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShopInvitationRepository extends JpaRepository<ShopInvitation, Long> {

    /** Find an unused, non-expired invitation by code. */
    @Query("SELECT i FROM ShopInvitation i WHERE i.code = :code AND i.usedAt IS NULL AND i.expiresAt > :now")
    Optional<ShopInvitation> findValidByCode(String code, LocalDateTime now);

    /** Check if a code already exists (for collision avoidance during generation). */
    boolean existsByCode(String code);
}
