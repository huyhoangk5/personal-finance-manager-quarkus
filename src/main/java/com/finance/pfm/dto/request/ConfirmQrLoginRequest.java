package com.finance.pfm.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConfirmQrLoginRequest {
    @NotBlank(message = "Token QR không được để trống")
    private String qrToken;

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
