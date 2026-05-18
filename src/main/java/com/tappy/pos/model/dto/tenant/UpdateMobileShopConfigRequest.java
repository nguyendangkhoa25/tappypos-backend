package com.tappy.pos.model.dto.tenant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMobileShopConfigRequest {
    private String shopName;
    private String address;
    private String phone;
    private String description;
}
