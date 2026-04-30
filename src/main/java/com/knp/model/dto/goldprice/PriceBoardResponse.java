package com.knp.model.dto.goldprice;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceBoardResponse {
    private String shopName;
    private String shopAddress;
    private List<GoldPriceDTO> prices;
}
