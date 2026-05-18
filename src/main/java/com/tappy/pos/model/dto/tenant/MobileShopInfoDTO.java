package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileShopInfoDTO {
    private String shopName;
    private String address;
    private String phone;
    private String description;
    private String logoUrl;
    private String shopTypeCode;
    private String posMode;
}
