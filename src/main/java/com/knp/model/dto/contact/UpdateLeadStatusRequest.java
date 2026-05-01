package com.knp.model.dto.contact;

import com.knp.model.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLeadStatusRequest {

    @NotNull
    private LeadStatus status;

    private String adminNote;
}
