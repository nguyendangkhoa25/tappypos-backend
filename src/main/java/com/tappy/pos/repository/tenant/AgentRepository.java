package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    @Query(value = "SELECT * FROM agents WHERE deleted = false AND active = true AND " +
           "(CAST(:search AS text) IS NULL OR LOWER(name) LIKE LOWER('%' || CAST(:search AS text) || '%'))",
           nativeQuery = true)
    List<Agent> findAllActive(@Param("search") String search);

    boolean existsByName(String name);

    @Query("SELECT a FROM Agent a WHERE a.deleted = false AND a.userId = :userId")
    java.util.Optional<Agent> findByUserId(@Param("userId") Long userId);
}
