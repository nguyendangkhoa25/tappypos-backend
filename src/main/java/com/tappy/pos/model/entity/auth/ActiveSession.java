package com.tappy.pos.model.entity.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "active_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId OR tenant_id IS NULL")
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "last_active", nullable = false)
    private LocalDateTime lastActive;
}
