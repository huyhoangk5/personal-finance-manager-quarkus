package com.finance.pfm.dto.request;

import jakarta.validation.constraints.NotBlank;

public class FacebookLoginRequest {
    @NotBlank(message = "Token Facebook không được để trống")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
