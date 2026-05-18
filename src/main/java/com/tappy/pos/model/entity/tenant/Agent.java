package com.tappy.pos.model.entity.tenant;

import com.tappy.pos.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity(name = "Agent")
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "user_id")
    private Long userId;
}
