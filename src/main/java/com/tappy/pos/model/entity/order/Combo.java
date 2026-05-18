package com.tappy.pos.model.entity.order;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Combo extends TenantAwareEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComboItem> items = new ArrayList<>();
}
