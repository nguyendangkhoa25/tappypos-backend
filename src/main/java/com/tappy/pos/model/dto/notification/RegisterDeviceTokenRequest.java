package com.tappy.pos.model.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Mobile client registers its Expo push token on login / app start. */
@Data
public class RegisterDeviceTokenRequest {

    @NotBlank
    @Size(max = 255)
    private String expoPushToken;

    @Size(max = 10)
    private String platform; // 'ios' | 'android'
}
