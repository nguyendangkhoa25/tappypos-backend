package com.tappy.pos.model.dto.modifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A modifier option chosen for a cart/order line, persisted as JSON on the line. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChosenModifierDTO {
    private String groupName;
    private String optionName;
    private BigDecimal priceDelta;
}
