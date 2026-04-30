package com.knp.repository.auth;

import com.knp.model.entity.auth.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    Optional<ActiveSession> findByUsername(String username);

    void deleteByUsername(String username);
}
