package com.knp.service.invoice.sinvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SAccessTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("iat")
    private long issuedAt;

    @JsonProperty("invoice_cluster")
    private String invoiceCluster;

    @JsonProperty("type")
    private int type;

    @JsonProperty("jti")
    private String jwtId;
}
