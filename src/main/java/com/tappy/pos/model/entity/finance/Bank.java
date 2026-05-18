package com.tappy.pos.model.entity.finance;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.BaseEntity;
import lombok.*;

@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 10)
    private String bin;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 999;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
