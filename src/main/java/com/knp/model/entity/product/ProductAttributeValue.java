package com.knp.model.entity.product;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_attribute_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductAttributeValue extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeDefinition attribute;

    @Column(length = 1000)
    private String valueString;

    @Column(precision = 15, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private LocalDate valueDate;

    public Object getValue() {
        return switch (attribute.getDataType()) {
            case STRING -> valueString;
            case TEXT -> valueString;
            case NUMBER -> valueNumber;
            case BOOLEAN -> valueBoolean;
            case DATE -> valueDate;
        };
    }

    public void setValue(Object value) {
        if (value == null) {
            switch (attribute.getDataType()) {
                case STRING, TEXT -> this.valueString = null;
                case NUMBER -> this.valueNumber = null;
                case BOOLEAN -> this.valueBoolean = null;
                case DATE -> this.valueDate = null;
            }
            return;
        }

        switch (attribute.getDataType()) {
            case STRING, TEXT -> {
                if (value instanceof String str) {
                    this.valueString = str;
                } else {
                    this.valueString = value.toString();
                }
            }
            case NUMBER -> {
                if (value instanceof BigDecimal bd) {
                    this.valueNumber = bd;
                } else if (value instanceof Integer i) {
                    this.valueNumber = new BigDecimal(i);
                } else if (value instanceof Long l) {
                    this.valueNumber = new BigDecimal(l);
                } else if (value instanceof Double d) {
                    this.valueNumber = BigDecimal.valueOf(d);
                } else if (value instanceof Float f) {
                    this.valueNumber = BigDecimal.valueOf(f);
                } else {
                    try {
                        this.valueNumber = new BigDecimal(value.toString());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cannot convert '" + value + "' to BigDecimal for attribute: " + attribute.getCode(), e);
                    }
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b) {
                    this.valueBoolean = b;
                } else if (value instanceof String str) {
                    this.valueBoolean = Boolean.parseBoolean(str);
                } else {
                    this.valueBoolean = (Boolean) value;
                }
            }
            case DATE -> {
                if (value instanceof LocalDate ld) {
                    this.valueDate = ld;
                } else if (value instanceof String str) {
                    this.valueDate = LocalDate.parse(str);
                } else {
                    throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to LocalDate");
                }
            }
        }
    }
}


