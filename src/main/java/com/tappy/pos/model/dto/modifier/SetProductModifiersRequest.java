package com.tappy.pos.model.dto.modifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Replace the set of modifier groups attached to a product (order preserved). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetProductModifiersRequest {
    private List<Long> groupIds;
}
