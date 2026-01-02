package com.barbershop.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_other_costs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueCost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_id", nullable = false)
    private Revenue revenue;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal amount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean deleted = false;
}

