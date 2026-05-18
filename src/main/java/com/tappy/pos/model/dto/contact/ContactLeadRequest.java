package com.tappy.pos.model.dto.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContactLeadRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Invalid phone number")
    private String phone;

    @Size(max = 50)
    private String shopType;

    @Size(max = 1000)
    private String note;
}
