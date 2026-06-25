package com.tappy.pos.repository.integration;

import com.tappy.pos.model.entity.integration.ZaloZnsCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZaloZnsCredentialRepository extends JpaRepository<ZaloZnsCredential, Long> {

    /**
     * Loads the single active platform credential (lowest id, ignoring soft-deleted rows),
     * taking a write lock so concurrent OTP sends do not refresh in parallel and invalidate
     * the rotating (single-use) refresh token. {@code findFirst} caps the result at one row,
     * so a stray second active row never breaks the read. Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ZaloZnsCredential> findFirstByDeletedFalseOrderByIdAsc();
}
