package com.tappy.pos.model.entity.marketgold;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketGoldPriceId implements Serializable {
    private String ktype;
    private String source;
}
