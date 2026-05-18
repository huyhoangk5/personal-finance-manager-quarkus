package com.finance.pfm.dto;

/**
 * DTO trả về sau khi đăng nhập thành công.
 * Chứa access token, refresh token và thông tin user.
 */
public class AuthResponse {
    public String accessToken;
    public String refreshToken;
    public long expiresIn; // seconds
    public UserDTO user;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
